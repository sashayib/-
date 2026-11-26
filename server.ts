import { existsSync } from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import express, { type Request, type Response } from 'express';
import { isValidActionId, isValidRoomCode, normalizeRoomCode, PROTOCOL_VERSION } from './shared/protocol.ts';
import { type AuthenticatedRequest, requireFirebaseAuth } from './server/authMiddleware.ts';
import { getFirebaseAdmin } from './server/firebaseAdmin.ts';
import {
  createAuthoritativeRoom,
  getMemberRoomSnapshot,
  getUserActiveSession,
  joinAuthoritativeRoom,
  MP_ROOMS_COLLECTION,
  RoomStoreError,
} from './server/roomStore.ts';
import { createWsHub } from './server/wsHub.ts';

function parseAllowedOrigins(): Set<string> {
  const defaults = [
    'http://localhost:3000',
    'http://127.0.0.1:3000',
    'http://localhost:5173',
    'http://127.0.0.1:5173',
    'https://corded-manifest-qk4jk.web.app',
    'https://corded-manifest-qk4jk.firebaseapp.com',
  ];
  const fromEnv = (process.env.ALLOWED_ORIGINS || '')
    .split(',')
    .map((o) => o.trim())
    .filter(Boolean);
  return new Set([...defaults, ...fromEnv]);
}

export function createMultiplayerApp() {
  const app = express();
  const allowedOrigins = parseAllowedOrigins();

  app.disable('x-powered-by');
  app.set('trust proxy', 1);
  app.use(express.json({ limit: '32kb' }));

  // Strict origin allowlist CORS (never wildcard '*' on authenticated endpoints)
  app.use((req, res, next) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');

    const origin = req.headers.origin;
    const host = req.headers.host;
    const isSameOrigin =
      Boolean(host) && (origin === `https://${host}` || origin === `http://${host}`);
    if (origin && (isSameOrigin || allowedOrigins.has(origin))) {
      res.setHeader('Access-Control-Allow-Origin', origin);
      res.setHeader('Vary', 'Origin');
      res.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS');
      res.setHeader('Access-Control-Allow-Headers', 'Authorization,Content-Type');
    }
    if (req.method === 'OPTIONS') {
      res.status(204).end();
      return;
    }
    next();
  });

  const httpServer = http.createServer(app);
  const wsHub = createWsHub(httpServer, allowedOrigins);

  // Cloud Run liveness / readiness health endpoint (zero secret disclosure)
  app.get('/api/health', (_req: Request, res: Response) => {
    const adminCtx = getFirebaseAdmin();
    const stats = wsHub.getConnectionStats();
    res.status(200).json({
      ok: true,
      service: 'shaddaha-multiplayer-server',
      phase: 1,
      protocolVersion: PROTOCOL_VERSION,
      roomsCollection: MP_ROOMS_COLLECTION,
      firebaseAdminReady: adminCtx.ready,
      connectedSockets: stats.connectedSockets,
      activeRooms: stats.activeRooms,
      serverTimeMs: Date.now(),
    });
  });

  // Reconnection session discovery for Web, Android, and iOS clients
  app.get('/api/multiplayer/session', requireFirebaseAuth, async (req: AuthenticatedRequest, res: Response) => {
    try {
      const session = await getUserActiveSession(req.user!.uid);
      res.status(200).json({ ok: true, activeSession: session, serverTimeMs: Date.now() });
    } catch (err) {
      handleRestError(res, err);
    }
  });

  // Authenticated REST room creation (shares identical transactional logic with WebSocket)
  app.post('/api/multiplayer/rooms', requireFirebaseAuth, async (req: AuthenticatedRequest, res: Response) => {
    const body = req.body as Record<string, unknown>;
    const roomType = body?.roomType === 'public' || body?.roomType === 'private' ? body.roomType : null;
    const actionId = body?.actionId;
    const fillWithBots = typeof body?.fillWithBots === 'boolean' ? body.fillWithBots : true;

    if (!roomType || !isValidActionId(actionId)) {
      res.status(400).json({
        ok: false,
        code: 'INVALID_MESSAGE',
        message: 'Valid roomType ("public"|"private") and actionId are required.',
      });
      return;
    }

    try {
      const room = await createAuthoritativeRoom({
        user: req.user!,
        roomType,
        fillWithBots,
        actionId,
      });
      res.status(201).json({ ok: true, room, serverTimeMs: Date.now() });
    } catch (err) {
      handleRestError(res, err);
    }
  });

  // Authenticated REST atomic join (strictly prevents 5th player via Firestore Transaction)
  app.post(
    '/api/multiplayer/rooms/:roomCode/join',
    requireFirebaseAuth,
    async (req: AuthenticatedRequest, res: Response) => {
      const rawCode = req.params.roomCode;
      const body = req.body as Record<string, unknown>;
      if (!isValidRoomCode(rawCode) || !isValidActionId(body?.actionId)) {
        res.status(400).json({
          ok: false,
          code: 'INVALID_MESSAGE',
          message: 'Valid 6-character roomCode and actionId are required.',
        });
        return;
      }

      try {
        const result = await joinAuthoritativeRoom({
          user: req.user!,
          roomCode: normalizeRoomCode(rawCode),
          actionId: body.actionId,
        });
        wsHub.broadcastPublicRoom(result.room);
        res.status(200).json({
          ok: true,
          room: result.room,
          hand: result.hand,
          seatIndex: result.seatIndex,
          reconnected: result.reconnected,
          serverTimeMs: Date.now(),
        });
      } catch (err) {
        handleRestError(res, err);
      }
    },
  );

  // Authenticated snapshot fetch (returns public room + caller's own secret hand ONLY)
  app.get(
    '/api/multiplayer/rooms/:roomCode',
    requireFirebaseAuth,
    async (req: AuthenticatedRequest, res: Response) => {
      const rawCode = req.params.roomCode;
      if (!isValidRoomCode(rawCode)) {
        res.status(400).json({ ok: false, code: 'INVALID_MESSAGE', message: 'Invalid roomCode.' });
        return;
      }

      try {
        const snapshot = await getMemberRoomSnapshot({
          roomCode: normalizeRoomCode(rawCode),
          uid: req.user!.uid,
        });
        res.status(200).json({
          ok: true,
          room: snapshot.room,
          hand: snapshot.hand,
          seatIndex: snapshot.seatIndex,
          serverTimeMs: Date.now(),
        });
      } catch (err) {
        handleRestError(res, err);
      }
    },
  );

  // Serve built Vite SPA static assets when `dist/index.html` exists (Cloud Run / production)
  const distDir = path.resolve(process.cwd(), 'dist');
  const indexHtmlPath = path.join(distDir, 'index.html');
  if (existsSync(indexHtmlPath)) {
    app.use(express.static(distDir));
    app.use((req, res, next) => {
      if (req.method === 'GET' && !req.path.startsWith('/api') && !req.path.startsWith('/ws')) {
        res.sendFile(indexHtmlPath);
        return;
      }
      next();
    });
  }

  return { app, httpServer, wsHub };
}

