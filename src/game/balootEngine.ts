import {
  BalootCard,
  BalootSeat,
  BalootTableState,
  DetectedProject,
  GameMode,
  PlayedCardEntry,
  Rank,
  Suit,
} from './balootTypes';

export const SUITS: Suit[] = ['spades', 'hearts', 'clubs', 'diamonds'];
export const RANKS: Rank[] = ['7', '8', '9', '10', 'J', 'Q', 'K', 'A'];

export const SUIT_INFO: Record<
  Suit,
  { symbol: string; arabic: string; colorClass: string; hex: string }
> = {
  spades: { symbol: '♠', arabic: 'سبيت', colorClass: 'text-[#111827]', hex: '#111827' },
  hearts: { symbol: '♥', arabic: 'هاص', colorClass: 'text-[#E52521]', hex: '#E52521' },
  clubs: { symbol: '♣', arabic: 'شريا', colorClass: 'text-[#111827]', hex: '#111827' },
  diamonds: { symbol: '♦', arabic: 'ديمن', colorClass: 'text-[#E52521]', hex: '#E52521' },
};

const SUN_ORDER: Record<Rank, number> = {
  '7': 0,
  '8': 1,
  '9': 2,
  J: 3,
  Q: 4,
  K: 5,
  '10': 6,
  A: 7,
};

const HOKM_ORDER: Record<Rank, number> = {
  '7': 0,
  '8': 1,
  Q: 2,
  K: 3,
  '10': 4,
  A: 5,
  '9': 6,
  J: 7,
};

const SEQ_ORDER: Record<Rank, number> = {
  '7': 0,
  '8': 1,
  '9': 2,
  '10': 3,
  J: 4,
  Q: 5,
  K: 6,
  A: 7,
};

const SUN_POINTS: Record<Rank, number> = {
  '7': 0,
  '8': 0,
  '9': 0,
  J: 2,
  Q: 3,
  K: 4,
  '10': 10,
  A: 11,
};

const HOKM_POINTS: Record<Rank, number> = {
  '7': 0,
  '8': 0,
  Q: 3,
  K: 4,
  '10': 10,
  A: 11,
  '9': 14,
  J: 20,
};

export const QUICK_REACTIONS = ['😂', '🔥', '👏', '😎', '🤩', '😮', '😅', '😢', '😡', '👍', '❤️', '👑'];

export const QUICK_CHAT_PHRASES = [
  'كفو يا خوي!',
  'أبشر بعزك',
  'ارمِ حكم!',
  'صن يا وحش!',
  'الله يعين!',
  'سلمت يدك',
  'لعيب والله',
  'شد حيلك!',
];

export const BOT_NAMES = ['أبو فهد', 'سعود', 'نواف', 'تركي', 'فيصل', 'خالد', 'بندر', 'ماجد'];

export function cardCode(card: BalootCard): string {
  const s =
    card.suit === 'spades' ? 'S' : card.suit === 'hearts' ? 'H' : card.suit === 'diamonds' ? 'D' : 'C';
  return `${card.rank}${s}`;
}

export function parseCardCode(code: string): BalootCard | null {
  if (!code || code.length < 2) return null;
  const sChar = code.slice(-1).toUpperCase();
  const rStr = code.slice(0, -1).toUpperCase() as Rank;
  const suit: Suit | null =
    sChar === 'S'
      ? 'spades'
      : sChar === 'H'
        ? 'hearts'
        : sChar === 'D'
          ? 'diamonds'
          : sChar === 'C'
            ? 'clubs'
            : null;
  if (!suit || !RANKS.includes(rStr)) return null;
  return { rank: rStr, suit };
}

export function encodeTableCard(entry: PlayedCardEntry): string {
  return `${entry.seatIndex}:${cardCode(entry.card)}`;
}

export function decodeTableCard(raw: string): PlayedCardEntry | null {
  const [seatStr, cStr] = raw.split(':');
  const seatIndex = Number(seatStr);
  const card = parseCardCode(cStr || '');
  if (!Number.isInteger(seatIndex) || seatIndex < 0 || seatIndex > 3 || !card) return null;
  return { seatIndex, card };
}

