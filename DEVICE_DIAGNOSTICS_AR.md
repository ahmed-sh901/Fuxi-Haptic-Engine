# قراءة تقرير Xiaomi 13 من Ever Haptics

التقرير الذي أرسله المستخدم:

- Vibrator: present
- amplitude control: yes
- Vibration state callback: unavailable
- جميع Primitive Effects: no
- Delivery: alarm / rich (checking)

## الاستنتاجات المدعومة من الكود

1. المحرك موجود ويقبل التحكم بالسعة؛ لا يوجد في التقرير ما يدل على عطل مادي بالمحرك.
2. Primitive Effects غير مدعومة على هذا المسار، لذلك لا ينبغي أن نبني التجربة الأساسية على `PRIMITIVE_CLICK` أو `PRIMITIVE_TICK` أو غيرها.
3. عدم توفر State Callback لا يثبت أن الاهتزاز لا يعمل؛ لكنه يمنع Ever Haptics من معرفة أن الاهتزاز بدأ فعلياً.
4. كود Ever Haptics يرتب استراتيجيات الإرسال بحيث يبدأ بـ `ALARM / RICH`. وإذا نجح استدعاء `vibrate()` دون استثناء، فإنه ينتظر التحقق عبر State Callback. وعندما يكون callback غير متاح، فإن `scheduleVerification()` يرجع مباشرة ولا يستطيع `judge()` تسجيل فشل المسار أو الانتقال إلى TOUCH/MEDIA/UNSPECIFIED. لذلك قد يبقى التطبيق عالقاً على `alarm / rich (checking)` حتى لو كان HyperOS يتجاهل هذا المسار بصمت.
5. التقرير منسوخ قبل اكتمال السطر `Delivery: alarm / rich (checking`، لذلك لا نستنتج منه شيئاً عن نجاح RichTap أو فشل بقية المسارات.

## قرار Fuxi Haptics Engine

- لا نعتمد على Primitive Effects.
- لا نعتمد على State Callback لتأكيد الاهتزاز.
- نستبدل طلبات اللمس الموجودة داخل `system_server` بموجات `createWaveform`، مع إبقاء Usage الأصلي `TOUCH` حتى لا نغيّر سياسة النظام إلى ALARM.
- نحتفظ بالإشعارات والرنين داخل مساريهما الأصليين.
- لا ندمج RichTap SDK في `system_server` قبل التحقق من API وclassloader على Xiaomi؛ مجرد وجود SDK في companion APK لا يجعله متاحاً تلقائياً داخل system_server.


## دلالة تقرير Ever Haptics على Fuxi
- `amplitude control: yes`: يمكن بناء waveform بسعات صريحة عبر Android API.
- `Primitives: ... no`: لا نعتمد على Primitive Effects.
- `Vibration state callback: unavailable`: لا نعتمد على callback للتحقق من بدء الاهتزاز.
- هذه القيم لا تثبت أن كل `VibrationAttributes.USAGE_TOUCH` يصل إلى `VibratorManagerService` بالصيغة المتوقعة؛ يجب التحقق من hook logs.
