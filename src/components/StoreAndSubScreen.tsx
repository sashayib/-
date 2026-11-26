import React from 'react';
import { Check, Crown, Lock, Sparkles, X } from 'lucide-react';
import {
  FREE_DAILY_SESSION_LIMIT,
  PURCHASES_ENABLED,
  SUBSCRIPTION_PLANS,
  SubscriptionPlanId,
  TABLE_BACKGROUNDS,
  TableBgId,
  UserSettings,
} from '../game/balootTypes';
import { TableBackgroundCanvas } from './TableBackgroundCanvas';

interface StoreScreenProps {
  settings: UserSettings;
  remainingFreeToday: number;
  onSelectOrUnlockBg: (bgId: TableBgId) => void;
  onActivatePlan: (planId: Exclude<SubscriptionPlanId, 'none'>) => void;
}

export const StoreAndSubScreen: React.FC<StoreScreenProps> = ({
  settings,
  remainingFreeToday,
  onSelectOrUnlockBg,
  onActivatePlan,
}) => {
  const isSubscribed = settings.subscriptionPlan !== 'none';

  return (
    <div className="max-w-6xl mx-auto px-4 py-6 sm:py-8 space-y-10">
      {/* Header */}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-[#111827]">متجر شدها واشتراكات بلس</h1>
          <p className="text-sm text-slate-500 mt-1">
            اختر خلفيات الطاولة السعودية الفاخرة أو اشترك في شدها بلس لإنشاء جلسات أونلاين بلا حدود.
          </p>
        </div>
        <div className="text-xs font-bold text-slate-600">
          {isSubscribed
            ? `اشتراكك النشط: ${SUBSCRIPTION_PLANS.find((p) => p.id === settings.subscriptionPlan)?.title || 'شدها بلس'}`
            : `الجلسات المجانية المتبقية اليوم: ${remainingFreeToday} من ${FREE_DAILY_SESSION_LIMIT}`}
        </div>
      </div>

      {!PURCHASES_ENABLED && (
        <div className="p-4 rounded-2xl border border-amber-200 bg-amber-50 text-amber-900 flex items-center gap-3 text-xs sm:text-sm font-bold">
          <Lock className="w-5 h-5 text-amber-700 shrink-0" />
          <span>
            عملية الشراء والاشتراكات المدفوعة مقفلة حالياً — الخيارات المجانية فقط مفعّلة في الوقت الحالي.
          </span>
        </div>
      )}

      {/* Section 1: 5 Table Backgrounds (2 Free + 3 Saudi 20 SAR) */}
      <section className="space-y-4">
        <div>
          <h2 className="text-lg font-bold text-[#111827]">خلفيات طاولات اللعب (5 خلفيات)</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            خلفيتان مجانيتان + 3 خلفيات سعودية رسمية بسعر 20 ريال لكل خلفية — تظهر كاملة على طاولة اللعب بدون أي دائرة في الوسط.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {TABLE_BACKGROUNDS.map((bg) => {
            const owned = bg.priceSar === 0 || settings.ownedTableBgs.includes(bg.id);
            const selected = settings.selectedTableBg === bg.id;
            return (
              <div
                key={bg.id}
                className={`rounded-3xl border-2 overflow-hidden bg-white flex flex-col transition-all ${
                  selected ? 'border-[#002BFF] shadow-md' : 'border-slate-200'
                }`}
              >
                {/* Exact Full-Bleed Preview */}
                <div className="relative h-48 w-full overflow-hidden border-b border-slate-100">
                  <TableBackgroundCanvas bgId={bg.id} />
                </div>

                <div className="p-4 flex-1 flex flex-col justify-between gap-3">
                  <div>
                    <div className="flex items-center justify-between gap-2">
                      <h3 className="font-bold text-base text-[#111827]">{bg.title}</h3>
                      <span
                        className={`text-xs font-bold shrink-0 ${
                          bg.priceSar === 0 ? 'text-emerald-700' : 'text-[#002BFF]'
                        }`}
                      >
                        {bg.priceSar === 0 ? 'مجانية' : `${bg.priceSar} ريال`}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-1">{bg.subtitle}</p>
                  </div>

                  <button
                    type="button"
                    disabled={!owned && !PURCHASES_ENABLED}
                    onClick={() => onSelectOrUnlockBg(bg.id)}
                    className={`w-full py-2.5 rounded-xl font-bold text-sm flex items-center justify-center gap-1.5 transition-colors ${
                      selected
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200 cursor-pointer'
                        : owned
                          ? 'bg-[#002BFF] hover:bg-[#0021CC] text-white cursor-pointer'
                          : !PURCHASES_ENABLED
                            ? 'bg-slate-100 text-slate-400 border border-slate-200 cursor-not-allowed'
                            : 'bg-[#FFD000] hover:bg-[#f0c300] text-[#111827] cursor-pointer'
                    }`}
                  >
                    {selected ? (
                      <>
                        <Check className="w-4 h-4" />
                        <span>مفعّلة حالياً</span>
                      </>
                    ) : owned ? (
                      <span>تفعيل الخلفية المجانية</span>
                    ) : !PURCHASES_ENABLED ? (
                      <>
                        <Lock className="w-4 h-4" />
                        <span>الشراء مقفل حالياً</span>
                      </>
                    ) : (
                      <>
                        <Lock className="w-4 h-4" />
                        <span>شراء وتفعيل — {bg.priceSar} ريال</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Section 2: Subscription Plans */}
      <section className="space-y-4 pt-4 border-t border-slate-200">
        <div>
          <h2 className="text-lg font-bold text-[#111827] flex items-center gap-2">
            <Crown className="w-5 h-5 text-[#FFD000]" />
            <span>باقات اشتراك شدها بلس</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            للمستخدم المجاني جلستان (2) أونلاين فقط يومياً للإنشاء. اشترك لفتح إنشاء جلسات غير محدود طوال اليوم.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          {SUBSCRIPTION_PLANS.map((plan) => {
            const active = settings.subscriptionPlan === plan.id;
            const featured = plan.id === 'monthly';
            return (
              <div
                key={plan.id}
                className={`rounded-3xl p-6 border-2 flex flex-col justify-between ${
                  active
                    ? 'border-[#002BFF] bg-blue-50/40'
                    : featured
                      ? 'border-[#002BFF] bg-white shadow-md'
                      : 'border-slate-200 bg-white'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between gap-2">
                    <h3 className="text-base font-bold text-[#111827]">{plan.title}</h3>
                    {plan.badge && (
                      <span className="text-xs font-bold text-[#E52521]">{plan.badge}</span>
                    )}
                  </div>
                  <div className="mt-3 flex items-baseline gap-1.5">
                    <span className="text-3xl font-bold text-[#002BFF]">{plan.priceText}</span>
                    <span className="text-xs text-slate-500">/ {plan.periodText}</span>
                  </div>
                  <ul className="mt-5 space-y-2.5 text-xs text-slate-700">
                    {plan.perks.map((perk) => (
                      <li key={perk} className="flex items-start gap-2">
                        <Check className="w-4 h-4 text-[#002BFF] shrink-0 mt-0.5" />
                        <span>{perk}</span>
                      </li>
                    ))}
                  </ul>
                </div>

                <button
                  type="button"
                  disabled={!active && !PURCHASES_ENABLED}
                  onClick={() => onActivatePlan(plan.id)}
                  className={`mt-6 w-full py-3 rounded-xl font-bold text-sm flex items-center justify-center gap-1.5 transition-colors ${
                    active
                      ? 'bg-emerald-600 text-white cursor-default'
                      : !PURCHASES_ENABLED
                        ? 'bg-slate-100 text-slate-400 border border-slate-200 cursor-not-allowed'
                        : featured
                          ? 'bg-[#002BFF] hover:bg-[#0021CC] text-white cursor-pointer'
                          : 'bg-[#FFD000] hover:bg-[#f0c300] text-[#111827] cursor-pointer'
                  }`}
                >
                  {active ? (
                    'باقتك النشطة حالياً'
                  ) : !PURCHASES_ENABLED ? (
                    <>
                      <Lock className="w-4 h-4" />
                      <span>الاشتراك مقفل حالياً</span>
                    </>
                  ) : (
                    `اشترك الآن — ${plan.priceText}`
                  )}
                </button>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
};

export const SubscriptionModal: React.FC<{
  open: boolean;
  reasonMessage?: string | null;
  activePlan: SubscriptionPlanId;
  onSelectPlan: (planId: Exclude<SubscriptionPlanId, 'none'>) => void;
  onClose: () => void;
}> = ({ open, reasonMessage, activePlan, onSelectPlan, onClose }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 bg-black/55 flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl max-w-xl w-full p-6 shadow-2xl border border-slate-200 max-h-[90vh] overflow-y-auto">
        <div className="flex items-start justify-between gap-3 mb-4">
          <div>
            <div className="flex items-center gap-2 text-[#002BFF]">
              <Sparkles className="w-5 h-5" />
              <h3 className="text-lg font-bold">اشترك في شدها بلس</h3>
            </div>
            <p className="text-xs text-slate-600 mt-1">
              {reasonMessage ||
                'للمستخدم المجاني جلستان فقط يومياً لإنشاء الجلسات الأونلاين. اشترك الآن لإنشاء جلسات بلا حدود!'}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-3">
          {SUBSCRIPTION_PLANS.map((plan) => {
            const isActive = activePlan === plan.id;
            return (
              <div
                key={plan.id}
                className={`p-4 rounded-2xl border-2 flex items-center justify-between gap-3 ${
                  plan.id === 'monthly' ? 'border-[#002BFF] bg-blue-50/40' : 'border-slate-200'
                }`}
              >
                <div>
                  <div className="text-sm font-bold text-[#111827]">
                    {plan.title}{' '}
                    {plan.badge && <span className="text-xs text-[#E52521] mr-1">· {plan.badge}</span>}
                  </div>
                  <div className="text-xs text-slate-500 mt-0.5">{plan.perks[0]}</div>
                </div>
                <button
                  type="button"
                  disabled={!isActive && !PURCHASES_ENABLED}
                  onClick={() => onSelectPlan(plan.id)}
                  className={`px-4 py-2.5 rounded-xl text-xs font-bold shrink-0 flex items-center gap-1 ${
                    isActive
                      ? 'bg-emerald-600 text-white'
                      : !PURCHASES_ENABLED
                        ? 'bg-slate-100 text-slate-400 border border-slate-200 cursor-not-allowed'
                        : 'bg-[#002BFF] hover:bg-[#0021CC] text-white cursor-pointer'
                  }`}
                >
                  {isActive ? (
                    'نشطة'
                  ) : !PURCHASES_ENABLED ? (
                    <>
                      <Lock className="w-3.5 h-3.5" />
                      <span>مقفل حالياً</span>
                    </>
                  ) : (
                    plan.priceText
                  )}
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
