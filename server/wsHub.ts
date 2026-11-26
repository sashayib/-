import type { IncomingMessage, Server as HttpServer } from 'node:http';
import { WebSocket, WebSocketServer } from 'ws';
import type { PrivatePlayerHand, PublicRoomState } from '../shared/balootTypes.ts';
import {
  type ClientMessage,
  MAX_WS_PAYLOAD_BYTES,
  parseClientMessage,
  PROTOCOL_VERSION,
  type ServerMessage,
} from '../shared/protocol.ts';
import { extractBearerToken } from './authMiddleware.ts';
import { type VerifiedUser, verifyFirebaseIdToken } from './firebaseAdmin.ts';
import {
  createAuthoritativeRoom,
  getMemberRoomSnapshot,
  getUserActiveSession,
  joinAuthoritativeRoom,
  leaveAuthoritativeRoom,
  RoomStoreError,
  startAuthoritativeMatch,
  updateSeatConnectionStatus,
} from './roomStore.ts';

interface AuthedSocket extends WebSocket {
  user?: VerifiedUser;
  roomCode?: string;
  isAlive?: boolean;
  msgWindowStart?: number;
  msgCountInWindow?: number;
}

const AUTH_HANDSHAKE_TIMEOUT_MS = 8_000;
const HEARTBEAT_INTERVAL_MS = 25_000;
const RATE_LIMIT_WINDOW_MS = 5_000;
const RATE_LIMIT_MAX_MSGS = 30;

export interface WsHubHandle {
  wss: WebSocketServer;
  broadcastPublicRoom: (room: PublicRoomState) => void;
  sendPrivateHands: (roomCode: string, hands: Record<string, PrivatePlayerHand>) => void;
  getConnectionStats: () => { connectedSockets: number; activeRooms: number };
  close: () => Promise<void>;
}

function sendJson(ws: WebSocket, payload: ServerMessage): void {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(payload));
  }
}

