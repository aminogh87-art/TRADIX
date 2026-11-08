package com.tradenex.app.data

import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import kotlin.math.max
import kotlin.math.min

data class Candle(val o: Double, val h: Double, val l: Double, val c: Double)

/** Equity-curve candles are built per bucket of [bucket] days. Day 0 (1970-01-01) was a Thursday. */
enum class Period(val label: String, val count: Int) {
    DAY("روزانه", 24),
    WEEK("هفتگی", 16),
    MONTH("ماهانه", 12),
    YEAR("سالانه", 3);

    fun bucket(epochDay: Long): Long {
        val date = LocalDate.ofEpochDay(epochDay)
        return when (this) {
            DAY -> epochDay
            WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
            MONTH -> date.year.toLong() * 12L + date.monthValue - 1L
            YEAR -> date.year.toLong()
        }
    }
}

object Analytics {
    /**
     * One equity candle per recorded trade, ordered by execution time.
     * With journal-only data the high/low are the two observable equity points
     * (before and after the trade); intratrade excursions are intentionally not invented.
     */
    fun tradeCandles(trades: List<Trade>, startBalance: Double, limit: Int = 60): List<Candle> {
        var equity = startBalance
        val out = ArrayList<Candle>(trades.size)
        val ordered = trades.sortedWith(compareBy<Trade> { it.epochDay }.thenBy { it.openedAtMillis }.thenBy { it.id })
        for (trade in ordered) {
            val open = equity
            val close = equity + trade.pnl
            out += Candle(open, max(open, close), min(open, close), close)
            equity = close
        }
        return out.takeLast(limit.coerceAtLeast(1))
    }

    /** Legacy period aggregation retained for callers that explicitly need bucket candles. */
    fun candles(trades: List<Trade>, period: Period, startBalance: Double): List<Candle> {
        val groups = trades.sortedWith(compareBy<Trade> { it.epochDay }.thenBy { it.openedAtMillis }.thenBy { it.id })
            .groupBy { period.bucket(it.epochDay) }
        var equity = startBalance
        val out = ArrayList<Candle>()
        for ((_, list) in groups) {
            val open = equity
            var hi = equity
            var lo = equity
            for (t in list) {
                equity += t.pnl
                hi = max(hi, equity)
                lo = min(lo, equity)
            }
            out.add(Candle(open, hi, lo, equity))
        }
        return out.takeLast(period.count)
    }

    fun dayPnl(trades: List<Trade>): Map<Long, Double> =
        trades.groupBy { it.epochDay }.mapValues { (_, v) -> v.sumOf { it.pnl } }

    /** Saturday-first column index: 0 Sat, 1 Sun, 2 Mon ... 6 Fri. */
    fun column(epochDay: Long): Int = ((epochDay + 5) % 7).toInt()

    fun noTradeDays(trades: List<Trade>, today: Long, lookback: Int, weekendHoliday: Boolean): Int {
        val days = trades.map { it.epochDay }.toSet()
        var n = 0
        for (d in (today - lookback + 1)..today) {
            if (weekendHoliday && column(d) < 2) continue
            if (d !in days) n++
        }
        return n
    }

    fun money(v: Double, currency: String = AppState.selectedAccount.baseCurrency): String {
        val digits = if (currency == "JPY") 0 else 2
        return (if (v >= 0) "+" else "-") + currency + " " +
            String.format(java.util.Locale.US, "%,.${digits}f", kotlin.math.abs(v))
    }
}
