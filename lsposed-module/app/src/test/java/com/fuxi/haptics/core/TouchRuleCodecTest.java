package com.fuxi.haptics.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class TouchRuleCodecTest {
 @Test public void parsesExactPackageRules() {
  java.util.List<TouchRuleCodec.Rule> r=TouchRuleCodec.parse("com.android.systemui|0,12,18,12|145\ncom.google.android.apps.nexuslauncher|0,20|100");
  assertEquals(2,r.size()); assertEquals("com.android.systemui",r.get(0).packageName); assertEquals(145,r.get(0).amplitude);
 }
 @Test public void rejectsWildcardAndBadAmplitude() {
  assertTrue(TouchRuleCodec.parse("*|0,20|100").isEmpty());
  assertTrue(TouchRuleCodec.parse("com.test|0,20|300").isEmpty());
 }
}
