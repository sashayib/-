import React, { useState } from 'react';
import { Check, Copy, Link2, LogOut, Play, Send, Users } from 'lucide-react';
import { FriendEntry, GameRoomDoc } from '../game/balootTypes';

interface WaitingRoomScreenProps {
  room: GameRoomDoc;
  currentUid: string;
  friends: FriendEntry[];
  busy: boolean;
  onInviteFriend: (friend: FriendEntry) => void;
  onStartMatch: () => void;
  onLeaveRoom: () => void;
}

export const WaitingRoomScreen: React.FC<WaitingRoomScreenProps> = ({
  room,
  currentUid,
  friends,
  busy,
  onInviteFriend,
  onStartMatch,
  onLeaveRoom,
}) => {
  const [copiedCode, setCopiedCode] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);
  const [invitedUids, setInvitedUids] = useState<string[]>([]);

  const isHost = room.hostId === currentUid;
  const shareUrl =
    typeof window !== 'undefined'
      ? `${window.location.origin}/?room=${room.roomCode}`
      : `?room=${room.roomCode}`;

  const seats = [
    { idx: 0, uid: room.seat0Uid, name: room.seat0Name, rating: room.seat0Rating, team: 'فريقنا' },
    { idx: 1, uid: room.seat1Uid, name: room.seat1Name, rating: room.seat1Rating, team: 'الخصم' },
    { idx: 2, uid: room.seat2Uid, name: room.seat2Name, rating: room.seat2Rating, team: 'فريقنا' },
    { idx: 3, uid: room.seat3Uid, name: room.seat3Name, rating: room.seat3Rating, team: 'الخصم' },
  ];

  const copyText = async (text: string, which: 'code' | 'link') => {
    try {
      await navigator.clipboard.writeText(text);
      if (which === 'code') {
        setCopiedCode(true);
        setTimeout(() => setCopiedCode(false), 2000);
      } else {
        setCopiedLink(true);
        setTimeout(() => setCopiedLink(false), 2000);
      }
    } catch {
      // Clipboard may be blocked
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 sm:py-10">
      <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="bg-[#002BFF] text-white p-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <div className="text-xs text-white/80 font-medium">
              {room.roomType === 'private' ? 'طاولة خاصة بالأصدقاء' : 'طاولة عامة أونلاين'} · صكة 152 نقطة
            </div>
            <h2 className="text-2xl font-bold mt-1">غرفة الانتظار · {room.roomCode}</h2>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => copyText(room.roomCode, 'code')}
              className="px-3.5 py-2 rounded-xl bg-white/15 hover:bg-white/25 text-xs font-bold flex items-center gap-1.5 cursor-pointer"
            >
              {copiedCode ? <Check className="w-4 h-4 text-[#FFD000]" /> : <Copy className="w-4 h-4" />}
              <span>{copiedCode ? 'تم النسخ' : 'نسخ الرمز'}</span>
            </button>
            <button
              type="button"
              onClick={() => copyText(shareUrl, 'link')}
              className="px-3.5 py-2 rounded-xl bg-[#FFD000] hover:bg-[#f0c300] text-[#111827] text-xs font-bold flex items-center gap-1.5 cursor-pointer"
            >
              {copiedLink ? <Check className="w-4 h-4" /> : <Link2 className="w-4 h-4" />}
              <span>{copiedLink ? 'تم نسخ الرابط' : 'مشاركة رابط الجلسة'}</span>
            </button>
          </div>
        </div>

        <div className="p-6 space-y-6">
          {/* 4 Seats Grid */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-bold text-[#111827] flex items-center gap-2">
                <Users className="w-4 h-4 text-[#002BFF]" />
                <span>المقاعد ({room.memberUids.length} / 4)</span>
              </h3>
              <span className="text-xs text-slate-500">
                المقاعد الفارغة تُستكمل بلاعبين آليين عند البدء
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {seats.map((seat) => (
                <div
                  key={seat.idx}
                  className={`p-4 rounded-2xl border flex items-center justify-between ${
                    seat.uid ? 'border-[#002BFF]/30 bg-blue-50/40' : 'border-dashed border-slate-300 bg-slate-50'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <img
                      src="/assets/shaddaha_avatar.png"
                      alt=""
                      className={`w-10 h-10 rounded-full border bg-white ${
                        seat.uid ? 'border-[#002BFF]' : 'opacity-40 border-slate-200'
                      }`}
                    />
                    <div>
                      <div className="text-sm font-bold text-[#111827]">
                        {seat.uid ? seat.name : 'مقعد متاح'}
                      </div>
                      <div className="text-xs text-slate-500">
                        {seat.team} {seat.uid ? `· تصنيف ${seat.rating}` : '· بانتظار لاعب'}
                      </div>
                    </div>
                  </div>
                  {seat.uid === room.hostId && (
                    <span className="text-xs font-bold text-[#002BFF]">المضيف</span>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Direct Invite Friends */}
          {friends.length > 0 && (
            <div className="border-t border-slate-200 pt-5">
              <h3 className="text-sm font-bold text-[#111827] mb-3">دعوة أصدقائك مباشرة</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                {friends
                  .filter((f) => !room.memberUids.includes(f.friendUid))
                  .map((friend) => {
                    const sent = invitedUids.includes(friend.friendUid);
                    return (
                      <div
                        key={friend.friendUid}
                        className="p-3 rounded-xl border border-slate-200 flex items-center justify-between"
                      >
                        <div>
                          <div className="text-sm font-bold text-[#111827]">{friend.friendName}</div>
                          <div className="text-xs text-slate-500">{friend.friendPlayerCode}</div>
                        </div>
                        <button
                          type="button"
                          disabled={sent}
                          onClick={() => {
                            onInviteFriend(friend);
                            setInvitedUids((prev) => [...prev, friend.friendUid]);
                          }}
                          className="px-3 py-1.5 rounded-lg bg-[#002BFF] hover:bg-[#0021CC] text-white text-xs font-bold disabled:bg-slate-200 disabled:text-slate-500 flex items-center gap-1 cursor-pointer"
                        >
                          <Send className="w-3.5 h-3.5" />
                          <span>{sent ? 'تمت الدعوة' : 'دعوة'}</span>
                        </button>
                      </div>
                    );
                  })}
              </div>
            </div>
          )}

          {/* Action Footer */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-slate-200">
            <button
              type="button"
              onClick={onLeaveRoom}
              className="px-4 py-3 rounded-xl border border-slate-200 hover:bg-slate-50 text-[#E52521] font-bold text-sm flex items-center gap-2 cursor-pointer"
            >
              <LogOut className="w-4 h-4" />
              <span>مغادرة الغرفة</span>
            </button>

            {isHost ? (
              <button
                type="button"
                disabled={busy}
                onClick={onStartMatch}
                className="px-6 py-3 rounded-xl bg-[#002BFF] hover:bg-[#0021CC] text-white font-bold text-sm flex items-center gap-2 shadow cursor-pointer disabled:opacity-60"
              >
                <Play className="w-4 h-4" />
                <span>ابدأ صكة 152 الآن</span>
              </button>
            ) : (
              <span className="text-sm font-semibold text-slate-600">
                بانتظار المضيف لبدء الجلسة...
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
