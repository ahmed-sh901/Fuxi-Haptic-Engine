# Changelog

## v0.5.0-alpha
- Corrected the touch hook to target `VibratorManagerService.vibrateInternal(...)`, the AOSP Android 15/16 funnel that receives `CombinedVibration`.
- Supports replacing mono CombinedVibration with a waveform while preserving the original usage attributes, caller attribution, token and framework lifecycle.
- Falls back to hooking `vibrate(...)` only when the internal method is absent.
- Leaves stereo/sequential combinations untouched.
- Added the official RichTap SDK Lite to the companion APK and a standalone preset preview bench (click/tick/toggle/success/failure) to verify Fuxi support. This is a diagnostic preview only; system hooks still render Android waveforms.
- Updated build workflow and Arabic documentation.
- Still requires real LSPosed runtime validation on Xiaomi HyperOS; no APK is claimed as built.

# Changelog

## v0.4.0-alpha
- Added per-package replacement for existing Android TOUCH-usage vibration requests in VibratorManagerService.
- Added strict exact-package touch rule parser and UI.
- Added waveform renderer using createWaveform; does not require primitive support or vibration-state callbacks.
- Notification and Telecom paths remain separate and retain native lifecycle/policy.
- No RichTap/MiHaptic SDK invocation yet; this release is a conservative system-server waveform stage.

## v0.3.0-alpha
- Per-app/channel notification waveform overrides.
- Standard incoming-call vibration override in Telecom.
- LSPosed shared preferences and duplicate-hook guards.
