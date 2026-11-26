import { randomInt, randomUUID } from 'node:crypto';
import type { DocumentReference, Firestore } from 'firebase-admin/firestore';
import type {
  BalootCard,
  BalootSeat,
  BalootTableState,
  PrivatePlayerHand,
  PublicRoomState,
  RoomType,
  SeatConnectionStatus,
  ServerEngineSecretDoc,
  UserActiveSessionDoc,
} from '../shared/balootTypes.ts';
import {
  BOT_NAMES,
  cardCode,
  dealNewRoundWithDeck,
  detectDeclarableProjectsForHand,
  encodeTableCard,
  generateRoomCode,
  getLegalCards,
  parseCardCode,
  type RandomFn,
} from '../shared/balootEngine.ts';
import { type ProtocolErrorCode, TURN_DURATION_MS } from '../shared/protocol.ts';
import { requireAdminFirestore, type VerifiedUser } from './firebaseAdmin.ts';

/**
 * Configurable Firestore collection name for authoritative multiplayer rooms.
 * Defaults to `mp_rooms` during Phase 1 so the existing web app's `rooms` collection
 * and live sessions remain 100% untouched until frontend integration.
 */
export const MP_ROOMS_COLLECTION = (process.env.MP_ROOMS_COLLECTION || 'mp_rooms').trim() || 'mp_rooms';
const MAX_PROCESSED_ACTIONS = 64;

export class RoomStoreError extends Error {
  readonly code: ProtocolErrorCode;
  readonly currentMoveSeq?: number;

  constructor(code: ProtocolErrorCode, message: string, currentMoveSeq?: number) {
    super(message);
    this.name = 'RoomStoreError';
    this.code = code;
    this.currentMoveSeq = currentMoveSeq;
  }
}

/** Cryptographically secure [0, 1) RNG using Node crypto.randomInt */
export const secureRandom: RandomFn = () => randomInt(0, 1_000_000_000) / 1_000_000_000;

function roomDocRef(db: Firestore, roomCode: string): DocumentReference {
  return db.collection(MP_ROOMS_COLLECTION).doc(roomCode);
}

function playerHandRef(db: Firestore, roomCode: string, uid: string): DocumentReference {
  return roomDocRef(db, roomCode).collection('playerHands').doc(uid);
}

function serverEngineRef(db: Firestore, roomCode: string): DocumentReference {
  return roomDocRef(db, roomCode).collection('serverState').doc('engine');
}

function userActiveSessionRef(db: Firestore, uid: string): DocumentReference {
  return db.collection('users').doc(uid).collection('session').doc('active');
}

async function resolvePlayerIdentity(db: Firestore, user: VerifiedUser): Promise<{
  name: string;
  rating: number;
  avatarUrl: string | null;
}> {
  try {
    const userSnap = await db.collection('users').doc(user.uid).get();
    if (userSnap.exists) {
      const d = userSnap.data() || {};
      return {
        name: String(d.displayName || user.displayName || 'لاعب شدها').slice(0, 40),
        rating: Number.isFinite(Number(d.xp)) ? Math.max(0, Math.floor(Number(d.xp))) : 22,
        avatarUrl: typeof d.avatarUrl === 'string' ? d.avatarUrl.slice(0, 500) : user.avatarUrl,
      };
    }
  } catch {
    // Fallback to verified Firebase Auth claims if user doc is not yet created
  }
  return {
    name: user.displayName.slice(0, 40) || 'لاعب شدها',
    rating: 22,
    avatarUrl: user.avatarUrl,
  };
}

function emptySeat(seatIndex: number): BalootSeat {
  return {
    seatIndex,
    uid: '',
    name: '',
    rating: 0,
    isBot: false,
    avatarUrl: null,
    connectionStatus: 'empty',
  };
}

/**
 * Projects a full internal BalootTableState into:
 * 1) PublicRoomState (safe to persist in `mp_rooms/{code}` and broadcast to all members — ZERO private cards)
 * 2) Per-seat PrivatePlayerHand documents (written strictly to `mp_rooms/{code}/playerHands/{uid}`)
 * 3) ServerEngineSecretDoc (written strictly to `mp_rooms/{code}/serverState/engine`)
 */
