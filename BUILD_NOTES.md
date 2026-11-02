# Build notes — v0.5.0-alpha

- JDK: 17 recommended.
- Android Gradle Plugin: 8.10.1.
- Gradle: 8.11.1.
- compileSdk: 36.
- Build: `gradle --no-daemon testDebugUnitTest assembleDebug` from `lsposed-module/`.
- The GitHub Actions workflow installs Android platform 36/build-tools 36 and runs the unit tests before assembling the debug APK.
- Xposed compile API: `de.robv.android.xposed:api:82`; runtime minimum metadata: 93 for LSPosed RemotePreferences.
- `xposedsharedprefs=true` is declared. The settings activity saves multiple notification rules and call settings in `haptic_profiles` with `MODE_WORLD_READABLE`; LSPosed API 93+ redirects this into its shared preferences mechanism. The module reads it with `XSharedPreferences`.
- Add `com.fuxi.haptics` to the LSPosed scope as well as the actual target processes so the settings preference redirection is active in the module UI.
- If the settings activity reports RemotePreferences unavailable, do not enable any overrides. Use the diagnostic logs and implement a tested Binder/ContentProvider transport for that LSPosed build instead.
- No APK was built in the current container because Android SDK and Gradle are not installed here. GitHub Actions is the reproducible build path.

- Touch override now hooks `VibratorManagerService.vibrateInternal(...)` first, handling the AOSP `CombinedVibration` funnel. If unavailable, it falls back to `vibrate(...)`.

- The official RichTap SDK Lite AAR is bundled for in-app preset preview only. It is not loaded into `system_server` and is not yet the event-hook renderer.
