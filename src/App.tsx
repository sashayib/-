import React, { useCallback, useEffect, useRef, useState } from 'react';
import { onAuthStateChanged, signOut, User } from 'firebase/auth';
import {
  Calculator,
  History,
  Home,
  ShoppingBag,
  Trophy,
  User as UserIcon,
  Users,
} from 'lucide-react';
import { auth } from './firebase';
import {
  addFriendByPlayerCode,
  createOnlineRoom,
  ensureUserProfile,
  ensureUserSettings,
  fetchUserProfile,
  joinOnlineRoom,
  leaveWaitingRoom,
  pushOnlineRoomState,
  recordMatchResult,
  removeFriend,
  respondToInvite,
  roomDocToTableState,
  saveUserSettings,
  sendRoomMessage,
  sendTableInvite,
  startOnlineRoomMatch,
  todayDateString,
  updatePlayerPresence,
  updatePlayerProfileFields,
  watchFriends,
  watchIncomingInvites,
  watchLeaderboard,
  watchMatchHistory,
  watchPublicRooms,
  watchRoom,
  watchRoomMessages,
  watchUserProfile,
  watchUserSettings,
} from './lib/firestoreRepo';
import {
  applyBidDecision,
  applyCardPlay,
  applyDoublingDecision,
  BidAction,
  BOT_NAMES,
  chooseBotBid,
  chooseBotCard,
  chooseBotDoubling,
  dealNewRound,
} from './game/balootEngine';
import {
  BalootCard,
  BalootSeat,
  BalootTableState,
  DetectedProject,
  FREE_DAILY_SESSION_LIMIT,
  FriendEntry,
  GameRoomDoc,
  MatchRecord,
  PlayerProfile,
  PURCHASES_ENABLED,
  RoomMessage,
  SubscriptionPlanId,
  Suit,
  TABLE_BACKGROUNDS,
  TableBgId,
  TableInvite,
  UserSettings,
} from './game/balootTypes';
import { type BalootVoiceCall, saudiVoice } from './lib/saudiVoice';
import { AuthScreen } from './components/AuthScreen';
import { LobbyScreen } from './components/LobbyScreen';
import { FriendsScreen } from './components/FriendsScreen';
import { LeaderboardScreen } from './components/LeaderboardScreen';
import { HistoryScreen } from './components/HistoryScreen';
import { ProfileScreen } from './components/ProfileScreen';
import { StoreAndSubScreen, SubscriptionModal } from './components/StoreAndSubScreen';
import { BalootCalculatorScreen } from './components/BalootCalculatorScreen';
import { WaitingRoomScreen } from './components/WaitingRoomScreen';
import { GameTableScreen } from './components/GameTableScreen';
import { LegalPage } from './components/LegalPages';

type MainTab =
  | 'lobby'
  | 'friends'
  | 'leaderboard'
  | 'history'
  | 'profile'
  | 'store'
  | 'calculator';

const EMOJI_PREFIX = '[EMOJI]';

