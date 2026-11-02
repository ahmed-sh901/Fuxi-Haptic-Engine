package com.fuxi.haptics.hook;

import android.media.AudioAttributes;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.CombinedVibration;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/** Legacy LSPosed entry point. All replacement paths are opt-in and fail closed. */
public final class HapticsEntry implements IXposedHookLoadPackage {
    private static final String TAG = "[FuxiHaptics] ";
    private static final AtomicInteger LOG_COUNT = new AtomicInteger(0);
    private static final int MAX_LOGS_PER_PROCESS = 350;
    private static final ThreadLocal<Integer> CALL_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final AtomicBoolean API_HOOKS_INSTALLED = new AtomicBoolean(false);

    @Override public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lp) {
        if (lp.packageName == null || "com.fuxi.haptics".equals(lp.packageName)) return;
        final boolean systemServer = "system_server".equals(lp.processName);
        final boolean telecom = "com.android.server.telecom".equals(lp.packageName)
                || (lp.processName != null && lp.processName.startsWith("com.android.server.telecom"));
        log("module_loaded package=" + lp.packageName + " process=" + lp.processName
                + " system_server=" + systemServer + " telecom=" + telecom);
        // system_server can receive repeated package-load callbacks during one boot.
        // Install process-wide hooks only once; duplicate hooks multiply every vibration.
        if (API_HOOKS_INSTALLED.compareAndSet(false, true)) hookVibratorApis();
        if (systemServer) SystemServerObserver.install(lp.classLoader);
        if (telecom) TelecomObserver.install(lp.classLoader);
    }

    private static void hookVibratorApis() {
        try { hook(Vibrator.class, "vibrate", new Object[]{VibrationEffect.class, VibrationAttributes.class}, "Vibrator.vibrate(effect,attributes)"); }
        catch (Throwable t) { log("hook_failed Vibrator modern: " + t); }
        try { hook(Vibrator.class, "vibrate", new Object[]{VibrationEffect.class, AudioAttributes.class}, "Vibrator.vibrate(effect,audioAttributes)"); }
        catch (Throwable t) { log("hook_failed Vibrator legacy attrs: " + t); }
        try { hook(Vibrator.class, "vibrate", new Object[]{long[].class, int.class, AudioAttributes.class}, "Vibrator.vibrate(waveform)"); }
        catch (Throwable t) { log("hook_failed Vibrator waveform: " + t); }
        try { hook(VibratorManager.class, "vibrate", new Object[]{CombinedVibration.class, VibrationAttributes.class}, "VibratorManager.vibrate(combined)"); }
        catch (Throwable t) { log("hook_failed VibratorManager: " + t); }
    }

    private static void hook(Class<?> owner, String method, Object[] signature, final String label) {
        Object[] full = new Object[signature.length + 1];
        System.arraycopy(signature, 0, full, 0, signature.length);
        full[signature.length] = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                int depth = CALL_DEPTH.get();
                CALL_DEPTH.set(depth + 1);
                if (depth != 0) return;
                StringBuilder s = new StringBuilder("api_call api=").append(label);
                for (int i = 0; i < p.args.length; i++) {
                    Object value = p.args[i];
                    if (value == null) continue;
                    String rendered = safeString(value, 180);
                    s.append(" arg").append(i).append('=').append(value.getClass().getSimpleName())
                            .append(':').append(rendered);
                }
                log(s.toString());
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                CALL_DEPTH.set(Math.max(0, CALL_DEPTH.get() - 1));
            }
        };
        XposedHelpers.findAndHookMethod(owner, method, full);
    }

    static void log(String message) {
        if (LOG_COUNT.incrementAndGet() <= MAX_LOGS_PER_PROCESS) XposedBridge.log(TAG + message);
    }

    static String safeString(Object o, int max) {
        String s;
        try { s = String.valueOf(o); } catch (Throwable t) { s = "<toString-error>"; }
        if (s == null) return "null";
        s = s.replace('\n',' ').replace('\r',' ');
        return s.length() > max ? s.substring(0, max) : s;
    }
}
