# مصفوفة الاختبار v0.5.0-alpha

| الاختبار | المتوقع | الحالة الحالية |
|---|---|---|
| PatternCodec | رفض القيم السالبة/الطويلة | اجتاز smoke test محلياً |
| TouchRuleCodec | قواعد package دقيقة ورفض wildcard | اجتاز smoke test محلياً |
| NotificationRuleCodec | مطابقة package/channel وعزل القنوات | اجتاز smoke test سابقاً |
| Java source compile | توافق صياغة core/hooks/UI مع stubs محلية | ناجح؛ ليس بديلاً عن Android Gradle build |
| Manifest XML | XML صالح | ناجح |
| Hook VMS vibrateInternal | اعتراض CombinedVibration Mono مع USAGE_TOUCH | لم يختبر على HyperOS |
| Hook fallback vibrate | يستخدم فقط إذا لم يوجد vibrateInternal | لم يختبر على HyperOS |
| HapticFeedback conversion | يصل إلى internal funnel | وفق AOSP؛ يلزم إثبات Xiaomi |
| إشعارات NMS | المحافظة على قواعد v0.3 | لم يختبر على الجهاز |
| Telecom Ringer | المحافظة على قواعد v0.3 | لم يختبر على الجهاز |
| RichTap preset preview | اختبار دعم SDK من تطبيق companion | مضاف في الواجهة؛ يحتاج تشغيل على Fuxi |
| RichTap داخل system_server | غير مدمج | مؤجل؛ لا نحمل AAR داخل system_server |
| Android Gradle build / APK | APK قابل للتثبيت | لم يُنفّذ في هذه البيئة؛ CI مطلوب |

لم يتم تنفيذ Gradle build أو إنتاج APK في بيئة العمل الحالية.
