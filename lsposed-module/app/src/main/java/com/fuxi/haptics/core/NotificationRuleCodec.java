package com.fuxi.haptics.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Text rule format: package.name|channel-id-or-empty|off,on,off,on,... */
public final class NotificationRuleCodec {
    private NotificationRuleCodec() {}

    public static List<Rule> parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        List<Rule> rules = new ArrayList<>();
        String[] lines = raw.split("\\r?\\n");
        for (String line : lines) {
            String value = line.trim();
            if (value.isEmpty() || value.startsWith("#")) continue;
            String[] parts = value.split("\\|", 3);
            if (parts.length != 3) return Collections.emptyList();
            String pkg = parts[0].trim();
            String channel = parts[1].trim();
            if (rules.size() >= 100 || pkg.length() > 255 || channel.length() > 255) {
                return Collections.emptyList();
            }
            long[] pattern = PatternCodec.parseTimings(parts[2].trim());
            if (pkg.isEmpty() || pkg.equals("*") || pattern == null) return Collections.emptyList();
            rules.add(new Rule(pkg, channel, pattern));
        }
        return Collections.unmodifiableList(rules);
    }

    public static final class Rule {
        public final String packageName;
        public final String channelId;
        public final long[] timings;
        Rule(String p, String c, long[] t) { packageName = p; channelId = c; timings = t; }
        public boolean matches(String pkg, String channel) {
            return packageName.equals(pkg) && (channelId.isEmpty() || channelId.equals(channel));
        }
    }
}
