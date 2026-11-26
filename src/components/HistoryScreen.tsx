import React from 'react';
import { History } from 'lucide-react';
import { MatchRecord, PlayerProfile } from '../game/balootTypes';

interface HistoryScreenProps {
  profile: PlayerProfile;
  records: MatchRecord[];
}

export const HistoryScreen: React.FC<HistoryScreenProps> = ({ profile, records }) => {
  const formatDate = (ms: number) => {
    try {
      return new Intl.DateTimeFormat('ar-SA', {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(new Date(ms));
    } catch {
      return '';
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-6 sm:py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-[#111827] flex items-center gap-2">
          <History className="w-6 h-6 text-[#002BFF]" />
          <span>سجل المباريات</span>
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          إجمالي {profile.matchesPlayed} صكة · {profile.wins} انتصار · {profile.losses} خسارة
        </p>
      </div>

      {records.length === 0 ? (
        <div className="p-10 rounded-3xl border border-slate-200 text-center text-slate-500 text-sm">
          لم تُكمل أي صكة حتى الآن. العب أول مباراة أونلاين أو ضد الذكاء الاصطناعي ليتم تسجيلها هنا تلقائياً!
        </div>
      ) : (
        <div className="space-y-3">
          {records.map((rec) => (
            <div
              key={rec.id}
              className="p-4 rounded-2xl border border-slate-200 bg-white flex flex-wrap items-center justify-between gap-3"
            >
              <div>
                <div className="flex items-center gap-2 text-sm font-bold">
                  <span className={rec.didWin ? 'text-emerald-700' : 'text-[#E52521]'}>
                    {rec.didWin ? 'فوز' : 'خسارة'}
                  </span>
                  <span className="text-slate-300">·</span>
                  <span className="text-[#111827]">
                    لنا {rec.usScore} — لهم {rec.themScore}
                  </span>
                </div>
                <div className="text-xs text-slate-500 mt-1">
                  {rec.mode === 'online' ? `جلسة أونلاين ${rec.roomCode ? `(${rec.roomCode})` : ''}` : 'ضد الذكاء الاصطناعي'}
                  {rec.opponentSummary ? ` · ${rec.opponentSummary}` : ''}
                  {rec.createdAtMs ? ` · ${formatDate(rec.createdAtMs)}` : ''}
                </div>
              </div>

              <div
                className={`text-sm font-bold ${
                  rec.ratingDelta >= 0 ? 'text-[#002BFF]' : 'text-[#E52521]'
                }`}
              >
                {rec.ratingDelta >= 0 ? `+${rec.ratingDelta}` : rec.ratingDelta} نقطة
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
