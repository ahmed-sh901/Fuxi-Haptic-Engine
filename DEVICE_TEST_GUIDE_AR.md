# اختبار v0.5.0 على Xiaomi 13 / HyperOS 3

## قبل التثبيت
- لا تثبّت حزمة KSU التشخيصية باعتبارها المحرك؛ المحرك هو APK LSPosed الناتج من GitHub Actions.
- لا تفعّل أي Override بعد التثبيت الأول.
- احتفظ بإمكانية تعطيل الوحدة من LSPosed أو Safe Mode.

## Scopes
أضف هذه النطاقات فقط:
1. `system` — system_server.
2. `com.android.server.telecom` — اعتراض الرنين.
3. `com.fuxi.haptics` — واجهة الإعدادات وقراءة RemotePreferences.

أعد التشغيل بعد تغيير Scopes.

## مرحلة التحقق الأولى — دون استبدال أي اهتزاز
افتح سجل LSPosed وابحث عن:
- `system_hook_installed class=com.android.server.vibrator.VibratorManagerService method=vibrateInternal`
- أو، إذا لم يوجد هذا المسار، `method=vibrate` كخطة احتياط.

إذا ظهر `system_hook_class_missing` أو لم يظهر أي من السطرين، لا تفعّل قواعد اللمس؛ هذا يعني أن اسم الخدمة/المسار في HyperOS مختلف ويجب تحديده من سجل الجهاز أولاً.

## مرحلة التحقق الثانية — قاعدة واحدة منخفضة
بعد نجاح مرحلة التحقق الأولى فقط، افتح التطبيق وأضف:

```text
com.android.systemui|0,12,18,12|100
```

فعّل مفتاح Touch فقط، واحفظ، ثم جرّب لمسات بسيطة في واجهة النظام.

النجاح المتوقع في السجل: `touch_effect_override pkg=com.android.systemui usage=... input=Mono output=waveform`.

إذا ظهر طلب `vibrator_service_request` لكن لم يظهر `touch_effect_override`، أرسل السجل؛ قد تكون Usage مختلفة أو CombinedVibration غير Mono أو اسم الحزمة مختلفاً. إذا لم يظهر طلب أصلاً، نحتاج تتبع مسار آخر في HyperOS.

## الإيقاف والتراجع
أطفئ مفتاح Touch أو امسح قواعده واحفظ. إذا حدث سلوك غير طبيعي، عطّل الوحدة من LSPosed وأعد التشغيل. لا حاجة لتعديل system/vendor أو sysfs أو firmware.
