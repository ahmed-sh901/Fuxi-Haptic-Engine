# Sources and human implementation notes

This project uses original code. The following projects and documentation informed architecture decisions; no source code is copied from them in this release.

## Human-built projects and reports

1. Ever Haptics — https://github.com/hari161008/Ever-Haptics
   - A maintained open-source app with a broad haptics feature set: tap, scrolling, keyboard, notifications, incoming calls, alarms and audio beat sync.
   - Its README describes Accessibility Service and Notification Listener integration. We use its coverage/UI ideas, but do not copy its event-trigger approach for replacing stock vibration because that can add a second vibration instead of editing the existing system request.

2. Lost Xposed — https://github.com/uraniam9/lost-xposed
   - Developer reports real testing on one Nothing AIN065 / Android 16 / Nothing OS 4 device using Vector 2.2. It explicitly distinguishes working, partly tested and untested features.
   - Important lessons adopted: scope omissions fail silently; system_server hooks only load at boot; Direct Boot affects early settings; duplicate package-load callbacks can stack duplicate hooks; settings transport and per-process storage namespaces must be tested rather than assumed.
   - Their reported ContentProvider/Binder settings transport is a fallback design candidate if LSPosed RemotePreferences fail on Fuxi.

3. RingVibe — https://github.com/zazaulola/lsposed-ringconn-notification-vibrator
   - Human-built LSPosed example using system_server hooks for notification and call-state events, then an explicit-package message to a scoped app process. It demonstrates a practical split between central event detection and a separate target-process worker. It targets a smart ring, not the phone's vibrator.

4. BuzzKill — https://buzzkillapp.com/ and https://www.reddit.com/r/androidapps/comments/1x9pnxi/really_configuring_the_notificationssounds/
   - A maintained commercial notification automation app with custom alerts by app/contact/phrase. User reports describe it working for per-app and per-contact patterns. Its documented custom-alert path mutes the original alert and plays a replacement, so it proves the feature demand and feasibility but is not the same as preserving the native NMS vibration pipeline.

5. Notifier — https://github.com/dilfruh/Notifier
   - An older working notification-vibration app based on NotificationListenerService and custom notification channels. Its author documents limitations: app/channel handling trade-offs, duplicate notifications and Android background process killing. This is why Fuxi aims to intercept before the system starts the original vibration.

## Android / AOSP references

- NotificationChannel API: https://developer.android.com/reference/android/app/NotificationChannel
  - `setVibrationPattern` is only modifiable before the channel is submitted. Android API 35 adds `setVibrationEffect`, with the same pre-submission restriction.
- NotificationRecord AOSP: https://android.googlesource.com/platform/frameworks/base/+/033a4120a8ed78c2b97a5ff9ce2f52de7a1a662d/services/core/java/com/android/server/notification/NotificationRecord.java
  - `calculateVibration()` uses `channel.shouldVibrate()` and `getVibration()` returns the resolved waveform.
- NotificationManagerService AOSP: https://android.googlesource.com/platform/frameworks/base/+/9649d433d0140ae2ded430aa1d52132385aa07cd/services/core/java/com/android/server/notification/NotificationManagerService.java
  - The system consumes the record vibration and keeps the alert decision, delay, and playback path.
- Telecom Ringer AOSP: https://android.googlesource.com/platform/packages/services/Telecomm/+/master/src/com/android/server/telecom/Ringer.java
  - `startRinging`, `tryReserveVibration`, `vibrateIfNeeded`, `getVibrationEffectForRingtone`, and `stopRinging` provide a natural replacement point while preserving Telecom's lifecycle.
- LSPosed API changes and system scope notes: https://docs.lsposed.org/release/api_changes
- LSPosed New XSharedPreferences: https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences
- Android Haptics APIs: https://developer.android.com/develop/ui/views/haptics/haptics-apis
- Xiaomi MiHaptic: https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1526

## Evidence boundaries

- The referenced AOSP code is not proof that HyperOS 3.0.309.0 kept identical private methods.
- Ever Haptics' advertised feature list is not a Xiaomi 13 compatibility test.
- Lost Xposed's Android 16 result is on a different phone and framework (Vector 2.2), not this user's LSPosed build.
- No Fuxi v0.3.0 APK build or on-device runtime test has been completed yet.


## v0.5.0 implementation reference
- AOSP `VibratorManagerService` Android 15/16: Binder `vibrate` accepts `CombinedVibration`; `performHapticFeedback` resolves its effect and forwards it through the internal vibration funnel. This guided the change from direct `VibrationEffect` matching to Mono `CombinedVibration` replacement. AOSP reference: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/vibrator/VibratorManagerService.java
- This is an AOSP reference, not a verified Xiaomi source drop. Runtime method presence and argument layout must be confirmed from LSPosed logs on Fuxi.

- RichTap SDK Lite AAR bundled from the user-provided RichTapDynamics 1.0.9 sample archive for companion-app preset preview. Its use in system_server is intentionally not implemented.
