import React, { useState } from 'react';
import { Check, Copy, Link2, Send, Trash2, UserPlus, Users } from 'lucide-react';
import { FriendEntry, PlayerProfile, TableInvite } from '../game/balootTypes';

interface FriendsScreenProps {
  profile: PlayerProfile;
  friends: FriendEntry[];
  leaderboard: PlayerProfile[];
  incomingInvites: TableInvite[];
  onAddFriendByCode: (code: string) => Promise<{ ok: boolean; message: string }>;
  onRemoveFriend: (friendUid: string) => void;
  onInviteFriendToPrivateTable: (friend: FriendEntry) => void;
  onAcceptInvite: (invite: TableInvite) => void;
  onDeclineInvite: (invite: TableInvite) => void;
  onCreatePrivateTable: () => void;
}

export const FriendsScreen: React.FC<FriendsScreenProps> = ({
  profile,
  friends,
  leaderboard,
  incomingInvites,
  onAddFriendByCode,
  onRemoveFriend,
  onInviteFriendToPrivateTable,
  onAcceptInvite,
  onDeclineInvite,
  onCreatePrivateTable,
}) => {
  const [codeInput, setCodeInput] = useState('');
  const [feedback, setFeedback] = useState<{ ok: boolean; text: string } | null>(null);
  const [searching, setSearching] = useState(false);
  const [copiedId, setCopiedId] = useState(false);

  const playerLookup = new Map(leaderboard.map((p) => [p.userId, p]));

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    setFeedback(null);
    setSearching(true);
    try {
      const res = await onAddFriendByCode(codeInput);
      setFeedback({ ok: res.ok, text: res.message });
      if (res.ok) setCodeInput('');
    } finally {
      setSearching(false);
    }
  };

  const copyMyId = async () => {
    try {
      await navigator.clipboard.writeText(profile.playerCode);
      setCopiedId(true);
      setTimeout(() => setCopiedId(false), 2000);
    } catch {
      // ignore
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-6 sm:py-8 space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-[#111827]">الأصدقاء والدعوات</h1>
          <p className="text-sm text-slate-500 mt-1">
            أضف أصدقاءك عبر الرقم التعريفي (Player ID) وادعهم مباشرة إلى طاولة بلوت خاصة.
          </p>
        </div>
        <button
          type="button"
          onClick={onCreatePrivateTable}
          className="px-4 py-2.5 rounded-xl bg-[#002BFF] hover:bg-[#0021CC] text-white font-bold text-sm flex items-center gap-2 cursor-pointer"
        >
          <Link2 className="w-4 h-4" />
          <span>إنشاء طاولة خاصة ومشاركة الرابط</span>
        </button>
      </div>

      {/* My Player ID + Add Friend Bar */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="p-5 rounded-2xl border border-slate-200 bg-slate-50 flex items-center justify-between gap-4">
          <div>
            <div className="text-xs font-semibold text-slate-500">رقمك التعريفي (Player ID)</div>
            <div className="text-2xl font-bold text-[#002BFF] tracking-wider mt-1" dir="ltr">
              {profile.playerCode}
            </div>
            <div className="text-xs text-slate-500 mt-1">شاركه مع أصدقائك ليضيفوك فوراً</div>
          </div>
          <button
            type="button"
            onClick={copyMyId}
            className="px-4 py-2.5 rounded-xl bg-[#FFD000] hover:bg-[#f0c300] text-[#111827] font-bold text-xs flex items-center gap-1.5 cursor-pointer shrink-0"
          >
            {copiedId ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
            <span>{copiedId ? 'تم النسخ' : 'نسخ الرقم'}</span>
          </button>
        </div>

        <form onSubmit={handleAdd} className="p-5 rounded-2xl border border-slate-200 bg-white">
          <label className="block text-xs font-bold text-slate-700 mb-2">
            إضافة صديق عبر Player ID
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              value={codeInput}
              onChange={(e) => setCodeInput(e.target.value.toUpperCase())}
              placeholder="مثال: SHD7K29P"
              dir="ltr"
              maxLength={16}
              className="flex-1 px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-[#002BFF] focus:outline-none text-sm font-bold text-right"
            />
            <button
              type="submit"
              disabled={searching}
              className="px-4 py-2.5 rounded-xl bg-[#002BFF] hover:bg-[#0021CC] text-white font-bold text-sm flex items-center gap-1.5 disabled:opacity-60 cursor-pointer"
            >
              <UserPlus className="w-4 h-4" />
              <span>{searching ? 'بحث...' : 'إضافة'}</span>
            </button>
          </div>
          {feedback && (
            <p
              className={`text-xs font-semibold mt-2 ${
                feedback.ok ? 'text-emerald-700' : 'text-[#E52521]'
              }`}
            >
              {feedback.text}
            </p>
          )}
        </form>
      </div>

      {/* Incoming Table Invites */}
      {incomingInvites.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-base font-bold text-[#111827]">دعوات الطاولات الواردة</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {incomingInvites.map((inv) => (
              <div
                key={inv.id}
                className="p-4 rounded-2xl border-2 border-[#FFD000] bg-yellow-50/40 flex items-center justify-between gap-3"
              >
                <div>
                  <div className="text-sm font-bold text-[#111827]">
                    دعوة من {inv.fromName}
                  </div>
                  <div className="text-xs text-slate-600 mt-0.5">
                    طاولة خاصة · رمز {inv.roomCode} · صكة 152
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => onAcceptInvite(inv)}
                    className="px-3.5 py-2 rounded-xl bg-[#002BFF] text-white text-xs font-bold cursor-pointer"
                  >
                    قبول
                  </button>
                  <button
                    type="button"
                    onClick={() => onDeclineInvite(inv)}
                    className="px-3 py-2 rounded-xl bg-white border border-slate-200 text-slate-600 text-xs font-bold cursor-pointer"
                  >
                    رفض
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Friends List */}
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-bold text-[#111827] flex items-center gap-2">
            <Users className="w-4 h-4 text-[#002BFF]" />
            <span>قائمة الأصدقاء ({friends.length})</span>
          </h2>
        </div>

        {friends.length === 0 ? (
          <div className="p-10 rounded-2xl border border-slate-200 text-center text-slate-500 text-sm">
            قائمة أصدقائك فارغة حالياً. أدخل الرقم التعريفي لأي لاعب في الأعلى لإضافته واللعب معه!
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {friends.map((f) => {
              const live = playerLookup.get(f.friendUid);
              const status = live?.presenceStatus || 'offline';
              const statusLabel =
                status === 'in_game' ? 'داخل طاولة' : status === 'online' ? 'متصل الآن' : 'غير متصل';
              return (
                <div
                  key={f.friendUid}
                  className="p-4 rounded-2xl border border-slate-200 bg-white flex items-center justify-between gap-3"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <img
                      src={live?.avatarUrl || '/assets/shaddaha_avatar.png'}
                      alt={f.friendName}
                      className="w-11 h-11 rounded-full object-cover border border-slate-200 bg-slate-100 shrink-0"
                    />
                    <div className="min-w-0">
                      <div className="text-sm font-bold text-[#111827] truncate">
                        {live?.displayName || f.friendName}
                      </div>
                      <div className="text-xs text-slate-500 flex items-center gap-1.5 mt-0.5">
                        <span dir="ltr">{f.friendPlayerCode}</span>
                        <span>·</span>
                        <span>تصنيف {live?.rating ?? f.friendRating}</span>
                        <span>·</span>
                        <span
                          className={
                            status === 'online'
                              ? 'text-emerald-600 font-semibold'
                              : status === 'in_game'
                                ? 'text-[#002BFF] font-semibold'
                                : 'text-slate-400'
                          }
                        >
                          {statusLabel}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0">
                    <button
                      type="button"
                      onClick={() => onInviteFriendToPrivateTable(f)}
                      className="px-3 py-2 rounded-xl bg-[#002BFF] hover:bg-[#0021CC] text-white text-xs font-bold flex items-center gap-1 cursor-pointer"
                    >
                      <Send className="w-3.5 h-3.5" />
                      <span>دعوة لطاولة</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => onRemoveFriend(f.friendUid)}
                      className="p-2 rounded-xl border border-slate-200 text-slate-400 hover:text-[#E52521] hover:border-red-200 cursor-pointer"
                      aria-label="إزالة صديق"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
};
