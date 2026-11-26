import React, { useState } from 'react';
import {
  Bot,
  Calculator,
  Crown,
  Globe,
  History,
  KeyRound,
  Lock,
  Play,
  ShoppingBag,
  Trophy,
  Users,
} from 'lucide-react';
import {
  FREE_DAILY_SESSION_LIMIT,
  GameRoomDoc,
  PlayerProfile,
  PURCHASES_ENABLED,
  TableInvite,
  UserSettings,
} from '../game/balootTypes';

interface LobbyScreenProps {
  profile: PlayerProfile;
  settings: UserSettings;
  publicRooms: GameRoomDoc[];
  incomingInvites: TableInvite[];
  remainingFreeToday: number;
  busy: boolean;
  onStartSoloAi: () => void;
  onCreateOnlineRoom: (type: 'public' | 'private') => void;
  onJoinRoomCode: (code: string) => void;
  onAcceptInvite: (invite: TableInvite) => void;
  onDeclineInvite: (invite: TableInvite) => void;
  onNavigate: (tab: 'friends' | 'leaderboard' | 'history' | 'profile' | 'store' | 'calculator') => void;
  onOpenSubscription: () => void;
}

export const LobbyScreen: React.FC<LobbyScreenProps> = ({
  profile,
  settings,
  publicRooms,
  incomingInvites,
  remainingFreeToday,
  busy,
  onStartSoloAi,
  onCreateOnlineRoom,
  onJoinRoomCode,
  onAcceptInvite,
  onDeclineInvite,
  onNavigate,
  onOpenSubscription,
}) => {
  const [joinCode, setJoinCode] = useState('');
  const isSubscribed = settings.subscriptionPlan !== 'none';

  const handleJoinSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!joinCode.trim()) return;
    onJoinRoomCode(joinCode.trim().toUpperCase());
  };

  return (
    <div className="max-w-6xl mx-auto px-4 pt-5 pb-10 space-y-8">
      {/* Incoming Invite Banner */}
      {incomingInvites.length > 0 && (
        <div className="p-4 rounded-2xl bg-[#FFD000] text-[#111827] flex flex-wrap items-center justify-between gap-3 shadow-sm">
          <div>
            <div className="text-sm font-bold">
              دعوة فورية من {incomingInvites[0].fromName} لطاولة خاصة (رمز {incomingInvites[0].roomCode})
            </div>
            <div className="text-xs opacity-80 mt-0.5">صكة 152 نقطة — انضم الآن</div>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => onAcceptInvite(incomingInvites[0])}
              className="px-4 py-2 rounded-xl bg-[#002BFF] text-white text-xs font-bold cursor-pointer"
            >
              قبول والانضمام
            </button>
            <button
              type="button"
              onClick={() => onDeclineInvite(incomingInvites[0])}
              className="px-3 py-2 rounded-xl bg-white text-[#111827] text-xs font-bold cursor-pointer"
            >
              رفض
            </button>
          </div>
        </div>
      )}

      {/* Main Split Hero — Faithful to Shaddaha Mockup 2 on Mobile & Expansive on Desktop */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
        {/* Left/Center Player Spotlight + Primary CTA (Mockup 2 Essence) */}
        <div className="lg:col-span-5 rounded-3xl border border-slate-200 bg-white overflow-hidden flex flex-col justify-between shadow-sm">
          <div className="p-6 flex flex-col items-center text-center">
            <img
              src="/assets/shaddaha_logo.png"
              alt="شعار شدها"
              className="w-36 h-auto object-contain mb-4"
            />

            {/* Quick 4 Tiles (Matching Mockup 2 Top Row) */}
            <div className="grid grid-cols-4 gap-2 w-full mb-6">
              <button
                type="button"
                onClick={() => onNavigate('friends')}
                className="p-2.5 rounded-2xl bg-slate-100 hover:bg-slate-200/80 flex flex-col items-center gap-1 transition-colors cursor-pointer"
              >
                <Users className="w-5 h-5 text-[#002BFF]" />
                <span className="text-[11px] font-bold text-slate-700">الأصدقاء</span>
              </button>
              <button
                type="button"
                onClick={() => onNavigate('leaderboard')}
                className="p-2.5 rounded-2xl bg-slate-100 hover:bg-slate-200/80 flex flex-col items-center gap-1 transition-colors cursor-pointer"
              >
                <Trophy className="w-5 h-5 text-[#FFD000]" />
                <span className="text-[11px] font-bold text-slate-700">المتصدرين</span>
              </button>
              <button
                type="button"
                onClick={() => onNavigate('history')}
                className="p-2.5 rounded-2xl bg-slate-100 hover:bg-slate-200/80 flex flex-col items-center gap-1 transition-colors cursor-pointer"
              >
                <History className="w-5 h-5 text-[#002BFF]" />
                <span className="text-[11px] font-bold text-slate-700">السجل</span>
              </button>
              <button
                type="button"
                onClick={() => onNavigate('profile')}
                className="p-2.5 rounded-2xl bg-slate-100 hover:bg-slate-200/80 flex flex-col items-center gap-1 transition-colors cursor-pointer"
              >
                <span className="text-xs font-bold text-[#002BFF] leading-5">ID</span>
                <span className="text-[11px] font-bold text-slate-700">ملفي</span>
              </button>
            </div>

            {/* Center Player Avatar + Yellow Name Banner + Blue Tier */}
            <button
              type="button"
              onClick={() => onNavigate('profile')}
              className="group flex flex-col items-center cursor-pointer"
            >
              <img
                src={profile.avatarUrl || '/assets/shaddaha_avatar.png'}
                alt={profile.displayName}
                className="w-28 h-28 rounded-full object-cover border-4 border-slate-100 bg-slate-100 shadow-sm group-hover:scale-105 transition-transform"
              />
              <div className="-mt-3 bg-[#FFD000] text-[#111827] px-6 py-1.5 rounded-xl font-bold text-base shadow-sm">
                {profile.displayName}
              </div>
              <div className="text-[#002BFF] font-bold text-sm mt-2">
                {profile.rankTier} · {profile.rating} نقطة
              </div>
              <div className="text-xs text-slate-400 mt-0.5" dir="ltr">
                Player ID: {profile.playerCode}
              </div>
            </button>
          </div>

          {/* Bottom Shaddaha Yellow Wave Section with Royal Blue Play Button & Red Subscribe Button */}
          <div className="relative bg-[#FFD000] px-5 pt-7 pb-5 rounded-t-[32px] mt-2">
            {/* Primary Royal Blue CTA straddling top edge */}
            <button
              type="button"
              onClick={onStartSoloAi}
              className="w-full -mt-12 mb-4 py-4 rounded-2xl bg-[#002BFF] hover:bg-[#0021CC] text-white font-bold text-xl shadow-lg flex items-center justify-center gap-2.5 transition-transform active:scale-[0.98] cursor-pointer"
            >
              <Play className="w-5 h-5 fill-current" />
              <span>العب الآن</span>
            </button>

            <div className="flex flex-col items-center">
              <button
                type="button"
                disabled={!PURCHASES_ENABLED}
                onClick={onOpenSubscription}
                className={`px-7 py-2.5 rounded-xl font-bold text-sm shadow flex items-center gap-1.5 ${
                  PURCHASES_ENABLED
                    ? 'bg-[#E52521] hover:bg-[#c81e1a] text-white cursor-pointer'
                    : 'bg-slate-800/85 text-white/90 cursor-not-allowed'
                }`}
              >
                {PURCHASES_ENABLED ? (
                  <>
                    <Crown className="w-4 h-4 text-[#FFD000]" />
                    <span>{isSubscribed ? 'إدارة اشتراك شدها بلس' : 'اشترك الآن — شدها بلس'}</span>
                  </>
                ) : (
                  <>
                    <Lock className="w-4 h-4 text-[#FFD000]" />
                    <span>الوضع المجاني مفعّل حالياً</span>
                  </>
                )}
              </button>

              {/* Bottom White Tool Shortcuts */}
              <div className="grid grid-cols-3 gap-2.5 w-full mt-4">
                <button
                  type="button"
                  onClick={() => onNavigate('store')}
                  className="bg-white hover:bg-slate-50 rounded-2xl py-3 px-2 flex flex-col items-center gap-1 shadow-sm cursor-pointer"
                >
                  <ShoppingBag className="w-5 h-5 text-[#002BFF]" />
                  <span className="text-xs font-bold text-[#111827]">المتجر</span>
                </button>
                <button
                  type="button"
                  onClick={() => onNavigate('calculator')}
                  className="bg-white hover:bg-slate-50 rounded-2xl py-3 px-2 flex flex-col items-center gap-1 shadow-sm cursor-pointer"
                >
                  <Calculator className="w-5 h-5 text-[#002BFF]" />
                  <span className="text-xs font-bold text-[#111827]">الحاسبة</span>
                </button>
                <button
                  type="button"
                  onClick={() => onNavigate('friends')}
                  className="bg-white hover:bg-slate-50 rounded-2xl py-3 px-2 flex flex-col items-center gap-1 shadow-sm cursor-pointer"
                >
                  <Users className="w-5 h-5 text-[#002BFF]" />
                  <span className="text-xs font-bold text-[#111827]">الدعوات</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Online Multiplayer Hub (Create Public/Private + Join Code + Open Public Rooms) */}
        <div className="lg:col-span-7 flex flex-col gap-5">
          {/* Online Session Creator Card */}
          <div className="p-6 rounded-3xl border border-slate-200 bg-white space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h2 className="text-lg font-bold text-[#111827]">الجلسات الأونلاين (صكة 152)</h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  أنشئ جلسة عامة أو خاصة وشارك رابطها فوراً مع أصدقائك.
                </p>
              </div>
              <div className="text-xs font-bold text-[#002BFF]">
                {isSubscribed
                  ? 'إنشاء جلسات غير محدود'
                  : `متبقي اليوم: ${remainingFreeToday} / ${FREE_DAILY_SESSION_LIMIT} مجانية`}
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <button
                type="button"
                disabled={busy}
                onClick={() => onCreateOnlineRoom('public')}
                className="p-4 rounded-2xl bg-[#002BFF] hover:bg-[#0021CC] text-white text-right flex items-center justify-between gap-3 transition-colors disabled:opacity-60 cursor-pointer"
              >
                <div>
                  <div className="font-bold text-sm">إنشاء جلسة عامة</div>
                  <div className="text-xs text-white/80 mt-0.5">يظهر للجميع في اللوبي</div>
                </div>
                <Globe className="w-6 h-6 text-[#FFD000] shrink-0" />
              </button>

              <button
                type="button"
                disabled={busy}
                onClick={() => onCreateOnlineRoom('private')}
                className="p-4 rounded-2xl bg-slate-900 hover:bg-slate-800 text-white text-right flex items-center justify-between gap-3 transition-colors disabled:opacity-60 cursor-pointer"
              >
                <div>
                  <div className="font-bold text-sm">إنشاء جلسة خاصة</div>
                  <div className="text-xs text-white/75 mt-0.5">برابط دعوة ورمز سري</div>
                </div>
                <Lock className="w-6 h-6 text-[#FFD000] shrink-0" />
              </button>
            </div>

            {/* Join by Room Code */}
            <form onSubmit={handleJoinSubmit} className="pt-4 border-t border-slate-100">
              <label className="block text-xs font-bold text-slate-700 mb-2">
                الانضمام إلى جلسة عبر الرمز
              </label>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <KeyRound className="w-4 h-4 text-slate-400 absolute right-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={joinCode}
                    onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
                    placeholder="أدخل رمز الجلسة (6 خانات)"
                    dir="ltr"
                    maxLength={6}
                    className="w-full pr-10 pl-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-[#002BFF] focus:outline-none text-sm font-bold text-right"
                  />
                </div>
                <button
                  type="submit"
                  disabled={busy}
                  className="px-5 py-2.5 rounded-xl bg-[#FFD000] hover:bg-[#f0c300] text-[#111827] font-bold text-sm cursor-pointer"
                >
                  انضمام
                </button>
              </div>
            </form>
          </div>

          {/* Open Public Rooms List */}
          <div className="p-6 rounded-3xl border border-slate-200 bg-white flex-1 flex flex-col">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base font-bold text-[#111827]">طاولات عامة مفتوحة الآن</h3>
              <span className="text-xs text-slate-500">{publicRooms.length} جلسة متاحة</span>
            </div>

            {publicRooms.length === 0 ? (
              <div className="flex-1 flex flex-col items-center justify-center py-8 text-center">
                <Bot className="w-8 h-8 text-slate-300 mb-2" />
                <p className="text-sm font-semibold text-slate-600">
                  لا توجد طاولات عامة بانتظار لاعبين حالياً
                </p>
                <p className="text-xs text-slate-400 mt-1">
                  أنشئ جلسة عامة جديدة أو العب فوراً ضد الذكاء الاصطناعي.
                </p>
              </div>
            ) : (
              <div className="space-y-2.5">
                {publicRooms.map((room) => (
                  <div
                    key={room.roomCode}
                    className="p-3.5 rounded-2xl border border-slate-200 hover:border-[#002BFF]/40 flex items-center justify-between gap-3"
                  >
                    <div>
                      <div className="text-sm font-bold text-[#111827]">
                        طاولة {room.seat0Name || 'لاعب شدها'} · {room.roomCode}
                      </div>
                      <div className="text-xs text-slate-500 mt-0.5">
                        {room.memberUids.length} / 4 لاعبين · صكة 152
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => onJoinRoomCode(room.roomCode)}
                      className="px-4 py-2 rounded-xl bg-[#002BFF] hover:bg-[#0021CC] text-white text-xs font-bold cursor-pointer"
                    >
                      دخول
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
