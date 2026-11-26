import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Check,
  Clock,
  LogOut,
  MessageCircle,
  Send,
  Smile,
  Sparkles,
  Volume2,
  VolumeX,
  X,
} from 'lucide-react';
import {
  BalootCard,
  BalootTableState,
  DetectedProject,
  RoomMessage,
  Suit,
  TableBgId,
} from '../game/balootTypes';
import {
  BidAction,
  cardCode,
  detectDeclarableProjectsForHand,
  getLegalCards,
  QUICK_CHAT_PHRASES,
  QUICK_REACTIONS,
  SUIT_INFO,
  SUITS,
} from '../game/balootEngine';
import { TableBackgroundCanvas } from './TableBackgroundCanvas';
import { CardBack, PlayingCard } from './PlayingCard';

interface SeatBubble {
  text: string;
  ts: number;
}

interface GameTableScreenProps {
  table: BalootTableState;
  tableBgId: TableBgId;
  myAvatarUrl: string | null;
  isMuted: boolean;
  chatMessages: RoomMessage[];
  seatBubbles: Record<number, SeatBubble>;
  turnSecondsLeft: number;
  lastRatingDelta: number | null;
  onToggleMute: () => void;
  onBid: (choice: BidAction, suit?: Suit | null) => void;
  onDoubling: (raise: boolean) => void;
  onDeclareProjects: (selected: DetectedProject[]) => void;
  onPlayCard: (card: BalootCard) => void;
  onNextRound: () => void;
  onSendReaction: (emoji: string) => void;
  onSendChat: (text: string) => void;
  onExitTable: () => void;
}

