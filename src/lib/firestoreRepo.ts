import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  limit,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';
import { db, handleFirestoreError, OperationType } from '../firebase';
import {
  BOT_NAMES,
  calculateMatchPointsDelta,
  cardCode,
  dealNewRound,
  decodeTableCard,
  encodeTableCard,
  generatePlayerCode,
  generateRoomCode,
  parseCardCode,
  rankTierForRating,
} from '../game/balootEngine';
import {
  BalootCard,
  BalootSeat,
  BalootTableState,
  FriendEntry,
  GameMode,
  GameRoomDoc,
  MatchRecord,
  PlayerProfile,
  RoomMessage,
  SubscriptionPlanId,
  Suit,
  TableBgId,
  TableInvite,
  UserSettings,
} from '../game/balootTypes';

export function todayDateString(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function toMillis(val: unknown): number {
  if (val instanceof Timestamp) return val.toMillis();
  if (typeof val === 'number') return val;
  return Date.now();
}

export function levelFromXp(xp: number): number {
  const safeXp = Math.max(0, Math.min(100000, Math.floor(xp)));
  return Math.floor(safeXp / 100) + 1;
}

function mapPlayerProfile(data: Record<string, unknown>, uid: string): PlayerProfile {
  const rating = Math.max(0, Math.floor(Number(data.rating ?? 22)));
  return {
    userId: String(data.userId || uid),
    displayName: String(data.displayName || 'لاعب شدها'),
    avatarUrl: typeof data.avatarUrl === 'string' ? data.avatarUrl : null,
    playerCode: String(data.playerCode || generatePlayerCode(uid)),
    presenceStatus: (data.presenceStatus as PlayerProfile['presenceStatus']) || 'online',
    lastSeenAtMs: data.lastSeenAt ? toMillis(data.lastSeenAt) : undefined,
    visibility: 'public',
    level: levelFromXp(rating),
    xp: rating,
    rating,
    rankTier: String(data.rankTier || rankTierForRating(rating)),
    wins: Number(data.wins ?? 0),
    losses: Number(data.losses ?? 0),
    matchesPlayed: Number(data.matchesPlayed ?? 0),
    streak: Number(data.streak ?? 0),
    lossStreak: Number(data.lossStreak ?? 0),
    totalPoints: Number(data.totalPoints ?? 0),
  };
}

function mapUserProfile(data: Record<string, unknown>, uid: string): PlayerProfile {
  const xp = Math.max(0, Math.min(100000, Math.floor(Number(data.xp ?? 22))));
  const level = Math.max(1, Math.floor(Number(data.level ?? levelFromXp(xp))));
  const wins = Math.max(0, Math.floor(Number(data.wins ?? 0)));
  const losses = Math.max(0, Math.floor(Number(data.losses ?? 0)));
  return {
    userId: String(data.userId || uid),
    displayName: String(data.displayName || 'لاعب شدها'),
    avatarUrl: typeof data.avatarUrl === 'string' ? data.avatarUrl : null,
    playerCode: String(data.playerCode || generatePlayerCode(uid)),
    presenceStatus: 'online',
    visibility: 'public',
    level,
    xp,
    rating: xp,
    rankTier: String(data.rankTier || rankTierForRating(xp)),
    wins,
    losses,
    matchesPlayed: Math.max(wins + losses, Math.floor(Number(data.matchesPlayed ?? wins + losses))),
    streak: Math.max(0, Math.floor(Number(data.streak ?? 0))),
    lossStreak: Math.max(0, Math.floor(Number(data.lossStreak ?? 0))),
    totalPoints: Math.max(0, Math.floor(Number(data.totalPoints ?? 0))),
    lastMatchId: typeof data.lastMatchId === 'string' ? data.lastMatchId : '',
  };
}

export async function ensurePlayerProfile(
  uid: string,
  defaultName: string,
  avatarUrl: string | null,
): Promise<PlayerProfile> {
  const path = `players/${uid}`;
  const ref = doc(db, 'players', uid);
  try {
    const snap = await getDoc(ref);
    const expectedCode = generatePlayerCode(uid);
    if (snap.exists()) {
      const existing = mapPlayerProfile(snap.data() as Record<string, unknown>, uid);
      const updates: Record<string, unknown> = {
        presenceStatus: 'online',
        lastSeenAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      };
      if (!snap.data().playerCode) {
        updates.playerCode = expectedCode;
      }
      await updateDoc(ref, updates).catch(() => {});
      return { ...existing, presenceStatus: 'online' };
    }
    const safeName = defaultName.trim().slice(0, 40) || 'لاعب شدها';
    const payload = {
      userId: uid,
      displayName: safeName,
      avatarUrl: avatarUrl ? avatarUrl.slice(0, 500) : null,
      playerCode: expectedCode,
      presenceStatus: 'online',
      lastSeenAt: serverTimestamp(),
      visibility: 'public',
      rating: 22,
      rankTier: rankTierForRating(22),
      wins: 0,
      losses: 0,
      matchesPlayed: 0,
      streak: 0,
      lossStreak: 0,
      totalPoints: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    await setDoc(ref, payload);
    return {
      userId: uid,
      displayName: safeName,
      avatarUrl: avatarUrl ? avatarUrl.slice(0, 500) : null,
      playerCode: expectedCode,
      presenceStatus: 'online',
      visibility: 'public',
      level: 1,
      xp: 22,
      rating: 22,
      rankTier: rankTierForRating(22),
      wins: 0,
      losses: 0,
      matchesPlayed: 0,
      streak: 0,
      lossStreak: 0,
      totalPoints: 0,
    };
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, path);
  }
}

export async function ensureUserProfile(
  uid: string,
  defaultName: string,
  avatarUrl: string | null,
): Promise<PlayerProfile> {
  const path = `users/${uid}`;
  const ref = doc(db, 'users', uid);
  const expectedCode = generatePlayerCode(uid);
  const safeName = defaultName.trim().slice(0, 40) || 'لاعب شدها';
  const safeAvatar = avatarUrl ? avatarUrl.slice(0, 500) : null;

  try {
    const snap = await getDoc(ref);
    let userProfile: PlayerProfile;
    if (snap.exists()) {
      userProfile = mapUserProfile(snap.data() as Record<string, unknown>, uid);
    } else {
      const payload = {
        userId: uid,
        displayName: safeName,
        avatarUrl: safeAvatar,
        playerCode: expectedCode,
        level: 1,
        xp: 22,
        rankTier: rankTierForRating(22),
        wins: 0,
        losses: 0,
        matchesPlayed: 0,
        streak: 0,
        lossStreak: 0,
        totalPoints: 0,
        lastMatchId: '',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      };
      await setDoc(ref, payload);
      userProfile = {
        userId: uid,
        displayName: safeName,
        avatarUrl: safeAvatar,
        playerCode: expectedCode,
        presenceStatus: 'online',
        visibility: 'public',
        level: 1,
        xp: 22,
        rating: 22,
        rankTier: rankTierForRating(22),
        wins: 0,
        losses: 0,
        matchesPlayed: 0,
        streak: 0,
        lossStreak: 0,
        totalPoints: 0,
        lastMatchId: '',
      };
    }

    // Keep public players/{uid} document provisioned for leaderboard, friends & rooms
    await ensurePlayerProfile(uid, userProfile.displayName, userProfile.avatarUrl);
    return userProfile;
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, path);
  }
}

