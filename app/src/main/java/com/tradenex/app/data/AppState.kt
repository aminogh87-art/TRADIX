package com.tradenex.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tradenex.app.core.NativeCore
import com.tradenex.app.core.TradeStats
import com.tradenex.app.domain.FxInstrument
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
    val strategyVersion: Int = 1,
)

data class TradingAccount(
    val id: String,
    val name: String,
    val broker: String,
    val type: String,
    val baseCurrency: String,
    val startingBalance: Double,
    val archived: Boolean = false,
)

data class Strategy(val id: Long, val name: String, val rules: List<String>, val active: Boolean, val version: Int = 1)

data class StrategyRevision(
    val id: Long,
    val strategyId: Long,
    val version: Int,
    val name: String,
    val rules: List<String>,
    val createdAt: Long,
)

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
    val tzMode: Int = 2, // 0 Tehran, 1 UTC, 2 device (default: local device time)
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
    val accounts = mutableStateListOf(
        TradingAccount("default", "حساب اصلی", "", "REAL", "USD", 10_000.0),
    )
    var selectedAccountId by mutableStateOf("default")
    val selectedAccount: TradingAccount
        get() = accounts.firstOrNull { it.id == selectedAccountId && !it.archived }
            ?: accounts.firstOrNull { !it.archived }
            ?: TradingAccount("default", "حساب اصلی", "", "REAL", "USD", 10_000.0)
    val visibleTrades: List<Trade>
        get() = trades.filter { it.accountId == selectedAccountId }
    val strategies = mutableStateListOf(
        Strategy(1, "ICT", listOf("تایید ساختار بازار", "ریسک حداکثر ۱٪", "فقط سشن لندن/نیویورک"), true),
        Strategy(2, "Supply & Demand", listOf("ناحیه تازه", "تایید کندلی"), true),
        Strategy(3, "Breakout", listOf("شکست با حجم", "استاپ زیر کندل شکست"), false),
    )
    val strategyRevisions = mutableStateListOf<StrategyRevision>()
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
                MutableSnapshot(trades, accounts, strategies, strategyRevisions, hashtags, lessons, settings, selectedAccountId),
            )
        }
    }

    /** Atomic save called after mutations and whenever the activity leaves the foreground. */
    fun persist() {
        val payload = SnapshotCodec.encode(trades, accounts, strategies, strategyRevisions, hashtags, lessons, settings, selectedAccountId)
        localStore?.writeSnapshot(payload)
    }

    fun stats(): TradeStats = NativeCore.stats(visibleTrades.map { it.pnl }.toDoubleArray())

    fun addTrade(t: Trade) {
        FxInstrument.standard(t.symbol)
        require(t.lot > 0.0 && t.pnl.isFinite()) { "Trade volume and P&L must be valid." }
        trades.add(t)
        persist()
    }

    fun today(): Long = LocalDate.now().toEpochDay()


}
