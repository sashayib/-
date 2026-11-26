import React from 'react';
import { Crown, Trophy } from 'lucide-react';
import { PlayerProfile } from '../game/balootTypes';

interface LeaderboardScreenProps {
  players: PlayerProfile[];
  currentUid: string;
}

export const LeaderboardScreen: React.FC<LeaderboardScreenProps> = ({ players, currentUid }) => {
  return (
    <div className="max-w-4xl mx-auto px-4 py-6 sm:py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-[#111827] flex items-center gap-2">
          <Trophy className="w-6 h-6 text-[#FFD000]" />
          <span>قائمة المتصدرين</span>
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          أعلى لاعبي شدها تصنيفاً في صكات الـ 152 نقطة.
        </p>
      </div>

      {players.length === 0 ? (
        <div className="p-10 rounded-3xl border border-slate-200 text-center text-slate-500 text-sm">
          لا توجد نتائج مصنفة بعد. ابدأ أول صكة لتتصدر القائمة!
        </div>
      ) : (
        <div className="rounded-3xl border border-slate-200 bg-white overflow-hidden">
          <div className="grid grid-cols-12 px-4 py-3 bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-500">
            <div className="col-span-2 sm:col-span-1">المركز</div>
            <div className="col-span-5 sm:col-span-6">اللاعب</div>
            <div className="col-span-2 text-center">الفوز</div>
            <div className="col-span-3 text-left">التصنيف</div>
          </div>

          <div className="divide-y divide-slate-100">
            {players.map((p, index) => {
              const rank = index + 1;
              const isMe = p.userId === currentUid;
              return (
                <div
                  key={p.userId}
                  className={`grid grid-cols-12 items-center px-4 py-3.5 text-sm ${
                    isMe ? 'bg-blue-50/60' : 'hover:bg-slate-50/70'
                  }`}
                >
                  <div className="col-span-2 sm:col-span-1 font-bold flex items-center gap-1">
                    {rank === 1 ? (
                      <Crown className="w-5 h-5 text-[#FFD000]" />
                    ) : (
                      <span className={rank <= 3 ? 'text-[#002BFF]' : 'text-slate-500'}>
                        #{rank}
                      </span>
                    )}
                  </div>

                  <div className="col-span-5 sm:col-span-6 flex items-center gap-2.5 min-w-0">
                    <img
                      src={p.avatarUrl || '/assets/shaddaha_avatar.png'}
                      alt={p.displayName}
                      className="w-9 h-9 rounded-full object-cover border border-slate-200 bg-slate-100 shrink-0"
                    />
                    <div className="min-w-0">
                      <div className="font-bold text-[#111827] truncate">
                        {p.displayName} {isMe && <span className="text-xs text-[#002BFF]">(أنت)</span>}
                      </div>
                      <div className="text-xs text-slate-500">{p.rankTier}</div>
                    </div>
                  </div>

                  <div className="col-span-2 text-center text-xs font-semibold text-slate-600">
                    {p.wins} / {p.matchesPlayed}
                  </div>

                  <div className="col-span-3 text-left font-bold text-[#002BFF]">
                    {p.rating}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