export async function fetchUserProfile(uid: string): Promise<PlayerProfile | null> {
  const path = `users/${uid}`;
  try {
    const snap = await getDoc(doc(db, 'users', uid));
    if (!snap.exists()) return null;
    return mapUserProfile(snap.data() as Record<string, unknown>, uid);
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, path);
  }
}

export function watchUserProfile(uid: string, cb: (profile: PlayerProfile | null) => void) {
  const path = `users/${uid}`;
  return onSnapshot(
    doc(db, 'users', uid),
    (snap) => {
      if (!snap.exists()) {
        cb(null);
        return;
      }
      cb(mapUserProfile(snap.data() as Record<string, unknown>, uid));
    },
    (error) => {
      handleFirestoreError(error, OperationType.GET, path);
    },
  );
}

export function watchPlayerProfile(uid: string, cb: (profile: PlayerProfile | null) => void) {
  const path = `players/${uid}`;
  return onSnapshot(
    doc(db, 'players', uid),
    (snap) => {
      if (!snap.exists()) {
        cb(null);
        return;
      }
      cb(mapPlayerProfile(snap.data() as Record<string, unknown>, uid));
    },
    (error) => {
      handleFirestoreError(error, OperationType.GET, path);
    },
  );
}

export async function updatePlayerProfileFields(
  uid: string,
  displayName: string,
  avatarUrl: string | null,
) {
  const path = `users/${uid}`;
  const safeName = displayName.trim().slice(0, 40) || 'لاعب شدها';
  const safeAvatar = avatarUrl ? avatarUrl.slice(0, 500) : null;
  try {
    const batch = writeBatch(db);
    const updatePayload = {
      displayName: safeName,
      avatarUrl: safeAvatar,
      updatedAt: serverTimestamp(),
    };
    batch.update(doc(db, 'users', uid), updatePayload);
    batch.update(doc(db, 'players', uid), updatePayload);
    await batch.commit();
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, path);
  }
}

