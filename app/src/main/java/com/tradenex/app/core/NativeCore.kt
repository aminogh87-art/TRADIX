package com.tradenex.app.core

/** [net], [winRate] (0..1), [profitFactor], [maxDrawdown] (<= 0). */
data class TradeStats(
    val net: Double,
    val winRate: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
)

/** Extended deterministic metrics from realized P&L only. */
data class ExtendedTradeStats(
    val net: Double,
    val winRate: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
    val expectancy: Double,
    val averageWin: Double,
    val averageLoss: Double,
    val recoveryFactor: Double,
    val largestWinShare: Double,
    val largestLossShare: Double,
    val sampleSize: Int,
)

/**
 * Bridge to the Rust library `libtradenex_core.so` (see /rust).
 * When the library is not bundled the identical Kotlin fallback is used,
 * so the app always works; [isNative] tells which engine is active.
 */
object NativeCore {
    private val loaded: Boolean = try {
        System.loadLibrary("tradenex_core")
        true
    } catch (_: Throwable) {
        false
    }

    val isNative: Boolean get() = loaded

    private external fun nSessionMask(minuteUtc: Int): Int
    private external fun nOverlapMinutes(a: Int, b: Int): Int
    private external fun nMinutesToChange(minuteUtc: Int, session: Int): Int
    private external fun nStats(pnls: DoubleArray): DoubleArray
    private external fun nPerformanceMetrics(pnls: DoubleArray): DoubleArray

    fun sessionMask(minuteUtc: Int): Int =
        if (loaded) nSessionMask(minuteUtc) else Fallback.sessionMask(minuteUtc)

    fun overlapMinutes(a: Int, b: Int): Int =
        if (loaded) nOverlapMinutes(a, b) else Fallback.overlapMinutes(a, b)

    fun minutesToChange(minuteUtc: Int, session: Int): Int =
        if (loaded) nMinutesToChange(minuteUtc, session) else Fallback.minutesToChange(minuteUtc, session)

    fun stats(pnls: DoubleArray): TradeStats {
        val r = if (loaded) nStats(pnls) else Fallback.stats(pnls)
        return TradeStats(r[0], r[1], r[2], r[3])
    }

    fun performanceMetrics(pnls: DoubleArray): ExtendedTradeStats {
        val values = if (loaded) nPerformanceMetrics(pnls) else Fallback.performanceMetrics(pnls)
        return ExtendedTradeStats(
            net = values[0],
            winRate = values[1],
            profitFactor = values[2],
            maxDrawdown = values[3],
            expectancy = values[4],
            averageWin = values[5],
            averageLoss = values[6],
            recoveryFactor = values[7],
            largestWinShare = values[8],
            largestLossShare = values[9],
            sampleSize = pnls.count { it.isFinite() },
        )
    }
}

/** Same maths as rust/src/lib.rs. */
internal object Fallback {
    // start, end in UTC minutes: Sydney, Tokyo, London, New York
    private val sessions = arrayOf(intArrayOf(1320, 420), intArrayOf(0, 540), intArrayOf(480, 1020), intArrayOf(780, 1320))

    private fun isOpen(minute: Int, start: Int, end: Int): Boolean =
        if (start < end) minute in start until end else (minute >= start || minute < end)

    fun sessionMask(minuteUtc: Int): Int {
        val m = ((minuteUtc % 1440) + 1440) % 1440
        var mask = 0
        for (i in sessions.indices) if (isOpen(m, sessions[i][0], sessions[i][1])) mask = mask or (1 shl i)
        return mask
    }

    fun overlapMinutes(a: Int, b: Int): Int {
        if (a !in 0..3 || b !in 0..3) return 0
        var n = 0
        for (m in 0 until 1440) {
            if (isOpen(m, sessions[a][0], sessions[a][1]) && isOpen(m, sessions[b][0], sessions[b][1])) n++
        }
        return n
    }

    fun minutesToChange(minuteUtc: Int, session: Int): Int {
        if (session !in 0..3) return 0
        val now = ((minuteUtc % 1440) + 1440) % 1440
        val open = isOpen(now, sessions[session][0], sessions[session][1])
        for (step in 1..1440) {
            if (isOpen((now + step) % 1440, sessions[session][0], sessions[session][1]) != open) return step
        }
        return 0
    }

    fun performanceMetrics(pnls: DoubleArray): DoubleArray {
        val values = pnls.filter { it.isFinite() }
        if (values.isEmpty()) return DoubleArray(10)
        val net = values.sum()
        val wins = values.filter { it > 0.0 }
        val losses = values.filter { it < 0.0 }
        val grossWin = wins.sum()
        val grossLoss = losses.sumOf { -it }
        var equity = 0.0
        var peak = 0.0
        var maxDrawdown = 0.0
        values.forEach { value ->
            equity += value
            peak = maxOf(peak, equity)
            maxDrawdown = minOf(maxDrawdown, equity - peak)
        }
        return doubleArrayOf(
            net,
            wins.size.toDouble() / values.size,
            if (grossLoss > 0.0) grossWin / grossLoss else if (grossWin > 0.0) 99.0 else 0.0,
            maxDrawdown,
            net / values.size,
            if (wins.isEmpty()) 0.0 else grossWin / wins.size,
            if (losses.isEmpty()) 0.0 else grossLoss / losses.size,
            if (maxDrawdown < 0.0) net / -maxDrawdown else 0.0,
            if (grossWin > 0.0) (wins.maxOrNull() ?: 0.0) / grossWin else 0.0,
            if (grossLoss > 0.0) (losses.minOrNull()?.let { -it } ?: 0.0) / grossLoss else 0.0,
        )
    }

    fun stats(pnls: DoubleArray): DoubleArray {
        if (pnls.isEmpty()) return doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        var net = 0.0
        var gain = 0.0
        var loss = 0.0
        var wins = 0
        var equity = 0.0
        var peak = 0.0
        var dd = 0.0
        for (p in pnls) {
            net += p
            if (p > 0) {
                gain += p
                wins++
            } else {
                loss += -p
            }
            equity += p
            if (equity > peak) peak = equity
            if (equity - peak < dd) dd = equity - peak
        }
        val pf = if (loss > 0) gain / loss else if (gain > 0) 99.0 else 0.0
        return doubleArrayOf(net, wins.toDouble() / pnls.size, pf, dd)
    }
}
