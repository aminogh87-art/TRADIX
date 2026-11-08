package com.tradenex.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tradenex.app.core.NativeCore
import com.tradenex.app.core.TradeStats
import java.time.LocalDate

data class Trade(
    val id: Long,
    val symbol: String,
    val buy: Boolean,
    val lot: Double,
    val pnl: Double,
    val strategy: String,
    val tags: List<String>,
    val epochDay: Long,
    val emotion: String,
    val accountId: String = "default",
    val openedAtMillis: Long = 0L,
    val closedAtMillis: Long = 0L,
    val entryPrice: Double? = null,
    val exitPrice: Double? = null,
    val stopLossPrice: Double? = null,
    val takeProfitPrice: Double? = null,
    val commission: Double = 0.0,
    val swap: Double = 0.0,
    val otherFees: Double = 0.0,
    val notes: String = "",
    val plannedRisk: Double? = null,
)

data class Strategy(val id: Long, val name: String, val rules: List<String>, val active: Boolean)

enum class ContentKind(val label: String) {
    TEXT("فقط متن"),
    IMAGE("فقط عکس"),
    VIDEO("فقط ویدیو"),
    TEXT_IMAGE("متن + عکس"),
    IMAGE_TEXT("عکس + متن"),
    TEXT_VIDEO("متن + ویدیو"),
    VIDEO_TEXT("ویدیو + متن"),
}

data class Lesson(val id: Long, val title: String, val kind: ContentKind, val progress: Float)

data class AppSettings(
    val language: Int = 0, // 0 fa, 1 en (UI picker only for now)
    val showGlobe: Boolean = true,
    val tzMode: Int = 0, // 0 Tehran, 1 UTC, 2 device
    val reminderOn: Boolean = true,
    val reminderHour: Int = 22,
    val newsImpact: Int = 0, // 0 all, 1 medium+, 2 high
    val startBalance: Double = 10_000.0,
    val defaultLot: Double = 0.10,
    val riskPercent: Double = 1.0,
    val haptics: Boolean = true,
    val animations: Int = 1, // 0 low, 1 normal, 2 ultra
    val accent: Int = 0, // 0 gold, 1 emerald, 2 violet
    val weekendHoliday: Boolean = true,
    val sessionAlerts: Boolean = true,
    val newsAlerts: Boolean = true,
    val autoBackup: Boolean = false,
    val hideBalance: Boolean = false,
)

object AppState {
    val trades = mutableStateListOf<Trade>()
    val strategies = mutableStateListOf(
        Strategy(1, "ICT", listOf("تایید ساختار بازار", "ریسک حداکثر ۱٪", "فقط سشن لندن/نیویورک"), true),
        Strategy(2, "Supply & Demand", listOf("ناحیه تازه", "تایید کندلی"), true),
        Strategy(3, "Breakout", listOf("شکست با حجم", "استاپ زیر کندل شکست"), false),
    )
    val hashtags = mutableStateListOf("#london", "#news", "#fomo", "#breakout", "#discipline", "#revenge")
    val lessons = mutableStateListOf(
        Lesson(1, "پرایس اکشن از صفر", ContentKind.VIDEO_TEXT, 0.65f),
        Lesson(2, "قوانین مدیریت ریسک", ContentKind.TEXT, 1f),
        Lesson(3, "الگوهای کندلی کلیدی", ContentKind.IMAGE_TEXT, 0.3f),
    )
    var settings by mutableStateOf(AppSettings())
    private var localStore: LocalStore? = null

    /** Load durable user data once, before Compose builds the first screen. */
    fun initialize(context: android.content.Context) {
        if (localStore != null) return
        localStore = LocalStore(context)
        val snapshot = localStore?.readSnapshot()
        if (snapshot != null) {
            SnapshotCodec.restore(
                snapshot,
                MutableSnapshot(trades, strategies, hashtags, lessons, settings),
            )
        }
    }

    /** Atomic save called after mutations and whenever the activity leaves the foreground. */
    fun persist() {
        val payload = SnapshotCodec.encode(trades, strategies, hashtags, lessons, settings)
        localStore?.writeSnapshot(payload)
    }

    fun stats(): TradeStats = NativeCore.stats(trades.map { it.pnl }.toDoubleArray())

    fun addTrade(t: Trade) {
        require(t.symbol.matches(Regex("[A-Z]{6}"))) { "Only Forex currency pairs are supported." }
        require(t.lot > 0.0 && t.pnl.isFinite()) { "Trade volume and P&L must be valid." }
        trades.add(t)
        persist()
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    private fun seedTrades(): List<Trade> {
        var s = 42L
        fun rnd(): Double {
            s = (s * 16807L) % 2147483647L
            return s / 2147483647.0
        }
        val syms = listOf("XAUUSD", "EURUSD", "GBPUSD", "USDJPY", "BTCUSD")
        val strats = listOf("ICT", "Supply & Demand", "Breakout")
        val tags = listOf("#london", "#news", "#breakout", "#discipline")
        val emos = listOf("آرام", "ترس", "طمع", "خسته")
        val lots = listOf(0.1, 0.2, 0.5, 1.0)
        val out = ArrayList<Trade>()
        val today = LocalDate.now().toEpochDay()
        var id = 1L
        for (d in (today - 700)..today) {
            val dow = ((d + 5) % 7).toInt() // 0 Sat, 1 Sun, 2 Mon ... 6 Fri
            if (dow < 2) continue
            val n = (rnd() * 3).toInt()
            repeat(n) {
                val win = rnd() < 0.62
                val amount = 30 + rnd() * 220
                out.add(
                    Trade(
                        id = id++,
                        symbol = syms[(rnd() * syms.size).toInt()],
                        buy = rnd() < 0.55,
                        lot = lots[(rnd() * lots.size).toInt()],
                        pnl = if (win) amount else -amount * 0.8,
                        strategy = strats[(rnd() * strats.size).toInt()],
                        tags = listOf(tags[(rnd() * tags.size).toInt()]),
                        epochDay = d,
                        emotion = emos[(rnd() * emos.size).toInt()],
                    ),
                )
            }
        }
        return out
    }
}
