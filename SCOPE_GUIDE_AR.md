# دليل Scopes — Fuxi Haptics Engine v0.5.0

## المطلوب الآن

| Scope | السبب | ملاحظة |
|---|---|---|
| `system` | تحميل Hooks في system_server (Legacy Xposed API) | الأهم لمسار الإشعارات المركزي |
| `com.android.server.telecom` | الوصول إلى Ringer | قد يختلف اسم الحزمة/العملية في ROM؛ تحقق من سجل module_loaded |
| `com.fuxi.haptics` | تفعيل LSPosed RemotePreferences داخل تطبيق الإعدادات | بدونه قد لا يستطيع النظام قراءة الإعدادات المشتركة |

## لا تضف الآن

- لا تضف جميع التطبيقات.
- لا تضف SystemUI أو Launcher أو Gboard حتى نبدأ تخصيص أحداثها.
- لا تضف `android` لمجرد أنه يبدو اسم النظام؛ في Legacy Xposed، LSPosed يميز بين `system` (system_server) و`android` (عمليات أخرى/حوارات النظام). استخدم Scope الذي يظهر كـ `system` للوحدة القديمة الحالية.

## إعادة التشغيل

- تغييرات system_server لا تُحمّل بمجرد فتح التطبيق؛ أعد تشغيل الجهاز بعد تعديل Scope.
- تغييرات Telecom تحتاج إعادة تشغيل العملية أو الجهاز؛ إعادة التشغيل الكامل هي الاختبار الأنظف.
- تغييرات التطبيق العادي تسري عند بدء العملية من جديد.

## تشخيص عدم التحميل

ابحث عن:
- `module_loaded package=... process=...`
- `system_hook_installed class=com.android.server.notification.NotificationRecord method=getVibration`
- `system_hook_installed class=com.android.server.notification.NotificationManagerService method=playVibration`
- `telecom_hook method=vibrateIfNeeded`

إذا ظهر `system_hook_class_missing` أو `telecom_hook_class_missing` فهذا يعني أن اسم الكلاس/العملية مختلف أو غير موجود في HyperOS. لا تفعّل الاستبدال حتى نحدد المسار الفعلي.


## v0.5.0: اعتراض لمس التطبيقات عبر CombinedVibration
لا تضف التطبيقات الفردية إلى Scope من أجل touch rules؛ hook موجود في `system_server` ويطابق package name داخل طلب الاهتزاز. ابدأ بقاعدة واحدة دقيقة فقط.


## v0.5.0 — نقطة اعتراض اهتزازات اللمس
- يعتمد المسار الأساسي على `VibratorManagerService.vibrateInternal(...)` لأنه يستقبل `CombinedVibration` بعد تحويل `performHapticFeedback` إلى تأثير اهتزاز، ويحمل `opPkg` و`VibrationAttributes`.
- إذا لم يوجد `vibrateInternal` في بناء الشركة، يجرب Hook احتياطي على `vibrate(...)`.
- Xiaomi 13 جهاز بمحرك واحد؛ لذلك لا نستبدل إلا `CombinedVibration.Mono` الذي يتيح استخراج `VibrationEffect`، ثم نعيد إنشاء Mono waveform. التركيبات Stereo/Sequential تُترك كما هي.
- لا تفعّل قواعد اللمس قبل أن يظهر `system_hook_installed ... vibrateInternal` في سجل LSPosed، ثم `touch_effect_override` عند لمس عناصر واجهة النظام.