export const GameTableScreen: React.FC<GameTableScreenProps> = ({
  table,
  tableBgId,
  myAvatarUrl,
  isMuted,
  chatMessages,
  seatBubbles,
  turnSecondsLeft,
  lastRatingDelta,
  onToggleMute,
  onBid,
  onDoubling,
  onDeclareProjects,
  onPlayCard,
  onNextRound,
  onSendReaction,
  onSendChat,
  onExitTable,
}) => {
  const [liftedCardKey, setLiftedCardKey] = useState<string | null>(null);
  const [showSuitPicker, setShowSuitPicker] = useState(false);
  const [showReactions, setShowReactions] = useState(false);
  const [showChat, setShowChat] = useState(false);
  const [chatInput, setChatInput] = useState('');
  const [selectedProjectIds, setSelectedProjectIds] = useState<string[]>([]);
  const chatEndRef = useRef<HTMLDivElement | null>(null);

  const mySeat = table.mySeatIndex;
  const myTeam = mySeat % 2;
  const ourScore = myTeam === 0 ? table.usScore : table.themScore;
  const oppScore = myTeam === 0 ? table.themScore : table.usScore;
  const myHand = table.hands[mySeat] || [];
  const isMyTurn = table.turnIndex === mySeat;
  const isStandby = table.standbySeats.includes(mySeat);

  // Relative seat mapping so player is always bottom
  const topSeatIdx = (mySeat + 2) % 4;
  const rightSeatIdx = (mySeat + 1) % 4;
  const leftSeatIdx = (mySeat + 3) % 4;

  // Detect human declarable projects after deal and before playing
  const declarableProjects = useMemo(() => {
    if (
      table.phase !== 'playing' ||
      table.usTricksCount > 0 ||
      table.themTricksCount > 0 ||
      table.tableCards.length > 0 ||
      table.declaredProjectSeats.includes(mySeat) ||
      isStandby
    ) {
      return [];
    }
    return detectDeclarableProjectsForHand(myHand, mySeat, table.mode, table.trumpSuit);
  }, [table, myHand, mySeat, isStandby]);

  const showProjectModal = declarableProjects.length > 0;

  // Pre-select all detected projects when modal opens
  useEffect(() => {
    if (declarableProjects.length > 0) {
      setSelectedProjectIds(declarableProjects.map((p) => p.id));
    }
  }, [declarableProjects]);

  // Reset lifted card when turn or hand changes
  useEffect(() => {
    setLiftedCardKey(null);
    setShowSuitPicker(false);
  }, [table.turnIndex, table.phase, myHand.length]);

  useEffect(() => {
    if (showChat) {
      chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [chatMessages, showChat]);

  const legalCards = useMemo(() => {
    if (table.phase !== 'playing' || !isMyTurn || isStandby || showProjectModal) return [];
    return getLegalCards(myHand, table.tableCards, table.mode, table.trumpSuit, mySeat);
  }, [table, myHand, mySeat, isMyTurn, isStandby, showProjectModal]);

  const legalSet = useMemo(() => new Set(legalCards.map(cardCode)), [legalCards]);

  // Split player hand cleanly into 4 top cards + 4 bottom cards
  const topRowCards = myHand.slice(0, 4);
  const bottomRowCards = myHand.slice(4, 8);

  const handleCardClick = (card: BalootCard) => {
    if (table.phase !== 'playing' || !isMyTurn || isStandby || showProjectModal) return;
    const key = cardCode(card);
    if (!legalSet.has(key)) return;
    if (liftedCardKey === key) {
      // Second tap throws card to center
      setLiftedCardKey(null);
      onPlayCard(card);
    } else {
      // First tap lifts card upward
      setLiftedCardKey(key);
    }
  };

  const handleChatSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const clean = chatInput.trim();
    if (!clean) return;
    onSendChat(clean);
    setChatInput('');
  };

  const contractLabel =
    table.mode === 'sun'
      ? 'صن'
      : table.mode === 'hokm' && table.trumpSuit
        ? `حكم ${SUIT_INFO[table.trumpSuit].arabic} ${SUIT_INFO[table.trumpSuit].symbol}`
        : table.phase === 'bidding_r2'
          ? 'مشترى ج2'
          : 'مشترى ج1';

  const bidMultiplierLabel =
    table.bidLevel === 2
      ? 'دبل ×2'
      : table.bidLevel === 3
        ? 'ثري ×3'
        : table.bidLevel >= 4
          ? 'قهوة ×4'
          : null;

  const isLightTable = tableBgId === 'classic_white';

  const renderOpponentSeat = (seatIdx: number, position: 'top' | 'right' | 'left') => {
    const seat = table.seats[seatIdx];
    const active = table.turnIndex === seatIdx && (table.phase === 'bidding_r1' || table.phase === 'bidding_r2' || table.phase === 'doubling' || table.phase === 'playing');
    const bubble = seatBubbles[seatIdx];
    const seatStandby = table.standbySeats.includes(seatIdx);
    const cardsLeft = (table.hands[seatIdx] || []).length;

    return (
      <div className="relative flex flex-col items-center gap-1 select-none">
        {/* White-background speech/reaction bubble */}
        {bubble && (
          <div
            className={`animate-bubble-pop absolute z-30 bg-white text-[#111827] border border-slate-200 shadow-lg rounded-2xl px-3 py-1.5 text-sm font-bold whitespace-nowrap max-w-[170px] truncate ${
              position === 'top'
                ? 'top-full mt-1.5'
                : position === 'right'
                  ? 'left-full ml-2 top-1'
                  : 'right-full mr-2 top-1'
            }`}
          >
            {bubble.text}
          </div>
        )}

        <div
          className={`relative rounded-2xl px-2.5 py-1.5 flex items-center gap-2 border-2 transition-all ${
            active
              ? 'bg-white border-[#FFD000] shadow-lg scale-105'
              : isLightTable
                ? 'bg-slate-100/95 border-slate-200 text-[#111827]'
                : 'bg-white/95 border-white/80 text-[#111827]'
          }`}
        >
          <div className="relative">
            <img
              src={seat?.avatarUrl || '/assets/shaddaha_avatar.png'}
              alt={seat?.name || 'لاعب'}
              className="w-8 h-8 sm:w-9 sm:h-9 rounded-full object-cover border border-slate-200 bg-slate-100"
            />
            {active && (
              <span
                className={`absolute -top-1.5 -right-1.5 text-[10px] font-bold w-5 h-5 rounded-full flex items-center justify-center shadow ${
                  turnSecondsLeft <= 3 ? 'bg-[#E52521] text-white' : 'bg-[#002BFF] text-white'
                }`}
              >
                {turnSecondsLeft}
              </span>
            )}
          </div>
          <div className="text-right leading-tight max-w-[88px] sm:max-w-[115px]">
            <div className="text-xs font-bold text-[#111827] truncate">
              {seat?.name || `لاعب ${seatIdx + 1}`}
            </div>
            <div className="text-[10px] text-slate-500 flex items-center gap-1">
              <span>{cardsLeft} ورق</span>
              {table.dealerIndex === seatIdx && <span className="text-[#002BFF] font-bold">· موزع</span>}
              {seatStandby && <span className="text-[#E52521] font-bold">· آلي</span>}
            </div>
          </div>
        </div>

        {/* Mini card stack */}
        {position === 'top' && cardsLeft > 0 && (
          <div className="flex items-center -space-x-4 space-x-reverse mt-0.5">
            {Array.from({ length: Math.min(cardsLeft, 5) }).map((_, idx) => (
              <CardBack key={idx} small />
            ))}
          </div>
        )}
      </div>
    );
  };

  // Position played cards around center according to seat relative to player
  const playedCardPos = (seatIdx: number): string => {
    if (seatIdx === mySeat) return 'bottom-1 left-1/2 -translate-x-1/2 z-20';
    if (seatIdx === topSeatIdx) return 'top-1 left-1/2 -translate-x-1/2 z-10';
    if (seatIdx === rightSeatIdx) return 'right-1 sm:right-3 top-1/2 -translate-y-1/2 z-10';
    return 'left-1 sm:left-3 top-1/2 -translate-y-1/2 z-10';
  };

  const nextDoubleLabel =
    table.bidLevel === 1 ? 'دبل (×2)' : table.bidLevel === 2 ? 'ثري (×3)' : 'قهوة (×4)';

  return (
    <div className="fixed inset-0 z-40 flex flex-col overflow-hidden bg-white">
      {/* FULL-BLEED TABLE BACKGROUND — ZERO WHITE CENTER CIRCLE */}
      <TableBackgroundCanvas bgId={tableBgId} />

      {/* TOP ROYAL BLUE HEADER & SCOREBOARD (Matching Mockup 3) */}
      <header className="relative z-20 shrink-0">
        <div className="bg-[#002BFF] text-white px-3 sm:px-6 pt-2.5 pb-6 rounded-b-[28px] shadow-md flex items-center justify-between">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onExitTable}
              className="w-10 h-10 rounded-xl bg-white/15 hover:bg-white/25 flex items-center justify-center transition-colors cursor-pointer"
              title="الخروج من الطاولة"
            >
              <LogOut className="w-5 h-5" />
            </button>
            <button
              type="button"
              onClick={onToggleMute}
              className="w-10 h-10 rounded-xl bg-white/15 hover:bg-white/25 flex items-center justify-center transition-colors cursor-pointer"
              title={isMuted ? 'تفعيل الصوت السعودي' : 'كتم الصوت'}
            >
              {isMuted ? <VolumeX className="w-5 h-5" /> : <Volume2 className="w-5 h-5" />}
            </button>
          </div>

          <div className="text-center hidden sm:block">
            <div className="text-xs text-white/80 font-medium">
              صكة 152 · جولة {table.roundNumber}
              {table.roomCode ? ` · غرفة ${table.roomCode}` : ' · ضد الذكاء الاصطناعي'}
            </div>
            <div className="text-sm font-bold text-[#FFD000] mt-0.5">
              {contractLabel}
              {bidMultiplierLabel ? ` · ${bidMultiplierLabel}` : ''}
            </div>
          </div>

          <div className="flex items-center gap-2 text-xs font-bold">
            <span className="bg-white/15 px-3 py-1.5 rounded-xl">
              {contractLabel}
              {bidMultiplierLabel ? ` (${bidMultiplierLabel})` : ''}
            </span>
          </div>
        </div>

        {/* Floating Scoreboard Card ("لنا / لهم" in Red #E52521) */}
        <div className="max-w-[260px] mx-auto -mt-5 bg-[#E6E8EE] rounded-2xl px-4 py-2 shadow-md border border-white flex items-center justify-around">
          <div className="text-center">
            <div className="text-xs font-bold text-slate-600 mb-1">لنا</div>
            <div className="bg-[#E52521] text-white font-bold text-lg px-4 py-0.5 rounded-full min-w-[72px] shadow-sm">
              {ourScore}
            </div>
          </div>
          <div className="text-xs font-bold text-slate-400">/ 152</div>
          <div className="text-center">
            <div className="text-xs font-bold text-slate-600 mb-1">لهم</div>
            <div className="bg-[#E52521] text-white font-bold text-lg px-4 py-0.5 rounded-full min-w-[72px] shadow-sm">
              {oppScore}
            </div>
          </div>
        </div>
      </header>

      {/* STANDBY BANNER IF PLAYER TIMED OUT */}
      {isStandby && table.phase !== 'round_summary' && table.phase !== 'match_finished' && (
        <div className="relative z-20 mx-auto mt-1.5 px-4 py-1.5 rounded-xl bg-[#FFD000] text-[#111827] text-xs font-bold shadow flex items-center gap-2">
          <Clock className="w-4 h-4 text-[#E52521]" />
          <span>انتهى وقتك — أنت في وضع الانتظار (اللعب التلقائي) حتى نهاية هذه الجولة</span>
        </div>
      )}

      {/* MIDDLE PLAY ARENA — TRANSPARENT SO FULL TABLE BACKGROUND SHOWS THROUGH */}
      <main className="relative z-10 flex-1 flex flex-col justify-between px-3 sm:px-8 py-2 min-h-0">
        {/* Top Opponent (Partner) */}
        <div className="flex justify-center">{renderOpponentSeat(topSeatIdx, 'top')}</div>

        {/* Left Seat + Center Play Field + Right Seat */}
        <div className="flex-1 flex items-center justify-between gap-2 my-1 min-h-0">
          <div>{renderOpponentSeat(rightSeatIdx, 'right')}</div>

          {/* Center Trick / Open Card Area — NO WHITE CIRCLE */}
          <div className="relative flex-1 max-w-[300px] h-[190px] sm:h-[225px] flex items-center justify-center">
            {/* Open Card during Bidding */}
            {(table.phase === 'bidding_r1' || table.phase === 'bidding_r2') && table.openCard && (
              <div className="flex flex-col items-center gap-2">
                <PlayingCard card={table.openCard} size="md" animatedThrow />
                <div className="bg-white/95 text-[#111827] px-3 py-1 rounded-xl text-xs font-bold shadow border border-slate-200">
                  الورقة المكشوفة · {SUIT_INFO[table.openCard.suit].arabic}
                </div>
              </div>
            )}

            {/* Played Cards on Table */}
            {table.phase === 'playing' &&
              table.tableCards.map((entry) => (
                <div
                  key={`${entry.seatIndex}-${cardCode(entry.card)}`}
                  className={`absolute ${playedCardPos(entry.seatIndex)}`}
                >
                  <PlayingCard
                    card={entry.card}
                    size="md"
                    highlightTrump={table.mode === 'hokm' && entry.card.suit === table.trumpSuit}
                    animatedThrow
                  />
                </div>
              ))}

            {/* Action Ticker */}
            {table.lastActionText && table.phase === 'playing' && table.tableCards.length === 0 && (
              <div className="bg-white/95 text-[#111827] px-3.5 py-1.5 rounded-xl text-xs font-bold shadow border border-slate-200 text-center">
                {table.lastActionText}
              </div>
            )}
          </div>

          <div>{renderOpponentSeat(leftSeatIdx, 'left')}</div>
        </div>

        {/* Declared Projects Strip */}
        {(table.usProjects.length > 0 || table.themProjects.length > 0) && (
          <div className="flex items-center justify-center gap-3 text-xs font-bold mb-1">
            {table.usProjects.length > 0 && (
              <span className="bg-white/95 text-[#002BFF] px-2.5 py-1 rounded-lg shadow-sm border border-slate-200">
                مشاريعنا: {table.usProjects.join('، ')}
              </span>
            )}
            {table.themProjects.length > 0 && (
              <span className="bg-white/95 text-[#E52521] px-2.5 py-1 rounded-lg shadow-sm border border-slate-200">
                مشاريعهم: {table.themProjects.join('، ')}
              </span>
            )}
          </div>
        )}

        {/* PLAYER HAND: 4 CARDS TOP ROW + 4 CARDS BOTTOM ROW (LARGE & CLEAR) */}
        <div className="relative flex flex-col items-center gap-1.5 pb-1">
          {/* Player's own white reaction bubble */}
          {seatBubbles[mySeat] && (
            <div className="animate-bubble-pop mb-1 bg-white text-[#111827] border border-slate-200 shadow-lg rounded-2xl px-3.5 py-1.5 text-sm font-bold">
              {seatBubbles[mySeat].text}
            </div>
          )}

          {/* Top Row (4 cards) */}
          <div className="flex items-center justify-center gap-1.5 sm:gap-2.5">
            {topRowCards.map((card) => {
              const key = cardCode(card);
              const isLegal = table.phase === 'playing' && isMyTurn && !isStandby && !showProjectModal && legalSet.has(key);
              const isDimmed = table.phase === 'playing' && isMyTurn && !isStandby && !legalSet.has(key);
              return (
                <PlayingCard
                  key={key}
                  card={card}
                  size="hand"
                  selected={liftedCardKey === key}
                  disabled={isDimmed || table.phase !== 'playing' || !isMyTurn || isStandby}
                  highlightTrump={table.mode === 'hokm' && card.suit === table.trumpSuit}
                  onClick={isLegal ? () => handleCardClick(card) : undefined}
                />
              );
            })}
          </div>

          {/* Bottom Row (4 cards) */}
          {bottomRowCards.length > 0 && (
            <div className="flex items-center justify-center gap-1.5 sm:gap-2.5">
              {bottomRowCards.map((card) => {
                const key = cardCode(card);
                const isLegal = table.phase === 'playing' && isMyTurn && !isStandby && !showProjectModal && legalSet.has(key);
                const isDimmed = table.phase === 'playing' && isMyTurn && !isStandby && !legalSet.has(key);
                return (
                  <PlayingCard
                    key={key}
                    card={card}
                    size="hand"
                    selected={liftedCardKey === key}
                    disabled={isDimmed || table.phase !== 'playing' || !isMyTurn || isStandby}
                    highlightTrump={table.mode === 'hokm' && card.suit === table.trumpSuit}
                    onClick={isLegal ? () => handleCardClick(card) : undefined}
                  />
                );
              })}
            </div>
          )}
        </div>
      </main>

      {/* BOTTOM ROYAL BLUE CONTROL DOCK (Matching Mockup 3) */}
      <footer className="relative z-20 bg-[#002BFF] text-white rounded-t-[28px] px-3 sm:px-6 pt-3 pb-4 shadow-[0_-6px_24px_rgba(0,0,0,0.18)] shrink-0">
        {/* Top Row inside dock: Player badge + Timer + White Reactions Button + Chat Button */}
        <div className="max-w-2xl mx-auto flex items-center justify-between gap-2 mb-2.5">
          {/* Player Identity Tag */}
          <div className="flex items-center gap-2">
            <div className="relative">
              <img
                src={myAvatarUrl || '/assets/shaddaha_avatar.png'}
                alt="صورتك"
                className="w-11 h-11 rounded-full object-cover border-2 border-white bg-white"
              />
              {isMyTurn && !isStandby && (
                <span
                  className={`absolute -top-1.5 -right-1.5 w-6 h-6 rounded-full text-xs font-bold flex items-center justify-center shadow border-2 border-white ${
                    turnSecondsLeft <= 3 ? 'bg-[#E52521] text-white' : 'bg-[#FFD000] text-[#111827]'
                  }`}
                >
                  {turnSecondsLeft}
                </span>
              )}
            </div>
            <div className="bg-[#FFD000] text-[#111827] px-3 py-1 rounded-xl font-bold text-xs sm:text-sm shadow-sm">
              {table.seats[mySeat]?.name || 'أنت'}
            </div>
          </div>

          {/* Reactions (White Background!) & Chat Buttons */}
          <div className="flex items-center gap-2 relative">
            <button
              type="button"
              onClick={() => {
                setShowReactions((v) => !v);
                setShowChat(false);
              }}
              className="bg-white hover:bg-slate-50 text-[#111827] border border-slate-200 rounded-xl px-3 py-2 flex items-center gap-1.5 text-xs font-bold shadow cursor-pointer"
            >
              <Smile className="w-4 h-4 text-[#002BFF]" />
              <span>تعابير</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setShowChat((v) => !v);
                setShowReactions(false);
              }}
              className="bg-white hover:bg-slate-50 text-[#111827] border border-slate-200 rounded-xl px-3 py-2 flex items-center gap-1.5 text-xs font-bold shadow cursor-pointer relative"
            >
              <MessageCircle className="w-4 h-4 text-[#002BFF]" />
              <span>دردشة</span>
              {chatMessages.length > 0 && (
                <span className="bg-[#E52521] text-white text-[10px] font-bold px-1.5 rounded-full">
                  {chatMessages.length}
                </span>
              )}
            </button>

            {/* Reactions Picker Popover — WHITE BACKGROUND */}
            {showReactions && (
              <div className="absolute bottom-full left-0 mb-2 bg-white border border-slate-200 rounded-2xl p-3 shadow-2xl w-64 z-40">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-[#111827]">اختر تعبيراً</span>
                  <button
                    type="button"
                    onClick={() => setShowReactions(false)}
                    className="text-slate-400 hover:text-slate-700 cursor-pointer"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
                <div className="grid grid-cols-6 gap-1.5">
                  {QUICK_REACTIONS.map((emoji) => (
                    <button
                      key={emoji}
                      type="button"
                      onClick={() => {
                        onSendReaction(emoji);
                        setShowReactions(false);
                      }}
                      className="h-10 rounded-xl bg-white hover:bg-slate-100 border border-slate-100 text-xl flex items-center justify-center transition-transform active:scale-90 cursor-pointer"
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Contextual Action Buttons: Bidding R1/R2, Doubling, or Turn Status */}
        <div className="max-w-2xl mx-auto">
          {isMyTurn && !isStandby && table.phase === 'bidding_r1' && (
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => onBid('sun')}
                className="py-3 rounded-xl bg-white hover:bg-slate-50 text-[#002BFF] font-bold text-sm sm:text-base shadow cursor-pointer"
              >
                صن
              </button>
              <button
                type="button"
                disabled={table.highestBidMode !== 'none'}
                onClick={() => onBid('hokm', table.openCard?.suit)}
                className="py-3 rounded-xl bg-white hover:bg-slate-50 text-[#002BFF] font-bold text-sm sm:text-base shadow disabled:opacity-45 cursor-pointer"
              >
                حكم {table.openCard ? SUIT_INFO[table.openCard.suit].symbol : ''}
              </button>
              <button
                type="button"
                onClick={() => onBid('pass')}
                className="py-3 rounded-xl bg-white hover:bg-slate-50 text-[#E52521] font-bold text-sm sm:text-base shadow cursor-pointer"
              >
                بس
              </button>
            </div>
          )}

          {isMyTurn && !isStandby && table.phase === 'bidding_r2' && !showSuitPicker && (
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => onBid('sun')}
                className="py-3 rounded-xl bg-white hover:bg-slate-50 text-[#002BFF] font-bold text-sm sm:text-base shadow cursor-pointer"
              >
                صن
              </button>
              <button
                type="button"
                onClick={() => setShowSuitPicker(true)}
                className="py-3 rounded-xl bg-white hover:bg-slate-50 text-[#002BFF] font-bold text-sm sm:text-base shadow cursor-pointer"
              >
                حكم (اختر نوع)
              </button>
              <button
                type="button"
                onClick={() => onBid('pass')}
                className="py-3 rounded-xl bg-white hover:bg-slate-50 text-[#E52521] font-bold text-sm sm:text-base shadow cursor-pointer"
              >
                ولا
              </button>
            </div>
          )}

          {isMyTurn && !isStandby && table.phase === 'bidding_r2' && showSuitPicker && (
            <div className="space-y-2">
              <div className="flex items-center justify-between text-xs text-white/90 font-bold">
                <span>اختر نوع الحكم (غير {table.openCard ? SUIT_INFO[table.openCard.suit].arabic : ''})</span>
                <button
                  type="button"
                  onClick={() => setShowSuitPicker(false)}
                  className="text-[#FFD000] underline cursor-pointer"
                >
                  رجوع
                </button>
              </div>
              <div className="grid grid-cols-3 gap-2">
                {SUITS.filter((s) => s !== table.openCard?.suit).map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => {
                      setShowSuitPicker(false);
                      onBid('hokm', s);
                    }}
                    className={`py-2.5 rounded-xl bg-white font-bold text-sm shadow flex items-center justify-center gap-1.5 cursor-pointer ${SUIT_INFO[s].colorClass}`}
                  >
                    <span className="text-lg">{SUIT_INFO[s].symbol}</span>
                    <span>{SUIT_INFO[s].arabic}</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {isMyTurn && !isStandby && table.phase === 'doubling' && (
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => onDoubling(true)}
                className="py-3 rounded-xl bg-[#FFD000] hover:bg-[#f0c300] text-[#111827] font-bold text-sm sm:text-base shadow cursor-pointer"
              >
                {nextDoubleLabel}
              </button>
              <button
                type="button"
                onClick={() => onDoubling(false)}
                className="py-3 rounded-xl bg-white hover:bg-slate-50 text-[#002BFF] font-bold text-sm sm:text-base shadow cursor-pointer"
              >
                فتح اللعب
              </button>
            </div>
          )}

          {(table.phase === 'playing' || !isMyTurn || isStandby) && (
            <div className="bg-white/15 rounded-xl px-4 py-2.5 flex items-center justify-between text-xs sm:text-sm font-medium">
              <span className="truncate">
                {isStandby
                  ? 'وضع الانتظار التلقائي مفعّل لهذه الجولة'
                  : isMyTurn && table.phase === 'playing'
                    ? liftedCardKey
                      ? 'اضغط مرة ثانية على الورقة المرفوعة لرميها في الوسط'
                      : 'دورك الآن — اضغط على الورقة لرفعها ثم رميها'
                    : table.lastActionText || `بانتظار دور ${table.seats[table.turnIndex]?.name || 'اللاعب'}`}
              </span>
              <span className="font-bold text-[#FFD000] shrink-0 mr-2">
                {turnSecondsLeft} ث
              </span>
            </div>
          )}
        </div>
      </footer>

      {/* PROJECT DECLARATION MODAL (سرا / خمسين / مية / أربعمية) */}
      {showProjectModal && (
        <div className="fixed inset-0 z-50 bg-black/55 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center gap-2 text-[#002BFF] mb-2">
              <Sparkles className="w-5 h-5" />
              <h3 className="text-lg font-bold">إعلان المشاريع</h3>
            </div>
            <p className="text-xs text-slate-600 mb-4">
              لديك مشاريع جاهزة في ورقك قبل بدء اللعب. اختر المشاريع التي ترغب بإعلانها الآن:
            </p>
            <div className="space-y-2 mb-5">
              {declarableProjects.map((proj) => {
                const checked = selectedProjectIds.includes(proj.id);
                const pts = table.mode === 'sun' ? proj.abnatSun : proj.abnatHokm;
                return (
                  <button
                    key={proj.id}
                    type="button"
                    onClick={() =>
                      setSelectedProjectIds((prev) =>
                        prev.includes(proj.id) ? prev.filter((id) => id !== proj.id) : [...prev, proj.id],
                      )
                    }
                    className={`w-full p-3.5 rounded-2xl border-2 flex items-center justify-between transition-colors cursor-pointer ${
                      checked
                        ? 'border-[#002BFF] bg-blue-50/70'
                        : 'border-slate-200 bg-white hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex items-center gap-2.5">
                      <span
                        className={`w-5 h-5 rounded-md flex items-center justify-center border ${
                          checked ? 'bg-[#002BFF] border-[#002BFF] text-white' : 'border-slate-300'
                        }`}
                      >
                        {checked && <Check className="w-3.5 h-3.5" />}
                      </span>
                      <span className="font-bold text-sm text-[#111827]">{proj.description}</span>
                    </div>
                    <span className="text-xs font-bold text-[#002BFF]">+{pts} نقطة</span>
                  </button>
                );
              })}
            </div>
            <div className="grid grid-cols-2 gap-2.5">
              <button
                type="button"
                onClick={() =>
                  onDeclareProjects(
                    declarableProjects.filter((p) => selectedProjectIds.includes(p.id)),
                  )
                }
                className="py-3 rounded-xl bg-[#002BFF] hover:bg-[#0021CC] text-white font-bold text-sm cursor-pointer"
              >
                إعلان المحدد
              </button>
              <button
                type="button"
                onClick={() => onDeclareProjects([])}
                className="py-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-sm cursor-pointer"
              >
                تخطي بدون إعلان
              </button>
            </div>
          </div>
        </div>
      )}

      {/* IN-TABLE CHAT DRAWER */}
      {showChat && (
        <div className="fixed inset-y-0 left-0 z-50 w-full max-w-sm bg-white shadow-2xl border-r border-slate-200 flex flex-col">
          <div className="p-4 bg-[#002BFF] text-white flex items-center justify-between">
            <h3 className="font-bold text-base">دردشة الطاولة</h3>
            <button
              type="button"
              onClick={() => setShowChat(false)}
              className="p-1.5 rounded-lg bg-white/15 hover:bg-white/25 cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Quick phrases */}
          <div className="p-3 border-b border-slate-200 flex flex-wrap gap-1.5 bg-slate-50">
            {QUICK_CHAT_PHRASES.map((phrase) => (
              <button
                key={phrase}
                type="button"
                onClick={() => onSendChat(phrase)}
                className="px-2.5 py-1.5 rounded-xl bg-white border border-slate-200 hover:border-[#002BFF] text-xs font-semibold text-[#111827] cursor-pointer"
              >
                {phrase}
              </button>
            ))}
          </div>

          <div className="flex-1 overflow-y-auto p-4 space-y-2.5">
            {chatMessages.length === 0 ? (
              <p className="text-center text-xs text-slate-400 mt-10">
                لا توجد رسائل بعد. أرسل تحية لزملائك على الطاولة!
              </p>
            ) : (
              chatMessages.map((m) => (
                <div
                  key={m.id}
                  className={`p-3 rounded-2xl text-sm ${
                    m.seatIndex === mySeat
                      ? 'bg-[#002BFF] text-white mr-8'
                      : 'bg-slate-100 text-[#111827] ml-8'
                  }`}
                >
                  <div
                    className={`text-[11px] font-bold mb-0.5 ${
                      m.seatIndex === mySeat ? 'text-[#FFD000]' : 'text-[#002BFF]'
                    }`}
                  >
                    {m.senderName}
                  </div>
                  <div>{m.text}</div>
                </div>
              ))
            )}
            <div ref={chatEndRef} />
          </div>

          <form onSubmit={handleChatSubmit} className="p-3 border-t border-slate-200 flex gap-2">
            <input
              type="text"
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              placeholder="اكتب رسالتك هنا..."
              maxLength={180}
              className="flex-1 px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-[#002BFF] focus:outline-none text-sm"
            />
            <button
              type="submit"
              className="px-4 py-2.5 rounded-xl bg-[#002BFF] hover:bg-[#0021CC] text-white flex items-center justify-center cursor-pointer"
              aria-label="إرسال"
            >
              <Send className="w-4 h-4" />
            </button>
          </form>
        </div>
      )}

      {/* ROUND SUMMARY / MATCH FINISHED MODAL */}
      {(table.phase === 'round_summary' || table.phase === 'match_finished') && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 text-center">
            <h3 className="text-xl font-bold text-[#111827]">
              {table.phase === 'match_finished'
                ? table.winnerTeam === myTeam
                  ? 'مبروك! فزتم بالصكة (152)'
                  : 'انتهت الصكة — حظ أوفر!'
                : `نتيجة الجولة رقم ${table.roundNumber}`}
            </h3>
            <p className="text-sm text-slate-600 mt-1.5">{table.lastActionText}</p>

            <div className="grid grid-cols-2 gap-3 my-5">
              <div className="p-4 rounded-2xl bg-blue-50 border border-[#002BFF]/20">
                <div className="text-xs font-bold text-slate-500">فريقنا (لنا)</div>
                <div className="text-3xl font-bold text-[#002BFF] mt-1">{ourScore}</div>
                <div className="text-[11px] text-slate-500 mt-1">
                  أكلات الجولة: {myTeam === 0 ? table.usTricksCount : table.themTricksCount}
                </div>
              </div>
              <div className="p-4 rounded-2xl bg-red-50 border border-[#E52521]/20">
                <div className="text-xs font-bold text-slate-500">الخصم (لهم)</div>
                <div className="text-3xl font-bold text-[#E52521] mt-1">{oppScore}</div>
                <div className="text-[11px] text-slate-500 mt-1">
                  أكلات الجولة: {myTeam === 0 ? table.themTricksCount : table.usTricksCount}
                </div>
              </div>
            </div>

            {table.phase === 'match_finished' && lastRatingDelta !== null && (
              <div
                className={`mb-4 py-2 px-4 rounded-xl text-sm font-bold inline-block ${
                  lastRatingDelta >= 0
                    ? 'bg-emerald-50 text-emerald-700'
                    : 'bg-red-50 text-[#E52521]'
                }`}
              >
                تغيّر التصنيف: {lastRatingDelta >= 0 ? `+${lastRatingDelta}` : lastRatingDelta} نقطة
              </div>
            )}

            {table.phase === 'round_summary' ? (
              <button
                type="button"
                onClick={onNextRound}
                className="w-full py-3.5 rounded-xl bg-[#002BFF] hover:bg-[#0021CC] text-white font-bold text-sm cursor-pointer"
              >
                ابدأ الجولة التالية
              </button>
            ) : (
              <button
                type="button"
                onClick={onExitTable}
                className="w-full py-3.5 rounded-xl bg-[#002BFF] hover:bg-[#0021CC] text-white font-bold text-sm cursor-pointer"
              >
                العودة إلى اللوبي
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
