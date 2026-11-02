package com.fuxi.haptics.hook;

import android.os.VibrationEffect;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import com.fuxi.haptics.core.ProfileStore;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Hooks framework decision points in system_server. Notification override is opt-in.
 * AOSP method names are discovery anchors; HyperOS may rename or wrap them.
 */
final class SystemServerObserver {
    private static volatile boolean installed;
    private static final String NMS = "com.android.server.notification.NotificationManagerService";
    private static final String VMS = "com.android.server.vibrator.VibratorManagerService";
    private static final String NR = "com.android.server.notification.NotificationRecord";

    static void install(ClassLoader loader) {
        if (installed) return;
        installed = true;
        hookAll(loader, NR, "getVibration", new NotificationRecordHook());
        hookAll(loader, NMS, "playVibration", new NotificationHook());
        // The important Android 16 path accepts CombinedVibration, not VibrationEffect.
        // Hook the internal funnel so direct vibrate() and performHapticFeedback() requests
        // are both seen after the framework has resolved their final attributes.
        int internalCount = hookAll(loader, VMS, "vibrateInternal", new VibratorServiceHook());
        if (internalCount == 0) {
            // Vendor fallback: some framework branches do not expose vibrateInternal.
            hookAll(loader, VMS, "vibrate", new VibratorServiceHook());
        }
        hookAll(loader, VMS, "performHapticFeedback", new VibratorServiceHook());
        hookAll(loader, VMS, "performHapticFeedbackForInputDevice", new VibratorServiceHook());
    }

    private static int hookAll(ClassLoader loader, String className, String method,
                               XC_MethodHook callback) {
        try {
            Class<?> c = XposedHelpers.findClassIfExists(className, loader);
            if (c == null) { HapticsEntry.log("system_hook_class_missing " + className); return 0; }
            int count = XposedBridge.hookAllMethods(c, method, callback).size();
            HapticsEntry.log("system_hook_installed class=" + className + " method=" + method + " overloads=" + count);
            return count;
        } catch (Throwable t) {
            HapticsEntry.log("system_hook_failed class=" + className + " method=" + method + " error=" + t);
            return 0;
        }
    }


    /**
     * NotificationRecord is the point where channel policy has already been resolved.
     * Returning a per-app waveform here lets NMS keep its own DND, ringer-mode, in-call,
     * alert-priority, cancellation and VibratorManager pipeline. It can also supply a
     * pattern when the channel itself has vibration disabled.
     */
    private static final class NotificationRecordHook extends XC_MethodHook {
        @Override protected void afterHookedMethod(MethodHookParam p) {
            try {
                Object record = p.thisObject;
                Object sbn = call(record, "getSbn");
                Object channel = call(record, "getChannel");
                String pkg = string(call(sbn, "getPackageName"));
                String channelId = string(call(channel, "getId"));
                long[] custom = ProfileStore.notificationPattern(pkg, channelId);
                if (custom != null) {
                    p.setResult(custom);
                    HapticsEntry.log("notification_pattern_override pkg=" + pkg
                            + " channel=" + channelId + " timings=" + Arrays.toString(custom));
                }
            } catch (Throwable t) {
                HapticsEntry.log("notification_pattern_override_error=" + t);
            }
        }
    }

    private static final class NotificationHook extends XC_MethodHook {
        @Override protected void beforeHookedMethod(MethodHookParam p) {
            try {
                Object record = firstInstance(p.args, "com.android.server.notification.NotificationRecord");
                if (record == null) {
                    HapticsEntry.log("notification_vibration method_called args=" + summarize(p.args));
                    return;
                }
                Object sbn = call(record, "getSbn");
                Object channel = call(record, "getChannel");
                String pkg = string(call(sbn, "getPackageName"));
                String opPkg = string(call(sbn, "getOpPkg"));
                String channelId = string(call(channel, "getId"));
                Object pattern = findLongArray(p.args);
                Object delay = findBoolean(p.args);
                HapticsEntry.log("notification_vibration pkg=" + pkg + " opPkg=" + opPkg
                        + " channel=" + channelId
                        + " pattern=" + HapticsEntry.safeString(pattern, 220)
                        + " delayForSound=" + delay);
            } catch (Throwable t) {
                HapticsEntry.log("notification_vibration_observer_error=" + t);
            }
        }
    }

