package com.fuxi.haptics.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Exact-package rules for replacing touch-usage effects in system_server. */
public final class TouchRuleCodec {
    private TouchRuleCodec() {}
    public static List<Rule> parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        String[] lines = raw.split("\\r?\\n");
        if (lines.length > 100) return Collections.emptyList();
        List<Rule> out = new ArrayList<>();
        for (String line : lines) {
            String v = line.trim();
            if (v.isEmpty() || v.startsWith("#")) continue;
            String[] p = v.split("\\|", 3);
            if (p.length != 3) return Collections.emptyList();
            String pkg = p[0].trim();
            if (pkg.isEmpty() || pkg.equals("*") || pkg.length() > 255) return Collections.emptyList();
            long[] timings = PatternCodec.parseTimings(p[1].trim());
            int amp;
            try { amp = Integer.parseInt(p[2].trim()); } catch (Exception e) { return Collections.emptyList(); }
            if (timings == null || amp < 1 || amp > 255) return Collections.emptyList();
            out.add(new Rule(pkg, timings, amp));
        }
        return Collections.unmodifiableList(out);
    }
    public static final class Rule {
        public final String packageName; public final long[] timings; public final int amplitude;
        Rule(String p, long[] t, int a) { packageName=p; timings=t.clone(); amplitude=a; }
    }
}
