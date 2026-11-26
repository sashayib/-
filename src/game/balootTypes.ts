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

export interface BalootSeat {
  seatIndex: number;
  uid: string;
  name: string;
  rating: number;
  isBot: boolean;
  avatarUrl?: string | null;
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

export type TableBgId =
  | 'classic_white'
  | 'classic_green'
  | 'saudi_gold'
  | 'saudi_royal_dark_gold'
  | 'saudi_green';

export interface TableBgMeta {
  id: TableBgId;
  title: string;
  subtitle: string;
  priceSar: number;
  isLight: boolean;
  isSaudiPremium: boolean;
}

export const TABLE_BACKGROUNDS: TableBgMeta[] = [
  {
    id: 'classic_white',
    title: 'خلفية شدها البيضاء',
    subtitle: 'خلفية بيضاء ناصعة بشعار شدها الكلاسيكي',
    priceSar: 0,
    isLight: true,
    isSaudiPremium: false,
  },
  {
    id: 'classic_green',
    title: 'خلفية المجلس الخضراء',
    subtitle: 'بساط بلوت أخضر تقليدي بشعار شدها',
    priceSar: 0,
    isLight: false,
    isSaudiPremium: false,
  },
  {
    id: 'saudi_gold',
    title: 'الخلفية الذهبية السعودية',
    subtitle: 'لون ذهبي ملكي مع شعار السيفين والنخلة الأبيض',
    priceSar: 20,
    isLight: false,
    isSaudiPremium: true,
  },
  {
    id: 'saudi_royal_dark_gold',
    title: 'خلفية المملكة الذهبية',
    subtitle: 'أخضر ملكي بخطوط بيضاء وشعار السيفين والنخلة الذهبي',
    priceSar: 20,
    isLight: false,
    isSaudiPremium: true,
  },
  {
    id: 'saudi_green',
    title: 'الخلفية الخضراء السعودية',
    subtitle: 'أخضر سعودي أصيل مع شعار السيفين والنخلة الأبيض',
    priceSar: 20,
    isLight: false,
    isSaudiPremium: true,
  },
];

export type SubscriptionPlanId = 'none' | 'weekly' | 'monthly' | 'yearly';

export interface SubscriptionPlanMeta {
  id: Exclude<SubscriptionPlanId, 'none'>;
  title: string;
  priceText: string;
  periodText: string;
  badge?: string;
  perks: string[];
}

export const SUBSCRIPTION_PLANS: SubscriptionPlanMeta[] = [
  {
    id: 'weekly',
    title: 'الباقة الأسبوعية',
    priceText: '9.99 ريال',
    periodText: 'أسبوعياً',
    perks: ['إنشاء جلسات أونلاين بلا حدود يومياً', 'أولوية الدخول للطاولات العامة', 'شارة مشترك شدها بلس'],
  },
  {
    id: 'monthly',
    title: 'الباقة الشهرية',
    priceText: '29 ريال',
    periodText: 'شهرياً',
    badge: 'الأكثر اختياراً',
    perks: ['إنشاء جلسات أونلاين بلا حدود يومياً', 'دعوة الأصدقاء لطاولات خاصة بلا قيود', 'تحديثات ومزايا حصرية مبكرة'],
  },
  {
    id: 'yearly',
    title: 'الباقة السنوية',
    priceText: '149 ريال',
    periodText: 'سنوياً',
    badge: 'أفضل توفير',
    perks: ['توفير 57% مقارنة بالباقة الشهرية', 'إنشاء جلسات أونلاين بلا حدود طوال العام', 'دعم أولوية ومزايا شدها بلس الكاملة'],
  },
];

export const FREE_DAILY_SESSION_LIMIT = 2;
export const PURCHASES_ENABLED = false;

export const AVATAR_PRESETS = [
  { id: '/assets/shaddaha_avatar.png', label: 'شخصية شدها' },
  { id: '/assets/avatar_preset_1.png', label: 'شخصية 1' },
  { id: '/assets/avatar_preset_2.png', label: 'شخصية 2' },
  { id: '/assets/avatar_preset_3.png', label: 'شخصية 3' },
  { id: '/assets/avatar_preset_4.png', label: 'شخصية 4' },
  { id: '/assets/avatar_preset_5.png', label: 'شخصية 5' },
  { id: '/assets/avatar_preset_6.png', label: 'شخصية 6' },
];

export interface PlayerProfile {
  userId: string;
  displayName: string;
  avatarUrl: string | null;
  playerCode: string;
  presenceStatus: 'online' | 'offline' | 'in_game';
  lastSeenAtMs?: number;
  visibility: 'public';
  level: number;
  xp: number;
  rating: number;
  rankTier: string;
  wins: number;
  losses: number;
  matchesPlayed: number;
  streak: number;
  lossStreak: number;
  totalPoints: number;
  lastMatchId?: string;
}

export interface UserSettings {
  userId: string;
  selectedTableBg: TableBgId;
  ownedTableBgs: TableBgId[];
  subscriptionPlan: SubscriptionPlanId;
  dailySessionsDate: string;
  dailySessionsCount: number;
}

export interface FriendEntry {
  userId: string;
  friendUid: string;
  friendPlayerCode: string;
  friendName: string;
  friendRating: number;
}

export interface TableInvite {
  id: string;
  fromUid: string;
  fromName: string;
  fromPlayerCode: string;
  toUid: string;
  toName: string;
  roomCode: string;
  targetScore: number;
  status: 'pending' | 'accepted' | 'declined' | 'cancelled';
}

export interface GameRoomDoc {
  roomCode: string;
  hostId: string;
  roomType: 'public' | 'private';
  status: 'waiting' | 'playing' | 'finished';
  targetScore: number;
  memberUids: string[];
  seat0Uid: string;
  seat1Uid: string;
  seat2Uid: string;
  seat3Uid: string;
  seat0Name: string;
  seat1Name: string;
  seat2Name: string;
  seat3Name: string;
  seat0Rating: number;
  seat1Rating: number;
  seat2Rating: number;
  seat3Rating: number;
  usScore: number;
  themScore: number;
  roundNumber: number;
  turnIndex: number;
  dealerIndex: number;
  gamePhase: GamePhase;
  gameMode: GameMode;
  trumpSuit: Suit | 'none';
  bidderIndex: number;
  bidLevel: number;
  openCard: string;
  hands: Record<string, string[]>;
  tableCards: string[];
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
  biddingPasses?: number;
  highestBidSeat?: number;
  highestBidMode?: GameMode;
  highestBidSuit?: Suit | 'none';
  standbySeats?: number[];
  declaredProjectSeats?: number[];
  createdAtMs?: number;
}

export interface RoomMessage {
  id: string;
  senderUid: string;
  senderName: string;
  seatIndex: number;
  text: string;
  createdAtMs: number;
}

export interface MatchRecord {
  id: string;
  userId: string;
  roomCode: string | null;
  mode: 'online' | 'solo';
  usScore: number;
  themScore: number;
  didWin: boolean;
  ratingDelta: number;
  opponentSummary: string;
  createdAtMs: number;
}
