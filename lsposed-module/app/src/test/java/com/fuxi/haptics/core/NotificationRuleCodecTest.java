package com.fuxi.haptics.core;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class NotificationRuleCodecTest {
    @Test public void parsesMultiplePackageAndChannelRules() {
        List<NotificationRuleCodec.Rule> rules = NotificationRuleCodec.parse(
                "org.telegram.messenger|private|0,40,60,40\ncom.google.android.gm||0,90");
        assertEquals(2, rules.size());
        assertTrue(rules.get(0).matches("org.telegram.messenger", "private"));
        assertFalse(rules.get(0).matches("org.telegram.messenger", "groups"));
        assertTrue(rules.get(1).matches("com.google.android.gm", "anything"));
    }
    @Test public void rejectsWildcardAndMalformedRules() {
        assertTrue(NotificationRuleCodec.parse("*||0,50").isEmpty());
        assertTrue(NotificationRuleCodec.parse("org.telegram.messenger|channel").isEmpty());
    }
}
