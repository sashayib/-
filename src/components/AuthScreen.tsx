import React, { useState } from 'react';
import {
  createUserWithEmailAndPassword,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  updateProfile,
} from 'firebase/auth';
import { Eye, EyeOff, Lock, Mail, Smartphone, User } from 'lucide-react';
import { auth, googleProvider } from '../firebase';

interface AuthScreenProps {
  onOpenLegal: (page: 'privacy' | 'terms') => void;
}

function mapAuthError(err: unknown): string {
  const code = (err as { code?: string })?.code || '';
  switch (code) {
    case 'auth/invalid-email':
      return 'صيغة البريد الإلكتروني غير صحيحة.';
    case 'auth/user-not-found':
      return 'لا يوجد حساب مسجل بهذا البريد الإلكتروني.';
    case 'auth/wrong-password':
    case 'auth/invalid-credential':
      return 'البريد الإلكتروني أو كلمة المرور غير صحيحة.';
    case 'auth/email-already-in-use':
      return 'هذا البريد الإلكتروني مسجل مسبقاً، جرّب تسجيل الدخول.';
    case 'auth/weak-password':
      return 'كلمة المرور ضعيفة، يجب أن تكون 6 أحرف على الأقل.';
    case 'auth/operation-not-allowed':
      return 'تسجيل الدخول بالبريد الإلكتروني غير مفعّل بعد في مشروع Firebase. يمكنك الدخول فوراً بحساب جوجل.';
    case 'auth/popup-closed-by-user':
      return 'تم إلغاء نافذة تسجيل الدخول بحساب جوجل.';
    case 'auth/network-request-failed':
      return 'تحقق من اتصالك بالإنترنت وحاول مرة أخرى.';
    default:
      return 'تعذّر إتمام عملية التسجيل، حاول مرة أخرى.';
  }
}