export async function updatePlayerPresence(
  uid: string,
  presenceStatus: 'online' | 'offline' | 'in_game',
) {
  const path = `players/${uid}`;
  try {
    await updateDoc(doc(db, 'players', uid), {
      presenceStatus,
      lastSeenAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  } catch {
    // Non-fatal heartbeat
  }
}

export async function ensureUserSettings(uid: string): Promise<UserSettings> {
  const path = `users/${uid}/settings/preferences`;
  const ref = doc(db, 'users', uid, 'settings', 'preferences');
  const today = todayDateString();
  try {
    const snap = await getDoc(ref);
    if (snap.exists()) {
      const d = snap.data();
      const sameDay = d.dailySessionsDate === today;
      return {
        userId: uid,
        selectedTableBg: (d.selectedTableBg as TableBgId) || 'classic_white',
        ownedTableBgs: (Array.isArray(d.ownedTableBgs)
          ? d.ownedTableBgs
          : ['classic_white', 'classic_green']) as TableBgId[],
        subscriptionPlan: (d.subscriptionPlan as SubscriptionPlanId) || 'none',
        dailySessionsDate: sameDay ? today : today,
        dailySessionsCount: sameDay ? Number(d.dailySessionsCount || 0) : 0,
      };
    }
    const defaults: UserSettings = {
      userId: uid,
      selectedTableBg: 'classic_white',
      ownedTableBgs: ['classic_white', 'classic_green'],
      subscriptionPlan: 'none',
      dailySessionsDate: today,
      dailySessionsCount: 0,
    };
    await setDoc(ref, { ...defaults, updatedAt: serverTimestamp() });
    return defaults;
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, path);
  }
}

export function watchUserSettings(uid: string, cb: (settings: UserSettings) => void) {
  const path = `users/${uid}/settings/preferences`;
  const today = todayDateString();
  return onSnapshot(
    doc(db, 'users', uid, 'settings', 'preferences'),
    (snap) => {
      if (!snap.exists()) return;
      const d = snap.data();
      const sameDay = d.dailySessionsDate === today;
      cb({
        userId: uid,
        selectedTableBg: (d.selectedTableBg as TableBgId) || 'classic_white',
        ownedTableBgs: (Array.isArray(d.ownedTableBgs)
          ? d.ownedTableBgs
          : ['classic_white', 'classic_green']) as TableBgId[],
        subscriptionPlan: (d.subscriptionPlan as SubscriptionPlanId) || 'none',
        dailySessionsDate: today,
        dailySessionsCount: sameDay ? Number(d.dailySessionsCount || 0) : 0,
      });
    },
    (error) => {
      handleFirestoreError(error, OperationType.GET, path);
    },
  );
}

export async function saveUserSettings(uid: string, partial: Partial<Omit<UserSettings, 'userId'>>) {
  const path = `users/${uid}/settings/preferences`;
  const ref = doc(db, 'users', uid, 'settings', 'preferences');
  try {
    await updateDoc(ref, {
      ...partial,
      updatedAt: serverTimestamp(),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, path);
  }
}

export function watchLeaderboard(cb: (players: PlayerProfile[]) => void) {
  const path = 'players';
  const q = query(collection(db, 'players'), where('visibility', '==', 'public'), limit(50));
  return onSnapshot(
    q,
    (snap) => {
      const list = snap.docs
        .map((d) => mapPlayerProfile(d.data() as Record<string, unknown>, d.id))
        .sort((a, b) => b.rating - a.rating);
      cb(list);
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, path);
    },
  );
}

export function watchMatchHistory(uid: string, cb: (records: MatchRecord[]) => void) {
  const path = `users/${uid}/matches`;
  const q = query(collection(db, 'users', uid, 'matches'), orderBy('createdAt', 'desc'), limit(40));
  return onSnapshot(
    q,
    (snap) => {
      const list: MatchRecord[] = snap.docs.map((d) => {
        const data = d.data();
        return {
          id: d.id,
          userId: String(data.userId || uid),
          roomCode: typeof data.roomCode === 'string' ? data.roomCode : null,
          mode: data.mode === 'online' ? 'online' : 'solo',
          usScore: Number(data.usScore || 0),
          themScore: Number(data.themScore || 0),
          didWin: Boolean(data.didWin),
          ratingDelta: Number(data.ratingDelta || 0),
          opponentSummary: String(data.opponentSummary || ''),
          createdAtMs: toMillis(data.createdAt),
        };
      });
      cb(list);
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, path);
    },
  );
}