export function buildProjectedRoomDocuments(params: {
  existingRoom: PublicRoomState;
  tableState: BalootTableState;
  undealtDeck: BalootCard[];
  nextMoveSeq: number;
  processedActionIds: string[];
  nowMs: number;
}): {
  publicRoom: PublicRoomState;
  privateHands: Record<string, PrivatePlayerHand>;
  serverSecret: ServerEngineSecretDoc;
} {
  const { existingRoom, tableState, undealtDeck, nextMoveSeq, processedActionIds, nowMs } = params;
  const activeTurnPhase =
    tableState.phase === 'bidding_r1' ||
    tableState.phase === 'bidding_r2' ||
    tableState.phase === 'doubling' ||
    tableState.phase === 'playing';

  const seatHands: Record<'0' | '1' | '2' | '3', string[]> = {
    '0': (tableState.hands[0] || []).map(cardCode),
    '1': (tableState.hands[1] || []).map(cardCode),
    '2': (tableState.hands[2] || []).map(cardCode),
    '3': (tableState.hands[3] || []).map(cardCode),
  };

  const handCounts: Record<'0' | '1' | '2' | '3', number> = {
    '0': seatHands['0'].length,
    '1': seatHands['1'].length,
    '2': seatHands['2'].length,
    '3': seatHands['3'].length,
  };

  const publicRoom: PublicRoomState = {
    ...existingRoom,
    status: tableState.phase === 'match_finished' ? 'finished' : 'playing',
    seats: tableState.seats,
    handCounts,
    usScore: tableState.usScore,
    themScore: tableState.themScore,
    roundNumber: tableState.roundNumber,
    turnIndex: tableState.turnIndex,
    dealerIndex: tableState.dealerIndex,
    phase: tableState.phase,
    mode: tableState.mode,
    trumpSuit: tableState.trumpSuit,
    bidderIndex: tableState.bidderIndex,
    bidLevel: tableState.bidLevel,
    openCard: tableState.openCard ? cardCode(tableState.openCard) : '',
    tableCards: tableState.tableCards.map(encodeTableCard),
    lastTrickCards: tableState.lastTrickCards.map(encodeTableCard),
    lastTrickWinner: tableState.lastTrickWinner,
    usTricksCount: tableState.usTricksCount,
    themTricksCount: tableState.themTricksCount,
    usRoundPoints: tableState.usRoundPoints,
    themRoundPoints: tableState.themRoundPoints,
    usProjects: tableState.usProjects.slice(0, 8),
    themProjects: tableState.themProjects.slice(0, 8),
    winnerTeam: tableState.winnerTeam,
    lastActionText: tableState.lastActionText.slice(0, 140),
    biddingPasses: tableState.biddingPasses,
    highestBidSeat: tableState.highestBidSeat,
    highestBidMode: tableState.highestBidMode,
    highestBidSuit: tableState.highestBidSuit,
    standbySeats: tableState.standbySeats.slice(0, 4),
    declaredProjectSeats: tableState.declaredProjectSeats.slice(0, 4),
    moveSeq: nextMoveSeq,
    turnDeadlineAtMs: activeTurnPhase ? nowMs + TURN_DURATION_MS : null,
    updatedAtMs: nowMs,
  };

  const privateHands: Record<string, PrivatePlayerHand> = {};
  for (const seat of tableState.seats) {
    if (!seat.uid || seat.isBot) continue;
    const seatCards = tableState.hands[seat.seatIndex] || [];
    const isTurn = tableState.phase === 'playing' && tableState.turnIndex === seat.seatIndex;
    const legal = isTurn
      ? getLegalCards(seatCards, tableState.tableCards, tableState.mode, tableState.trumpSuit, seat.seatIndex).map(
          cardCode,
        )
      : [];
    const declarable =
      tableState.phase === 'playing' &&
      tableState.usTricksCount === 0 &&
      tableState.themTricksCount === 0 &&
      tableState.tableCards.length === 0 &&
      !tableState.declaredProjectSeats.includes(seat.seatIndex)
        ? detectDeclarableProjectsForHand(seatCards, seat.seatIndex, tableState.mode, tableState.trumpSuit)
        : [];

    privateHands[seat.uid] = {
      roomCode: existingRoom.roomCode,
      uid: seat.uid,
      seatIndex: seat.seatIndex,
      roundNumber: tableState.roundNumber,
      moveSeq: nextMoveSeq,
      cards: seatCards.map(cardCode),
      legalCards: legal,
      declarableProjects: declarable,
      updatedAtMs: nowMs,
    };
  }

  const serverSecret: ServerEngineSecretDoc = {
    roomCode: existingRoom.roomCode,
    matchId: existingRoom.matchId,
    roundNumber: tableState.roundNumber,
    moveSeq: nextMoveSeq,
    undealtDeck: undealtDeck.map(cardCode),
    seatHands,
    processedActionIds: processedActionIds.slice(-MAX_PROCESSED_ACTIONS),
    updatedAtMs: nowMs,
  };

  return { publicRoom, privateHands, serverSecret };
}

