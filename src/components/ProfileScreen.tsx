import React, { useEffect, useState } from 'react';
import { AlertCircle, Check, CheckCircle2, Copy, Edit3, Loader2, LogOut, Save, ShoppingBag } from 'lucide-react';
import {
  AVATAR_PRESETS,
  PlayerProfile,
  PURCHASES_ENABLED,
  TABLE_BACKGROUNDS,
  TableBgId,
  UserSettings,
} from '../game/balootTypes';
import { TableBackgroundThumbnail } from './TableBackgroundCanvas';

interface ProfileScreenProps {
  profile: PlayerProfile;
  settings: UserSettings;
  googlePhotoUrl: string | null;
  loading?: boolean;
  saving?: boolean;
  errorMessage?: string | null;
  successMessage?: string | null;
  onRefresh?: () => Promise<void>;
  onUpdateProfile: (displayName: string, avatarUrl: string | null) => Promise<boolean>;
  onSelectTableBg: (bgId: TableBgId) => void;
  onOpenStore: () => void;
  onSignOut: () => void;
}

export const ProfileScreen: React.FC<ProfileScreenProps> = ({
  profile,
  settings,
  googlePhotoUrl,
  loading = false,
  saving = false,
  errorMessage = null,
  successMessage = null,
  onRefresh,
  onUpdateProfile,
  onSelectTableBg,
  onOpenStore,
  onSignOut,
}) => {
  const [editingName, setEditingName] = useState(false);
  const [nameDraft, setNameDraft] = useState(profile.displayName);
  const [localError, setLocalError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  // Fetch latest users/{uid} data from Firestore when opening the Profile screen
  useEffect(() => {
    if (onRefresh) {
      void onRefresh();
    }
  }, [onRefresh]);

  // Keep draft synced with latest Firestore profile when not actively editing
  useEffect(() => {
    if (!editingName) {
      setNameDraft(profile.displayName);
    }
  }, [profile.displayName, editingName]);

  const totalMatches = profile.wins + profile.losses;
  const winPct = totalMatches > 0 ? Math.round((profile.wins / totalMatches) * 100) : 0;
  const radius = 54;
  const circumference = 2 * Math.PI * radius;
  const winStroke = totalMatches > 0 ? (profile.wins / totalMatches) * circumference : 0;

  const handleSaveName = async () => {
    const clean = nameDraft.trim();
    if (clean.length < 2) {
      setLocalError('يجب أن يتكون اسم اللاعب من حرفين على الأقل.');
      return;
    }
    setLocalError(null);
    const ok = await onUpdateProfile(clean, profile.avatarUrl);
    if (ok) {
      setEditingName(false);
    }
  };

  const handleSelectAvatar = async (avatarId: string) => {
    if (saving) return;
    setLocalError(null);
    await onUpdateProfile(profile.displayName, avatarId);
  };

  const handleCopyId = async () => {
    try {
      await navigator.clipboard.writeText(profile.playerCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // ignore
    }
  };

  const activeError = localError || errorMessage;

  const avatarOptions = [
    ...(googlePhotoUrl ? [{ id: googlePhotoUrl, label: 'صورة Google' }] : []),
    ...AVATAR_PRESETS,
  ];

  return (
    <div className="max-w-5xl mx-auto px-4 py-6 sm:py-8 space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-[#111827]">الملف الشخصي</h1>
          <p className="text-sm text-slate-500 mt-1">
            تحكّم بصورتك الرمزية، تصنيفك، إحصائياتك، وخلفية طاولة الجلسة.
          </p>
        </div>
        <button
          type="button"
          onClick={onSignOut}
          className="px-4 py-2.5 rounded-xl border border-slate-200 hover:bg-red-50 text-[#E52521] font-bold text-sm flex items-center gap-2 cursor-pointer"
        >
          <LogOut className="w-4 h-4" />
          <span>تسجيل الخروج</span>
        </button>
      </div>

      {/* Top Row: Identity Card + Win/Loss Donut Chart */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
        {/* Identity & Avatar Picker */}
        <div className="lg:col-span-7 p-6 rounded-3xl border border-slate-200 bg-white space-y-5">
          {loading && (
            <div className="flex items-center gap-2 px-3.5 py-2.5 rounded-xl bg-blue-50/70 border border-blue-100 text-xs font-semibold text-[#002BFF]">
              <Loader2 className="w-4 h-4 animate-spin shrink-0" />
              <span>جاري تحديث بيانات ملفك الشخصي...</span>
            </div>
          )}

          {activeError && (
            <div className="flex items-center gap-2 px-3.5 py-2.5 rounded-xl bg-red-50 border border-red-200 text-xs font-semibold text-[#E52521]">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{activeError}</span>
            </div>
          )}

          {!activeError && successMessage && (
            <div className="flex items-center gap-2 px-3.5 py-2.5 rounded-xl bg-emerald-50 border border-emerald-200 text-xs font-semibold text-emerald-700">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>{successMessage}</span>
            </div>
          )}

          <div className="flex flex-wrap items-center gap-4">
            <img
              src={profile.avatarUrl || '/assets/shaddaha_avatar.png'}
              alt={profile.displayName}
              className="w-20 h-20 rounded-full object-cover border-2 border-[#002BFF] bg-slate-100"
            />
            <div className="flex-1 min-w-[200px]">
              {editingName ? (
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    value={nameDraft}
                    onChange={(e) => {
                      setNameDraft(e.target.value);
                      if (localError) setLocalError(null);
                    }}
                    maxLength={40}
                    disabled={saving}
                    className="flex-1 px-3 py-2 rounded-xl border border-[#002BFF] text-sm font-bold disabled:opacity-60"
                  />
                  <button
                    type="button"
                    disabled={saving}
                    onClick={handleSaveName}
                    className="px-3.5 py-2 rounded-xl bg-[#002BFF] text-white text-xs font-bold flex items-center gap-1 cursor-pointer disabled:opacity-60"
                  >
                    {saving ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Save className="w-3.5 h-3.5" />}
                    <span>{saving ? 'جاري الحفظ...' : 'حفظ'}</span>
                  </button>
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <h2 className="text-xl font-bold text-[#111827]">{profile.displayName}</h2>
                  <button
                    type="button"
                    disabled={saving}
                    onClick={() => {
                      setLocalError(null);
                      setNameDraft(profile.displayName);
                      setEditingName(true);
                    }}
                    className="p-1.5 rounded-lg text-slate-400 hover:text-[#002BFF] cursor-pointer disabled:opacity-50"
                    aria-label="تعديل الاسم"
                  >
                    <Edit3 className="w-4 h-4" />
                  </button>
                </div>
              )}

              <div className="flex flex-wrap items-center gap-2 mt-1.5 text-xs text-slate-600">
                <span className="font-bold text-[#002BFF]">{profile.rankTier}</span>
                <span>·</span>
                <span>المستوى {profile.level}</span>
                <span>·</span>
                <span>{profile.xp} نقطة خبرة</span>
                <span>·</span>
                <button
                  type="button"
                  onClick={handleCopyId}
                  className="inline-flex items-center gap-1 font-bold text-[#111827] hover:text-[#002BFF] cursor-pointer"
                >
                  <span dir="ltr">ID: {profile.playerCode}</span>
                  {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-100">
            <div className="text-xs font-bold text-slate-700 mb-3">اختر صورتك الرمزية</div>
            <div className="flex flex-wrap gap-2.5">
              {avatarOptions.map((opt) => {
                const selected =
                  (profile.avatarUrl || '/assets/shaddaha_avatar.png') === opt.id;
                return (
                  <button
                    key={opt.id}
                    type="button"
                    disabled={saving}
                    onClick={() => void handleSelectAvatar(opt.id)}
                    className={`p-1 rounded-full border-2 transition-transform cursor-pointer disabled:opacity-60 ${
                      selected
                        ? 'border-[#002BFF] scale-105 shadow-sm'
                        : 'border-slate-200 hover:border-slate-400'
                    }`}
                    title={opt.label}
                  >
                    <img
                      src={opt.id}
                      alt={opt.label}
                      className="w-12 h-12 rounded-full object-cover bg-slate-100"
                    />
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Win / Loss Pie / Donut Chart Card */}
        <div className="lg:col-span-5 p-6 rounded-3xl border border-slate-200 bg-white flex flex-col justify-between">
          <div>
            <h3 className="text-base font-bold text-[#111827]">إحصائيات الفوز والخسارة</h3>
            <p className="text-xs text-slate-500 mt-0.5">مخطط دائري لنسبة انتصاراتك في شدها</p>
          </div>

          <div className="flex items-center justify-around gap-4 my-5">
            <div className="relative w-36 h-36 flex items-center justify-center">
              <svg className="w-36 h-36 -rotate-90" viewBox="0 0 140 140">
                {/* Base circle (Losses or empty) */}
                <circle
                  cx="70"
                  cy="70"
                  r={radius}
                  fill="none"
                  stroke={totalMatches > 0 ? '#E52521' : '#E5E7EB'}
                  strokeWidth="16"
                />
                {/* Win arc */}
                {totalMatches > 0 && (
                  <circle
                    cx="70"
                    cy="70"
                    r={radius}
                    fill="none"
                    stroke="#002BFF"
                    strokeWidth="16"
                    strokeDasharray={`${winStroke} ${circumference}`}
                    strokeLinecap="round"
                  />
                )}
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
                <span className="text-2xl font-bold text-[#111827]">{winPct}%</span>
                <span className="text-[11px] text-slate-500">نسبة الفوز</span>
              </div>
            </div>

            <div className="space-y-2.5 text-sm">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-[#002BFF]" />
                <span className="text-slate-600">فوز:</span>
                <span className="font-bold text-[#111827]">{profile.wins}</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-[#E52521]" />
                <span className="text-slate-600">خسارة:</span>
                <span className="font-bold text-[#111827]">{profile.losses}</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-[#FFD000]" />
                <span className="text-slate-600">سلسلة الفوز:</span>
                <span className="font-bold text-[#111827]">{profile.streak}</span>
              </div>
              <div className="text-xs text-slate-500 pt-1">
                إجمالي المباريات: {profile.matchesPlayed}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Table Background Selector */}
      <section className="p-6 rounded-3xl border border-slate-200 bg-white">
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <div>
            <h3 className="text-base font-bold text-[#111827]">خلفية طاولة اللعب (الجلسة)</h3>
            <p className="text-xs text-slate-500 mt-0.5">
              اختر الخلفية التي تظهر على طاولة اللعب بالكامل بدون أي دائرة في الوسط.
            </p>
          </div>
          <button
            type="button"
            onClick={onOpenStore}
            className="px-3.5 py-2 rounded-xl bg-[#FFD000] hover:bg-[#f0c300] text-[#111827] text-xs font-bold flex items-center gap-1.5 cursor-pointer"
          >
            <ShoppingBag className="w-4 h-4" />
            <span>فتح المتجر</span>
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
          {TABLE_BACKGROUNDS.map((bg) => {
            const owned = bg.priceSar === 0 || settings.ownedTableBgs.includes(bg.id);
            const selected = settings.selectedTableBg === bg.id;
            return (
              <button
                key={bg.id}
                type="button"
                disabled={!owned && !PURCHASES_ENABLED}
                onClick={() => (owned ? onSelectTableBg(bg.id) : onOpenStore())}
                className={`p-3.5 rounded-2xl border-2 text-right flex items-center gap-3.5 transition-all ${
                  selected
                    ? 'border-[#002BFF] bg-blue-50/40 cursor-pointer'
                    : owned
                      ? 'border-slate-200 hover:border-slate-300 bg-white cursor-pointer'
                      : !PURCHASES_ENABLED
                        ? 'border-slate-200 bg-slate-50/80 opacity-70 cursor-not-allowed'
                        : 'border-slate-200 hover:border-slate-300 bg-white cursor-pointer'
                }`}
              >
                <TableBackgroundThumbnail bgId={bg.id} selected={selected} locked={!owned} />
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-bold text-[#111827]">{bg.title}</div>
                  <div className="text-xs text-slate-500 mt-0.5 line-clamp-2">{bg.subtitle}</div>
                  <div
                    className={`text-xs font-bold mt-2 ${
                      selected
                        ? 'text-[#002BFF]'
                        : owned
                          ? 'text-emerald-700'
                          : !PURCHASES_ENABLED
                            ? 'text-slate-400'
                            : 'text-[#E52521]'
                    }`}
                  >
                    {selected
                      ? 'مفعّلة الآن'
                      : owned
                        ? 'مجانية — اضغط للتفعيل'
                        : !PURCHASES_ENABLED
                          ? 'مقفل حالياً (الشراء غير مفعل)'
                          : `${bg.priceSar} ريال في المتجر`}
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </section>
    </div>
  );
};
