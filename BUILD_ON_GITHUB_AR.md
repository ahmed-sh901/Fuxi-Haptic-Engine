# بناء Fuxi Haptics Engine v0.5.0 على GitHub

1. ارفع محتويات مجلد المشروع إلى مستودع GitHub جديد.
2. افتح تبويب Actions واسمح بتشغيل GitHub Actions إذا طُلب ذلك.
3. شغّل workflow باسم Build Fuxi Haptics Engine v0.5.0 APK، أو ادفع commit جديداً.
4. يستخدم البناء JDK 17 وAGP 8.10.1 وGradle 8.11.1 وAndroid SDK 36.
5. ينفذ `testDebugUnitTest` ثم `assembleDebug`.
6. نزّل Artifact باسم `Fuxi-Haptics-Engine-v0.5.0-debug`.

ملف APK الناتج داخل artifact هو `app-debug.apk`. هذه نسخة alpha: نجاح البناء لا يثبت توافق Hooks مع HyperOS؛ يجب مراجعة سجل LSPosed قبل تفعيل أي Override.
