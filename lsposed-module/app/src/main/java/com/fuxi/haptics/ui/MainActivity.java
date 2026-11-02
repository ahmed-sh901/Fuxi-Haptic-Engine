package com.fuxi.haptics.ui;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import com.apprichtap.haptic.RichTapUtils;

/** Minimal first settings surface; deliberately avoids AndroidX/UI dependencies. */
public final class MainActivity extends Activity {
    private static final String PREFS = "haptic_profiles";
    private Switch notifications, calls, touch;
    private EditText notificationRules, callPattern, callIntensity, touchRules;
    private TextView status;
    private TextView richTapStatus;
    private boolean richTapInitialized = false;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences p = loadPrefs();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Fuxi Haptics Engine");
        title.setTextSize(25);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("v0.5.0 · Xiaomi 13 / Fuxi · Waveform fallback; no primitive or callback dependency.");
        subtitle.setTextSize(13);
        root.addView(subtitle);

        addLabel(root, "RichTap test bench — verifies the Xiaomi high-fidelity path independently.");
        richTapStatus = new TextView(this);
        richTapStatus.setTextSize(13);
        root.addView(richTapStatus);
        try {
            RichTapUtils richTap = RichTapUtils.getInstance().init(this);
            richTapInitialized = true;
            boolean supported = richTap.isSupportedRichTap();
            richTapStatus.setText(supported
                    ? "RichTap: supported — test the native presets below."
                    : "RichTap: not reported as supported; native preset buttons are disabled.");
            addRichTapButton(root, "Preview · Click", 10001, supported);
            addRichTapButton(root, "Preview · Tick", 10004, supported);
            addRichTapButton(root, "Preview · Toggle", 10009, supported);
            addRichTapButton(root, "Preview · Success", 10007, supported);
            addRichTapButton(root, "Preview · Failure", 10006, supported);
            Button stopRichTap = new Button(this);
            stopRichTap.setText("Stop RichTap preview");
            stopRichTap.setEnabled(supported);
            stopRichTap.setOnClickListener(v -> {
                try { RichTapUtils.getInstance().stop(); }
                catch (Throwable t) { Toast.makeText(this, "RichTap stop failed: " + t.getClass().getSimpleName(), Toast.LENGTH_SHORT).show(); }
            });
            root.addView(stopRichTap);
        } catch (Throwable t) {
            richTapStatus.setText("RichTap initialization failed: " + t.getClass().getSimpleName());
        }

        notifications = new Switch(this);
        notifications.setText("Override notification vibration");
        notifications.setChecked(p.getBoolean("notification_enabled", false));
        root.addView(notifications);
        addLabel(root, "Notification rules — one per line: package|channel-id|timings");
        addLabel(root, "Leave channel-id empty to match every channel of that app.");
        notificationRules = field(root, p.getString("notification_rules", ""), "org.telegram.messenger||0,45,70,45");
        notificationRules.setSingleLine(false);
        notificationRules.setMinLines(4);