export function createWsHub(httpServer: HttpServer, allowedOrigins?: Set<string>): WsHubHandle {
  const wss = new WebSocketServer({
    server: httpServer,
    path: '/ws',
    maxPayload: MAX_WS_PAYLOAD_BYTES,
    verifyClient: allowedOrigins
      ? (info, cb) => {
          const origin = info.origin;
          const host = info.req.headers.host;
          const isSameOrigin =
            Boolean(host) && (origin === `https://${host}` || origin === `http://${host}`);
          if (!origin || isSameOrigin || allowedOrigins.has(origin)) {
            cb(true);
            return;
          }
          cb(false, 403, 'Origin not allowed');
        }
      : undefined,
  });

  // In-memory routing index ONLY (all durable room, hand, and timer state lives in Firestore)
  const roomSockets = new Map<string, Set<AuthedSocket>>();
  const userSockets = new Map<string, Set<AuthedSocket>>();

  function trackUserSocket(uid: string, ws: AuthedSocket): void {
    let set = userSockets.get(uid);
    if (!set) {
      set = new Set();
      userSockets.set(uid, set);
    }
    set.add(ws);
  }

  function untrackUserSocket(uid: string, ws: AuthedSocket): void {
    const set = userSockets.get(uid);
    if (!set) return;
    set.delete(ws);
    if (set.size === 0) userSockets.delete(uid);
  }

  function subscribeSocketToRoom(ws: AuthedSocket, roomCode: string): void {
    if (ws.roomCode && ws.roomCode !== roomCode) {
      unsubscribeSocketFromRoom(ws);
    }
    ws.roomCode = roomCode;
    let set = roomSockets.get(roomCode);
    if (!set) {
      set = new Set();
      roomSockets.set(roomCode, set);
    }
    set.add(ws);
  }

  function unsubscribeSocketFromRoom(ws: AuthedSocket): void {
    const code = ws.roomCode;
    if (!code) return;
    const set = roomSockets.get(code);
    if (set) {
      set.delete(ws);
      if (set.size === 0) roomSockets.delete(code);
    }
    ws.roomCode = undefined;
  }

  function broadcastPublicRoom(room: PublicRoomState): void {
    const sockets = roomSockets.get(room.roomCode);
    if (!sockets) return;
    const msg: ServerMessage = {
      type: 'room:state',
      room,
      serverTimeMs: Date.now(),
    };
    const serialized = JSON.stringify(msg);
    for (const client of sockets) {
      if (client.readyState === WebSocket.OPEN && client.user && room.memberUids.includes(client.user.uid)) {
        client.send(serialized);
      }
    }
  }

  function broadcastRoomEvent(
    roomCode: string,
    payload: Omit<Extract<ServerMessage, { type: 'room:event' }>, 'type' | 'roomCode' | 'serverTimeMs'>,
  ): void {
    const sockets = roomSockets.get(roomCode);
    if (!sockets) return;
    const msg: ServerMessage = {
      type: 'room:event',
      roomCode,
      ...payload,
      serverTimeMs: Date.now(),
    };
    const serialized = JSON.stringify(msg);
    for (const client of sockets) {
      if (client.readyState === WebSocket.OPEN && client.user) {
        client.send(serialized);
      }
    }
  }

  /**
   * SECURITY INVARIANT: Each player's private hand is delivered EXCLUSIVELY
   * to sockets authenticated with that exact Firebase Auth UID.
   */
  function sendPrivateHands(roomCode: string, hands: Record<string, PrivatePlayerHand>): void {
    const nowMs = Date.now();
    for (const [uid, hand] of Object.entries(hands)) {
      const sockets = userSockets.get(uid);
      if (!sockets) continue;
      const msg: ServerMessage = {
        type: 'hand:private',
        hand,
        serverTimeMs: nowMs,
      };
      const serialized = JSON.stringify(msg);
      for (const client of sockets) {
        if (client.readyState === WebSocket.OPEN && client.roomCode === roomCode) {
          client.send(serialized);
        }
      }
    }
  }

  async function completeAuthentication(ws: AuthedSocket, token: string): Promise<boolean> {
    try {
      const user = await verifyFirebaseIdToken(token);
      ws.user = user;
      trackUserSocket(user.uid, ws);

      let activeSession = null;
      try {
        activeSession = await getUserActiveSession(user.uid);
      } catch {
        // Non-fatal if session lookup fails transiently
      }

      sendJson(ws, {
        type: 'auth:ok',
        uid: user.uid,
        displayName: user.displayName,
        protocolVersion: PROTOCOL_VERSION,
        serverTimeMs: Date.now(),
        activeSession,
      });
      return true;
    } catch (err) {
      const isUnconfigured = err instanceof Error && err.message === 'FIREBASE_ADMIN_UNCONFIGURED';
      sendJson(ws, {
        type: 'auth:error',
        code: isUnconfigured ? 'SERVER_NOT_CONFIGURED' : 'AUTH_FAILED',
        message: isUnconfigured
          ? 'Firebase Admin credentials are not configured on the server.'
          : 'Invalid or expired Firebase Authentication ID Token.',
      });
      ws.close(4001, 'Authentication failed');
      return false;
    }
  }

  async function handleAuthedMessage(ws: AuthedSocket, msg: ClientMessage): Promise<void> {
    const user = ws.user;
    if (!user) return;

    try {
      switch (msg.type) {
        case 'ping': {
          sendJson(ws, {
            type: 'pong',
            clientTimeMs: msg.clientTimeMs,
            serverTimeMs: Date.now(),
          });
          return;
        }

        case 'room:create': {
          const room = await createAuthoritativeRoom({
            user,
            roomType: msg.roomType,
            fillWithBots: msg.fillWithBots,
            actionId: msg.actionId,
          });
          subscribeSocketToRoom(ws, room.roomCode);
          sendJson(ws, {
            type: 'action:ack',
            actionId: msg.actionId,
            roomCode: room.roomCode,
            moveSeq: room.moveSeq,
          });
          sendJson(ws, { type: 'room:state', room, serverTimeMs: Date.now() });
          return;
        }

        case 'room:join': {
          const res = await joinAuthoritativeRoom({
            user,
            roomCode: msg.roomCode,
            actionId: msg.actionId,
          });
          subscribeSocketToRoom(ws, res.room.roomCode);
          sendJson(ws, {
            type: 'action:ack',
            actionId: msg.actionId,
            roomCode: res.room.roomCode,
            moveSeq: res.room.moveSeq,
          });
          broadcastPublicRoom(res.room);
          if (res.hand) {
            sendJson(ws, { type: 'hand:private', hand: res.hand, serverTimeMs: Date.now() });
          }
          broadcastRoomEvent(res.room.roomCode, {
            event: res.reconnected ? 'player_reconnected' : 'player_joined',
            seatIndex: res.seatIndex,
            uid: user.uid,
            displayName: res.room.seats[res.seatIndex]?.name || user.displayName,
          });
          return;
        }

        case 'room:leave': {
          const res = await leaveAuthoritativeRoom({
            user,
            roomCode: msg.roomCode,
            actionId: msg.actionId,
          });
          unsubscribeSocketFromRoom(ws);
          sendJson(ws, {
            type: 'action:ack',
            actionId: msg.actionId,
            roomCode: msg.roomCode,
            moveSeq: res.room?.moveSeq,
          });
          if (res.room) {
            broadcastPublicRoom(res.room);
            broadcastRoomEvent(msg.roomCode, {
              event: res.room.status === 'playing' ? 'player_standby' : 'player_left',
              uid: user.uid,
              displayName: user.displayName,
            });
          }
          return;
        }

        case 'room:resume': {
          const session = await getUserActiveSession(user.uid);
          const targetCode = msg.roomCode || session?.activeRoomCode;
          if (!targetCode) {
            sendJson(ws, {
              type: 'action:error',
              code: 'ROOM_NOT_FOUND',
              message: 'لا توجد جلسة نشطة لاستعادتها.',
            });
            return;
          }
          const snapshot = await getMemberRoomSnapshot({ roomCode: targetCode, uid: user.uid });
          subscribeSocketToRoom(ws, snapshot.room.roomCode);
          const updatedRoom =
            (await updateSeatConnectionStatus({
              roomCode: snapshot.room.roomCode,
              uid: user.uid,
              status: 'connected',
            })) || snapshot.room;

          sendJson(ws, { type: 'room:state', room: updatedRoom, serverTimeMs: Date.now() });
          if (snapshot.hand) {
            sendJson(ws, { type: 'hand:private', hand: snapshot.hand, serverTimeMs: Date.now() });
          }
          broadcastPublicRoom(updatedRoom);
          broadcastRoomEvent(updatedRoom.roomCode, {
            event: 'player_reconnected',
            seatIndex: snapshot.seatIndex,
            uid: user.uid,
            displayName: updatedRoom.seats[snapshot.seatIndex]?.name || user.displayName,
          });
          return;
        }

        case 'room:sync': {
          const snapshot = await getMemberRoomSnapshot({ roomCode: msg.roomCode, uid: user.uid });
          subscribeSocketToRoom(ws, snapshot.room.roomCode);
          sendJson(ws, { type: 'room:state', room: snapshot.room, serverTimeMs: Date.now() });
          if (snapshot.hand) {
            sendJson(ws, { type: 'hand:private', hand: snapshot.hand, serverTimeMs: Date.now() });
          }
          return;
        }

        case 'game:start': {
          const started = await startAuthoritativeMatch({
            user,
            roomCode: msg.roomCode,
            expectedMoveSeq: msg.expectedMoveSeq,
            actionId: msg.actionId,
          });
          sendJson(ws, {
            type: 'action:ack',
            actionId: msg.actionId,
            roomCode: msg.roomCode,
            moveSeq: started.publicRoom.moveSeq,
            duplicate: started.duplicate,
          });
          if (!started.duplicate) {
            broadcastPublicRoom(started.publicRoom);
            sendPrivateHands(msg.roomCode, started.privateHands);
          }
          return;
        }

        case 'reaction:send': {
          const snapshot = await getMemberRoomSnapshot({ roomCode: msg.roomCode, uid: user.uid });
          broadcastRoomEvent(msg.roomCode, {
            event: 'reaction',
            seatIndex: snapshot.seatIndex,
            uid: user.uid,
            displayName: snapshot.room.seats[snapshot.seatIndex]?.name || user.displayName,
            text: msg.emoji,
          });
          return;
        }

        case 'chat:send': {
          const snapshot = await getMemberRoomSnapshot({ roomCode: msg.roomCode, uid: user.uid });
          broadcastRoomEvent(msg.roomCode, {
            event: 'chat',
            seatIndex: snapshot.seatIndex,
            uid: user.uid,
            displayName: snapshot.room.seats[snapshot.seatIndex]?.name || user.displayName,
            text: msg.text,
          });
          sendJson(ws, { type: 'action:ack', actionId: msg.actionId, roomCode: msg.roomCode });
          return;
        }

        // Phase 2 authoritative turn execution commands (protocol locked in Phase 1)
        case 'game:bid':
        case 'game:double':
        case 'game:declare_projects':
        case 'game:play_card':
        case 'game:next_round':
        case 'game:claim_timeout':
        case 'game:resume_control': {
          sendJson(ws, {
            type: 'action:error',
            actionId: msg.actionId,
            code: 'INVALID_PHASE',
            message: `Action "${msg.type}" is reserved for Phase 2 turn execution engine.`,
          });
          return;
        }

        case 'auth':
          return;
      }
    } catch (err) {
      const actionId = 'actionId' in msg ? msg.actionId : undefined;
      if (err instanceof RoomStoreError) {
        sendJson(ws, {
          type: 'action:error',
          actionId,
          code: err.code,
          message: err.message,
          currentMoveSeq: err.currentMoveSeq,
        });
        return;
      }
      if (err instanceof Error && err.message === 'FIREBASE_ADMIN_UNCONFIGURED') {
        sendJson(ws, {
          type: 'action:error',
          actionId,
          code: 'SERVER_NOT_CONFIGURED',
          message: 'Firebase Admin SDK is not configured on this server.',
        });
        return;
      }
      sendJson(ws, {
        type: 'action:error',
        actionId,
        code: 'INTERNAL_ERROR',
        message: 'حدث خطأ داخلي في الخادم أثناء معالجة الطلب.',
      });
    }
  }

  wss.on('connection', (rawWs: WebSocket, req: IncomingMessage) => {
    const ws = rawWs as AuthedSocket;
    ws.isAlive = true;
    ws.msgWindowStart = Date.now();
    ws.msgCountInWindow = 0;

    ws.on('pong', () => {
      ws.isAlive = true;
    });

    // Support immediate Bearer token authentication on WebSocket upgrade header (for Android/iOS/Server clients)
    const headerToken = extractBearerToken(req.headers.authorization);
    let authTimeout: ReturnType<typeof setTimeout> | null = null;

    if (headerToken) {
      void completeAuthentication(ws, headerToken);
    } else {
      authTimeout = setTimeout(() => {
        if (!ws.user && ws.readyState === WebSocket.OPEN) {
          sendJson(ws, {
            type: 'auth:error',
            code: 'UNAUTHENTICATED',
            message: 'Authentication handshake timed out.',
          });
          ws.close(4001, 'Auth timeout');
        }
      }, AUTH_HANDSHAKE_TIMEOUT_MS);
    }

    ws.on('message', (data) => {
      const now = Date.now();
      if (!ws.msgWindowStart || now - ws.msgWindowStart > RATE_LIMIT_WINDOW_MS) {
        ws.msgWindowStart = now;
        ws.msgCountInWindow = 0;
      }
      ws.msgCountInWindow = (ws.msgCountInWindow || 0) + 1;
      if (ws.msgCountInWindow > RATE_LIMIT_MAX_MSGS) {
        sendJson(ws, {
          type: 'action:error',
          code: 'RATE_LIMITED',
          message: 'تم تجاوز الحد المسموح للرسائل، يرجى الانتظار لحظات.',
        });
        return;
      }

      const text = typeof data === 'string' ? data : data.toString('utf-8');
      const parsed = parseClientMessage(text);
      if (!parsed.ok) {
        sendJson(ws, {
          type: 'action:error',
          code: 'INVALID_MESSAGE',
          message: parsed.reason,
        });
        return;
      }

      if (!ws.user) {
        if (parsed.msg.type !== 'auth') {
          sendJson(ws, {
            type: 'auth:error',
            code: 'UNAUTHENTICATED',
            message: 'Must authenticate with a valid Firebase ID Token before sending commands.',
          });
          return;
        }
        if (authTimeout) {
          clearTimeout(authTimeout);
          authTimeout = null;
        }
        void completeAuthentication(ws, parsed.msg.token);
        return;
      }

      void handleAuthedMessage(ws, parsed.msg);
    });

    ws.on('close', () => {
      if (authTimeout) clearTimeout(authTimeout);
      const uid = ws.user?.uid;
      const roomCode = ws.roomCode;
      unsubscribeSocketFromRoom(ws);
      if (uid) {
        untrackUserSocket(uid, ws);
        // Only mark seat disconnected in Firestore if user has no other open socket
        const remainingSockets = userSockets.get(uid);
        if (roomCode && (!remainingSockets || remainingSockets.size === 0)) {
          void updateSeatConnectionStatus({ roomCode, uid, status: 'disconnected' })
            .then((updatedRoom) => {
              if (updatedRoom) {
                broadcastPublicRoom(updatedRoom);
                broadcastRoomEvent(roomCode, {
                  event: 'player_disconnected',
                  uid,
                  displayName: ws.user?.displayName,
                });
              }
            })
            .catch(() => {
              // Non-fatal disconnect update
            });
        }
      }
    });
  });

  // Periodic WebSocket liveness ping
  const heartbeat = setInterval(() => {
    for (const client of wss.clients) {
      const authed = client as AuthedSocket;
      if (authed.isAlive === false) {
        authed.terminate();
        continue;
      }
      authed.isAlive = false;
      authed.ping();
    }
  }, HEARTBEAT_INTERVAL_MS);

  wss.on('close', () => {
    clearInterval(heartbeat);
  });

  return {
    wss,
    broadcastPublicRoom,
    sendPrivateHands,
    getConnectionStats: () => ({
      connectedSockets: wss.clients.size,
      activeRooms: roomSockets.size,
    }),
    close: () =>
      new Promise<void>((resolve) => {
        clearInterval(heartbeat);
        wss.close(() => resolve());
      }),
  };
}
