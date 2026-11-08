package com.tradenex.app.data

import com.tradenex.app.core.SessionEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

data class TxAiSlice(
    val label: String,
    val trades: Int,
    val net: Double,
    val winRate: Double,
    val average: Double,
)

data class TxAiScores(
    val discipline: Int?,
    val risk: Int?,
    val strategy: Int?,
    val decision: Int?,
    val monthLabel: String,
    val sampleSize: Int,
)

data class TxAiReport(
    val totalTrades: Int,
    val net: Double,
    val winRate: Double,
    val bestSession: TxAiSlice?,
    val sessions: List<TxAiSlice>,
    val weekdays: List<TxAiSlice>,
    val days: List<TxAiSlice>,
    val personality: String,
    val weeks: List<TxAiSlice>,
    val bestStrategy: TxAiSlice?,
    val outsideStrategyCount: Int,
    val outsideStrategyNet: Double,
    val allNet: Double,
    val halfRiskNet: Double,
    val halfRiskDelta: Double,
    val scores: TxAiScores,
    val insights: List<String>,
    val timeframeDataAvailable: Boolean = false,
    val marketRegimeDataAvailable: Boolean = false,
)

object TxAiEngine {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun analyze(trades: List<Trade>, account: TradingAccount, riskPercent: Double): TxAiReport {
        val ordered = trades.sortedWith(compareBy<Trade> { it.epochDay }.thenBy { it.openedAtMillis })
        val sessions = ordered.filter { it.openedAtMillis > 0L }.groupBy { sessionOf(it.openedAtMillis) }
            .filterKeys { it != "نامشخص" }.map { (name, rows) -> slice(name, rows) }
            .sortedByDescending { it.average }
        val weekdays = ordered.groupBy { LocalDate.ofEpochDay(it.epochDay).dayOfWeek.value }
            .map { (day, rows) -> slice(dayName(day), rows) }.sortedByDescending { it.average }
        val days = ordered.groupBy { LocalDate.ofEpochDay(it.epochDay) }.toSortedMap().map { (date, rows) -> slice(date.toString(), rows) }
        val weeks = ordered.groupBy {
            val d = LocalDate.ofEpochDay(it.epochDay)
            d.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        }.toSortedMap().map { (start, rows) -> slice("${start.dayOfMonth}/${start.monthValue}", rows) }
        val strategySlices = ordered.filter { it.strategy.isNotBlank() }.groupBy { it.strategy }
            .map { (name, rows) -> slice(name, rows) }.sortedByDescending { it.average }
        val outside = ordered.filter { isOutsideStrategy(it) }
        val month = LocalDate.now().withDayOfMonth(1)
        val monthTrades = ordered.filter { LocalDate.ofEpochDay(it.epochDay).let { d -> d.year == month.year && d.month == month.month } }
        val knownRisk = monthTrades.filter { it.plannedRisk != null && it.plannedRisk > 0.0 }
        val strategyKnown = monthTrades.filter { it.strategy.isNotBlank() }
        val discipline = if (monthTrades.size >= 5) {
            val violations = monthTrades.count { hasBehaviorFlag(it) }
            ((1.0 - violations.toDouble() / monthTrades.size) * 100).roundToInt().coerceIn(0, 100)
        } else null
        val strategyScore = if (monthTrades.size >= 5) {
            (strategyKnown.size.toDouble() / monthTrades.size * 100).roundToInt()
        } else null
        val riskScore = if (knownRisk.size >= 3) {
            val within = knownRisk.count { it.plannedRisk!! <= account.startingBalance * riskPercent.coerceAtLeast(0.1) / 100.0 * 1.25 }
            (within.toDouble() / knownRisk.size * 100).roundToInt()
        } else null
        val decisionScore = if (monthTrades.size >= 5) {
            val complete = monthTrades.count { it.entryPrice != null && it.exitPrice != null && it.stopLossPrice != null && it.strategy.isNotBlank() }
            (complete.toDouble() / monthTrades.size * 100).roundToInt()
        } else null
        val halfRisk = ordered.sumOf { it.pnl * 0.5 }
        val insights = buildList {
            if (ordered.size < 10) add("برای تشخیص الگوی پایدار، حداقل ۱۰ معامله ثبت کن؛ نتیجه فعلی مقدماتی است.")
            sessions.firstOrNull()?.let { add("بهترین میانگین نتیجه ثبت‌شده در سشن ${it.label} است؛ ${it.trades} معامله با میانگین ${money(it.average)}.") }
            weekdays.firstOrNull()?.let { add("بهترین روز هفته از نظر میانگین هر معامله: ${it.label}.") }
            strategySlices.firstOrNull()?.let { add("استراتژی ${it.label} در داده‌های فعلی بهترین میانگین را دارد؛ قبل از نتیجه‌گیری تعداد نمونه را بررسی کن.") }
            if (outside.isNotEmpty()) add("${outside.size} معامله با نشانه‌های خارج از پلن/بدون استراتژی شناسایی شد؛ سود و زیان خالص آن‌ها ${money(outside.sumOf { it.pnl })} است.")
            if (knownRisk.size < 3) add("برای امتیاز دقیق مدیریت ریسک، ریسک برنامه‌ریزی‌شده را هنگام ثبت هر معامله وارد کن.")
            add("تایم‌فریم و وضعیت بازار در مدل فعلی معامله ذخیره نمی‌شوند؛ برای تحلیل معتبر این دو مورد باید به فرم ثبت معامله افزوده شوند.")
        }
        return TxAiReport(
            totalTrades = ordered.size, net = ordered.sumOf { it.pnl },
            winRate = if (ordered.isEmpty()) 0.0 else ordered.count { it.pnl > 0 }.toDouble() / ordered.size,
            bestSession = sessions.firstOrNull(), sessions = sessions, weekdays = weekdays, days = days, weeks = weeks,
            days = days, personality = when { ordered.size < 10 -> "داده ناکافی برای تشخیص پایدار"; sessions.firstOrNull()?.let { it.trades.toDouble() / ordered.size >= 0.5 } == true -> "معامله‌گر سشن‌محور"; strategyKnown.size.toDouble() / monthTrades.size.coerceAtLeast(1) >= 0.8 -> "معامله‌گر استراتژی‌محور"; else -> "سبک ترکیبی و انعطاف‌پذیر" },
            bestStrategy = strategySlices.firstOrNull(), outsideStrategyCount = outside.size,
            outsideStrategyNet = outside.sumOf { it.pnl }, allNet = ordered.sumOf { it.pnl },
            halfRiskNet = halfRisk, halfRiskDelta = halfRisk - ordered.sumOf { it.pnl },
            scores = TxAiScores(discipline, riskScore, strategyScore, decisionScore,
                "${month.year}/${month.monthValue}", monthTrades.size),
            insights = insights,
        )
    }