export async function recordMatchResult(params: {
  profile: PlayerProfile;
  mode: 'online' | 'solo';
  roomCode: string | null;
  usScore: number;
  themScore: number;
  didWin: boolean;
  opponentSummary: string;
}): Promise<number> {
  const { profile, mode, roomCode, usScore, themScore, didWin, opponentSummary } = params;
  const uid = profile.userId;
  const userRef = doc(db, 'users', uid);
  const playerRef = doc(db, 'players', uid);
  const matchRef = doc(collection(db, 'users', uid, 'matches'));
  const path = `users/${uid}`;

  try {
    const [userSnap, playerSnap] = await Promise.all([getDoc(userRef), getDoc(playerRef)]);
    const freshUser = userSnap.exists()
      ? mapUserProfile(userSnap.data() as Record<string, unknown>, uid)
      : profile;

    const delta = Math.max(-500, Math.min(80, calculateMatchPointsDelta(didWin, freshUser.lossStreak)));
    const safeUsScore = Math.min(500, Math.max(0, Math.floor(usScore)));
    const safeThemScore = Math.min(500, Math.max(0, Math.floor(themScore)));

    const newXp = Math.max(0, Math.min(100000, freshUser.xp + delta));
    const newLevel = levelFromXp(newXp);
    const newTier = rankTierForRating(newXp);
    const newWins = freshUser.wins + (didWin ? 1 : 0);
    const newLosses = freshUser.losses + (didWin ? 0 : 1);
    const newMatches = newWins + newLosses;
    const newStreak = didWin ? freshUser.streak + 1 : 0;
    const newLossStreak = didWin ? 0 : freshUser.lossStreak + 1;
    const newTotalPoints = freshUser.totalPoints + safeUsScore;

    const batch = writeBatch(db);

    batch.set(matchRef, {
      userId: uid,
      roomCode: roomCode ? roomCode.slice(0, 16) : null,
      mode,
      usScore: safeUsScore,
      themScore: safeThemScore,
      didWin,
      ratingDelta: delta,
      opponentSummary: opponentSummary.slice(0, 120),
      createdAt: serverTimestamp(),
    });

    if (userSnap.exists()) {
      batch.update(userRef, {
        level: newLevel,
        xp: newXp,
        rankTier: newTier,
        wins: newWins,
        losses: newLosses,
        matchesPlayed: newMatches,
        streak: newStreak,
        lossStreak: newLossStreak,
        totalPoints: newTotalPoints,
        lastMatchId: matchRef.id,
        updatedAt: serverTimestamp(),
      });
    }

    if (playerSnap.exists()) {
      const freshPlayer = mapPlayerProfile(playerSnap.data() as Record<string, unknown>, uid);
      const pRating = Math.max(0, Math.min(100000, freshPlayer.rating + delta));
      batch.update(playerRef, {
        rating: pRating,
        rankTier: rankTierForRating(pRating),
        wins: freshPlayer.wins + (didWin ? 1 : 0),
        losses: freshPlayer.losses + (didWin ? 0 : 1),
        matchesPlayed: freshPlayer.matchesPlayed + 1,
        streak: didWin ? freshPlayer.streak + 1 : 0,
        lossStreak: didWin ? 0 : freshPlayer.lossStreak + 1,
        totalPoints: freshPlayer.totalPoints + safeUsScore,
        updatedAt: serverTimestamp(),
      });
    }

    await batch.commit();
    return delta;
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, path);
  }
}

// --- Friends ---
export function watchFriends(uid: string, cb: (friends: FriendEntry[]) => void) {
  const path = `users/${uid}/friends`;
  return onSnapshot(
    collection(db, 'users', uid, 'friends'),
    (snap) => {
      const list: FriendEntry[] = snap.docs.map((d) => {
        const data = d.data();
        return {
          userId: String(data.userId || uid),
          friendUid: String(data.friendUid || d.id),
          friendPlayerCode: String(data.friendPlayerCode || ''),
          friendName: String(data.friendName || 'لاعب'),
          friendRating: Number(data.friendRating || 22),
        };
      });
      cb(list);
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, path);
    },
  );
}

