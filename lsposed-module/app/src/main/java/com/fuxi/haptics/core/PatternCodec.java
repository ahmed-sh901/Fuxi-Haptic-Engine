package com.fuxi.haptics.core;

import android.os.VibrationEffect;

/** Pure-Java waveform validation shared by UI and hooks. Timings alternate OFF/ON, starting OFF. */
public final class PatternCodec {
    private PatternCodec() {}

    public static long[] parseTimings(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;
        String[] parts = value.split(",");
        if (parts.length < 2 || parts.length > 17) return null;
        long[] result = new long[parts.length];
        long total = 0;
        boolean hasOn = false;
        try {
            for (int i = 0; i < parts.length; i++) {
                long n = Long.parseLong(parts[i].trim());
                if (n < 0 || n > 10_000) return null;
                result[i] = n;
                total += n;
                if ((i & 1) == 1 && n > 0) hasOn = true;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        if (!hasOn || total > 30_000) return null;
        return result;
    }

    public static int[] amplitudesFor(long[] timings, int intensity) {
        if (timings == null || intensity < 1 || intensity > 255) return null;
        int[] amplitudes = new int[timings.length];
        for (int i = 0; i < timings.length; i++) {
            amplitudes[i] = ((i & 1) == 1 && timings[i] > 0) ? intensity : 0;
        }
        return amplitudes;
    }


    /** Builds a one-shot waveform without relying on unsupported device primitives or callbacks. */
    public static VibrationEffect createWaveform(long[] timings, int intensity) {
        int[] amplitudes = amplitudesFor(timings, intensity);
        if (amplitudes == null) return null;
        try { return VibrationEffect.createWaveform(timings.clone(), amplitudes, -1); }
        catch (Throwable ignored) { return null; }
    }

    public static String normalize(long[] timings) {
        if (timings == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < timings.length; i++) {
            if (i > 0) out.append(',');
            out.append(timings[i]);
        }
        return out.toString();
    }
}
