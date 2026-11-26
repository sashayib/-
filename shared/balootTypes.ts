// Shared Baloot & Multiplayer domain types (Web, Express/WS Server, Android/iOS compatible)

export type Suit = 'spades' | 'hearts' | 'diamonds' | 'clubs';
export type Rank = '7' | '8' | '9' | '10' | 'J' | 'Q' | 'K' | 'A';

export interface BalootCard {
  rank: Rank;
  suit: Suit;
}

export type GameMode = 'none' | 'sun' | 'hokm';
export type GamePhase =
  | 'lobby'
  | 'bidding_r1'
  | 'bidding_r2'
  | 'doubling'
  | 'playing'
  | 'round_summary'
  | 'match_finished';

export type ProjectKind = 'sira' | 'fifty' | 'hundred' | 'four_hundred' | 'baloot';

export interface DetectedProject {
  id: string;
  kind: ProjectKind;
  seatIndex: number;
  description: string;
  abnatSun: number;
  abnatHokm: number;
}

export interface PlayedCardEntry {
  seatIndex: number;
  card: BalootCard;
}

export type SeatConnectionStatus = 'connected' | 'disconnected' | 'standby' | 'bot' | 'empty';

export interface BalootSeat {
  seatIndex: number;
  uid: string;
  name: string;
  rating: number;
  isBot: boolean;
  avatarUrl?: string | null;
  connectionStatus?: SeatConnectionStatus;
}

export interface BalootTableState {
  roomCode: string | null;
  isOnline: boolean;
  hostId: string;
  targetScore: number; // Always 152
  mySeatIndex: number;
  seats: BalootSeat[];
  usScore: number;
  themScore: number;
  roundNumber: number;
  turnIndex: number;
  dealerIndex: number;
  phase: GamePhase;
  mode: GameMode;
  trumpSuit: Suit | null;
  bidderIndex: number;
  bidLevel: number; // 1 normal, 2 double, 3 triple, 4 qahwa
  openCard: BalootCard | null;
  hands: Record<number, BalootCard[]>;
  tableCards: PlayedCardEntry[];
  lastTrickCards: PlayedCardEntry[];
  lastTrickWinner: number;
  usTricksCount: number;
  themTricksCount: number;
  usRoundPoints: number;
  themRoundPoints: number;
  usProjects: string[];
  themProjects: string[];
  winnerTeam: number; // -1 none, 0 us, 1 them
  lastActionText: string;
  biddingPasses: number;
  highestBidSeat: number;
  highestBidMode: GameMode;
  highestBidSuit: Suit | null;
  standbySeats: number[];
  declaredProjectSeats: number[];
}

export type RoomStatus = 'waiting' | 'playing' | 'finished';
export type RoomType = 'public' | 'private';

/**
 * Public room state persisted in Firestore `rooms/{roomCode}` and broadcast to room members.
 * SECURITY INVARIANT: Never includes private player cards — only `handCounts` per seat.
 */
export interface PublicRoomState {
  roomCode: string;
  matchId: string;
  hostId: string;
  roomType: RoomType;
  status: RoomStatus;
  targetScore: 152;
  maxPlayers: 4;
  fillWithBots: boolean;
  memberUids: string[];
  seats: BalootSeat[];
  handCounts: Record<'0' | '1' | '2' | '3', number>;
  usScore: number;
  themScore: number;
  roundNumber: number;
  turnIndex: number;
  dealerIndex: number;
  phase: GamePhase;
  mode: GameMode;
  trumpSuit: Suit | null;
  bidderIndex: number;
  bidLevel: number;
  openCard: string; // Encoded cardCode (e.g. "AS") during bidding, "" otherwise
  tableCards: string[]; // Encoded "seatIndex:cardCode"
  lastTrickCards: string[];
  lastTrickWinner: number;
  usTricksCount: number;
  themTricksCount: number;
  usRoundPoints: number;
  themRoundPoints: number;
  usProjects: string[];
  themProjects: string[];
  winnerTeam: number;
  lastActionText: string;
  biddingPasses: number;
  highestBidSeat: number;
  highestBidMode: GameMode;
  highestBidSuit: Suit | null;
  standbySeats: number[];
  declaredProjectSeats: number[];
  /** Strictly increasing sequence number for optimistic concurrency & anti-replay */
  moveSeq: number;
  /** Persistent turn deadline (epoch ms) stored in Firestore — never RAM-only */
  turnDeadlineAtMs: number | null;
  /** Marks whether final match stats have been atomically committed to users/{uid} */
  resultsCommitted: boolean;
  createdAtMs: number;
  updatedAtMs: number;
}

/**
 * Secret per-player hand document stored exclusively in `rooms/{roomCode}/playerHands/{uid}`.
 * Only readable by the owning `uid` and the authoritative server.
 */
export interface PrivatePlayerHand {
  roomCode: string;
  uid: string;
  seatIndex: number;
  roundNumber: number;
  moveSeq: number;
  cards: string[]; // Encoded card codes (e.g. ["AS", "10H", ...])
  legalCards: string[];
  declarableProjects: DetectedProject[];
  updatedAtMs: number;
}

/**
 * Server-only confidential engine state stored in `rooms/{roomCode}/serverState/engine`.
 * Never readable or writable by any client (`allow read, write: if false`).
 */
export interface ServerEngineSecretDoc {
  roomCode: string;
  matchId: string;
  roundNumber: number;
  moveSeq: number;
  /** Undealt cards reserved for the second deal (+3 / +2 after bidding) */
  undealtDeck: string[];
  /** Full secret hands for all 4 seats (server-authoritative source of truth) */
  seatHands: Record<'0' | '1' | '2' | '3', string[]>;
  /** Bounded ring-buffer of recent processed client actionIds for idempotency */
  processedActionIds: string[];
  updatedAtMs: number;
}

/**
 * Reconnection pointer persisted at `users/{uid}/session/active` in Firestore.
 */
export interface UserActiveSessionDoc {
  uid: string;
  activeRoomCode: string | null;
  seatIndex: number | null;
  status: 'in_waiting_room' | 'in_match' | 'idle';
  updatedAtMs: number;
}
