import React, { useState } from 'react';
import { Calculator, RotateCcw, Undo2 } from 'lucide-react';

interface CalcEntry {
  us: number;
  them: number;
  note?: string;
}

export const BalootCalculatorScreen: React.FC = () => {
  const [entries, setEntries] = useState<CalcEntry[]>([]);
  const [usInput, setUsInput] = useState('');
  const [themInput, setThemInput] = useState('');

  const usTotal = entries.reduce((sum, e) => sum + e.us, 0);
  const themTotal = entries.reduce((sum, e) => sum + e.them, 0);
  const winner =
    usTotal >= 152 || themTotal >= 152
      ? usTotal >= themTotal
        ? 'فاز فريقنا (لنا) بالصكة!'
        : 'فاز الخصم (لهم) بالصكة!'
      : null;

  const addRound = (usVal: number, themVal: number, note?: string) => {
    if (usVal === 0 && themVal === 0) return;
    setEntries((prev) => [...prev, { us: Math.max(0, usVal), them: Math.max(0, themVal), note }]);
    setUsInput('');
    setThemInput('');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    addRound(Number(usInput) || 0, Number(themInput) || 0);
  };

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 sm:py-8 space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-[#111827] flex items-center gap-2">
            <Calculator className="w-6 h-6 text-[#002BFF]" />
            <span>حاسبة البلوت (صكة 152)</span>
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            احسب نقاط الجلسة الواقعية بسرعة ودقة مع تراجع وسجل كامل للجولات.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            disabled={entries.length === 0}
            onClick={() => setEntries((prev) => prev.slice(0, -1))}
            className="px-3.5 py-2 rounded-xl border border-slate-200 hover:bg-slate-50 text-xs font-bold text-slate-700 disabled:opacity-40 flex items-center gap-1.5 cursor-pointer"
          >
            <Undo2 className="w-4 h-4" />
            <span>تراجع</span>
          </button>
          <button
            type="button"
            onClick={() => setEntries([])}
            className="px-3.5 py-2 rounded-xl border border-slate-200 hover:bg-red-50 text-xs font-bold text-[#E52521] flex items-center gap-1.5 cursor-pointer"
          >
            <RotateCcw className="w-4 h-4" />
            <span>صكة جديدة</span>
          </button>
        </div>
      </div>

      {/* Main Scoreboard */}
      <div className="grid grid-cols-2 gap-4">
        <div className="p-6 rounded-3xl bg-[#002BFF] text-white text-center shadow-sm">
          <div className="text-sm font-bold text-white/85">لنا</div>
          <div className="text-5xl font-bold mt-2">{usTotal}</div>
          <div className="text-xs text-[#FFD000] mt-2">
            المتبقي: {Math.max(0, 152 - usTotal)}
          </div>
        </div>
        <div className="p-6 rounded-3xl bg-[#E52521] text-white text-center shadow-sm">
          <div className="text-sm font-bold text-white/85">لهم</div>
          <div className="text-5xl font-bold mt-2">{themTotal}</div>
          <div className="text-xs text-[#FFD000] mt-2">
            المتبقي: {Math.max(0, 152 - themTotal)}
          </div>
        </div>
      </div>

      {winner && (
        <div className="p-4 rounded-2xl bg-[#FFD000] text-[#111827] text-center font-bold text-base">
          {winner}
        </div>
      )}

      {/* Manual Round Input */}
      <form
        onSubmit={handleSubmit}
        className="p-5 rounded-3xl border border-slate-200 bg-white space-y-4"
      >
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-bold text-slate-600 mb-1.5">نقاط لنا</label>
            <input
              type="number"
              min={0}
              max={300}
              value={usInput}
              onChange={(e) => setUsInput(e.target.value)}
              placeholder="0"
              className="w-full px-4 py-3 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-[#002BFF] focus:outline-none text-lg font-bold text-center"
            />
          </div>
          <div>
            <label className="block text-xs font-bold text-slate-600 mb-1.5">نقاط لهم</label>
            <input
              type="number"
              min={0}
              max={300}
              value={themInput}
              onChange={(e) => setThemInput(e.target.value)}
              placeholder="0"
              className="w-full px-4 py-3 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-[#E52521] focus:outline-none text-lg font-bold text-center"
            />
          </div>
        </div>

        <button
          type="submit"
          className="w-full py-3 rounded-xl bg-[#002BFF] hover:bg-[#0021CC] text-white font-bold text-sm cursor-pointer"
        >
          تسجيل الجولة
        </button>

        {/* Quick Baloot Presets */}
        <div className="pt-3 border-t border-slate-100">
          <div className="text-xs font-bold text-slate-500 mb-2">إضافة سريعة (خسارة / كبوت)</div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => addRound(16, 0, 'حكم كامل لنا')}
              className="px-3 py-1.5 rounded-lg bg-blue-50 text-[#002BFF] text-xs font-bold cursor-pointer"
            >
              +16 لنا (حكم)
            </button>
            <button
              type="button"
              onClick={() => addRound(26, 0, 'صن كامل لنا')}
              className="px-3 py-1.5 rounded-lg bg-blue-50 text-[#002BFF] text-xs font-bold cursor-pointer"
            >
              +26 لنا (صن)
            </button>
            <button
              type="button"
              onClick={() => addRound(44, 0, 'كبوت صن لنا')}
              className="px-3 py-1.5 rounded-lg bg-blue-50 text-[#002BFF] text-xs font-bold cursor-pointer"
            >
              +44 لنا (كبوت صن)
            </button>
            <button
              type="button"
              onClick={() => addRound(0, 16, 'حكم كامل لهم')}
              className="px-3 py-1.5 rounded-lg bg-red-50 text-[#E52521] text-xs font-bold cursor-pointer"
            >
              +16 لهم (حكم)
            </button>
            <button
              type="button"
              onClick={() => addRound(0, 26, 'صن كامل لهم')}
              className="px-3 py-1.5 rounded-lg bg-red-50 text-[#E52521] text-xs font-bold cursor-pointer"
            >
              +26 لهم (صن)
            </button>
            <button
              type="button"
              onClick={() => addRound(0, 44, 'كبوت صن لهم')}
              className="px-3 py-1.5 rounded-lg bg-red-50 text-[#E52521] text-xs font-bold cursor-pointer"
            >
              +44 لهم (كبوت صن)
            </button>
          </div>
        </div>
      </form>

      {/* Round Ledger */}
      {entries.length > 0 && (
        <div className="rounded-3xl border border-slate-200 bg-white overflow-hidden">
          <div className="grid grid-cols-3 px-4 py-2.5 bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-500 text-center">
            <div>الجولة</div>
            <div>لنا</div>
            <div>لهم</div>
          </div>
          <div className="divide-y divide-slate-100">
            {entries.map((e, idx) => (
              <div key={idx} className="grid grid-cols-3 px-4 py-2.5 text-sm text-center">
                <div className="text-xs text-slate-500">
                  #{idx + 1} {e.note ? `(${e.note})` : ''}
                </div>
                <div className="font-bold text-[#002BFF]">{e.us}</div>
                <div className="font-bold text-[#E52521]">{e.them}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