export const AuthScreen: React.FC<AuthScreenProps> = ({ onOpenLegal }) => {
  const [mode, setMode] = useState<'signin' | 'signup'>('signin');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [loading, setLoading] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [infoMsg, setInfoMsg] = useState<string | null>(null);

  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setInfoMsg(null);
    const cleanEmail = email.trim();
    if (!cleanEmail || !cleanEmail.includes('@')) {
      setErrorMsg('الرجاء إدخال بريد إلكتروني صحيح.');
      return;
    }
    if (password.length < 6) {
      setErrorMsg('كلمة المرور يجب أن تكون 6 أحرف على الأقل.');
      return;
    }
    if (mode === 'signup' && displayName.trim().length < 2) {
      setErrorMsg('الرجاء إدخال اسمك في اللعبة (حرفين على الأقل).');
      return;
    }
    setLoading(true);
    try {
      if (mode === 'signup') {
        const cred = await createUserWithEmailAndPassword(auth, cleanEmail, password);
        await updateProfile(cred.user, { displayName: displayName.trim() });
      } else {
        await signInWithEmailAndPassword(auth, cleanEmail, password);
      }
    } catch (err) {
      setErrorMsg(mapAuthError(err));
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async () => {
    setErrorMsg(null);
    setInfoMsg(null);
    const cleanEmail = email.trim();
    if (!cleanEmail || !cleanEmail.includes('@')) {
      setErrorMsg('أدخل بريدك الإلكتروني أولاً لإرسال رابط استعادة كلمة المرور.');
      return;
    }
    setLoading(true);
    try {
      await sendPasswordResetEmail(auth, cleanEmail);
      setInfoMsg('تم إرسال رابط استعادة كلمة المرور إلى بريدك الإلكتروني.');
    } catch (err) {
      setErrorMsg(mapAuthError(err));
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setErrorMsg(null);
    setInfoMsg(null);
    setGoogleLoading(true);
    try {
      await signInWithPopup(auth, googleProvider);
    } catch (err) {
      setErrorMsg(mapAuthError(err));
    } finally {
      setGoogleLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-white flex flex-col justify-between">
      <div className="flex-1 flex flex-col lg:flex-row items-stretch">
        {/* Desktop Brand Showcase Column */}
        <div className="hidden lg:flex lg:w-[46%] bg-[#002BFF] text-white flex-col justify-between p-12 relative overflow-hidden">
          <div>
            <div className="bg-white rounded-3xl p-5 inline-flex items-center justify-center shadow-lg">
              <img
                src="/assets/shaddaha_logo.png"
                alt="شعار شدها"
                className="w-44 h-auto object-contain"
              />
            </div>
            <h1 className="text-4xl font-bold mt-8 leading-tight">
              لعبة بلوت .. تفهــــمك
            </h1>
            <p className="text-white/85 text-base mt-4 max-w-md leading-relaxed">
              العب صكة 152 أونلاين مع أصدقائك أو ضد الذكاء الاصطناعي، أعلن مشاريعك، نافس على صدارة المتصدرين، واختر طاولتك السعودية المفضلة.
            </p>
          </div>

          <div className="grid grid-cols-3 gap-4 pt-8 border-t border-white/15">
            <div>
              <div className="text-2xl font-bold text-[#FFD000]">152</div>
              <div className="text-xs text-white/80 mt-1">صكة رسمية معتمدة</div>
            </div>
            <div>
              <div className="text-2xl font-bold text-[#FFD000]">10 ث</div>
              <div className="text-xs text-white/80 mt-1">مؤقت دور سريع</div>
            </div>
            <div>
              <div className="text-2xl font-bold text-[#FFD000]">أونلاين</div>
              <div className="text-xs text-white/80 mt-1">جلسات عامة وخاصة</div>
            </div>
          </div>
        </div>

        {/* Auth Form Column */}
        <div className="flex-1 flex flex-col items-center justify-center px-5 py-8 sm:px-10">
          <div className="w-full max-w-md">
            {/* Mobile Logo */}
            <div className="flex flex-col items-center mb-6 lg:hidden">
              <img
                src="/assets/shaddaha_logo.png"
                alt="شعار شدها"
                className="w-44 h-auto object-contain"
              />
              <p className="text-[#002BFF] font-bold text-sm mt-1">لعبة بلوت .. تفهــــمك</p>
            </div>

            <div className="hidden lg:block mb-6">
              <h2 className="text-2xl font-bold text-[#111827]">
                {mode === 'signin' ? 'أهلاً بعودتك إلى شدها' : 'أنشئ حسابك في شدها'}
              </h2>
              <p className="text-sm text-slate-500 mt-1">
                سجّل دخولك لحفظ تصنيفك، إضافة أصدقائك، واللعب في الطاولات الأونلاين.
              </p>
            </div>

            {/* Segmented Mode Toggle */}
            <div className="grid grid-cols-2 gap-1 p-1 bg-slate-100 rounded-xl border border-slate-200 mb-5">
              <button
                type="button"
                onClick={() => {
                  setMode('signin');
                  setErrorMsg(null);
                  setInfoMsg(null);
                }}
                className={`py-2.5 text-sm font-bold rounded-lg transition-colors cursor-pointer ${
                  mode === 'signin'
                    ? 'bg-[#002BFF] text-white shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                تسجيل الدخول
              </button>
              <button
                type="button"
                onClick={() => {
                  setMode('signup');
                  setErrorMsg(null);
                  setInfoMsg(null);
                }}
                className={`py-2.5 text-sm font-bold rounded-lg transition-colors cursor-pointer ${
                  mode === 'signup'
                    ? 'bg-[#002BFF] text-white shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                حساب جديد
              </button>
            </div>

            {errorMsg && (
              <div className="mb-4 p-3.5 rounded-xl bg-red-50 border border-[#E52521]/30 text-[#E52521] text-sm font-medium">
                {errorMsg}
              </div>
            )}

            {infoMsg && (
              <div className="mb-4 p-3.5 rounded-xl bg-blue-50 border border-[#002BFF]/25 text-[#002BFF] text-sm font-medium">
                {infoMsg}
              </div>
            )}

            <form onSubmit={handleEmailSubmit} className="space-y-3.5">
              {mode === 'signup' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                    الاسم في اللعبة
                  </label>
                  <div className="relative">
                    <User className="w-4 h-4 text-slate-400 absolute right-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={displayName}
                      onChange={(e) => setDisplayName(e.target.value)}
                      placeholder="مثال: أبو فهد"
                      maxLength={40}
                      className="w-full pr-10 pl-3.5 py-3 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-[#002BFF] focus:outline-none text-sm"
                    />
                  </div>
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  البريد الإلكتروني
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute right-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="name@example.com"
                    dir="ltr"
                    className="w-full pr-10 pl-3.5 py-3 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-[#002BFF] focus:outline-none text-sm text-right"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-xs font-semibold text-slate-700">كلمة المرور</label>
                  {mode === 'signin' && (
                    <button
                      type="button"
                      onClick={handleResetPassword}
                      className="text-xs font-semibold text-[#002BFF] hover:underline cursor-pointer"
                    >
                      نسيت كلمة المرور؟
                    </button>
                  )}
                </div>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute right-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type={showPass ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="6 أحرف على الأقل"
                    className="w-full pr-10 pl-10 py-3 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-[#002BFF] focus:outline-none text-sm"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPass((v) => !v)}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700 cursor-pointer"
                    aria-label="إظهار أو إخفاء كلمة المرور"
                  >
                    {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 rounded-xl bg-[#002BFF] hover:bg-[#0021CC] text-white font-bold text-sm transition-colors disabled:opacity-60 cursor-pointer"
              >
                {loading
                  ? 'جاري المعالجة...'
                  : mode === 'signin'
                    ? 'دخول بالبريد الإلكتروني'
                    : 'إنشاء حساب جديد'}
              </button>
            </form>

            <div className="flex items-center gap-3 my-5">
              <div className="flex-1 h-px bg-slate-200" />
              <span className="text-xs text-slate-400 font-medium">أو المتابعة عبر</span>
              <div className="flex-1 h-px bg-slate-200" />
            </div>

            {/* Google Sign-In */}
            <button
              type="button"
              onClick={handleGoogleSignIn}
              disabled={googleLoading}
              className="w-full py-3.5 px-4 rounded-xl bg-[#FFD000] hover:bg-[#f0c300] text-[#111827] font-bold text-sm flex items-center justify-center gap-2.5 transition-colors disabled:opacity-60 cursor-pointer"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                />
              </svg>
              <span>{googleLoading ? 'جاري تسجيل الدخول...' : 'التسجيل بحساب جوجل'}</span>
            </button>

            {/* Phone Sign-In Coming Soon */}
            <div className="mt-4 p-3.5 rounded-xl border border-dashed border-slate-300 bg-slate-50">
              <div className="flex items-center justify-between mb-2">
                <span className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
                  <Smartphone className="w-4 h-4 text-[#002BFF]" />
                  التسجيل برقم الجوال
                </span>
                <span className="text-xs font-bold text-[#E52521]">قريباً</span>
              </div>
              <input
                type="tel"
                disabled
                placeholder="05X XXX XXXX — سيتوفر الدخول برسالة SMS قريباً"
                className="w-full px-3 py-2.5 rounded-lg border border-slate-200 bg-white/70 text-xs text-slate-400 cursor-not-allowed"
              />
            </div>

            {/* Legal links */}
            <div className="mt-6 flex items-center justify-center gap-4 text-xs text-slate-500">
              <button
                type="button"
                onClick={() => onOpenLegal('privacy')}
                className="hover:text-[#002BFF] hover:underline cursor-pointer"
              >
                سياسة الخصوصية
              </button>
              <span aria-hidden="true">·</span>
              <button
                type="button"
                onClick={() => onOpenLegal('terms')}
                className="hover:text-[#002BFF] hover:underline cursor-pointer"
              >
                الشروط والأحكام
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Shaddaha Yellow Wave Accent Strip (Mobile) */}
      <div className="h-3 bg-[#FFD000] w-full" />
    </div>
  );
};