/**
 * Creates a new authoritative multiplayer room inside a Firestore Transaction,
 * registers seat 0 for the host, initializes serverState/engine, and writes the
 * host's reconnection pointer at `users/{uid}/session/active`.
 */
export async function createAuthoritativeRoom(params: {
  user: VerifiedUser;
  roomType: RoomType;
  fillWithBots?: boolean;
  actionId: string;
}): Promise<PublicRoomState> {
  const db = requireAdminFirestore();
  const identity = await resolvePlayerIdentity(db, params.user);
  const nowMs = Date.now();
  const matchId = randomUUID();

  for (let attempt = 0; attempt < 5; attempt++) {
    const roomCode = generateRoomCode(secureRandom);
    const rRef = roomDocRef(db, roomCode);
    const sRef = serverEngineRef(db, roomCode);
    const sessionRef = userActiveSessionRef(db, params.user.uid);

    const created = await db.runTransaction(async (tx) => {
      const snap = await tx.get(rRef);
      if (snap.exists) return null;

      const hostSeat: BalootSeat = {
        seatIndex: 0,
        uid: params.user.uid,
        name: identity.name,
        rating: identity.rating,
        isBot: false,
        avatarUrl: identity.avatarUrl,
        connectionStatus: 'connected',
      };

      const publicRoom: PublicRoomState = {
        roomCode,
        matchId,
        hostId: params.user.uid,
        roomType: params.roomType,
        status: 'waiting',
        targetScore: 152,
        maxPlayers: 4,
        fillWithBots: params.fillWithBots ?? true,
        memberUids: [params.user.uid],
        seats: [hostSeat, emptySeat(1), emptySeat(2), emptySeat(3)],
        handCounts: { '0': 0, '1': 0, '2': 0, '3': 0 },
        usScore: 0,
        themScore: 0,
        roundNumber: 0,
        turnIndex: 0,
        dealerIndex: 0,
        phase: 'lobby',
        mode: 'none',
        trumpSuit: null,
        bidderIndex: -1,
        bidLevel: 1,
        openCard: '',
        tableCards: [],
        lastTrickCards: [],
        lastTrickWinner: -1,
        usTricksCount: 0,
        themTricksCount: 0,
        usRoundPoints: 0,
        themRoundPoints: 0,
        usProjects: [],
        themProjects: [],
        winnerTeam: -1,
        lastActionText: `أنشأ ${identity.name} جلسة بلوت (صكة 152)`,
        biddingPasses: 0,
        highestBidSeat: -1,
        highestBidMode: 'none',
        highestBidSuit: null,
        standbySeats: [],
        declaredProjectSeats: [],
        moveSeq: 0,
        turnDeadlineAtMs: null,
        resultsCommitted: false,
        createdAtMs: nowMs,
        updatedAtMs: nowMs,
      };

      const secretDoc: ServerEngineSecretDoc = {
        roomCode,
        matchId,
        roundNumber: 0,
        moveSeq: 0,
        undealtDeck: [],
        seatHands: { '0': [], '1': [], '2': [], '3': [] },
        processedActionIds: [params.actionId],
        updatedAtMs: nowMs,
      };

      const activeSession: UserActiveSessionDoc = {
        uid: params.user.uid,
        activeRoomCode: roomCode,
        seatIndex: 0,
        status: 'in_waiting_room',
        updatedAtMs: nowMs,
      };

      tx.set(rRef, publicRoom);
      tx.set(sRef, secretDoc);
      tx.set(sessionRef, activeSession);
      return publicRoom;
    });

    if (created) return created;
  }

  throw new RoomStoreError('INTERNAL_ERROR', 'تعذر توليد رمز غرفة فريد، يرجى المحاولة مجدداً.');
}

