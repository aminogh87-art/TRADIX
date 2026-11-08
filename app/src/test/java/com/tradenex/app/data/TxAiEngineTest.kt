package com.tradenex.app.data

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TxAiEngineTest {
    private val account = TradingAccount("a", "Test", "", "REAL", "USD", 10_000.0)

    private fun trade(
        id: Long,
        pnl: Double,
        strategy: String = "ICT",
        tags: List<String> = listOf("#discipline"),
    ) = Trade(
        id = id,
        symbol = "EURUSD",
        buy = true,
        lot = 0.1,
        pnl = pnl,
        strategy = strategy,
        tags = tags,
        epochDay = LocalDate.of(2026, 11, 2).plusDays(id - 1).toEpochDay(),
        emotion = "آرام",
        accountId = "a",
        openedAtMillis = LocalDate.of(2026, 11, 2).plusDays(id - 1)
            .atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
        entryPrice = 1.1,
        exitPrice = 1.101,
        stopLossPrice = 1.099,
        plannedRisk = 100.0,
        timeframe = "M15",
        marketCondition = "رونددار",
    )

    @Test
    fun counterfactualExcludesTradesFlaggedOutsideStrategy() {
        val rows = (1L..6L).map { id ->
            if (id == 6L) trade(id, -80.0, strategy = "", tags = listOf("#fomo"))
            else trade(id, 20.0)
        }
        val report = TxAiEngine.analyze(rows, account, 1.0)
        assertEquals(6, report.totalTrades)
        assertEquals(1, report.outsideStrategyCount)
        assertEquals(100.0, report.withoutOutsideStrategyNet, 0.001)
        assertEquals(-80.0, report.outsideStrategyNet, 0.001)
        assertEquals(80.0, report.withoutOutsideStrategyDelta, 0.001)
    }

    @Test
    fun disciplineScoreIsWithheldWhenBehaviorTagCoverageIsLow() {
        val rows = (1L..6L).map { id ->
            trade(id, 10.0, tags = if (id <= 3) listOf("#discipline") else emptyList())
        }
        val report = TxAiEngine.analyze(rows, account, 1.0)
        assertEquals(null, report.scores.discipline)
        assertEquals(6, report.monthlyProgress.size)
    }

    @Test
    fun reportGroupsContextAndComputesHalfRisk() {
        val report = TxAiEngine.analyze((1L..6L).map { trade(it, 40.0) }, account, 1.0)
        assertEquals("M15", report.timeframes.single().label)
        assertEquals("رونددار", report.marketConditions.single().label)
        assertEquals(120.0, report.halfRiskNet, 0.001)
        assertEquals(6, report.weeks.sumOf { it.trades })
        assertTrue(report.sessions.any { it.label == "London" })
    }
}
