# Fuxi Haptics Engine — v0.5.0-alpha

## ما الجديد؟

هذه النسخة تصحح نقطة اعتراض اهتزازات اللمس لتتعامل مع `CombinedVibration` ومسار `vibrateInternal` المستخدم في AOSP Android 15/16، مع إبقاء مساري الإشعارات والمكالمات.

### 1) اهتزازات اللمس لكل تطبيق

تعترض الوحدة `VibratorManagerService.vibrateInternal(...)` داخل `system_server`، وهو مسار داخلي يستقبل `CombinedVibration` واسم الحزمة `opPkg` و`VibrationAttributes` بعد أن يحوّل النظام بعض طلبات `performHapticFeedback` إلى تأثير فعلي. إذا كان الاستخدام `USAGE_TOUCH` وكان التأثير من نوع Mono، تستبدله بموجة مخصصة قبل وصول الطلب إلى HAL. إذا لم يتوفر `vibrateInternal` في بناء الشركة، تجرب Hook احتياطياً على `vibrate(...)`.

صيغة القاعدة: `package|timings|amplitude`
مثال: `com.android.systemui|0,12,18,12|145`

هذا لا يضيف اهتزازاً مستقلاً؛ بل يستبدل طلب اللمس الأصلي. لا توجد wildcard عامة. لا تُعدّل طلبات الإشعارات أو الرنين عبر هذا المسار؛ فهما لهما قواعد منفصلة.

### 2) مسار التوافق مع جهازك

تقرير Ever Haptics على Xiaomi 13 أظهر: amplitude control متاح، لكن primitive effects غير مدعومة، وVibration State Callback غير متاح. كما أن كود Ever يبدأ بمسار ALARM/RICH، ولا يستطيع الانتقال إلى المسارات البديلة عند غياب callback إذا تم قبول استدعاء الاهتزاز دون استثناء؛ وهذا يفسّر بقاء التقرير على `alarm / rich (checking)`. لذلك لا تعتمد هذه النسخة على primitives أو callback، وتُبقي Usage الأصلي TOUCH بدلاً من التحويل إلى ALARM. تُبنى التأثيرات المخصصة باستخدام `VibrationEffect.createWaveform(timings, amplitudes, -1)`، وهي واجهة Android القياسية.

### 3) مختبر RichTap داخل التطبيق
أُضيف SDK الرسمي `richtap_sdk_lite.aar` إلى تطبيق الإعدادات، مع أزرار تجريب مستقلة لـ Click وTick وToggle وSuccess وFailure. الغرض هو التحقق مباشرة من دعم RichTap على Fuxi وسماع/الإحساس بالـ presets قبل اتخاذ قرار دمجه في محرك الأحداث. هذه المعاينات مستقلة ولا تعني أن اعتراضات النظام تستخدم RichTap بالفعل؛ مسارات hooks ما زالت تستخدم Android waveform.

### 4) ما بقي كما هو
- استبدال اهتزاز الإشعار حسب package وchannel ID.
- استبدال نمط الاهتزاز القياسي للمكالمة الواردة مع إبقاء دورة حياة Telecom.
- عدم تغيير HAL أو sysfs أو firmware أو LRA calibration أو خصائص vendor.

## Scopes

- `system` (system_server)
- `com.android.server.telecom`
- `com.fuxi.haptics`

لا تحتاج إلى إضافة كل تطبيق إلى LSPosed scope لمسار اللمس؛ الاعتراض يجري مركزياً في system_server.

## حدود مهمة

- هذه نسخة مصدر alpha؛ لم يُنتج APK ولم تُختبر hooks على Xiaomi 13 فعلياً.
- HyperOS قد يغير أسماء أو توقيعات `VibratorManagerService`؛ يجب فحص سجل LSPosed لمعرفة هل hook يعمل وهل تظهر `touch_effect_override`.
- يُفترض أن تمر طلبات `performHapticFeedback` التي يحوّلها VMS إلى مسار الاهتزاز الداخلي عبر `vibrateInternal`. تُستبدل تأثيرات Mono فقط؛ أما Stereo وSequential فتُترك كما هي. هذا السلوك يحتاج تحققاً على HyperOS.
- MiHaptic/RichTap غير موصولين بعد. دمج SDK مباشرة داخل system_server غير آمن دون إثبات إمكانية تحميله في هذا السياق؛ سنختبر واجهة Xiaomi أولاً في تطبيق companion، ثم نقرر جسر التنفيذ.
- لا يوجد تخصيص لكل جهة اتصال في هذه المرحلة.

## الاختبار

ابدأ بإبقاء المفاتيح مغلقة، وتأكد من تثبيت الوحدة وتفعيل Scopes. بعد ذلك فعّل قاعدة واحدة فقط لـ `com.android.systemui`، واستخدم نمطاً قصيراً مثل `0,12,18,12` وشدة 100–145. إذا لم يظهر `touch_effect_override` في سجل LSPosed عند لمس عناصر النظام، فهذا يعني أن طلبات جهازك تسلك مساراً مختلفاً؛ لا نرفع الشدة ولا نغير ملفات النظام لتعويض ذلك.