        View divider = new View(this);
        divider.setBackgroundColor(0xff888888);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, dp(1));
        dp.topMargin = dp(18); dp.bottomMargin = dp(12); root.addView(divider, dp);

        calls = new Switch(this);
        calls.setText("Override standard incoming-call vibration");
        calls.setChecked(p.getBoolean("call_enabled", false));
        root.addView(calls);
        addLabel(root, "Call timings in ms (OFF, ON, OFF, ON...; repeats while ringing)");
        callPattern = field(root, p.getString("call_pattern", "0,65,100,65,700"), "0,65,100,65,700");
        addLabel(root, "Call amplitude (1–255)");
        callIntensity = field(root, String.valueOf(p.getInt("call_intensity", 170)), "170");

        View divider2 = new View(this);
        divider2.setBackgroundColor(0xff888888);
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(-1, dp(1));
        dp2.topMargin = dp(18); dp2.bottomMargin = dp(12); root.addView(divider2, dp2);

        touch = new Switch(this);
        touch.setText("Replace existing touch haptics per app");
        touch.setChecked(p.getBoolean("touch_enabled", false));
        root.addView(touch);
        addLabel(root, "Touch rules — package|timings|amplitude (one per line). Only Android TOUCH-usage requests are changed.");
        addLabel(root, "Example: com.android.systemui|0,12,18,12|145");
        touchRules = field(root, p.getString("touch_rules", ""), "com.android.systemui|0,12,18,12|145");
        touchRules.setSingleLine(false); touchRules.setMinLines(3);

        Button save = new Button(this);
        save.setText("Save profiles");
        root.addView(save);
        save.setOnClickListener(v -> save());
        status = new TextView(this);
        status.setTextSize(12);
        status.setPadding(0, dp(12), 0, 0);
        root.addView(status);
        status.setText("Required LSPosed scopes: this app (com.fuxi.haptics), system, and com.android.server.telecom. Reboot after changing scopes.");
        setContentView(scroll);
    }

    private SharedPreferences loadPrefs() {
        try {
            return getSharedPreferences(PREFS, Context.MODE_WORLD_READABLE);
        } catch (SecurityException e) {
            Toast.makeText(this, "LSPosed RemotePreferences unavailable. Add this app to the module scope, then reopen.", Toast.LENGTH_LONG).show();
            return getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
    }

    private void save() {
        String rules = notificationRules.getText().toString().trim();
        String cp = callPattern.getText().toString().trim();
        int intensity;
        try { intensity = Integer.parseInt(callIntensity.getText().toString().trim()); }
        catch (Exception e) { intensity = -1; }
        if (notifications.isChecked() && !validRules(rules)) {
            Toast.makeText(this, "Enter valid rules: package|channel-id|timings (one rule per line).", Toast.LENGTH_LONG).show(); return;
        }
        if (calls.isChecked() && (!validPattern(cp) || intensity < 1 || intensity > 255)) {
            Toast.makeText(this, "Invalid call pattern or amplitude (1–255).", Toast.LENGTH_LONG).show(); return;
        }
        String tr = touchRules.getText().toString().trim();
        if (touch.isChecked() && !validTouchRules(tr)) {
            Toast.makeText(this, "Invalid touch rules: package|timings|amplitude.", Toast.LENGTH_LONG).show(); return;
        }
        try {
            SharedPreferences p = getSharedPreferences(PREFS, Context.MODE_WORLD_READABLE);
            boolean ok = p.edit()
                    .putBoolean("notification_enabled", notifications.isChecked())
                    .putString("notification_rules", rules)
                    .putBoolean("call_enabled", calls.isChecked())
                    .putString("call_pattern", cp)
                    .putInt("call_intensity", intensity)
                    .putBoolean("touch_enabled", touch.isChecked())
                    .putString("touch_rules", tr)
                    .commit();
            status.setText(ok ? "Saved. Existing hooks read the new profile within ~1.5 seconds." : "Save failed.");
            Toast.makeText(this, ok ? "Profiles saved" : "Could not save profiles", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            status.setText("Remote preferences are not active. Add com.fuxi.haptics to LSPosed scope and confirm LSPosed API 93+.");
        }
    }

    private boolean validRules(String raw) {
        if (raw.isEmpty()) return false;
        String[] lines = raw.split("\\r?\\n");
        int count = 0;
        for (String line : lines) {
            String v = line.trim();
            if (v.isEmpty() || v.startsWith("#")) continue;
            String[] parts = v.split("\\|", 3);
            if (parts.length != 3 || parts[0].trim().isEmpty() || parts[0].trim().equals("*")
                    || !validPattern(parts[2].trim())) return false;
            count++;
        }
        return count > 0;
    }

    private boolean validTouchRules(String raw) {
        if (raw.isEmpty()) return false;
        String[] lines = raw.split("\\r?\\n"); int count = 0;
        for (String line : lines) {
            String v = line.trim(); if (v.isEmpty() || v.startsWith("#")) continue;
            String[] a = v.split("\\|", 3); if (a.length != 3 || a[0].trim().isEmpty() || a[0].trim().equals("*") || !validPattern(a[1].trim())) return false;
            try { int amp = Integer.parseInt(a[2].trim()); if (amp < 1 || amp > 255) return false; } catch (Exception e) { return false; }
            count++;
        }
        return count > 0;
    }
    private boolean validPattern(String raw) {
        String[] a = raw.split(",");
        if (a.length < 2 || a.length > 17) return false;
        long sum = 0; boolean on = false;
        try {
            for (int i=0;i<a.length;i++) {
                long n = Long.parseLong(a[i].trim());
                if (n < 0 || n > 10000) return false;
                sum += n;
                if ((i & 1) == 1 && n > 0) on = true;
            }
        } catch (Exception e) { return false; }
        return on && sum <= 30000;
    }
    private void addRichTapButton(LinearLayout root, String label, int effectId, boolean enabled) {
        Button b = new Button(this);
        b.setText(label);
        b.setEnabled(enabled);
        b.setOnClickListener(v -> {
            try {
                RichTapUtils.getInstance().playExtPrebaked(effectId, 180);
            } catch (Throwable t) {
                Toast.makeText(this, "RichTap preview failed: " + t.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(b);
    }

    @Override protected void onDestroy() {
        if (richTapInitialized) {
            try { RichTapUtils.getInstance().quit(); } catch (Throwable ignored) { }
            richTapInitialized = false;
        }
        super.onDestroy();
    }

    private EditText field(LinearLayout root, String value, String hint) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setText(value); e.setHint(hint); root.addView(e); return e;
    }
    private void addLabel(LinearLayout root, String value) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(13); t.setPadding(0, dp(10), 0, 0); root.addView(t);
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