export async function addFriendByPlayerCode(
  myUid: string,
  codeInput: string,
): Promise<{ ok: boolean; message: string }> {
  const cleanCode = codeInput.trim().toUpperCase();
  if (!cleanCode || cleanCode.length < 6) {
    return { ok: false, message: 'الرجاء إدخال رقم تعريفي (Player ID) صحيح.' };
  }
  try {
    const q = query(
      collection(db, 'players'),
      where('visibility', '==', 'public'),
      where('playerCode', '==', cleanCode),
      limit(1),
    );
    const snap = await getDocs(q);
    if (snap.empty) {
      return { ok: false, message: 'لم يتم العثور على لاعب بهذا الرقم التعريفي.' };
    }
    const targetDoc = snap.docs[0];
    const target = mapPlayerProfile(targetDoc.data() as Record<string, unknown>, targetDoc.id);
    if (target.userId === myUid) {
      return { ok: false, message: 'لا يمكنك إضافة نفسك إلى قائمة الأصدقاء.' };
    }
    const friendPath = `users/${myUid}/friends/${target.userId}`;
    await setDoc(doc(db, 'users', myUid, 'friends', target.userId), {
      userId: myUid,
      friendUid: target.userId,
      friendPlayerCode: target.playerCode,
      friendName: target.displayName.slice(0, 40),
      friendRating: target.rating,
      createdAt: serverTimestamp(),
    }).catch((err) => handleFirestoreError(err, OperationType.CREATE, friendPath));
    return { ok: true, message: `تمت إضافة ${target.displayName} إلى أصدقائك!` };
  } catch (error) {
    if (error instanceof Error && error.message.startsWith('{')) throw error;
    return { ok: false, message: 'حدث خطأ أثناء البحث عن اللاعب.' };
  }
}

export async function removeFriend(myUid: string, friendUid: string) {
  const path = `users/${myUid}/friends/${friendUid}`;
  try {
    await deleteDoc(doc(db, 'users', myUid, 'friends', friendUid));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, path);
  }
}

// --- Table Invites ---
export function watchIncomingInvites(uid: string, cb: (invites: TableInvite[]) => void) {
  const path = 'invites';
  const q = query(
    collection(db, 'invites'),
    where('toUid', '==', uid),
    where('status', '==', 'pending'),
    limit(20),
  );
  return onSnapshot(
    q,
    (snap) => {
      cb(
        snap.docs.map((d) => ({
          id: d.id,
          ...(d.data() as Omit<TableInvite, 'id'>),
        })),
      );
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, path);
    },
  );
}

