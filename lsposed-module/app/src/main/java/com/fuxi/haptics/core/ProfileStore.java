package com.fuxi.haptics.core;

import android.os.SystemClock;
import android.os.VibrationEffect;
import java.util.List;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

/** Read-only cross-process profile cache. Writes are made by the companion app UI. */
public final class ProfileStore {
    private static final String TAG = "[FuxiHaptics] ";
    private static final String MODULE_PACKAGE = "com.fuxi.haptics";
    private static final String PREFS_NAME = "haptic_profiles";
    private static final long RELOAD_INTERVAL_MS = 1500;
    private static XSharedPreferences prefs;
    private static long lastReload;
    private static volatile Config cached = Config.disabled();
    private static boolean warned;

    private ProfileStore() {}

    public static Config get() {
        long now = SystemClock.uptimeMillis();
        if (now - lastReload < RELOAD_INTERVAL_MS) return cached;
        synchronized (ProfileStore.class) {
            now = SystemClock.uptimeMillis();
            if (now - lastReload < RELOAD_INTERVAL_MS) return cached;
            try {
                if (prefs == null) prefs = new XSharedPreferences(MODULE_PACKAGE, PREFS_NAME);
                if (!prefs.getFile().canRead()) {
                    throw new IllegalStateException("LSPosed shared preference file is not readable in this process");
                }
                prefs.reload();
                String rawRules = prefs.getString("notification_rules", "");
                List<NotificationRuleCodec.Rule> rules = NotificationRuleCodec.parse(rawRules);
                // Backward-compatible single-rule keys from the first prototype.
                if (rawRules.trim().isEmpty() && rules.isEmpty()) {
                    String oldPkg = prefs.getString("notification_package", "").trim();
                    String oldChannel = prefs.getString("notification_channel", "").trim();
                    long[] oldPattern = PatternCodec.parseTimings(
                            prefs.getString("notification_pattern", "0,45,70,45"));
                    if (!oldPkg.isEmpty() && oldPattern != null) {
                        rules = java.util.Collections.singletonList(
                                new NotificationRuleCodec.Rule(oldPkg, oldChannel, oldPattern));
                    }
                }
                long[] call = PatternCodec.parseTimings(
                        prefs.getString("call_pattern", "0,65,100,65,700"));
                int intensity = prefs.getInt("call_intensity", 170);
                if (intensity < 1 || intensity > 255) intensity = 170;
                String touchRulesRaw = prefs.getString("touch_rules", "");
                List<TouchRuleCodec.Rule> touchRules = TouchRuleCodec.parse(touchRulesRaw);
                cached = new Config(
                        prefs.getBoolean("notification_enabled", false), rules,
                        prefs.getBoolean("call_enabled", false), call, intensity,
                        prefs.getBoolean("touch_enabled", false), touchRules);
                warned = false;
            } catch (Throwable t) {
                cached = Config.disabled();
                if (!warned) {
                    XposedBridge.log(TAG + "profiles_unavailable; all overrides remain disabled: " + t);
                    warned = true;
                }
            }
            lastReload = now;
            return cached;
        }
    }

    public static long[] notificationPattern(String packageName, String channelId) {
        Config c = get();
        if (!c.notificationEnabled || c.notificationRules.isEmpty() || packageName == null) return null;
        for (NotificationRuleCodec.Rule rule : c.notificationRules) {
            if (rule.matches(packageName, channelId)) return rule.timings.clone();
        }
        return null;
    }

    /** Only touch-usage requests are eligible; notification/call/alarms remain on their native paths. */
    public static VibrationEffect touchEffect(String packageName, int usage) {
        if (usage != android.os.VibrationAttributes.USAGE_TOUCH || packageName == null) return null;
        Config c = get();
        if (!c.touchEnabled || c.touchRules.isEmpty()) return null;
        for (TouchRuleCodec.Rule rule : c.touchRules) {
            if (rule.packageName.equals(packageName))
                return PatternCodec.createWaveform(rule.timings, rule.amplitude);
        }
        return null;
    }

    public static VibrationEffect callEffect(boolean audioCoupled) {
        if (audioCoupled) return null; // preserve haptics embedded in the ringtone audio.
        Config c = get();
        if (!c.callEnabled || c.callPattern == null) return null;
        int[] amplitudes = PatternCodec.amplitudesFor(c.callPattern, c.callIntensity);
        if (amplitudes == null) return null;
        try {
            return VibrationEffect.createWaveform(c.callPattern.clone(), amplitudes, 0);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "call_effect_build_failed=" + t);
            return null;
        }
    }

    public static final class Config {
        public final boolean notificationEnabled;
        public final List<NotificationRuleCodec.Rule> notificationRules;
        public final boolean callEnabled;
        public final long[] callPattern;
        public final int callIntensity;
        public final boolean touchEnabled;
        public final List<TouchRuleCodec.Rule> touchRules;

        Config(boolean ne, List<NotificationRuleCodec.Rule> rules, boolean ce, long[] cv, int ci,
               boolean te, List<TouchRuleCodec.Rule> tr) {
            notificationEnabled = ne;
            notificationRules = rules == null ? java.util.Collections.emptyList() : rules;
            callEnabled = ce;
            callPattern = cv;
            callIntensity = ci;
            touchEnabled = te;
            touchRules = tr == null ? java.util.Collections.emptyList() : tr;
        }
        static Config disabled() { return new Config(false, java.util.Collections.emptyList(), false, null, 170, false, java.util.Collections.emptyList()); }
    }
}