export function rankTierForRating(rating: number): string {
  if (rating >= 18000) return 'ملك البلوت';
  if (rating >= 16000) return 'زعيم';
  if (rating >= 14000) return 'نوخذة';
  if (rating >= 12000) return 'ذيبان';
  if (rating >= 10000) return 'صنديد';
  if (rating >= 8000) return 'متمرس';
  if (rating >= 6000) return 'لاعب';
  if (rating >= 4000) return 'هاوي';
  if (rating >= 2000) return 'ناشئ';
  return 'مبتدئ';
}

export function calculateMatchPointsDelta(didWin: boolean, currentLossStreak: number): number {
  if (didWin) return 22;
  const consecutiveLosses = Math.max(1, currentLossStreak + 1);
  return -(7 * consecutiveLosses);
}

export function generatePlayerCode(uid: string): string {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let hash = 2166136261;
  for (let i = 0; i < uid.length; i++) {
    hash ^= uid.charCodeAt(i);
    hash = Math.imul(hash, 16777619);
  }
  let code = 'SHD';
  let v = Math.abs(hash);
  for (let i = 0; i < 5; i++) {
    code += alphabet[v % alphabet.length];
    v = Math.floor(v / alphabet.length) + (uid.charCodeAt(i % uid.length) || 7) * (i + 3);
  }
  return code;
}

export function generateRoomCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let out = '';
  for (let i = 0; i < 6; i++) {
    out += chars[Math.floor(Math.random() * chars.length)];
  }
  return out;
}

export function fullDeck(): BalootCard[] {
  const cards: BalootCard[] = [];
  for (const suit of SUITS) {
    for (const rank of RANKS) {
      cards.push({ rank, suit });
    }
  }
  return cards;
}