/**
 * Atomically joins a player into the first free seat (0..3) using a Firestore Transaction.
 * Strictly blocks a 5th player, prevents concurrent seat overwrites, and idempotently
 * returns the room if the player is already seated (reconnecting).
 */
export async function joinAuthoritativeRoom(params: {
  user: VerifiedUser;
  roomCode: string;
  actionId: string;
}): Promise<{ room: PublicRoomState; hand: PrivatePlayerHand | null; seatIndex: number; reconnected: boolean }> {
  const db = requireAdminFirestore();
  const identity = await resolvePlayerIdentity(db, params.user);
  const rRef = roomDocRef(db, params.roomCode);
  const sRef = serverEngineRef(db, params.roomCode);
  const hRef = playerHandRef(db, params.roomCode, params.user.uid);
  const sessionRef = userActiveSessionRef(db, params.user.uid);
  const nowMs = Date.now();

  return db.runTransaction(async (tx) => {
    const [roomSnap, secretSnap, handSnap] = await Promise.all([
      tx.get(rRef),
      tx.get(sRef),
      tx.get(hRef),
    ]);

    if (!roomSnap.exists) {
      throw new RoomStoreError('ROOM_NOT_FOUND', 'لم يتم العثور على جلسة بهذا الرمز.');
    }

    const room = roomSnap.data() as PublicRoomState;
    const secret = secretSnap.exists ? (secretSnap.data() as ServerEngineSecretDoc) : null;

    // Reconnection path if player is already a seated member
    const existingSeatIdx = room.seats.findIndex((s) => s.uid === params.user.uid);
    if (existingSeatIdx >= 0) {
      const updatedSeats = room.seats.map((s) =>
        s.seatIndex === existingSeatIdx ? { ...s, connectionStatus: 'connected' as SeatConnectionStatus } : s,
      );
      const updatedRoom: PublicRoomState = {
        ...room,
        seats: updatedSeats,
        updatedAtMs: nowMs,
      };
      tx.set(rRef, updatedRoom);
      tx.set(sessionRef, {
        uid: params.user.uid,
        activeRoomCode: room.roomCode,
        seatIndex: existingSeatIdx,
        status: room.status === 'waiting' ? 'in_waiting_room' : 'in_match',
        updatedAtMs: nowMs,
      } satisfies UserActiveSessionDoc);

      return {
        room: updatedRoom,
        hand: handSnap.exists ? (handSnap.data() as PrivatePlayerHand) : null,
        seatIndex: existingSeatIdx,
        reconnected: true,
      };
    }

    if (room.status !== 'waiting') {
      throw new RoomStoreError('ROOM_ALREADY_STARTED', 'هذه الجلسة بدأت بالفعل أو انتهت.');
    }

    if (room.memberUids.length >= 4) {
      throw new RoomStoreError('ROOM_FULL', 'الجلسة مكتملة (4 لاعبين كحد أقصى).');
    }

    const freeSeatIndex = room.seats.findIndex((s) => !s.uid);
    if (freeSeatIndex < 0) {
      throw new RoomStoreError('ROOM_FULL', 'لا يوجد مقعد متاح في هذه الجلسة.');
    }

    const nextSeats = room.seats.map((s) =>
      s.seatIndex === freeSeatIndex
        ? {
            seatIndex: freeSeatIndex,
            uid: params.user.uid,
            name: identity.name,
            rating: identity.rating,
            isBot: false,
            avatarUrl: identity.avatarUrl,
            connectionStatus: 'connected' as SeatConnectionStatus,
          }
        : s,
    );

    const nextMoveSeq = room.moveSeq + 1;
    const updatedRoom: PublicRoomState = {
      ...room,
      memberUids: [...room.memberUids, params.user.uid],
      seats: nextSeats,
      moveSeq: nextMoveSeq,
      lastActionText: `انضم ${identity.name} إلى الطاولة`,
      updatedAtMs: nowMs,
    };

    tx.set(rRef, updatedRoom);
    if (secret) {
      tx.set(sRef, {
        ...secret,
        moveSeq: nextMoveSeq,
        processedActionIds: [...secret.processedActionIds, params.actionId].slice(-MAX_PROCESSED_ACTIONS),
        updatedAtMs: nowMs,
      } satisfies ServerEngineSecretDoc);
    }
    tx.set(sessionRef, {
      uid: params.user.uid,
      activeRoomCode: room.roomCode,
      seatIndex: freeSeatIndex,
      status: 'in_waiting_room',
      updatedAtMs: nowMs,
    } satisfies UserActiveSessionDoc);

    return {
      room: updatedRoom,
      hand: null,
      seatIndex: freeSeatIndex,
      reconnected: false,
    };
  });
}