    private static final class VibratorServiceHook extends XC_MethodHook {
        @Override protected void beforeHookedMethod(MethodHookParam p) {
            try {
                if ("vibrateInternal".equals(p.method.getName()) || "vibrate".equals(p.method.getName()))
                    applyTouchOverride(p);
                StringBuilder s = new StringBuilder("vibrator_service_request method=")
                        .append(p.method.getName());
                for (int i = 0; i < p.args.length; i++) {
                    Object a = p.args[i];
                    if (a == null) continue;
                    String type = a.getClass().getSimpleName();
                    // Avoid dumping Binder tokens and potentially huge object graphs.
                    if (type.contains("Binder") || type.contains("Token")) continue;
                    s.append(" a").append(i).append('=');
                    if (a instanceof Integer || a instanceof Long || a instanceof Boolean || a instanceof String) {
                        s.append(HapticsEntry.safeString(a, 140));
                    } else {
                        s.append(type).append(':').append(HapticsEntry.safeString(a, 180));
                    }
                }
                HapticsEntry.log(s.toString());
            } catch (Throwable t) { HapticsEntry.log("vibrator_service_observer_error=" + t); }
        }
    }

    private static void applyTouchOverride(XC_MethodHook.MethodHookParam p) {
        try {
            int effectIndex = -1;
            Object effectArg = null;
            String packageName = null;
            Object attributes = null;
            for (int i = 0; i < p.args.length; i++) {
                Object a = p.args[i];
                if (a instanceof VibrationEffect || a instanceof android.os.CombinedVibration) {
                    effectIndex = i;
                    effectArg = a;
                } else if (a != null && a.getClass().getName().equals("android.os.VibrationAttributes")) {
                    attributes = a;
                }
            }
            // In AOSP 15/16 the server funnel is (uid, deviceId, opPkg, CombinedVibration,
            // attrs, reason, token). Only trust a package-like string before the effect;
            // the later reason string is not package attribution.
            if (effectIndex >= 0) {
                for (int i = 0; i < effectIndex; i++) {
                    Object a = p.args[i];
                    if (a instanceof String) {
                        String candidate = ((String) a).trim();
                        if (candidate.contains(".") && candidate.length() <= 255) {
                            packageName = candidate;
                            break;
                        }
                    }
                }
            }
            if (effectIndex < 0 || packageName == null || attributes == null) return;
            Object usageValue = call(attributes, "getUsage");
            if (!(usageValue instanceof Integer)) return;
            int usage = (Integer) usageValue;
            if (usage != android.os.VibrationAttributes.USAGE_TOUCH) return;

            VibrationEffect replacement = ProfileStore.touchEffect(packageName, usage);
            if (replacement == null) return;

            if (effectArg instanceof VibrationEffect) {
                p.args[effectIndex] = replacement;
            } else if (effectArg instanceof android.os.CombinedVibration) {
                // Fuxi is a single-LRA device. Rebuild a mono CombinedVibration so the
                // service's normal policy, cancellation, scaling and HAL path remain intact.
                Object monoEffect = call(effectArg, "getEffect");
                if (monoEffect == null) {
                    HapticsEntry.log("touch_override_skip unsupported_combined="
                            + effectArg.getClass().getName());
                    return;
                }
                p.args[effectIndex] = android.os.CombinedVibration.createParallel(replacement);
            }
            HapticsEntry.log("touch_effect_override pkg=" + packageName + " usage=" + usage
                    + " input=" + effectArg.getClass().getSimpleName() + " output=waveform");
        } catch (Throwable t) { HapticsEntry.log("touch_effect_override_error=" + t); }
    }

    private static Object firstInstance(Object[] args, String name) {
        for (Object a : args) if (a != null && a.getClass().getName().equals(name)) return a;
        return null;
    }
    private static Object findLongArray(Object[] args) {
        for (Object a : args) if (a instanceof long[]) return Arrays.toString((long[]) a);
        return "not-found";
    }
    private static Object findBoolean(Object[] args) {
        for (Object a : args) if (a instanceof Boolean) return a;
        return "not-found";
    }
    private static Object call(Object target, String method) {
        if (target == null) return null;
        try { Method m = target.getClass().getMethod(method); m.setAccessible(true); return m.invoke(target); }
        catch (Throwable ignored) { return null; }
    }
    private static String string(Object o) { return o == null ? "unknown" : HapticsEntry.safeString(o, 120); }
    private static String summarize(Object[] args) {
        StringBuilder s = new StringBuilder("[");
        for (int i=0;i<args.length;i++) { if(i>0)s.append(','); Object a=args[i]; s.append(a==null?"null":a.getClass().getSimpleName()); }
        return s.append(']').toString();
    }
}