export async function sendTableInvite(params: {
  fromProfile: PlayerProfile;
  toFriend: FriendEntry;
  roomCode: string;
}) {
  const path = 'invites';
  try {
    await addDoc(collection(db, 'invites'), {
      fromUid: params.fromProfile.userId,
      fromName: params.fromProfile.displayName.slice(0, 40),
      fromPlayerCode: params.fromProfile.playerCode,
      toUid: params.toFriend.friendUid,
      toName: params.toFriend.friendName.slice(0, 40),
      roomCode: params.roomCode,
      targetScore: 152,
      status: 'pending',
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, path);
  }
}

export async function respondToInvite(inviteId: string, status: 'accepted' | 'declined') {
  const path = `invites/${inviteId}`;
  try {
    await updateDoc(doc(db, 'invites', inviteId), {
      status,
      updatedAt: serverTimestamp(),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, path);
  }
}

// --- Rooms & Online Sync ---
export function roomDocToTableState(room: GameRoomDoc, myUid: string): BalootTableState {
  const seats: BalootSeat[] = [
    {
      seatIndex: 0,
      uid: room.seat0Uid,
      name: room.seat0Name || 'مقعد 1',
      rating: room.seat0Rating || 22,
      isBot: room.seat0Uid.startsWith('bot_'),
    },
    {
      seatIndex: 1,
      uid: room.seat1Uid,
      name: room.seat1Name || 'مقعد 2',
      rating: room.seat1Rating || 22,
      isBot: room.seat1Uid.startsWith('bot_'),
    },
    {
      seatIndex: 2,
      uid: room.seat2Uid,
      name: room.seat2Name || 'مقعد 3',
      rating: room.seat2Rating || 22,
      isBot: room.seat2Uid.startsWith('bot_'),
    },
    {
      seatIndex: 3,
      uid: room.seat3Uid,
      name: room.seat3Name || 'مقعد 4',
      rating: room.seat3Rating || 22,
      isBot: room.seat3Uid.startsWith('bot_'),
    },
  ];
  const mySeatIndex = Math.max(0, seats.findIndex((s) => s.uid === myUid));
  const hands: Record<number, BalootCard[]> = { 0: [], 1: [], 2: [], 3: [] };
  for (let i = 0; i < 4; i++) {
    const raw = room.hands?.[String(i)] || [];
    hands[i] = raw.map(parseCardCode).filter((c): c is BalootCard => c !== null);
  }
  return {
    roomCode: room.roomCode,
    isOnline: true,
    hostId: room.hostId,
    targetScore: 152,
    mySeatIndex,
    seats,
    usScore: room.usScore,
    themScore: room.themScore,
    roundNumber: room.roundNumber,
    turnIndex: room.turnIndex,
    dealerIndex: room.dealerIndex,
    phase: room.gamePhase,
    mode: room.gameMode,
    trumpSuit: room.trumpSuit === 'none' ? null : (room.trumpSuit as Suit),
    bidderIndex: room.bidderIndex,
    bidLevel: room.bidLevel,
    openCard: parseCardCode(room.openCard),
    hands,
    tableCards: (room.tableCards || [])
      .map(decodeTableCard)
      .filter((x): x is NonNullable<typeof x> => x !== null),
    lastTrickCards: (room.lastTrickCards || [])
      .map(decodeTableCard)
      .filter((x): x is NonNullable<typeof x> => x !== null),
    lastTrickWinner: room.lastTrickWinner,
    usTricksCount: room.usTricksCount,
    themTricksCount: room.themTricksCount,
    usRoundPoints: room.usRoundPoints,
    themRoundPoints: room.themRoundPoints,
    usProjects: room.usProjects || [],
    themProjects: room.themProjects || [],
    winnerTeam: room.winnerTeam,
    lastActionText: room.lastActionText || '',
    biddingPasses: room.biddingPasses ?? 0,
    highestBidSeat: room.highestBidSeat ?? -1,
    highestBidMode: room.highestBidMode ?? 'none',
    highestBidSuit:
      !room.highestBidSuit || room.highestBidSuit === 'none'
        ? null
        : (room.highestBidSuit as Suit),
    standbySeats: room.standbySeats || [],
    declaredProjectSeats: room.declaredProjectSeats || [],
  };
}

export function tableStateToRoomUpdate(state: BalootTableState): Record<string, unknown> {
  const hands: Record<string, string[]> = {
    '0': (state.hands[0] || []).map(cardCode),
    '1': (state.hands[1] || []).map(cardCode),
    '2': (state.hands[2] || []).map(cardCode),
    '3': (state.hands[3] || []).map(cardCode),
  };
  return {
    status: state.phase === 'match_finished' ? 'finished' : 'playing',
    seat0Uid: state.seats[0]?.uid || '',
    seat1Uid: state.seats[1]?.uid || '',
    seat2Uid: state.seats[2]?.uid || '',
    seat3Uid: state.seats[3]?.uid || '',
    seat0Name: (state.seats[0]?.name || '').slice(0, 40),
    seat1Name: (state.seats[1]?.name || '').slice(0, 40),
    seat2Name: (state.seats[2]?.name || '').slice(0, 40),
    seat3Name: (state.seats[3]?.name || '').slice(0, 40),
    seat0Rating: state.seats[0]?.rating || 22,
    seat1Rating: state.seats[1]?.rating || 22,
    seat2Rating: state.seats[2]?.rating || 22,
    seat3Rating: state.seats[3]?.rating || 22,
    usScore: state.usScore,
    themScore: state.themScore,
    roundNumber: state.roundNumber,
    turnIndex: state.turnIndex,
    dealerIndex: state.dealerIndex,
    gamePhase: state.phase,
    gameMode: state.mode,
    trumpSuit: state.trumpSuit || 'none',
    bidderIndex: state.bidderIndex,
    bidLevel: state.bidLevel,
    openCard: state.openCard ? cardCode(state.openCard) : '',
    hands,
    tableCards: state.tableCards.map(encodeTableCard),
    lastTrickCards: state.lastTrickCards.map(encodeTableCard),
    lastTrickWinner: state.lastTrickWinner,
    usTricksCount: state.usTricksCount,
    themTricksCount: state.themTricksCount,
    usRoundPoints: state.usRoundPoints,
    themRoundPoints: state.themRoundPoints,
    usProjects: state.usProjects.slice(0, 8),
    themProjects: state.themProjects.slice(0, 8),
    winnerTeam: state.winnerTeam,
    lastActionText: state.lastActionText.slice(0, 140),
    biddingPasses: state.biddingPasses,
    highestBidSeat: state.highestBidSeat,
    highestBidMode: state.highestBidMode as GameMode,
    highestBidSuit: state.highestBidSuit || 'none',
    standbySeats: state.standbySeats.slice(0, 4),
    declaredProjectSeats: state.declaredProjectSeats.slice(0, 4),
    updatedAt: serverTimestamp(),
  };
}

export async function createOnlineRoom(
  profile: PlayerProfile,
  roomType: 'public' | 'private',
): Promise<string> {
  const roomCode = generateRoomCode();
  const path = `rooms/${roomCode}`;
  try {
    await setDoc(doc(db, 'rooms', roomCode), {
      roomCode,
      hostId: profile.userId,
      roomType,
      status: 'waiting',
      targetScore: 152,
      memberUids: [profile.userId],
      seat0Uid: profile.userId,
      seat1Uid: '',
      seat2Uid: '',
      seat3Uid: '',
      seat0Name: profile.displayName.slice(0, 40),
      seat1Name: '',
      seat2Name: '',
      seat3Name: '',
      seat0Rating: profile.rating,
      seat1Rating: 0,
      seat2Rating: 0,
      seat3Rating: 0,
      usScore: 0,
      themScore: 0,
      roundNumber: 0,
      turnIndex: 0,
      dealerIndex: 0,
      gamePhase: 'lobby',
      gameMode: 'none',
      trumpSuit: 'none',
      bidderIndex: -1,
      bidLevel: 1,
      openCard: '',
      hands: { '0': [], '1': [], '2': [], '3': [] },
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
      lastActionText: `أنشأ ${profile.displayName} جلسة بلوت (صكة 152)`,
      biddingPasses: 0,
      highestBidSeat: -1,
      highestBidMode: 'none',
      highestBidSuit: 'none',
      standbySeats: [],
      declaredProjectSeats: [],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    return roomCode;
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, path);
  }
}

export async function joinOnlineRoom(
  roomCodeInput: string,
  profile: PlayerProfile,
): Promise<{ ok: boolean; roomCode?: string; message?: string }> {
  const roomCode = roomCodeInput.trim().toUpperCase();
  if (!/^[A-Z0-9]{6}$/.test(roomCode)) {
    return { ok: false, message: 'رمز الجلسة يجب أن يتكون من 6 أحرف وأرقام.' };
  }
  const path = `rooms/${roomCode}`;
  const ref = doc(db, 'rooms', roomCode);
  try {
    const snap = await getDoc(ref);
    if (!snap.exists()) {
      return { ok: false, message: 'لم يتم العثور على جلسة بهذا الرمز.' };
    }
    const room = snap.data() as GameRoomDoc;
    if (room.memberUids.includes(profile.userId)) {
      return { ok: true, roomCode };
    }
    if (room.status !== 'waiting') {
      return { ok: false, message: 'هذه الجلسة بدأت بالفعل أو انتهت.' };
    }
    if (room.memberUids.length >= 4) {
      return { ok: false, message: 'الجلسة مكتملة (4 لاعبين).' };
    }
    const updates: Record<string, unknown> = {
      memberUids: [...room.memberUids, profile.userId],
      lastActionText: `انضم ${profile.displayName} إلى الطاولة`,
      updatedAt: serverTimestamp(),
    };
    if (!room.seat1Uid) {
      updates.seat1Uid = profile.userId;
      updates.seat1Name = profile.displayName.slice(0, 40);
      updates.seat1Rating = profile.rating;
    } else if (!room.seat2Uid) {
      updates.seat2Uid = profile.userId;
      updates.seat2Name = profile.displayName.slice(0, 40);
      updates.seat2Rating = profile.rating;
    } else if (!room.seat3Uid) {
      updates.seat3Uid = profile.userId;
      updates.seat3Name = profile.displayName.slice(0, 40);
      updates.seat3Rating = profile.rating;
    }
    await updateDoc(ref, updates);
    return { ok: true, roomCode };
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, path);
  }
}

export async function leaveWaitingRoom(room: GameRoomDoc, myUid: string) {
  const path = `rooms/${room.roomCode}`;
  const ref = doc(db, 'rooms', room.roomCode);
  try {
    if (room.hostId === myUid && room.status === 'waiting') {
      await deleteDoc(ref);
      return;
    }
    const nextMembers = room.memberUids.filter((u) => u !== myUid);
    if (nextMembers.length === 0) {
      await deleteDoc(ref);
      return;
    }
    const updates: Record<string, unknown> = {
      memberUids: nextMembers,
      lastActionText: 'غادر أحد اللاعبين غرفة الانتظار',
      updatedAt: serverTimestamp(),
    };
    if (room.seat1Uid === myUid) {
      updates.seat1Uid = '';
      updates.seat1Name = '';
      updates.seat1Rating = 0;
    } else if (room.seat2Uid === myUid) {
      updates.seat2Uid = '';
      updates.seat2Name = '';
      updates.seat2Rating = 0;
    } else if (room.seat3Uid === myUid) {
      updates.seat3Uid = '';
      updates.seat3Name = '';
      updates.seat3Rating = 0;
    }
    await updateDoc(ref, updates);
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, path);
  }
}

export async function startOnlineRoomMatch(room: GameRoomDoc) {
  const path = `rooms/${room.roomCode}`;
  const botPool = [...BOT_NAMES];
  const fillSeat = (idx: number, uid: string, name: string, rating: number): BalootSeat => {
    if (uid) {
      return { seatIndex: idx, uid, name, rating, isBot: false };
    }
    return {
      seatIndex: idx,
      uid: `bot_${idx}`,
      name: `${botPool[idx % botPool.length]} (آلي)`,
      rating: 1200,
      isBot: true,
    };
  };
  const seats: BalootSeat[] = [
    fillSeat(0, room.seat0Uid, room.seat0Name, room.seat0Rating),
    fillSeat(1, room.seat1Uid, room.seat1Name, room.seat1Rating),
    fillSeat(2, room.seat2Uid, room.seat2Name, room.seat2Rating),
    fillSeat(3, room.seat3Uid, room.seat3Name, room.seat3Rating),
  ];
  const initialState = dealNewRound({
    dealerIndex: 0,
    roundNumber: 1,
    seats,
    existingUsScore: 0,
    existingThemScore: 0,
    roomCode: room.roomCode,
    isOnline: true,
    hostId: room.hostId,
    mySeatIndex: 0,
  });
  try {
    await updateDoc(doc(db, 'rooms', room.roomCode), tableStateToRoomUpdate(initialState));
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, path);
  }
}

