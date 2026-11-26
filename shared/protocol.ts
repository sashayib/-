import type {
  PrivatePlayerHand,
  PublicRoomState,
  RoomType,
  Suit,
  UserActiveSessionDoc,
} from './balootTypes.ts';
import { type BidAction, parseCardCode, SUITS } from './balootEngine.ts';

export const PROTOCOL_VERSION = 1;
export const MAX_WS_PAYLOAD_BYTES = 16 * 1024;
export const TURN_DURATION_MS = 10_000;
export const RECONNECT_GRACE_MS = 30_000;

export const ROOM_CODE_REGEX = /^[A-Z0-9]{6}$/;
export const ACTION_ID_REGEX = /^[a-zA-Z0-9_-]{8,64}$/;

export type ProtocolErrorCode =
  | 'INVALID_MESSAGE'
  | 'UNAUTHENTICATED'
  | 'AUTH_FAILED'
  | 'RATE_LIMITED'
  | 'ROOM_NOT_FOUND'
  | 'ROOM_FULL'
  | 'ROOM_ALREADY_STARTED'
  | 'NOT_A_MEMBER'
  | 'NOT_HOST'
  | 'NOT_YOUR_TURN'
  | 'INVALID_PHASE'
  | 'ILLEGAL_CARD'
  | 'STALE_MOVE_SEQ'
  | 'DUPLICATE_ACTION'
  | 'SERVER_NOT_CONFIGURED'
  | 'INTERNAL_ERROR';

// --- Client -> Server WebSocket Messages ---
export type ClientMessage =
  | {
      type: 'auth';
      token: string;
      protocolVersion?: number;
    }
  | {
      type: 'room:create';
      actionId: string;
      roomType: RoomType;
      fillWithBots?: boolean;
    }
  | {
      type: 'room:join';
      actionId: string;
      roomCode: string;
    }
  | {
      type: 'room:leave';
      actionId: string;
      roomCode: string;
    }
  | {
      type: 'room:resume';
      roomCode?: string;
    }
  | {
      type: 'room:sync';
      roomCode: string;
    }
  | {
      type: 'game:start';
      actionId: string;
      roomCode: string;
      expectedMoveSeq: number;
    }
  | {
      type: 'game:bid';
      actionId: string;
      roomCode: string;
      expectedMoveSeq: number;
      choice: BidAction;
      suit?: Suit | null;
    }
  | {
      type: 'game:double';
      actionId: string;
      roomCode: string;
      expectedMoveSeq: number;
      raise: boolean;
    }
  | {
      type: 'game:declare_projects';
      actionId: string;
      roomCode: string;
      expectedMoveSeq: number;
      projectIds: string[];
    }
  | {
      type: 'game:play_card';
      actionId: string;
      roomCode: string;
      expectedMoveSeq: number;
      cardCode: string;
    }
  | {
      type: 'game:next_round';
      actionId: string;
      roomCode: string;
      expectedMoveSeq: number;
    }
  | {
      type: 'game:claim_timeout';
      actionId: string;
      roomCode: string;
      expectedMoveSeq: number;
    }
  | {
      type: 'game:resume_control';
      actionId: string;
      roomCode: string;
    }
  | {
      type: 'chat:send';
      actionId: string;
      roomCode: string;
      text: string;
    }
  | {
      type: 'reaction:send';
      roomCode: string;
      emoji: string;
    }
  | {
      type: 'ping';
      clientTimeMs?: number;
    };

// --- Server -> Client WebSocket Messages ---
export type ServerMessage =
  | {
      type: 'auth:ok';
      uid: string;
      displayName: string;
      protocolVersion: number;
      serverTimeMs: number;
      activeSession: UserActiveSessionDoc | null;
    }
  | {
      type: 'auth:error';
      code: ProtocolErrorCode;
      message: string;
    }
  | {
      type: 'room:state';
      room: PublicRoomState;
      serverTimeMs: number;
    }
  | {
      type: 'hand:private';
      hand: PrivatePlayerHand;
      serverTimeMs: number;
    }
  | {
      type: 'room:event';
      roomCode: string;
      event:
        | 'player_joined'
        | 'player_left'
        | 'player_disconnected'
        | 'player_reconnected'
        | 'player_standby'
        | 'speech'
        | 'reaction'
        | 'chat';
      seatIndex?: number;
      uid?: string;
      displayName?: string;
      text?: string;
      serverTimeMs: number;
    }
  | {
      type: 'action:ack';
      actionId: string;
      roomCode?: string;
      moveSeq?: number;
      duplicate?: boolean;
    }
  | {
      type: 'action:error';
      actionId?: string;
      code: ProtocolErrorCode;
      message: string;
      currentMoveSeq?: number;
    }
  | {
      type: 'pong';
      clientTimeMs?: number;
      serverTimeMs: number;
    };

export function isValidRoomCode(value: unknown): value is string {
  return typeof value === 'string' && ROOM_CODE_REGEX.test(value.trim().toUpperCase());
}

