package com.fuxi.haptics.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class PatternCodecTest {
    @Test public void acceptsAlternatingWaveform() {
        assertArrayEquals(new long[]{0,45,70,45}, PatternCodec.parseTimings("0,45,70,45"));
    }
    @Test public void rejectsNegativeAndOversizedValues() {
        assertNull(PatternCodec.parseTimings("0,-1,20"));
        assertNull(PatternCodec.parseTimings("0,10001"));
    }
    @Test public void rejectsNoOnSegment() {
        assertNull(PatternCodec.parseTimings("0,0,50,0"));
    }
    @Test public void rejectsTooManySegments() {
        assertNull(PatternCodec.parseTimings("0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1"));
    }
    @Test public void createsOffAndOnAmplitudes() {
        long[] timings = new long[]{0,50,100,40};
        assertArrayEquals(new int[]{0,170,0,170}, PatternCodec.amplitudesFor(timings, 170));
    }
    @Test public void rejectsOutOfRangeAmplitude() {
        assertNull(PatternCodec.amplitudesFor(new long[]{0,20}, 256));
    }
}