/**
 * Starts a waiting room match transactionally, deals secret 5+1 cards with CSPRNG,
 * persists ONLY handCounts in `mp_rooms/{code}`, and writes each human player's secret hand
 * exclusively to `mp_rooms/{code}/playerHands/{uid}`.
 */
export async function startAuthoritativeMatch(params: {
  user: VerifiedUser;
  roomCode: string;
  expectedMoveSeq: number;
  actionId: string;
}): Promise<{ publicRoom: PublicRoomState; privateHands: Record<string, PrivatePlayerHand>; duplicate?: boolean }> {
  const db = requireAdminFirestore();
  const rRef = roomDocRef(db, params.roomCode);
  const sRef = serverEngineRef(db, params.roomCode);
  const nowMs = Date.now();

  return db.runTransaction(async (tx) => {
    const [roomSnap, secretSnap] = await Promise.all([tx.get(rRef), tx.get(sRef)]);
    if (!roomSnap.exists || !secretSnap.exists) {
      throw new RoomStoreError('ROOM_NOT_FOUND', 'الغرفة غير موجودة.');
    }

    const room = roomSnap.data() as PublicRoomState;
    const secret = secretSnap.data() as ServerEngineSecretDoc;

    if (secret.processedActionIds.includes(params.actionId)) {
      return { publicRoom: room, privateHands: {}, duplicate: true };
    }
    if (room.hostId !== params.user.uid) {
      throw new RoomStoreError('NOT_HOST', 'المضيف فقط يمكنه بدء الصكة.');
    }
    if (room.status !== 'waiting') {
      throw new RoomStoreError('INVALID_PHASE', 'الصكة بدأت بالفعل.');
    }
    if (room.moveSeq !== params.expectedMoveSeq) {
      throw new RoomStoreError('STALE_MOVE_SEQ', 'تغيّرت حالة الغرفة، يرجى تحديث الحالة.', room.moveSeq);
    }
    if (!room.fillWithBots && room.memberUids.length < 4) {
      throw new RoomStoreError('INVALID_PHASE', 'تتطلب هذه الطاولة اكتمال 4 لاعبين حقيقيين قبل البدء.');
    }

    const filledSeats: BalootSeat[] = room.seats.map((seat) => {
      if (seat.uid) return seat;
      return {
        seatIndex: seat.seatIndex,
        uid: `bot_${seat.seatIndex}`,
        name: `${BOT_NAMES[seat.seatIndex % BOT_NAMES.length]} (آلي)`,
        rating: 1200,
        isBot: true,
        avatarUrl: null,
        connectionStatus: 'bot',
      };
    });

    const dealt = dealNewRoundWithDeck(
      {
        dealerIndex: 0,
        roundNumber: 1,
        seats: filledSeats,
        existingUsScore: 0,
        existingThemScore: 0,
        roomCode: room.roomCode,
        isOnline: true,
        hostId: room.hostId,
        mySeatIndex: 0,
      },
      secureRandom,
    );

    const projected = buildProjectedRoomDocuments({
      existingRoom: room,
      tableState: dealt.state,
      undealtDeck: dealt.undealtDeck,
      nextMoveSeq: room.moveSeq + 1,
      processedActionIds: [...secret.processedActionIds, params.actionId],
      nowMs,
    });

    tx.set(rRef, projected.publicRoom);
    tx.set(sRef, projected.serverSecret);
    for (const [uid, handDoc] of Object.entries(projected.privateHands)) {
      tx.set(playerHandRef(db, room.roomCode, uid), handDoc);
      tx.set(userActiveSessionRef(db, uid), {
        uid,
        activeRoomCode: room.roomCode,
        seatIndex: handDoc.seatIndex,
        status: 'in_match',
        updatedAtMs: nowMs,
      } satisfies UserActiveSessionDoc);
    }

    return { publicRoom: projected.publicRoom, privateHands: projected.privateHands };
  });
}