export function normalizeRoomCode(value: string): string {
  return value.trim().toUpperCase();
}

export function isValidActionId(value: unknown): value is string {
  return typeof value === 'string' && ACTION_ID_REGEX.test(value);
}

function isNonNegativeInt(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0 && value <= 100_000;
}

/**
 * Strict runtime validator for untrusted client WebSocket payloads.
 * Rejects malformed types, oversized fields, or unknown message shapes.
 */
export function parseClientMessage(raw: string): { ok: true; msg: ClientMessage } | { ok: false; reason: string } {
  if (raw.length > MAX_WS_PAYLOAD_BYTES) {
    return { ok: false, reason: 'Payload exceeds maximum allowed size.' };
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return { ok: false, reason: 'Malformed JSON.' };
  }

  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return { ok: false, reason: 'Payload must be a JSON object.' };
  }

  const obj = parsed as Record<string, unknown>;
  const type = obj.type;
  if (typeof type !== 'string') {
    return { ok: false, reason: 'Missing message type.' };
  }

  switch (type) {
    case 'auth': {
      if (typeof obj.token !== 'string' || obj.token.length < 20 || obj.token.length > 8192) {
        return { ok: false, reason: 'Invalid auth token.' };
      }
      return {
        ok: true,
        msg: {
          type: 'auth',
          token: obj.token,
          protocolVersion: typeof obj.protocolVersion === 'number' ? obj.protocolVersion : PROTOCOL_VERSION,
        },
      };
    }

    case 'ping': {
      return {
        ok: true,
        msg: {
          type: 'ping',
          clientTimeMs: typeof obj.clientTimeMs === 'number' ? obj.clientTimeMs : undefined,
        },
      };
    }

    case 'room:create': {
      if (!isValidActionId(obj.actionId)) return { ok: false, reason: 'Invalid actionId.' };
      if (obj.roomType !== 'public' && obj.roomType !== 'private') {
        return { ok: false, reason: 'Invalid roomType.' };
      }
      return {
        ok: true,
        msg: {
          type: 'room:create',
          actionId: obj.actionId,
          roomType: obj.roomType,
          fillWithBots: typeof obj.fillWithBots === 'boolean' ? obj.fillWithBots : true,
        },
      };
    }

    case 'room:join': {
      if (!isValidActionId(obj.actionId)) return { ok: false, reason: 'Invalid actionId.' };
      if (!isValidRoomCode(obj.roomCode)) return { ok: false, reason: 'Invalid roomCode.' };
      return {
        ok: true,
        msg: { type: 'room:join', actionId: obj.actionId, roomCode: normalizeRoomCode(obj.roomCode) },
      };
    }

    case 'room:leave': {
      if (!isValidActionId(obj.actionId)) return { ok: false, reason: 'Invalid actionId.' };
      if (!isValidRoomCode(obj.roomCode)) return { ok: false, reason: 'Invalid roomCode.' };
      return {
        ok: true,
        msg: { type: 'room:leave', actionId: obj.actionId, roomCode: normalizeRoomCode(obj.roomCode) },
      };
    }

    case 'room:resume': {
      if (obj.roomCode !== undefined && !isValidRoomCode(obj.roomCode)) {
        return { ok: false, reason: 'Invalid roomCode.' };
      }
      return {
        ok: true,
        msg: {
          type: 'room:resume',
          roomCode: typeof obj.roomCode === 'string' ? normalizeRoomCode(obj.roomCode) : undefined,
        },
      };
    }

    case 'room:sync': {
      if (!isValidRoomCode(obj.roomCode)) return { ok: false, reason: 'Invalid roomCode.' };
      return { ok: true, msg: { type: 'room:sync', roomCode: normalizeRoomCode(obj.roomCode) } };
    }

    case 'game:start':
    case 'game:next_round':
    case 'game:claim_timeout': {
      if (!isValidActionId(obj.actionId)) return { ok: false, reason: 'Invalid actionId.' };
      if (!isValidRoomCode(obj.roomCode)) return { ok: false, reason: 'Invalid roomCode.' };
      if (!isNonNegativeInt(obj.expectedMoveSeq)) return { ok: false, reason: 'Invalid expectedMoveSeq.' };
      return {
        ok: true,
        msg: {
          type,
          actionId: obj.actionId,
          roomCode: normalizeRoomCode(obj.roomCode),
          expectedMoveSeq: obj.expectedMoveSeq,
        },
      };
    }

    case 'game:bid': {
      if (!isValidActionId(obj.actionId)) return { ok: false, reason: 'Invalid actionId.' };
      if (!isValidRoomCode(obj.roomCode)) return { ok: false, reason: 'Invalid roomCode.' };
      if (!isNonNegativeInt(obj.expectedMoveSeq)) return { ok: false, reason: 'Invalid expectedMoveSeq.' };
      if (obj.choice !== 'sun' && obj.choice !== 'hokm' && obj.choice !== 'pass') {
        return { ok: false, reason: 'Invalid bid choice.' };
      }
      if (obj.suit !== undefined && obj.suit !== null && !SUITS.includes(obj.suit as Suit)) {
        return { ok: false, reason: 'Invalid suit.' };
      }
      return {
        ok: true,
        msg: {
          type: 'game:bid',
          actionId: obj.actionId,
          roomCode: normalizeRoomCode(obj.roomCode),
          expectedMoveSeq: obj.expectedMoveSeq,
          choice: obj.choice,
          suit: (obj.suit as Suit | null | undefined) ?? null,
        },
      };
    }

    case 'game:double': {
      if (!isValidActionId(obj.actionId)) return { ok: false, reason: 'Invalid actionId.' };
      if (!isValidRoomCode(obj.roomCode)) return { ok: false, reason: 'Invalid roomCode.' };
      if (!isNonNegativeInt(obj.expectedMoveSeq)) return { ok: false, reason: 'Invalid expectedMoveSeq.' };
      if (typeof obj.raise !== 'boolean') return { ok: false, reason: 'Invalid raise boolean.' };
      return {
        ok: true,
        msg: {
          type: 'game:double',
          actionId: obj.actionId,
          roomCode: normalizeRoomCode(obj.roomCode),
          expectedMoveSeq: obj.expectedMoveSeq,
          raise: obj.raise,
        },
      };
    }

    case 'game:declare_projects': {
      if (!isValidActionId(obj.actionId)) return { ok: false, reason: 'Invalid actionId.' };
      if (!isValidRoomCode(obj.roomCode)) return { ok: false, reason: 'Invalid roomCode.' };
      if (!isNonNegativeInt(obj.expectedMoveSeq)) return { ok: false, reason: 'Invalid expectedMoveSeq.' };
      if (
        !Array.isArray(obj.projectIds) ||
        obj.projectIds.length > 8 ||
        !obj.projectIds.every((id) => typeof id === 'string' && id.length > 0 && id.length <= 64)
      ) {
        return { ok: false, reason: 'Invalid projectIds array.' };
      }
      return {
        ok: true,
        msg: {
          type: 'game:declare_projects',
          actionId: obj.actionId,
          roomCode: normalizeRoomCode(obj.roomCode),
          expectedMoveSeq: obj.expectedMoveSeq,
          projectIds: obj.projectIds as string[],
        },
      };
    }

    case 'game:play_card': {
      if (!isValidActionId(obj.actionId)) return { ok: false, reason: 'Invalid actionId.' };
      if (!isValidRoomCode(obj.roomCode)) return { ok: false, reason: 'Invalid roomCode.' };
      if (!isNonNegativeInt(obj.expectedMoveSeq)) return { ok: false, reason: 'Invalid expectedMoveSeq.' };
      if (typeof obj.cardCode !== 'string' || !parseCardCode(obj.cardCode)) {
        return { ok: false, reason: 'Invalid cardCode.' };
      }
      return {
        ok: true,
        msg: {
          type: 'game:play_card',
          actionId: obj.actionId,
          roomCode: normalizeRoomCode(obj.roomCode),
          expectedMoveSeq: obj.expectedMoveSeq,
          cardCode: obj.cardCode.toUpperCase(),
        },
      };
    }

    case 'game:resume_control': {
      if (!isValidActionId(obj.actionId)) return { ok: false, reason: 'Invalid actionId.' };
      if (!isValidRoomCode(obj.roomCode)) return { ok: false, reason: 'Invalid roomCode.' };
      return {
        ok: true,
        msg: {
          type: 'game:resume_control',
          actionId: obj.actionId,
          roomCode: normalizeRoomCode(obj.roomCode),
        },
      };
    }

    case 'chat:send': {
      if (!isValidActionId(obj.actionId)) return { ok: false, reason: 'Invalid actionId.' };
      if (!isValidRoomCode(obj.roomCode)) return { ok: false, reason: 'Invalid roomCode.' };
      if (typeof obj.text !== 'string' || obj.text.trim().length === 0 || obj.text.length > 200) {
        return { ok: false, reason: 'Invalid chat text.' };
      }
      return {
        ok: true,
        msg: {
          type: 'chat:send',
          actionId: obj.actionId,
          roomCode: normalizeRoomCode(obj.roomCode),
          text: obj.text.trim().slice(0, 200),
        },
      };
    }

    case 'reaction:send': {
      if (!isValidRoomCode(obj.roomCode)) return { ok: false, reason: 'Invalid roomCode.' };
      if (typeof obj.emoji !== 'string' || obj.emoji.length === 0 || obj.emoji.length > 16) {
        return { ok: false, reason: 'Invalid emoji.' };
      }
      return {
        ok: true,
        msg: {
          type: 'reaction:send',
          roomCode: normalizeRoomCode(obj.roomCode),
          emoji: obj.emoji,
        },
      };
    }

    default:
      return { ok: false, reason: `Unsupported message type: ${type}` };
  }
}