export async function pushOnlineRoomState(roomCode: string, state: BalootTableState) {
  const path = `rooms/${roomCode}`;
  try {
    await updateDoc(doc(db, 'rooms', roomCode), tableStateToRoomUpdate(state));
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, path);
  }
}

export function watchPublicRooms(cb: (rooms: GameRoomDoc[]) => void) {
  const path = 'rooms';
  const q = query(
    collection(db, 'rooms'),
    where('roomType', '==', 'public'),
    where('status', '==', 'waiting'),
    limit(20),
  );
  return onSnapshot(
    q,
    (snap) => {
      cb(
        snap.docs.map((d) => ({
          ...(d.data() as GameRoomDoc),
          createdAtMs: toMillis(d.data().createdAt),
        })),
      );
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, path);
    },
  );
}

export function watchRoom(roomCode: string, cb: (room: GameRoomDoc | null) => void) {
  const path = `rooms/${roomCode}`;
  return onSnapshot(
    doc(db, 'rooms', roomCode),
    (snap) => {
      if (!snap.exists()) {
        cb(null);
        return;
      }
      cb({
        ...(snap.data() as GameRoomDoc),
        createdAtMs: toMillis(snap.data().createdAt),
      });
    },
    (error) => {
      handleFirestoreError(error, OperationType.GET, path);
    },
  );
}