/**
 * Handles player leaving cleanly:
 * - In `waiting`: removes seat & transfers host if another human remains (or closes room if empty).
 * - In `playing`: keeps seat ownership for reconnection, marks connectionStatus = 'standby',
 *   and adds seatIndex to `standbySeats` so the match never freezes.
 */
export async function leaveAuthoritativeRoom(params: {
  user: VerifiedUser;
  roomCode: string;
  actionId: string;
}): Promise<{ closed: boolean; room: PublicRoomState | null }> {
  const db = requireAdminFirestore();
  const rRef = roomDocRef(db, params.roomCode);
  const sRef = serverEngineRef(db, params.roomCode);
  const sessionRef = userActiveSessionRef(db, params.user.uid);
  const nowMs = Date.now();

  return db.runTransaction(async (tx) => {
    const [roomSnap, secretSnap] = await Promise.all([tx.get(rRef), tx.get(sRef)]);
    if (!roomSnap.exists) {
      tx.set(sessionRef, {
        uid: params.user.uid,
        activeRoomCode: null,
        seatIndex: null,
        status: 'idle',
        updatedAtMs: nowMs,
      } satisfies UserActiveSessionDoc);
      return { closed: true, room: null };
    }

    const room = roomSnap.data() as PublicRoomState;
    const seatIndex = room.seats.findIndex((s) => s.uid === params.user.uid);
    if (seatIndex < 0) {
      throw new RoomStoreError('NOT_A_MEMBER', 'أنت لست عضواً في هذه الغرفة.');
    }

    if (room.status === 'waiting') {
      const nextMembers = room.memberUids.filter((u) => u !== params.user.uid);
      tx.set(sessionRef, {
        uid: params.user.uid,
        activeRoomCode: null,
        seatIndex: null,
        status: 'idle',
        updatedAtMs: nowMs,
      } satisfies UserActiveSessionDoc);

      if (nextMembers.length === 0) {
        tx.delete(rRef);
        if (secretSnap.exists) tx.delete(sRef);
        return { closed: true, room: null };
      }

      const nextSeats = room.seats.map((s) => (s.seatIndex === seatIndex ? emptySeat(seatIndex) : s));
      const nextHostId = room.hostId === params.user.uid ? nextMembers[0] : room.hostId;
      const updatedRoom: PublicRoomState = {
        ...room,
        hostId: nextHostId,
        memberUids: nextMembers,
        seats: nextSeats,
        moveSeq: room.moveSeq + 1,
        lastActionText: 'غادر أحد اللاعبين غرفة الانتظار',
        updatedAtMs: nowMs,
      };
      tx.set(rRef, updatedRoom);
      return { closed: false, room: updatedRoom };
    }

    // Active match: switch seat to standby instead of corrupting the 4-seat game
    const nextSeats = room.seats.map((s) =>
      s.seatIndex === seatIndex ? { ...s, connectionStatus: 'standby' as SeatConnectionStatus } : s,
    );
    const nextStandby = room.standbySeats.includes(seatIndex)
      ? room.standbySeats
      : [...room.standbySeats, seatIndex];
    const updatedRoom: PublicRoomState = {
      ...room,
      seats: nextSeats,
      standbySeats: nextStandby,
      moveSeq: room.moveSeq + 1,
      lastActionText: `غادر ${room.seats[seatIndex].name} — تم تفعيل اللعب التلقائي لمقعده`,
      updatedAtMs: nowMs,
    };

    tx.set(rRef, updatedRoom);
    tx.set(sessionRef, {
      uid: params.user.uid,
      activeRoomCode: room.roomCode,
      seatIndex,
      status: 'in_match',
      updatedAtMs: nowMs,
    } satisfies UserActiveSessionDoc);
    return { closed: false, room: updatedRoom };
  });
}

