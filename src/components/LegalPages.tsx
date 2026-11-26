import React from 'react';
import { ArrowRight, FileText, ShieldCheck } from 'lucide-react';

interface LegalPageProps {
  page: 'privacy' | 'terms';
  onBack: () => void;
}

export const LegalPage: React.FC<LegalPageProps> = ({ page, onBack }) => {
  return (
    <div className="max-w-3xl mx-auto px-4 py-8 space-y-6">
      <button
        type="button"
        onClick={onBack}
        className="inline-flex items-center gap-1.5 text-sm font-bold text-[#002BFF] hover:underline cursor-pointer"
      >
        <ArrowRight className="w-4 h-4" />
        <span>العودة</span>
      </button>

      <div className="p-6 sm:p-8 rounded-3xl border border-slate-200 bg-white space-y-5">
        <div className="flex items-center gap-2.5 text-[#002BFF]">
          {page === 'privacy' ? (
            <ShieldCheck className="w-6 h-6" />
          ) : (
            <FileText className="w-6 h-6" />
          )}
          <h1 className="text-2xl font-bold text-[#111827]">
            {page === 'privacy' ? 'سياسة الخصوصية — شدها' : 'الشروط والأحكام — شدها'}
          </h1>
        </div>

        {page === 'privacy' ? (
          <div className="space-y-4 text-sm text-slate-700 leading-relaxed">
            <p>
              تلتزم منصة <strong>شدها</strong> بحماية خصوصية جميع اللاعبين وبياناتهم الشخصية وفق أعلى معايير الأمان الرقمي.
            </p>
            <h2 className="text-base font-bold text-[#111827]">1. البيانات التي نجمعها</h2>
            <p>
              نجمع فقط المعلومات الضرورية لتشغيل حسابك في اللعبة: الاسم المستعار، الصورة الرمزية المختارة، الرقم التعريفي الفريد (Player ID)، إحصائيات اللعب والتصنيف، وسجل الجلسات عبر خدمات Firebase Authentication و Cloud Firestore.
            </p>
            <h2 className="text-base font-bold text-[#111827]">2. عدم مشاركة البيانات الحساسة</h2>
            <p>
              لا يظهر بريدك الإلكتروني أبداً للاعبين الآخرين في قائمة المتصدرين أو الأصدقاء أو داخل الطاولات؛ يقتصر العرض العام على اسمك المستعار، صورتك الرمزية، رقمك التعريفي، وتصنيفك في اللعبة.
            </p>
            <h2 className="text-base font-bold text-[#111827]">3. حفظ التفضيلات والمشتريات</h2>
            <p>
              يتم حفظ خلفيات الطاولات المفعّلة وحالة الاشتراك وعدد الجلسات اليومية بشكل خاص مرتبط بحسابك فقط لضمان استمرارية تجربتك عبر الجوال والكمبيوتر.
            </p>
            <h2 className="text-base font-bold text-[#111827]">4. حذف الحساب والتواصل</h2>
            <p>
              يحق لكل لاعب تعديل اسمه أو صورته أو طلب حذف حسابه وبياناته المرتبطة في أي وقت عبر إعدادات الحساب.
            </p>
          </div>
        ) : (
          <div className="space-y-4 text-sm text-slate-700 leading-relaxed">
            <p>
              باستخدامك لموقع وتطبيق <strong>شدها — لعبة بلوت .. تفهمك</strong>، فإنك توافق على الالتزام بالشروط والأحكام التالية:
            </p>
            <h2 className="text-base font-bold text-[#111827]">1. قواعد اللعب النظيف</h2>
            <p>
              تعتمد جميع الجلسات في شدها قوانين البلوت السعودية الرسمية (صكة 152 نقطة فقط). يُمنع استخدام أي وسائل تلاعب أو ألفاظ مسيئة في دردشة الطاولة.
            </p>
            <h2 className="text-base font-bold text-[#111827]">2. الحد اليومي والاشتراكات</h2>
            <p>
              يحق للحساب المجاني إنشاء جلستين (2) أونلاين يومياً مع إمكانية اللعب غير المحدود ضد الذكاء الاصطناعي أو الانضمام لطاولات الأصدقاء. تتيح باقات شدها بلس (الأسبوعية 9.99 ريال، الشهرية 29 ريال، السنوية 149 ريال) إنشاء جلسات أونلاين بلا حدود يومياً.
            </p>
            <h2 className="text-base font-bold text-[#111827]">3. خلفيات المتجر</h2>
            <p>
              تتوفر خلفيتان مجانيتان لجميع اللاعبين، بالإضافة إلى ثلاث خلفيات سعودية فاخرة في المتجر بسعر 20 ريالاً لكل خلفية.
            </p>
            <h2 className="text-base font-bold text-[#111827]">4. المؤقت واللعب التلقائي</h2>
            <p>
              يُمنح كل لاعب 10 ثوانٍ لاتخاذ قراره في المشترى أو اللعب؛ وفي حال انتهاء المهلة يتولى النظام اللعب التلقائي ويضع اللاعب في وضع الانتظار حتى نهاية الجولة حفاظاً على سرعة واستمرارية الجلسة.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