function handleRestError(res: Response, err: unknown): void {
  if (err instanceof RoomStoreError) {
    const status =
      err.code === 'ROOM_NOT_FOUND'
        ? 404
        : err.code === 'NOT_A_MEMBER' || err.code === 'NOT_HOST'
          ? 403
          : 409;
    res.status(status).json({
      ok: false,
      code: err.code,
      message: err.message,
      currentMoveSeq: err.currentMoveSeq,
    });
    return;
  }
  if (err instanceof Error && err.message === 'FIREBASE_ADMIN_UNCONFIGURED') {
    res.status(503).json({
      ok: false,
      code: 'SERVER_NOT_CONFIGURED',
      message: 'Firebase Admin credentials are not configured on this server.',
    });
    return;
  }
  res.status(500).json({
    ok: false,
    code: 'INTERNAL_ERROR',
    message: 'Internal server error.',
  });
}

// Start standalone multiplayer server only when executed directly (never interferes with Vite SPA)
const isDirectEntry =
  typeof process !== 'undefined' &&
  Array.isArray(process.argv) &&
  Boolean(process.argv[1]) &&
  path.basename(process.argv[1]) === 'server.ts';

if (isDirectEntry) {
  const defaultPort = process.env.NODE_ENV === 'production' ? 8080 : 4000;
  const port = Number(process.env.PORT || process.env.SERVER_PORT || defaultPort);
  const { httpServer, wsHub } = createMultiplayerApp();

  httpServer.listen(port, '0.0.0.0', () => {
    const adminCtx = getFirebaseAdmin();
    console.log(
      `[shaddaha-mp] Express + WebSocket server listening on http://0.0.0.0:${port} (ws: /ws, firebaseAdminReady=${adminCtx.ready})`,
    );
  });

  const shutdown = () => {
    void wsHub.close().finally(() => {
      httpServer.close(() => process.exit(0));
    });
  };
  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);
}