/**
 * Updates a seated player's connection state (`connected` | `disconnected`) in Firestore
 * without dropping them from the match so reconnection works across Cloud Run instances.
 */
export async function updateSeatConnectionStatus(params: {
  roomCode: string;
  uid: string;
  status: 'connected' | 'disconnected';
}): Promise<PublicRoomState | null> {
  const db = requireAdminFirestore();
  const rRef = roomDocRef(db, params.roomCode);
  const nowMs = Date.now();

  return db.runTransaction(async (tx) => {
    const snap = await tx.get(rRef);
    if (!snap.exists) return null;
    const room = snap.data() as PublicRoomState;
    const idx = room.seats.findIndex((s) => s.uid === params.uid);
    if (idx < 0) return null;

    const currentStatus = room.seats[idx].connectionStatus;
    if (currentStatus === params.status) return room;

    const nextSeats = room.seats.map((s) =>
      s.seatIndex === idx ? { ...s, connectionStatus: params.status as SeatConnectionStatus } : s,
    );
    const updated: PublicRoomState = {
      ...room,
      seats: nextSeats,
      updatedAtMs: nowMs,
    };
    tx.set(rRef, updated);
    return updated;
  });
}

/**
 * Fetches a user's persistent active session pointer from Firestore (`users/{uid}/session/active`)
 * and verifies the referenced room still exists and contains the user.
 */
export async function getUserActiveSession(uid: string): Promise<UserActiveSessionDoc | null> {
  const db = requireAdminFirestore();
  const snap = await userActiveSessionRef(db, uid).get();
  if (!snap.exists) return null;
  const session = snap.data() as UserActiveSessionDoc;
  if (!session.activeRoomCode) return null;

  const roomSnap = await roomDocRef(db, session.activeRoomCode).get();
  if (!roomSnap.exists) {
    return null;
  }
  const room = roomSnap.data() as PublicRoomState;
  if (!room.memberUids.includes(uid) || room.status === 'finished') {
    return null;
  }
  return session;
}

/**
 * Fetches full reconciliation snapshot for a seated member:
 * - Public room state (`mp_rooms/{roomCode}`)
 * - Caller's own secret hand ONLY (`mp_rooms/{roomCode}/playerHands/{uid}`)
 */
export async function getMemberRoomSnapshot(params: {
  roomCode: string;
  uid: string;
}): Promise<{ room: PublicRoomState; hand: PrivatePlayerHand | null; seatIndex: number }> {
  const db = requireAdminFirestore();
  const [roomSnap, handSnap] = await Promise.all([
    roomDocRef(db, params.roomCode).get(),
    playerHandRef(db, params.roomCode, params.uid).get(),
  ]);

  if (!roomSnap.exists) {
    throw new RoomStoreError('ROOM_NOT_FOUND', 'الغرفة غير موجودة.');
  }
  const room = roomSnap.data() as PublicRoomState;
  const seatIndex = room.seats.findIndex((s) => s.uid === params.uid);
  if (seatIndex < 0) {
    throw new RoomStoreError('NOT_A_MEMBER', 'ليست لديك صلاحية الوصول إلى هذه الغرفة.');
  }

  return {
    room,
    hand: handSnap.exists ? (handSnap.data() as PrivatePlayerHand) : null,
    seatIndex,
  };
}

/** Helper to reconstruct decoded BalootCard[] from ServerEngineSecretDoc (used by Phase 2 game actions) */
export function decodeSecretHands(secret: ServerEngineSecretDoc): Record<number, BalootCard[]> {
  const result: Record<number, BalootCard[]> = { 0: [], 1: [], 2: [], 3: [] };
  for (let s = 0; s < 4; s++) {
    const raw = secret.seatHands[String(s) as '0' | '1' | '2' | '3'] || [];
    result[s] = raw.map(parseCardCode).filter((c): c is BalootCard => c !== null);
  }
  return result;
}
