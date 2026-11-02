package com.fuxi.haptics.hook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.os.VibrationEffect;
import com.fuxi.haptics.core.ProfileStore;

/** Observe call lifecycle and optionally replace the ordinary ringtone vibration effect. */
final class TelecomObserver {
    private static volatile boolean installed;
    private static final String RINGER = "com.android.server.telecom.Ringer";

    static void install(ClassLoader loader) {
        if (installed) return;
        installed = true;
        try {
            Class<?> c = XposedHelpers.findClassIfExists(RINGER, loader);
            if (c == null) {
                HapticsEntry.log("telecom_hook_class_missing " + RINGER);
                return;
            }
            hook(c, "vibrateIfNeeded");
            hook(c, "getVibrationEffectForRingtone");
            hook(c, "tryReserveVibration");
            hook(c, "startRinging");
            hook(c, "stopRinging");
            hook(c, "startCallWaiting");
            hook(c, "stopCallWaiting");
            HapticsEntry.log("telecom_ringer_observer_installed class=" + RINGER);
            hookAsyncRingtonePlayer(loader);
        } catch (Throwable t) {
            HapticsEntry.log("telecom_observer_install_failed=" + t);
        }
    }

    private static void hookAsyncRingtonePlayer(ClassLoader loader) {
        final String playerName = "com.android.server.telecom.AsyncRingtonePlayer";
        try {
            Class<?> player = XposedHelpers.findClassIfExists(playerName, loader);
            if (player == null) {
                HapticsEntry.log("telecom_hook_class_missing " + playerName);
                return;
            }
            hook(player, "play");
            hook(player, "stop");
            HapticsEntry.log("telecom_async_ringtone_observer_installed class=" + playerName);
        } catch (Throwable t) {
            HapticsEntry.log("telecom_async_ringtone_observer_failed=" + t);
        }
    }

    /**
     * Replace only the ordinary vibrator effect argument, not the ringtone's embedded
     * audio-coupled haptic track. Ringer still owns reservation, timing and cancellation.
     */
    private static void hook(Class<?> c, final String method) {
        if ("vibrateIfNeeded".equals(method)) {
            try {
                int count = XposedBridge.hookAllMethods(c, method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        try {
                            Boolean audioCoupled = null;
                            int effectIndex = -1;
                            for (int i = 0; i < p.args.length; i++) {
                                Object a = p.args[i];
                                if (a instanceof Boolean && audioCoupled == null) audioCoupled = (Boolean) a;
                                if (a instanceof VibrationEffect) effectIndex = i;
                            }
                            if (audioCoupled == null || effectIndex < 0) {
                                HapticsEntry.log("call_override_skip signature_unrecognized");
                                return;
                            }
                            VibrationEffect custom = ProfileStore.callEffect(audioCoupled);
                            if (custom != null) {
                                p.args[effectIndex] = custom;
                                HapticsEntry.log("call_pattern_override applied; audio_coupled=" + audioCoupled);
                            }
                        } catch (Throwable t) {
                            HapticsEntry.log("call_pattern_override_error=" + t);
                        }
                    }
                }).size();
                HapticsEntry.log("telecom_hook method=" + method + " overloads=" + count);
            } catch (Throwable t) {
                HapticsEntry.log("telecom_hook_failed method=" + method + " error=" + t);
            }
            return;
        }

        try {
            int count = XposedBridge.hookAllMethods(c, method, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    StringBuilder s = new StringBuilder("telecom_ringer method=").append(method);
                    for (int i=0;i<p.args.length;i++) {
                        Object a=p.args[i];
                        if (a==null) continue;
                        s.append(" a").append(i).append('=');
                        if (a instanceof Boolean || a instanceof Integer || a instanceof Long) {
                            s.append(a);
                        } else {
                            // Do not call Call.toString(), getHandle(), or log contact identifiers.
                            s.append(a.getClass().getSimpleName());
                            if (a.getClass().getName().contains("VibrationEffect")) {
                                s.append(':').append(HapticsEntry.safeString(a, 180));
                            }
                        }
                    }
                    HapticsEntry.log(s.toString());
                }
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if ("getVibrationEffectForRingtone".equals(method)) {
                        Object result = p.getResult();
                        HapticsEntry.log("telecom_selected_haptic method=" + method
                                + " result=" + (result == null ? "null"
                                : result.getClass().getSimpleName() + ":"
                                + HapticsEntry.safeString(result, 180)));
                    }
                }
            }).size();
            if (count > 0) HapticsEntry.log("telecom_hook method=" + method + " overloads=" + count);
        } catch (Throwable t) {
            HapticsEntry.log("telecom_hook_failed method=" + method + " error=" + t);
        }
    }
}