function shuffle<T>(arr: T[]): T[] {
  const copy = [...arr];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

export function cardStrength(card: BalootCard, mode: GameMode, trumpSuit: Suit | null): number {
  if (mode === 'hokm' && card.suit === trumpSuit) return HOKM_ORDER[card.rank];
  return SUN_ORDER[card.rank];
}

export function cardPoints(card: BalootCard, mode: GameMode, trumpSuit: Suit | null): number {
  if (mode === 'hokm' && card.suit === trumpSuit) return HOKM_POINTS[card.rank];
  return SUN_POINTS[card.rank];
}

export function sortHand(hand: BalootCard[], mode: GameMode, trumpSuit: Suit | null): BalootCard[] {
  const order: Record<Suit, number> = { spades: 0, hearts: 1, clubs: 2, diamonds: 3 };
  return [...hand].sort((a, b) => {
    const sa = mode === 'hokm' && a.suit === trumpSuit ? -1 : order[a.suit];
    const sb = mode === 'hokm' && b.suit === trumpSuit ? -1 : order[b.suit];
    if (sa !== sb) return sa - sb;
    return cardStrength(b, mode, trumpSuit) - cardStrength(a, mode, trumpSuit);
  });
}

export function dealNewRound(params: {
  dealerIndex: number;
  roundNumber: number;
  seats: BalootSeat[];
  existingUsScore: number;
  existingThemScore: number;
  roomCode: string | null;
  isOnline: boolean;
  hostId: string;
  mySeatIndex: number;
}): BalootTableState {
  const deck = shuffle(fullDeck());
  const hands: Record<number, BalootCard[]> = {};
  for (let seat = 0; seat < 4; seat++) {
    hands[seat] = sortHand(deck.slice(seat * 5, seat * 5 + 5), 'sun', null);
  }
  const openCard = deck[20];
  const firstTurn = (params.dealerIndex + 1) % 4;
  return {
    roomCode: params.roomCode,
    isOnline: params.isOnline,
    hostId: params.hostId,
    targetScore: 152,
    mySeatIndex: params.mySeatIndex,
    seats: params.seats,
    usScore: params.existingUsScore,
    themScore: params.existingThemScore,
    roundNumber: params.roundNumber,
    turnIndex: firstTurn,
    dealerIndex: params.dealerIndex,
    phase: 'bidding_r1',
    mode: 'none',
    trumpSuit: null,
    bidderIndex: -1,
    bidLevel: 1,
    openCard,
    hands,
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
    lastActionText: `توزيع الصكة ${params.roundNumber} — الورقة المكشوفة ${openCard.rank}${SUIT_INFO[openCard.suit].symbol}`,
    biddingPasses: 0,
    highestBidSeat: -1,
    highestBidMode: 'none',
    highestBidSuit: null,
    standbySeats: [],
    declaredProjectSeats: [],
  };
}

export function detectProjectsForHand(
  hand: BalootCard[],
  seatIndex: number,
  mode: GameMode,
  trumpSuit: Suit | null,
): DetectedProject[] {
  const result: DetectedProject[] = [];
  if (mode === 'hokm' && trumpSuit) {
    const hasK = hand.some((c) => c.suit === trumpSuit && c.rank === 'K');
    const hasQ = hand.some((c) => c.suit === trumpSuit && c.rank === 'Q');
    if (hasK && hasQ) {
      result.push({
        id: `baloot_${seatIndex}`,
        kind: 'baloot',
        seatIndex,
        description: 'بلوت',
        abnatSun: 0,
        abnatHokm: 2,
      });
    }
  }

  // Four of a kind
  const byRank = new Map<Rank, BalootCard[]>();
  for (const c of hand) {
    const list = byRank.get(c.rank) || [];
    list.push(c);
    byRank.set(c.rank, list);
  }
  for (const [rank, cards] of byRank.entries()) {
    if (cards.length === 4) {
      if (rank === 'A') {
        if (mode === 'sun') {
          result.push({
            id: `400_${seatIndex}_${rank}`,
            kind: 'four_hundred',
            seatIndex,
            description: 'أربعمية',
            abnatSun: 40,
            abnatHokm: 20,
          });
        } else {
          result.push({
            id: `100_${seatIndex}_${rank}`,
            kind: 'hundred',
            seatIndex,
            description: 'مية (إكة)',
            abnatSun: 20,
            abnatHokm: 10,
          });
        }
      } else if (rank === 'K' || rank === 'Q' || rank === 'J' || rank === '10') {
        result.push({
          id: `100_${seatIndex}_${rank}`,
          kind: 'hundred',
          seatIndex,
          description: `مية (${rank})`,
          abnatSun: 20,
          abnatHokm: 10,
        });
      }
    }
  }

  // Sequences (3+ consecutive same suit)
  for (const suit of SUITS) {
    const suitCards = hand
      .filter((c) => c.suit === suit)
      .sort((a, b) => SEQ_ORDER[a.rank] - SEQ_ORDER[b.rank]);
    if (suitCards.length < 3) continue;
    let runLen = 1;
    let runIdx = 0;
    const flushRun = (len: number) => {
      if (len >= 5) {
        result.push({
          id: `100_seq_${seatIndex}_${suit}_${runIdx++}`,
          kind: 'hundred',
          seatIndex,
          description: 'مية (تسلسل ٥)',
          abnatSun: 20,
          abnatHokm: 10,
        });
      } else if (len === 4) {
        result.push({
          id: `50_${seatIndex}_${suit}_${runIdx++}`,
          kind: 'fifty',
          seatIndex,
          description: 'خمسين',
          abnatSun: 10,
          abnatHokm: 5,
        });
      } else if (len === 3) {
        result.push({
          id: `sira_${seatIndex}_${suit}_${runIdx++}`,
          kind: 'sira',
          seatIndex,
          description: 'سرا',
          abnatSun: 4,
          abnatHokm: 2,
        });
      }
    };
    for (let i = 1; i < suitCards.length; i++) {
      if (SEQ_ORDER[suitCards[i].rank] === SEQ_ORDER[suitCards[i - 1].rank] + 1) {
        runLen++;
      } else {
        flushRun(runLen);
        runLen = 1;
      }
    }
    flushRun(runLen);
  }
  return result;
}

export function detectDeclarableProjectsForHand(
  hand: BalootCard[],
  seatIndex: number,
  mode: GameMode,
  trumpSuit: Suit | null,
): DetectedProject[] {
  return detectProjectsForHand(hand, seatIndex, mode, trumpSuit).filter((p) => p.kind !== 'baloot');
}

export function projectAbnat(desc: string, mode: GameMode): number {
  if (desc.startsWith('أربعمية')) return mode === 'sun' ? 40 : 20;
  if (desc.startsWith('مية')) return mode === 'sun' ? 20 : 10;
  if (desc.startsWith('خمسين')) return mode === 'sun' ? 10 : 5;
  if (desc.startsWith('سرا')) return mode === 'sun' ? 4 : 2;
  if (desc === 'بلوت') return mode === 'hokm' ? 2 : 0;
  return 0;
}

export function completeDealAfterBid(
  state: BalootTableState,
  buyerSeat: number,
  mode: GameMode,
  trumpSuit: Suit | null,
): BalootTableState {
  const dealtCodes = new Set<string>();
  for (let s = 0; s < 4; s++) {
    for (const c of state.hands[s] || []) dealtCodes.add(cardCode(c));
  }
  if (state.openCard) dealtCodes.add(cardCode(state.openCard));
  const remaining = shuffle(fullDeck().filter((c) => !dealtCodes.has(cardCode(c))));
  let cursor = 0;
  const newHands: Record<number, BalootCard[]> = {};
  for (let seat = 0; seat < 4; seat++) {
    const existing = [...(state.hands[seat] || [])];
    if (seat === buyerSeat) {
      if (state.openCard) existing.push(state.openCard);
      for (let i = 0; i < 2 && cursor < remaining.length; i++) existing.push(remaining[cursor++]);
    } else {
      for (let i = 0; i < 3 && cursor < remaining.length; i++) existing.push(remaining[cursor++]);
    }
    newHands[seat] = sortHand(existing, mode, trumpSuit);
  }

  // Auto-declare projects for bots (and Baloot K+Q); human players pick Sira/50/100/400 before playing
  const autoProjects: DetectedProject[] = [];
  for (let seat = 0; seat < 4; seat++) {
    const detected = detectProjectsForHand(newHands[seat], seat, mode, trumpSuit);
    const isBot = state.seats[seat]?.isBot ?? false;
    if (isBot) {
      autoProjects.push(...detected);
    } else {
      autoProjects.push(...detected.filter((p) => p.kind === 'baloot'));
    }
  }
  const usProj = autoProjects
    .filter((p) => p.seatIndex % 2 === 0)
    .map((p) => p.description)
    .slice(0, 8);
  const themProj = autoProjects
    .filter((p) => p.seatIndex % 2 === 1)
    .map((p) => p.description)
    .slice(0, 8);

  const buyerName = state.seats[buyerSeat]?.name || 'لاعب';
  const contractDesc = mode === 'sun' ? 'صن' : `حكم ${trumpSuit ? SUIT_INFO[trumpSuit].arabic : ''}`;
  const firstDefender = (buyerSeat + 1) % 4;

  return {
    ...state,
    phase: 'doubling',
    mode,
    trumpSuit,
    bidderIndex: buyerSeat,
    bidLevel: 1,
    openCard: null,
    hands: newHands,
    usProjects: usProj,
    themProjects: themProj,
    turnIndex: firstDefender,
    lastActionText: `${buyerName} اشترى ${contractDesc}`,
    declaredProjectSeats: [],
  };
}

export type BidAction = 'sun' | 'hokm' | 'pass';

export function applyBidDecision(
  state: BalootTableState,
  seatIndex: number,
  choice: BidAction,
  chosenSuit?: Suit | null,
): { nextState: BalootTableState; speechText: string; redealt?: boolean } {
  const playerName = state.seats[seatIndex]?.name || 'لاعب';
  const isR2 = state.phase === 'bidding_r2';

  if (choice === 'sun') {
    const completed = completeDealAfterBid(state, seatIndex, 'sun', null);
    return {
      nextState: { ...completed, lastActionText: `${playerName}: صن!` },
      speechText: 'صن',
    };
  }

  if (choice === 'hokm') {
    const trump = isR2 ? chosenSuit || SUITS.find((s) => s !== state.openCard?.suit) || 'spades' : state.openCard?.suit || 'spades';
    if (!isR2) {
      // In Round 1 Hokm, remember highest bid; remaining players may still call Sun or pass
      const nextTurn = (seatIndex + 1) % 4;
      const passes = state.biddingPasses + 1;
      if (passes >= 4) {
        const completed = completeDealAfterBid(state, seatIndex, 'hokm', trump);
        return {
          nextState: {
            ...completed,
            lastActionText: `${playerName} اشترى حكم ${SUIT_INFO[trump].arabic}`,
          },
          speechText: `حكم ${SUIT_INFO[trump].arabic}`,
        };
      }
      return {
        nextState: {
          ...state,
          turnIndex: nextTurn,
          biddingPasses: passes,
          highestBidSeat: seatIndex,
          highestBidMode: 'hokm',
          highestBidSuit: trump,
          lastActionText: `${playerName}: حكم ${SUIT_INFO[trump].arabic}`,
        },
        speechText: `حكم ${SUIT_INFO[trump].arabic}`,
      };
    } else {
      // Round 2 Hokm locks immediately
      const completed = completeDealAfterBid(state, seatIndex, 'hokm', trump);
      return {
        nextState: {
          ...completed,
          lastActionText: `${playerName} اشترى حكم ${SUIT_INFO[trump].arabic}`,
        },
        speechText: `حكم ${SUIT_INFO[trump].arabic}`,
      };
    }
  }

  // Pass ("بس" in R1, "ولا" in R2)
  const passWord = isR2 ? 'ولا' : 'بس';
  const newPasses = state.biddingPasses + 1;

  if (!isR2 && newPasses >= 4) {
    if (state.highestBidSeat >= 0 && state.highestBidMode === 'hokm') {
      const completed = completeDealAfterBid(
        state,
        state.highestBidSeat,
        'hokm',
        state.highestBidSuit,
      );
      return {
        nextState: completed,
        speechText: passWord,
      };
    }
    // Move to Round 2 bidding
    return {
      nextState: {
        ...state,
        phase: 'bidding_r2',
        turnIndex: (state.dealerIndex + 1) % 4,
        biddingPasses: 0,
        lastActionText: `${playerName}: بس — بدأت الجولة الثانية للمشترى`,
      },
      speechText: passWord,
    };
  }

  if (isR2 && newPasses >= 4) {
    // Everyone passed in Round 2 -> Redeal with next dealer!
    const nextDealer = (state.dealerIndex + 1) % 4;
    const redeal = dealNewRound({
      dealerIndex: nextDealer,
      roundNumber: state.roundNumber,
      seats: state.seats,
      existingUsScore: state.usScore,
      existingThemScore: state.themScore,
      roomCode: state.roomCode,
      isOnline: state.isOnline,
      hostId: state.hostId,
      mySeatIndex: state.mySeatIndex,
    });
    return {
      nextState: {
        ...redeal,
        lastActionText: 'الجميع قال ولا — إعادة توزيع الورق',
      },
      speechText: 'ولا، إعادة توزيع',
      redealt: true,
    };
  }

  return {
    nextState: {
      ...state,
      turnIndex: (seatIndex + 1) % 4,
      biddingPasses: newPasses,
      lastActionText: `${playerName}: ${passWord}`,
    },
    speechText: passWord,
  };
}

export function applyDoublingDecision(
  state: BalootTableState,
  seatIndex: number,
  raiseDouble: boolean,
): { nextState: BalootTableState; speechText: string } {
  const playerName = state.seats[seatIndex]?.name || 'لاعب';
  const firstPlayer = (state.dealerIndex + 1) % 4;

  if (!raiseDouble) {
    return {
      nextState: {
        ...state,
        phase: 'playing',
        turnIndex: firstPlayer,
        lastActionText: `${playerName}: فتح اللعب`,
      },
      speechText: 'بسم الله',
    };
  }

  const nextLevel = Math.min(4, state.bidLevel + 1);
  const label = nextLevel === 2 ? 'دبل' : nextLevel === 3 ? 'ثري' : 'قهوة';

  if (nextLevel >= 4) {
    return {
      nextState: {
        ...state,
        bidLevel: 4,
        phase: 'playing',
        turnIndex: firstPlayer,
        lastActionText: `${playerName}: قهوة! الصكة على الكل`,
      },
      speechText: 'قهوة',
    };
  }

  // Pass doubling counter-offer to opponent team
  const nextCounterSeat = (seatIndex + 1) % 4;
  return {
    nextState: {
      ...state,
      bidLevel: nextLevel,
      turnIndex: nextCounterSeat,
      lastActionText: `${playerName}: ${label}!`,
    },
    speechText: label,
  };
}

export function determineTrickWinner(
  tableCards: PlayedCardEntry[],
  mode: GameMode,
  trumpSuit: Suit | null,
): number {
  if (tableCards.length === 0) return -1;
  const ledSuit = tableCards[0].card.suit;
  if (mode === 'hokm' && trumpSuit) {
    const trumps = tableCards.filter((e) => e.card.suit === trumpSuit);
    if (trumps.length > 0) {
      return trumps.reduce((best, cur) =>
        HOKM_ORDER[cur.card.rank] > HOKM_ORDER[best.card.rank] ? cur : best,
      ).seatIndex;
    }
  }
  const ledCards = tableCards.filter((e) => e.card.suit === ledSuit);
  return ledCards.reduce((best, cur) =>
    SUN_ORDER[cur.card.rank] > SUN_ORDER[best.card.rank] ? cur : best,
  ).seatIndex;
}

export function getLegalCards(
  hand: BalootCard[],
  tableCards: PlayedCardEntry[],
  mode: GameMode,
  trumpSuit: Suit | null,
  seatIndex: number,
): BalootCard[] {
  if (tableCards.length === 0) return hand;
  const ledSuit = tableCards[0].card.suit;
  const sameSuit = hand.filter((c) => c.suit === ledSuit);
  if (sameSuit.length > 0) {
    if (mode === 'hokm' && ledSuit === trumpSuit) {
      const highestTrump = Math.max(
        -1,
        ...tableCards.filter((e) => e.card.suit === trumpSuit).map((e) => HOKM_ORDER[e.card.rank]),
      );
      const higher = sameSuit.filter((c) => HOKM_ORDER[c.rank] > highestTrump);
      if (higher.length > 0) return higher;
    }
    return sameSuit;
  }

  if (mode === 'hokm' && trumpSuit) {
    const currentWinner = determineTrickWinner(tableCards, mode, trumpSuit);
    const partnerWinning = currentWinner >= 0 && currentWinner % 2 === seatIndex % 2;
    const trumps = hand.filter((c) => c.suit === trumpSuit);
    if (!partnerWinning && trumps.length > 0) {
      const highestTrump = Math.max(
        -1,
        ...tableCards.filter((e) => e.card.suit === trumpSuit).map((e) => HOKM_ORDER[e.card.rank]),
      );
      const higher = trumps.filter((c) => HOKM_ORDER[c.rank] > highestTrump);
      return higher.length > 0 ? higher : trumps;
    }
  }
  return hand;
}

function convertRawToAbnat(rawPoints: number, mode: GameMode): number {
  if (mode === 'sun') {
    return Math.floor((rawPoints * 2 + 5) / 10);
  }
  return Math.floor((rawPoints + 5) / 10);
}

export function applyCardPlay(
  state: BalootTableState,
  seatIndex: number,
  card: BalootCard,
): BalootTableState {
  const hand = state.hands[seatIndex] || [];
  const updatedHand = hand.filter((c) => !(c.rank === card.rank && c.suit === card.suit));
  const newHands = { ...state.hands, [seatIndex]: updatedHand };
  const newTable = [...state.tableCards, { seatIndex, card }];
  const playerName = state.seats[seatIndex]?.name || 'لاعب';

  if (newTable.length < 4) {
    return {
      ...state,
      hands: newHands,
      tableCards: newTable,
      turnIndex: (seatIndex + 1) % 4,
      lastActionText: `${playerName} رمى ${card.rank}${SUIT_INFO[card.suit].symbol}`,
    };
  }

  // Trick complete
  const winnerSeat = determineTrickWinner(newTable, state.mode, state.trumpSuit);
  const trickRawPoints = newTable.reduce(
    (sum, e) => sum + cardPoints(e.card, state.mode, state.trumpSuit),
    0,
  );
  const isLastTrick = [0, 1, 2, 3].every((s) => (newHands[s]?.length ?? 0) === 0);
  const bonusLast = isLastTrick ? 10 : 0;
  const winnerTeam = winnerSeat % 2;
  const newUsPoints = state.usRoundPoints + (winnerTeam === 0 ? trickRawPoints + bonusLast : 0);
  const newThemPoints = state.themRoundPoints + (winnerTeam === 1 ? trickRawPoints + bonusLast : 0);
  const newUsTricks = state.usTricksCount + (winnerTeam === 0 ? 1 : 0);
  const newThemTricks = state.themTricksCount + (winnerTeam === 1 ? 1 : 0);
  const winnerName = state.seats[winnerSeat]?.name || 'الفائز';

  if (!isLastTrick) {
    return {
      ...state,
      hands: newHands,
      tableCards: [],
      lastTrickCards: newTable,
      lastTrickWinner: winnerSeat,
      turnIndex: winnerSeat,
      usRoundPoints: newUsPoints,
      themRoundPoints: newThemPoints,
      usTricksCount: newUsTricks,
      themTricksCount: newThemTricks,
      lastActionText: `${winnerName} أكل اللفة (+${trickRawPoints})`,
    };
  }

  // Round complete
  const buyingTeam = state.bidderIndex >= 0 ? state.bidderIndex % 2 : 0;
  const usBaloot = state.usProjects.includes('بلوت') && state.mode === 'hokm' ? 2 : 0;
  const themBaloot = state.themProjects.includes('بلوت') && state.mode === 'hokm' ? 2 : 0;
  const usOtherProj = state.usProjects
    .filter((p) => p !== 'بلوت')
    .reduce((sum, p) => sum + projectAbnat(p, state.mode), 0);
  const themOtherProj = state.themProjects
    .filter((p) => p !== 'بلوت')
    .reduce((sum, p) => sum + projectAbnat(p, state.mode), 0);

  const isKabootUs = newUsTricks === 8;
  const isKabootThem = newThemTricks === 8;
  const baseTotalAbnat = state.mode === 'sun' ? 26 : 16;

  let usAbnat = 0;
  let themAbnat = 0;
  let isKhassara = false;

  if (isKabootUs) {
    usAbnat = (state.mode === 'sun' ? 44 : 25) + usOtherProj + usBaloot;
    themAbnat = themBaloot;
  } else if (isKabootThem) {
    themAbnat = (state.mode === 'sun' ? 44 : 25) + themOtherProj + themBaloot;
    usAbnat = usBaloot;
  } else {
    const usCardAbnat = convertRawToAbnat(newUsPoints, state.mode);
    const themCardAbnat = baseTotalAbnat - usCardAbnat;
    const usTotal = usCardAbnat + usOtherProj;
    const themTotal = themCardAbnat + themOtherProj;

    if (buyingTeam === 0 && usTotal < themTotal) {
      isKhassara = true;
      usAbnat = usBaloot;
      themAbnat = (baseTotalAbnat + usOtherProj + themOtherProj) * state.bidLevel + themBaloot;
    } else if (buyingTeam === 1 && themTotal < usTotal) {
      isKhassara = true;
      themAbnat = themBaloot;
      usAbnat = (baseTotalAbnat + usOtherProj + themOtherProj) * state.bidLevel + usBaloot;
    } else {
      usAbnat = (usCardAbnat + usOtherProj) * state.bidLevel + usBaloot;
      themAbnat = (themCardAbnat + themOtherProj) * state.bidLevel + themBaloot;
    }
  }

  if (state.bidLevel >= 4 && !isKabootUs && !isKabootThem) {
    if (usAbnat > themAbnat) usAbnat = 152;
    else themAbnat = 152;
  }

  const updatedUsScore = state.usScore + usAbnat;
  const updatedThemScore = state.themScore + themAbnat;
  const matchFinished = updatedUsScore >= 152 || updatedThemScore >= 152;
  const finalWinner = !matchFinished ? -1 : updatedUsScore >= updatedThemScore ? 0 : 1;

  return {
    ...state,
    hands: newHands,
    tableCards: [],
    lastTrickCards: newTable,
    lastTrickWinner: winnerSeat,
    turnIndex: winnerSeat,
    usRoundPoints: newUsPoints,
    themRoundPoints: newThemPoints,
    usTricksCount: newUsTricks,
    themTricksCount: newThemTricks,
    usScore: updatedUsScore,
    themScore: updatedThemScore,
    phase: matchFinished ? 'match_finished' : 'round_summary',
    winnerTeam: finalWinner,
    standbySeats: [],
    lastActionText: isKabootUs
      ? `كبوت لصالح فريقنا! (+${usAbnat})`
      : isKabootThem
        ? `كبوت لصالح الخصم! (+${themAbnat})`
        : isKhassara
          ? 'خسارة المشتري!'
          : `انتهت الجولة: لنا +${usAbnat} | لهم +${themAbnat}`,
  };
}

// --- Smart AI Bots ---
export function chooseBotBid(
  hand: BalootCard[],
  openCard: BalootCard | null,
  isRound2: boolean,
  currentHighestMode: GameMode,
): { choice: BidAction; suit?: Suit } {
  const sunPower = hand.reduce((sum, c) => {
    if (c.rank === 'A') return sum + 3;
    if (c.rank === '10') return sum + 2;
    if (c.rank === 'K') return sum + 1;
    return sum;
  }, 0);
  if (sunPower >= 6 && currentHighestMode !== 'sun') {
    return { choice: 'sun' };
  }
  if (!isRound2 && openCard && currentHighestMode === 'none') {
    const openCount = hand.filter((c) => c.suit === openCard.suit).length;
    const hasJ9 = hand.some((c) => c.suit === openCard.suit && (c.rank === 'J' || c.rank === '9'));
    if (openCount >= 2 || (hasJ9 && openCount >= 1) || openCard.rank === 'J') {
      return { choice: 'hokm', suit: openCard.suit };
    }
  }
  if (isRound2 && currentHighestMode === 'none') {
    let bestSuit: Suit | null = null;
    let bestScore = -1;
    for (const s of SUITS) {
      if (s === openCard?.suit) continue;
      const sc = hand.filter((c) => c.suit === s);
      const score = sc.reduce((acc, c) => acc + HOKM_ORDER[c.rank] + 2, 0);
      if (score > bestScore) {
        bestScore = score;
        bestSuit = s;
      }
    }
    if (bestSuit) {
      const count = hand.filter((c) => c.suit === bestSuit).length;
      const hasStrong = hand.some(
        (c) => c.suit === bestSuit && (c.rank === 'J' || c.rank === '9' || c.rank === 'A'),
      );
      if (count >= 3 || (count >= 2 && hasStrong)) {
        return { choice: 'hokm', suit: bestSuit };
      }
    }
  }
  return { choice: 'pass' };
}

export function chooseBotDoubling(hand: BalootCard[], mode: GameMode, trumpSuit: Suit | null): boolean {
  const power =
    mode === 'sun'
      ? hand.filter((c) => c.rank === 'A' || c.rank === '10').length
      : hand.filter(
          (c) => c.suit === trumpSuit && (c.rank === 'J' || c.rank === '9' || c.rank === 'A'),
        ).length;
  return power >= 4;
}

export function chooseBotCard(
  hand: BalootCard[],
  tableCards: PlayedCardEntry[],
  mode: GameMode,
  trumpSuit: Suit | null,
  seatIndex: number,
): BalootCard {
  const legal = getLegalCards(hand, tableCards, mode, trumpSuit, seatIndex);
  if (legal.length === 1) return legal[0];

  if (tableCards.length === 0) {
    return legal.reduce((best, c) =>
      cardStrength(c, mode, trumpSuit) > cardStrength(best, mode, trumpSuit) ? c : best,
    );
  }

  const currentWinner = determineTrickWinner(tableCards, mode, trumpSuit);
  const partnerWinning = currentWinner >= 0 && currentWinner % 2 === seatIndex % 2;

  if (partnerWinning) {
    const nonTrump = legal.filter((c) => !(mode === 'hokm' && c.suit === trumpSuit));
    const bestFeed = nonTrump.sort((a, b) => cardPoints(b, mode, trumpSuit) - cardPoints(a, mode, trumpSuit))[0];
    if (tableCards.length === 3 && bestFeed && cardPoints(bestFeed, mode, trumpSuit) >= 10) {
      return bestFeed;
    }
    return legal.reduce((low, c) =>
      cardStrength(c, mode, trumpSuit) < cardStrength(low, mode, trumpSuit) ? c : low,
    );
  }

  const winningCandidates = legal.filter((cand) => {
    const hyp = [...tableCards, { seatIndex, card: cand }];
    return determineTrickWinner(hyp, mode, trumpSuit) === seatIndex;
  });
  if (winningCandidates.length > 0) {
    return winningCandidates.reduce((low, c) =>
      cardStrength(c, mode, trumpSuit) < cardStrength(low, mode, trumpSuit) ? c : low,
    );
  }

  return legal.reduce((low, c) =>
    cardPoints(c, mode, trumpSuit) * 10 + cardStrength(c, mode, trumpSuit) <
    cardPoints(low, mode, trumpSuit) * 10 + cardStrength(low, mode, trumpSuit)
      ? c
      : low,
  );
}
