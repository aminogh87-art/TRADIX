package com.tradenex.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Same cases as the Rust `candle_tone` tests and the Dart `candleToneFor` tests. */
class CandleToneTest {
    @Test
    fun toneFollowsTheSignOfPnl() {
        assertEquals(NativeCore.TONE_GREEN, Fallback.candleTone(98.9, 0.5))
        assertEquals(NativeCore.TONE_RED, Fallback.candleTone(-64.2, 0.5))
        assertEquals(NativeCore.TONE_GOLD, Fallback.candleTone(0.0, 0.5))
    }

    @Test
    fun flatBandIsSymmetricAndInclusive() {
        assertEquals(NativeCore.TONE_GOLD, Fallback.candleTone(0.5, 0.5))
        assertEquals(NativeCore.TONE_GOLD, Fallback.candleTone(-0.5, 0.5))
        assertEquals(NativeCore.TONE_GREEN, Fallback.candleTone(0.51, 0.5))
        assertEquals(NativeCore.TONE_RED, Fallback.candleTone(-0.51, -0.5))
    }

    @Test
    fun nonFiniteInputIsBreakEven() {
        assertEquals(NativeCore.TONE_GOLD, Fallback.candleTone(Double.NaN, 0.5))
        assertEquals(NativeCore.TONE_GOLD, Fallback.candleTone(Double.POSITIVE_INFINITY, 0.5))
        assertEquals(NativeCore.TONE_GREEN, Fallback.candleTone(5.0, Double.NaN))
    }

    @Test
    fun publicEntryPointFallsBackWhenTheNativeLibraryIsMissing() {
        // On the JVM unit-test classpath libtradenex_core.so is not loadable, so this exercises the fallback.
        assertEquals(NativeCore.TONE_RED, NativeCore.candleTone(-10.0))
    }
}