    private fun sessionOf(millis: Long): String {
        val instant = Instant.ofEpochMilli(millis)
        return SessionEngine.defs.firstOrNull { def ->
            val local = instant.atZone(def.zoneId).toLocalTime()
            if (def.localOpen < def.localClose) !local.isBefore(def.localOpen) && local.isBefore(def.localClose)
            else !local.isBefore(def.localOpen) || local.isBefore(def.localClose)
        }?.name ?: "بین سشن‌ها"
    }

    private fun isOutsideStrategy(t: Trade): Boolean {
        val tags = t.tags.map { it.lowercase() }
        return t.strategy.isBlank() || tags.any {
            it.contains("fomo") || it.contains("revenge") || it.contains("no-plan") ||
                it.contains("off-strategy") || it.contains("خارج از استراتژی") || it.contains("بدون پلن")
        }
    }

    private fun hasBehaviorFlag(t: Trade): Boolean = t.tags.any {
        val tag = it.lowercase()
        tag.contains("fomo") || tag.contains("revenge") || tag.contains("overtrade") ||
            tag.contains("no-plan") || tag.contains("خارج از استراتژی")
    }

    private fun slice(label: String, rows: List<Trade>) = TxAiSlice(
        label, rows.size, rows.sumOf { it.pnl },
        if (rows.isEmpty()) 0.0 else rows.count { it.pnl > 0 }.toDouble() / rows.size,
        if (rows.isEmpty()) 0.0 else rows.sumOf { it.pnl } / rows.size,
    )

    private fun dayName(day: Int) = when (day) {
        1 -> "دوشنبه"; 2 -> "سه‌شنبه"; 3 -> "چهارشنبه"; 4 -> "پنجشنبه"
        5 -> "جمعه"; 6 -> "شنبه"; else -> "یکشنبه"
    }

    private fun money(v: Double) = String.format(java.util.Locale.US, "%+.2f", v)
}