export default function App() {
  const [authReady, setAuthReady] = useState(false);
  const [fbUser, setFbUser] = useState<User | null>(null);
  const [legalPage, setLegalPage] = useState<'privacy' | 'terms' | null>(null);

  const [profile, setProfile] = useState<PlayerProfile | null>(null);
  const [settings, setSettings] = useState<UserSettings | null>(null);
  const [leaderboard, setLeaderboard] = useState<PlayerProfile[]>([]);
  const [history, setHistory] = useState<MatchRecord[]>([]);
  const [friends, setFriends] = useState<FriendEntry[]>([]);
  const [incomingInvites, setIncomingInvites] = useState<TableInvite[]>([]);
  const [publicRooms, setPublicRooms] = useState<GameRoomDoc[]>([]);

  const [tab, setTab] = useState<MainTab>('lobby');
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [profileSuccess, setProfileSuccess] = useState<string | null>(null);
  const [bootstrapError, setBootstrapError] = useState<string | null>(null);
  const [subModalOpen, setSubModalOpen] = useState(false);
  const [subModalReason, setSubModalReason] = useState<string | null>(null);

  // Active online room & table state
  const [activeRoomCode, setActiveRoomCode] = useState<string | null>(null);
  const [activeRoom, setActiveRoom] = useState<GameRoomDoc | null>(null);
  const [soloTable, setSoloTable] = useState<BalootTableState | null>(null);
  const [localChat, setLocalChat] = useState<RoomMessage[]>([]);
  const [onlineChat, setOnlineChat] = useState<RoomMessage[]>([]);
  const [seatBubbles, setSeatBubbles] = useState<Record<number, { text: string; ts: number }>>({});
  const [isMuted, setIsMuted] = useState(false);
  const [turnSecondsLeft, setTurnSecondsLeft] = useState(10);
  const [lastRatingDelta, setLastRatingDelta] = useState<number | null>(null);

  const recordedMatchKeyRef = useRef<string | null>(null);
  const lastProcessedMsgIdRef = useRef<string | null>(null);
  const pendingRoomFromUrlRef = useRef<string | null>(null);

  const showToast = useCallback((msg: string) => {
    setToast(msg);
  }, []);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 3400);
    return () => clearTimeout(t);
  }, [toast]);

  useEffect(() => {
    if (!profileSuccess) return;
    const t = setTimeout(() => setProfileSuccess(null), 3500);
    return () => clearTimeout(t);
  }, [profileSuccess]);

  // Capture ?room=XXXXXX from shareable session link
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get('room');
    if (code && /^[A-Za-z0-9]{6}$/.test(code.trim())) {
      pendingRoomFromUrlRef.current = code.trim().toUpperCase();
    }
  }, []);

  // Firebase Auth subscription
  useEffect(() => {
    return onAuthStateChanged(auth, (user) => {
      setFbUser(user);
      setAuthReady(true);
      if (!user) {
        setProfile(null);
        setSettings(null);
        setActiveRoomCode(null);
        setActiveRoom(null);
        setSoloTable(null);
        setProfileLoading(false);
        setProfileSaving(false);
        setProfileError(null);
        setProfileSuccess(null);
        setBootstrapError(null);
      }
    });
  }, []);

  // Bootstrap & watch Firestore collections when authenticated (authoritative profile at users/{uid})
  useEffect(() => {
    if (!fbUser) return;
    let alive = true;
    const uid = fbUser.uid;
    const fallbackName =
      fbUser.displayName?.trim() ||
      fbUser.email?.split('@')[0]?.trim() ||
      'لاعب شدها';

    setProfileLoading(true);
    setBootstrapError(null);

    void Promise.all([
      ensureUserProfile(uid, fallbackName, fbUser.photoURL),
      ensureUserSettings(uid),
    ])
      .then(([p, s]) => {
        if (!alive) return;
        setProfile(p);
        setSettings(s);
        setProfileError(null);
      })
      .catch(() => {
        if (!alive) return;
        setBootstrapError('تعذر تحميل بيانات ملفك الشخصي من قاعدة البيانات. يرجى المحاولة مرة أخرى.');
      })
      .finally(() => {
        if (alive) setProfileLoading(false);
      });

    const unsubs = [
      watchUserProfile(uid, (p) => {
        if (alive && p) setProfile(p);
      }),
      watchUserSettings(uid, (s) => {
        if (alive) setSettings(s);
      }),
      watchLeaderboard((list) => {
        if (alive) setLeaderboard(list);
      }),
      watchMatchHistory(uid, (list) => {
        if (alive) setHistory(list);
      }),
      watchFriends(uid, (list) => {
        if (alive) setFriends(list);
      }),
      watchIncomingInvites(uid, (list) => {
        if (alive) setIncomingInvites(list);
      }),
      watchPublicRooms((list) => {
        if (alive) setPublicRooms(list);
      }),
    ];

    return () => {
      alive = false;
      unsubs.forEach((fn) => fn());
    };
  }, [fbUser]);

  // Periodic online/in_game presence heartbeat on public players/{uid}
  useEffect(() => {
    if (!fbUser) return;
    const uid = fbUser.uid;
    const status = activeRoomCode || soloTable ? 'in_game' : 'online';
    void updatePlayerPresence(uid, status);
    const heartbeat = setInterval(() => {
      void updatePlayerPresence(uid, status);
    }, 45_000);
    return () => clearInterval(heartbeat);
  }, [fbUser, activeRoomCode, soloTable]);

  // Fetch latest users/{uid} document when opening ProfileScreen
  const handleRefreshProfile = useCallback(async () => {
    if (!fbUser) return;
    setProfileLoading(true);
    setProfileError(null);
    try {
      const fresh = await fetchUserProfile(fbUser.uid);
      if (fresh) {
        setProfile(fresh);
      }
    } catch {
      setProfileError('تعذر جلب بيانات الملف الشخصي من قاعدة البيانات. يرجى المحاولة مرة أخرى.');
    } finally {
      setProfileLoading(false);
    }
  }, [fbUser]);

  // Persist displayName & avatarUrl edits to users/{uid} (and sync public players/{uid})
  const handleUpdateProfile = useCallback(
    async (displayName: string, avatarUrl: string | null): Promise<boolean> => {
      if (!profile) return false;
      setProfileSaving(true);
      setProfileError(null);
      setProfileSuccess(null);
      try {
        await updatePlayerProfileFields(profile.userId, displayName, avatarUrl);
        setProfileSuccess('تم حفظ تعديلات الملف الشخصي بنجاح.');
        showToast('تم تحديث ملفك الشخصي بنجاح');
        return true;
      } catch {
        setProfileError('تعذر حفظ التعديلات في قاعدة البيانات. تأكد من الاتصال وحاول مجدداً.');
        return false;
      } finally {
        setProfileSaving(false);
      }
    },
    [profile, showToast],
  );

  // Auto-join room from URL parameter once profile is loaded
  useEffect(() => {
    if (!profile || !pendingRoomFromUrlRef.current) return;
    const code = pendingRoomFromUrlRef.current;
    pendingRoomFromUrlRef.current = null;
    void joinOnlineRoom(code, profile).then((res) => {
      if (res.ok && res.roomCode) {
        setActiveRoomCode(res.roomCode);
        showToast(`تم الانضمام إلى الجلسة ${res.roomCode}`);
        window.history.replaceState({}, '', window.location.pathname);
      } else if (res.message) {
        showToast(res.message);
      }
    });
  }, [profile, showToast]);

  // Watch active online room & its chat/reactions
  useEffect(() => {
    if (!activeRoomCode) {
      setActiveRoom(null);
      setOnlineChat([]);
      return;
    }
    const unsubRoom = watchRoom(activeRoomCode, (room) => {
      if (!room) {
        setActiveRoom(null);
        setActiveRoomCode(null);
        showToast('تم إغلاق غرفة اللعب');
        return;
      }
      setActiveRoom(room);
    });
    const unsubMsgs = watchRoomMessages(activeRoomCode, (msgs) => {
      const chatOnly: RoomMessage[] = [];
      for (const m of msgs) {
        if (m.text.startsWith(EMOJI_PREFIX)) {
          if (m.id !== lastProcessedMsgIdRef.current && Date.now() - m.createdAtMs < 6000) {
            const emoji = m.text.slice(EMOJI_PREFIX.length);
            setSeatBubbles((prev) => ({ ...prev, [m.seatIndex]: { text: emoji, ts: Date.now() } }));
          }
        } else {
          chatOnly.push(m);
        }
      }
      if (msgs.length > 0) {
        lastProcessedMsgIdRef.current = msgs[msgs.length - 1].id;
      }
      setOnlineChat(chatOnly);
    });
    return () => {
      unsubRoom();
      unsubMsgs();
    };
  }, [activeRoomCode, showToast]);

  // Expire seat reaction bubbles after 3.2 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      const now = Date.now();
      setSeatBubbles((prev) => {
        let changed = false;
        const next: Record<number, { text: string; ts: number }> = {};
        for (const [k, v] of Object.entries(prev)) {
          if (now - v.ts < 3200) {
            next[Number(k)] = v;
          } else {
            changed = true;
          }
        }
        return changed ? next : prev;
      });
    }, 600);
    return () => clearInterval(interval);
  }, []);

  // Derive active TableState (either solo AI table or live Firestore online table)
  const activeTable: BalootTableState | null =
    soloTable ??
    (activeRoom && fbUser && activeRoom.status !== 'waiting'
      ? roomDocToTableState(activeRoom, fbUser.uid)
      : null);

  const triggerBubble = useCallback((seatIndex: number, text: string) => {
    setSeatBubbles((prev) => ({ ...prev, [seatIndex]: { text, ts: Date.now() } }));
  }, []);

  const commitTableState = useCallback(
    async (next: BalootTableState) => {
      if (next.isOnline && next.roomCode) {
        await pushOnlineRoomState(next.roomCode, next);
      } else {
        setSoloTable(next);
      }
    },
    [],
  );

  // Record match completion once per match
  useEffect(() => {
    if (!activeTable || !profile || activeTable.phase !== 'match_finished') return;
    const matchKey = `${activeTable.roomCode || 'solo'}_${activeTable.roundNumber}_${activeTable.usScore}_${activeTable.themScore}`;
    if (recordedMatchKeyRef.current === matchKey) return;
    recordedMatchKeyRef.current = matchKey;

    const myTeam = activeTable.mySeatIndex % 2;
    const didWin = activeTable.winnerTeam === myTeam;
    const ourScore = myTeam === 0 ? activeTable.usScore : activeTable.themScore;
    const oppScore = myTeam === 0 ? activeTable.themScore : activeTable.usScore;
    const oppNames = activeTable.seats
      .filter((s) => s.seatIndex % 2 !== myTeam)
      .map((s) => s.name)
      .join(' و ');

    void recordMatchResult({
      profile,
      mode: activeTable.isOnline ? 'online' : 'solo',
      roomCode: activeTable.roomCode,
      usScore: ourScore,
      themScore: oppScore,
      didWin,
      opponentSummary: oppNames,
    }).then((delta) => setLastRatingDelta(delta));
  }, [activeTable, profile]);

  // Execute a move for either a bot or a timed-out/standby human seat
  const performAutoMove = useCallback(
    (table: BalootTableState, seatIndex: number, markStandby: boolean) => {
      const standbySeats =
        markStandby && !table.standbySeats.includes(seatIndex)
          ? [...table.standbySeats, seatIndex]
          : table.standbySeats;

      const workingTable = { ...table, standbySeats };

      if (workingTable.phase === 'bidding_r1' || workingTable.phase === 'bidding_r2') {
        const isR2 = workingTable.phase === 'bidding_r2';
        const hand = workingTable.hands[seatIndex] || [];
        const decision = chooseBotBid(
          hand,
          workingTable.openCard,
          isR2,
          workingTable.highestBidMode,
        );
        const { nextState, speechText } = applyBidDecision(
          workingTable,
          seatIndex,
          decision.choice,
          decision.suit,
        );
        const voiceCall: BalootVoiceCall =
          decision.choice === 'sun'
            ? 'sun'
            : decision.choice === 'hokm'
              ? isR2
                ? 'hokm_r2'
                : 'hokm'
              : isR2
                ? 'pass_r2'
                : 'pass_r1';
        triggerBubble(seatIndex, speechText);
        saudiVoice.speakCall(voiceCall);
        void commitTableState(nextState);
        return;
      }

      if (workingTable.phase === 'doubling') {
        const hand = workingTable.hands[seatIndex] || [];
        const shouldDouble =
          !markStandby && chooseBotDoubling(hand, workingTable.mode, workingTable.trumpSuit);
        const { nextState, speechText } = applyDoublingDecision(workingTable, seatIndex, shouldDouble);
        triggerBubble(seatIndex, speechText);
        void commitTableState(nextState);
        return;
      }

      if (workingTable.phase === 'playing') {
        const hand = workingTable.hands[seatIndex] || [];
        if (hand.length === 0) return;
        const card = chooseBotCard(
          hand,
          workingTable.tableCards,
          workingTable.mode,
          workingTable.trumpSuit,
          seatIndex,
        );
        saudiVoice.playCardSnap();
        const nextState = applyCardPlay(workingTable, seatIndex, card);
        void commitTableState(nextState);
      }
    },
    [commitTableState, triggerBubble],
  );

  // 10-Second Turn Countdown Timer + Bot / Standby Orchestrator
  useEffect(() => {
    if (!activeTable) return;
    const activePhase =
      activeTable.phase === 'bidding_r1' ||
      activeTable.phase === 'bidding_r2' ||
      activeTable.phase === 'doubling' ||
      activeTable.phase === 'playing';
    if (!activePhase) return;

    setTurnSecondsLeft(10);

    const turnSeat = activeTable.seats[activeTable.turnIndex];
    const isBotTurn = Boolean(turnSeat?.isBot);
    const isStandbyTurn = activeTable.standbySeats.includes(activeTable.turnIndex);
    const iDriveBots = !activeTable.isOnline || (fbUser && activeTable.hostId === fbUser.uid);

    // If Bot or Standby seat, play smoothly after short delay
    if ((isBotTurn && iDriveBots) || (isStandbyTurn && (activeTable.turnIndex === activeTable.mySeatIndex || iDriveBots))) {
      const botTimer = setTimeout(() => {
        performAutoMove(activeTable, activeTable.turnIndex, false);
      }, 950);
      return () => clearTimeout(botTimer);
    }

    // Human turn: 10-second countdown; if it reaches 0 on MY turn, auto-play & put me in standby for the round!
    const interval = setInterval(() => {
      setTurnSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          if (activeTable.turnIndex === activeTable.mySeatIndex) {
            showToast('انتهى الوقت (10 ثوانٍ) — تم تفعيل اللعب التلقائي ووضع الانتظار حتى نهاية الجولة');
            performAutoMove(activeTable, activeTable.mySeatIndex, true);
          }
          return 0;
        }
        if (prev <= 4 && activeTable.turnIndex === activeTable.mySeatIndex) {
          saudiVoice.playTurnTick();
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [
    activeTable?.phase,
    activeTable?.turnIndex,
    activeTable?.tableCards.length,
    activeTable?.roundNumber,
    activeTable?.biddingPasses,
    activeTable?.bidLevel,
    activeTable?.standbySeats.length,
    fbUser,
    performAutoMove,
    showToast,
  ]);

  // --- Gameplay User Handlers ---
  const handleBid = (choice: BidAction, suit?: Suit | null) => {
    if (!activeTable || activeTable.turnIndex !== activeTable.mySeatIndex) return;
    const isR2 = activeTable.phase === 'bidding_r2';
    const { nextState, speechText } = applyBidDecision(
      activeTable,
      activeTable.mySeatIndex,
      choice,
      suit,
    );
    const voiceCall: BalootVoiceCall =
      choice === 'sun'
        ? 'sun'
        : choice === 'hokm'
          ? isR2
            ? 'hokm_r2'
            : 'hokm'
          : isR2
            ? 'pass_r2'
            : 'pass_r1';
    triggerBubble(activeTable.mySeatIndex, speechText);
    saudiVoice.speakCall(voiceCall);
    void commitTableState(nextState);
  };

  const handleDoubling = (raise: boolean) => {
    if (!activeTable || activeTable.turnIndex !== activeTable.mySeatIndex) return;
    const { nextState, speechText } = applyDoublingDecision(
      activeTable,
      activeTable.mySeatIndex,
      raise,
    );
    triggerBubble(activeTable.mySeatIndex, speechText);
    void commitTableState(nextState);
  };

  const handleDeclareProjects = (selected: DetectedProject[]) => {
    if (!activeTable) return;
    const mySeat = activeTable.mySeatIndex;
    const myTeam = mySeat % 2;
    const labels = selected.map((p) => p.description);
    const nextDeclared = activeTable.declaredProjectSeats.includes(mySeat)
      ? activeTable.declaredProjectSeats
      : [...activeTable.declaredProjectSeats, mySeat];

    const nextUsProj =
      myTeam === 0 ? [...activeTable.usProjects, ...labels].slice(0, 8) : activeTable.usProjects;
    const nextThemProj =
      myTeam === 1 ? [...activeTable.themProjects, ...labels].slice(0, 8) : activeTable.themProjects;

    if (labels.length > 0) {
      const announce = labels.join(' و ');
      triggerBubble(mySeat, announce);
    }

    void commitTableState({
      ...activeTable,
      usProjects: nextUsProj,
      themProjects: nextThemProj,
      declaredProjectSeats: nextDeclared,
      lastActionText:
        labels.length > 0
          ? `${activeTable.seats[mySeat]?.name || 'لاعب'} أعلن: ${labels.join('، ')}`
          : activeTable.lastActionText,
    });
  };

  const handlePlayCard = (card: BalootCard) => {
    if (!activeTable || activeTable.turnIndex !== activeTable.mySeatIndex) return;
    saudiVoice.playCardSnap();
    const nextState = applyCardPlay(activeTable, activeTable.mySeatIndex, card);
    void commitTableState(nextState);
  };

  const handleNextRound = () => {
    if (!activeTable) return;
    const nextDealer = (activeTable.dealerIndex + 1) % 4;
    const nextState = dealNewRound({
      dealerIndex: nextDealer,
      roundNumber: activeTable.roundNumber + 1,
      seats: activeTable.seats,
      existingUsScore: activeTable.usScore,
      existingThemScore: activeTable.themScore,
      roomCode: activeTable.roomCode,
      isOnline: activeTable.isOnline,
      hostId: activeTable.hostId,
      mySeatIndex: activeTable.mySeatIndex,
    });
    void commitTableState(nextState);
  };

  const handleSendReaction = (emoji: string) => {
    if (!activeTable || !profile) return;
    triggerBubble(activeTable.mySeatIndex, emoji);
    if (activeTable.isOnline && activeTable.roomCode) {
      void sendRoomMessage({
        roomCode: activeTable.roomCode,
        senderUid: profile.userId,
        senderName: profile.displayName,
        seatIndex: activeTable.mySeatIndex,
        text: `${EMOJI_PREFIX}${emoji}`,
      });
    }
  };

  const handleSendChat = (text: string) => {
    if (!activeTable || !profile) return;
    triggerBubble(activeTable.mySeatIndex, text);
    if (activeTable.isOnline && activeTable.roomCode) {
      void sendRoomMessage({
        roomCode: activeTable.roomCode,
        senderUid: profile.userId,
        senderName: profile.displayName,
        seatIndex: activeTable.mySeatIndex,
        text,
      });
    } else {
      setLocalChat((prev) => [
        ...prev,
        {
          id: `${Date.now()}`,
          senderUid: profile.userId,
          senderName: profile.displayName,
          seatIndex: activeTable.mySeatIndex,
          text,
          createdAtMs: Date.now(),
        },
      ]);
    }
  };

  const handleExitTable = () => {
    setSoloTable(null);
    setActiveRoomCode(null);
    setActiveRoom(null);
    setLocalChat([]);
    setLastRatingDelta(null);
    recordedMatchKeyRef.current = null;
  };

  // --- Solo AI & Online Session Actions ---
  const handleStartSoloAi = () => {
    if (!profile) return;
    recordedMatchKeyRef.current = null;
    setLastRatingDelta(null);
    setLocalChat([]);
    const seats: BalootSeat[] = [
      {
        seatIndex: 0,
        uid: profile.userId,
        name: profile.displayName,
        rating: profile.rating,
        isBot: false,
        avatarUrl: profile.avatarUrl,
      },
      { seatIndex: 1, uid: 'bot_1', name: `${BOT_NAMES[0]} (آلي)`, rating: 1350, isBot: true },
      { seatIndex: 2, uid: 'bot_2', name: `${BOT_NAMES[1]} (خويك)`, rating: 1420, isBot: true },
      { seatIndex: 3, uid: 'bot_3', name: `${BOT_NAMES[2]} (آلي)`, rating: 1380, isBot: true },
    ];
    const dealt = dealNewRound({
      dealerIndex: 3,
      roundNumber: 1,
      seats,
      existingUsScore: 0,
      existingThemScore: 0,
      roomCode: null,
      isOnline: false,
      hostId: profile.userId,
      mySeatIndex: 0,
    });
    setSoloTable(dealt);
  };

  const isSubscribed = settings?.subscriptionPlan !== 'none';
  const usedToday =
    settings && settings.dailySessionsDate === todayDateString() ? settings.dailySessionsCount : 0;
  const remainingFreeToday = Math.max(0, FREE_DAILY_SESSION_LIMIT - usedToday);

  const handleCreateOnlineRoom = async (roomType: 'public' | 'private') => {
    if (!profile || !settings) return;
    if (!isSubscribed && remainingFreeToday <= 0) {
      setSubModalReason(
        PURCHASES_ENABLED
          ? 'استهلكت الجلستين المجانيتين المسموحتين لليوم. اشترك في شدها بلس (أسبوعي 9.99 ريال، شهري 29 ريال، سنوي 149 ريال) لإنشاء جلسات أونلاين بلا حدود!'
          : 'استهلكت الجلستين المجانيتين المسموحتين لليوم. الاشتراكات المدفوعة مقفلة حالياً — يمكنك الانضمام لأي جلسة أو اللعب الفوري مجاناً!',
      );
      setSubModalOpen(true);
      return;
    }
    setBusy(true);
    try {
      const code = await createOnlineRoom(profile, roomType);
      const today = todayDateString();
      const nextCount = settings.dailySessionsDate === today ? settings.dailySessionsCount + 1 : 1;
      await saveUserSettings(profile.userId, {
        dailySessionsDate: today,
        dailySessionsCount: nextCount,
      });
      setActiveRoomCode(code);
      showToast(`تم إنشاء الجلسة ${code} بنجاح!`);
    } finally {
      setBusy(false);
    }
  };

  const handleJoinRoomCode = async (code: string) => {
    if (!profile) return;
    setBusy(true);
    try {
      const res = await joinOnlineRoom(code, profile);
      if (res.ok && res.roomCode) {
        setActiveRoomCode(res.roomCode);
      } else if (res.message) {
        showToast(res.message);
      }
    } finally {
      setBusy(false);
    }
  };

  const handleInviteFriendFromFriendsPage = async (friend: FriendEntry) => {
    if (!profile || !settings) return;
    if (activeRoom && activeRoom.status === 'waiting') {
      await sendTableInvite({ fromProfile: profile, toFriend: friend, roomCode: activeRoom.roomCode });
      showToast(`تم إرسال دعوة مباشرة إلى ${friend.friendName}`);
      return;
    }
    if (!isSubscribed && remainingFreeToday <= 0) {
      setSubModalReason(
        PURCHASES_ENABLED
          ? 'لديك جلستان مجانيتان فقط يومياً لإنشاء الجلسات الأونلاين. اشترك في شدها بلس لإنشاء طاولات خاصة بلا حدود.'
          : 'لديك جلستان مجانيتان فقط يومياً لإنشاء الجلسات الأونلاين. الاشتراكات المدفوعة مقفلة حالياً — الوضع المجاني فقط مفعّل.',
      );
      setSubModalOpen(true);
      return;
    }
    setBusy(true);
    try {
      const code = await createOnlineRoom(profile, 'private');
      const today = todayDateString();
      const nextCount = settings.dailySessionsDate === today ? settings.dailySessionsCount + 1 : 1;
      await saveUserSettings(profile.userId, {
        dailySessionsDate: today,
        dailySessionsCount: nextCount,
      });
      await sendTableInvite({ fromProfile: profile, toFriend: friend, roomCode: code });
      setActiveRoomCode(code);
      showToast(`تم إنشاء طاولة خاصة (${code}) وإرسال الدعوة إلى ${friend.friendName}`);
    } finally {
      setBusy(false);
    }
  };

  // --- Store & Subscription Handlers ---
  const handleSelectOrUnlockBg = async (bgId: TableBgId) => {
    if (!profile || !settings) return;
    const meta = TABLE_BACKGROUNDS.find((b) => b.id === bgId);
    if (!meta) return;
    const owned = meta.priceSar === 0 || settings.ownedTableBgs.includes(bgId);
    if (owned) {
      await saveUserSettings(profile.userId, { selectedTableBg: bgId });
      showToast(`تم تفعيل ${meta.title} كخلفية لطاولتك`);
      return;
    }
    if (!PURCHASES_ENABLED) {
      showToast('عملية الشراء مقفلة حالياً — الخلفيات المجانية فقط مفعّلة في الوقت الحالي.');
      return;
    }
    const nextOwned = Array.from(new Set([...settings.ownedTableBgs, bgId])) as TableBgId[];
    await saveUserSettings(profile.userId, {
      ownedTableBgs: nextOwned,
      selectedTableBg: bgId,
    });
    showToast(`تم فتح وتفعيل ${meta.title} (${meta.priceSar} ريال)`);
  };

  const handleActivatePlan = async (planId: Exclude<SubscriptionPlanId, 'none'>) => {
    if (!profile) return;
    if (!PURCHASES_ENABLED) {
      showToast('الاشتراكات المدفوعة مقفلة حالياً — الوضع المجاني فقط مفعّل في الوقت الحالي.');
      return;
    }
    await saveUserSettings(profile.userId, { subscriptionPlan: planId });
    setSubModalOpen(false);
    setSubModalReason(null);
    showToast('تم تفعيل اشتراك شدها بلس! يمكنك الآن إنشاء جلسات أونلاين بلا حدود.');
  };

  // --- Render Gates ---
  if (legalPage) {
    return <LegalPage page={legalPage} onBack={() => setLegalPage(null)} />;
  }

  if (!authReady) {
    return (
      <div className="min-h-screen bg-white flex flex-col items-center justify-center gap-4">
        <img src="/assets/shaddaha_logo.png" alt="شدها" className="w-44 object-contain" />
        <p className="text-sm font-bold text-[#002BFF]">لعبة بلوت .. تفهــــمك</p>
      </div>
    );
  }

  if (!fbUser) {
    return <AuthScreen onOpenLegal={setLegalPage} />;
  }

  if (!profile || !settings) {
    return (
      <div className="min-h-screen bg-white flex flex-col items-center justify-center gap-3 px-4 text-center">
        <img src="/assets/shaddaha_logo.png" alt="شدها" className="w-40 object-contain" />
        {bootstrapError ? (
          <>
            <p className="text-sm font-bold text-[#E52521]">{bootstrapError}</p>
            <button
              type="button"
              onClick={() => window.location.reload()}
              className="px-4 py-2 rounded-xl bg-[#002BFF] text-white text-xs font-bold cursor-pointer"
            >
              إعادة المحاولة
            </button>
          </>
        ) : (
          <p className="text-sm font-semibold text-slate-600">جاري تجهيز حسابك...</p>
        )}
      </div>
    );
  }

  // Full-screen Active Baloot Table
  if (activeTable) {
    return (
      <>
        <GameTableScreen
          table={activeTable}
          tableBgId={settings.selectedTableBg}
          myAvatarUrl={profile.avatarUrl}
          isMuted={isMuted}
          chatMessages={activeTable.isOnline ? onlineChat : localChat}
          seatBubbles={seatBubbles}
          turnSecondsLeft={turnSecondsLeft}
          lastRatingDelta={lastRatingDelta}
          onToggleMute={() => {
            const next = !isMuted;
            setIsMuted(next);
            saudiVoice.setMuted(next);
          }}
          onBid={handleBid}
          onDoubling={handleDoubling}
          onDeclareProjects={handleDeclareProjects}
          onPlayCard={handlePlayCard}
          onNextRound={handleNextRound}
          onSendReaction={handleSendReaction}
          onSendChat={handleSendChat}
          onExitTable={handleExitTable}
        />
        {toast && (
          <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[60] bg-[#111827] text-white text-xs font-bold px-4 py-2.5 rounded-xl shadow-lg">
            {toast}
          </div>
        )}
      </>
    );
  }

  const navItems: { id: MainTab; label: string; icon: React.FC<{ className?: string }> }[] = [
    { id: 'lobby', label: 'الرئيسية', icon: Home },
    { id: 'friends', label: 'الأصدقاء', icon: Users },
    { id: 'leaderboard', label: 'المتصدرين', icon: Trophy },
    { id: 'store', label: 'المتجر', icon: ShoppingBag },
    { id: 'calculator', label: 'الحاسبة', icon: Calculator },
    { id: 'history', label: 'السجل', icon: History },
    { id: 'profile', label: 'حسابي', icon: UserIcon },
  ];

  return (
    <div className="min-h-screen bg-white flex flex-col">
      {/* Top Official Navigation Header (Clean Typography Links) */}
      <header className="sticky top-0 z-30 bg-white/95 backdrop-blur border-b border-slate-200">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between gap-4">
          <button
            type="button"
            onClick={() => {
              setActiveRoomCode(null);
              setTab('lobby');
            }}
            className="flex items-center gap-2.5 cursor-pointer shrink-0"
          >
            <img src="/assets/shaddaha_logo.png" alt="شدها" className="h-10 w-auto object-contain" />
            <span className="hidden sm:inline text-xs font-bold text-[#002BFF]">
              لعبة بلوت .. تفهمك
            </span>
          </button>

          {/* Desktop Navigation Links */}
          <nav className="hidden md:flex items-center gap-6">
            {navItems.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => setTab(item.id)}
                className={`text-sm font-bold py-1 border-b-2 transition-colors cursor-pointer ${
                  tab === item.id
                    ? 'text-[#002BFF] border-[#002BFF]'
                    : 'text-slate-600 border-transparent hover:text-[#111827]'
                }`}
              >
                {item.label}
              </button>
            ))}
          </nav>

          {/* Compact User Summary */}
          <button
            type="button"
            onClick={() => setTab('profile')}
            className="flex items-center gap-2 cursor-pointer"
          >
            <div className="text-left hidden sm:block">
              <div className="text-xs font-bold text-[#111827] leading-tight">{profile.displayName}</div>
              <div className="text-[11px] font-semibold text-[#002BFF]">
                {profile.rating} نقطة
              </div>
            </div>
            <img
              src={profile.avatarUrl || '/assets/shaddaha_avatar.png'}
              alt={profile.displayName}
              className="w-9 h-9 rounded-full object-cover border border-slate-200 bg-slate-100"
            />
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 pb-20 md:pb-8">
        {activeRoom && activeRoom.status === 'waiting' ? (
          <WaitingRoomScreen
            room={activeRoom}
            currentUid={profile.userId}
            friends={friends}
            busy={busy}
            onInviteFriend={(f) => {
              void sendTableInvite({
                fromProfile: profile,
                toFriend: f,
                roomCode: activeRoom.roomCode,
              });
              showToast(`تم إرسال الدعوة إلى ${f.friendName}`);
            }}
            onStartMatch={async () => {
              setBusy(true);
              try {
                await startOnlineRoomMatch(activeRoom);
              } finally {
                setBusy(false);
              }
            }}
            onLeaveRoom={async () => {
              await leaveWaitingRoom(activeRoom, profile.userId);
              setActiveRoomCode(null);
              setActiveRoom(null);
            }}
          />
        ) : (
          <>
            {tab === 'lobby' && (
              <LobbyScreen
                profile={profile}
                settings={settings}
                publicRooms={publicRooms}
                incomingInvites={incomingInvites}
                remainingFreeToday={remainingFreeToday}
                busy={busy}
                onStartSoloAi={handleStartSoloAi}
                onCreateOnlineRoom={handleCreateOnlineRoom}
                onJoinRoomCode={handleJoinRoomCode}
                onAcceptInvite={async (inv) => {
                  await respondToInvite(inv.id, 'accepted');
                  await handleJoinRoomCode(inv.roomCode);
                }}
                onDeclineInvite={(inv) => void respondToInvite(inv.id, 'declined')}
                onNavigate={(dest) => setTab(dest)}
                onOpenSubscription={() => {
                  setSubModalReason(null);
                  setSubModalOpen(true);
                }}
              />
            )}

            {tab === 'friends' && (
              <FriendsScreen
                profile={profile}
                friends={friends}
                leaderboard={leaderboard}
                incomingInvites={incomingInvites}
                onAddFriendByCode={(code) => addFriendByPlayerCode(profile.userId, code)}
                onRemoveFriend={(uid) => {
                  void removeFriend(profile.userId, uid);
                  showToast('تمت إزالة الصديق من قائمتك');
                }}
                onInviteFriendToPrivateTable={handleInviteFriendFromFriendsPage}
                onAcceptInvite={async (inv) => {
                  await respondToInvite(inv.id, 'accepted');
                  await handleJoinRoomCode(inv.roomCode);
                }}
                onDeclineInvite={(inv) => void respondToInvite(inv.id, 'declined')}
                onCreatePrivateTable={() => void handleCreateOnlineRoom('private')}
              />
            )}

            {tab === 'leaderboard' && (
              <LeaderboardScreen players={leaderboard} currentUid={profile.userId} />
            )}

            {tab === 'history' && <HistoryScreen profile={profile} records={history} />}

            {tab === 'store' && (
              <StoreAndSubScreen
                settings={settings}
                remainingFreeToday={remainingFreeToday}
                onSelectOrUnlockBg={handleSelectOrUnlockBg}
                onActivatePlan={handleActivatePlan}
              />
            )}

            {tab === 'calculator' && <BalootCalculatorScreen />}

            {tab === 'profile' && (
              <ProfileScreen
                profile={profile}
                settings={settings}
                googlePhotoUrl={fbUser.photoURL}
                loading={profileLoading}
                saving={profileSaving}
                errorMessage={profileError}
                successMessage={profileSuccess}
                onRefresh={handleRefreshProfile}
                onUpdateProfile={handleUpdateProfile}
                onSelectTableBg={handleSelectOrUnlockBg}
                onOpenStore={() => setTab('store')}
                onSignOut={async () => {
                  await updatePlayerPresence(profile.userId, 'offline');
                  await signOut(auth);
                }}
              />
            )}
          </>
        )}
      </main>

      {/* Quiet Official Footer (Desktop) */}
      <footer className="hidden md:block border-t border-slate-200 bg-white">
        <div className="max-w-6xl mx-auto px-4 py-5 flex items-center justify-between text-xs text-slate-500">
          <div>© {new Date().getFullYear()} شدها — لعبة بلوت .. تفهمك. جميع الحقوق محفوظة.</div>
          <div className="flex items-center gap-4">
            <button
              type="button"
              onClick={() => setLegalPage('privacy')}
              className="hover:text-[#002BFF] hover:underline cursor-pointer"
            >
              سياسة الخصوصية
            </button>
            <button
              type="button"
              onClick={() => setLegalPage('terms')}
              className="hover:text-[#002BFF] hover:underline cursor-pointer"
            >
              الشروط والأحكام
            </button>
          </div>
        </div>
      </footer>

      {/* Mobile Bottom Navigation */}
      <nav className="md:hidden fixed bottom-0 inset-x-0 z-30 bg-white border-t border-slate-200 px-2 py-1.5 flex items-center justify-around">
        {[
          { id: 'lobby' as MainTab, label: 'الرئيسية', icon: Home },
          { id: 'friends' as MainTab, label: 'الأصدقاء', icon: Users },
          { id: 'store' as MainTab, label: 'المتجر', icon: ShoppingBag },
          { id: 'leaderboard' as MainTab, label: 'المتصدرين', icon: Trophy },
          { id: 'profile' as MainTab, label: 'حسابي', icon: UserIcon },
        ].map((item) => {
          const Icon = item.icon;
          const active = tab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => setTab(item.id)}
              className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl text-[11px] font-bold cursor-pointer ${
                active ? 'text-[#002BFF]' : 'text-slate-500'
              }`}
            >
              <Icon className="w-5 h-5" />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Subscription Modal */}
      <SubscriptionModal
        open={subModalOpen}
        reasonMessage={subModalReason}
        activePlan={settings.subscriptionPlan}
        onSelectPlan={handleActivatePlan}
        onClose={() => {
          setSubModalOpen(false);
          setSubModalReason(null);
        }}
      />

      {/* Toast Notification */}
      {toast && (
        <div className="fixed bottom-20 md:bottom-6 left-1/2 -translate-x-1/2 z-50 bg-[#111827] text-white text-xs sm:text-sm font-bold px-5 py-3 rounded-2xl shadow-xl">
          {toast}
        </div>
      )}
    </div>
  );
}