export function watchRoomMessages(roomCode: string, cb: (msgs: RoomMessage[]) => void) {
  const path = `rooms/${roomCode}/messages`;
  const q = query(
    collection(db, 'rooms', roomCode, 'messages'),
    orderBy('createdAt', 'asc'),
    limit(60),
  );
  return onSnapshot(
    q,
    (snap) => {
      cb(
        snap.docs.map((d) => {
          const data = d.data();
          return {
            id: d.id,
            senderUid: String(data.senderUid || ''),
            senderName: String(data.senderName || 'لاعب'),
            seatIndex: Number(data.seatIndex || 0),
            text: String(data.text || ''),
            createdAtMs: toMillis(data.createdAt),
          };
        }),
      );
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, path);
    },
  );
}

export async function sendRoomMessage(params: {
  roomCode: string;
  senderUid: string;
  senderName: string;
  seatIndex: number;
  text: string;
}) {
  const path = `rooms/${params.roomCode}/messages`;
  const clean = params.text.trim().slice(0, 200);
  if (!clean) return;
  try {
    await addDoc(collection(db, 'rooms', params.roomCode, 'messages'), {
      senderUid: params.senderUid,
      senderName: params.senderName.slice(0, 40),
      seatIndex: params.seatIndex,
      text: clean,
      createdAt: serverTimestamp(),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, path);
  }
}
