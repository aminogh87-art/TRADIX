package com.tradenex.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsTradeCandleTest {
    @Test
    fun createsOneCandleForEachTradeInExecutionOrder() {
        val trades = listOf(
            Trade(2, "EURUSD", true, 0.1, -5.0, "ICT", emptyList(), 10, "", openedAtMillis = 2000),
            Trade(1, "EURUSD", true, 0.1, 10.0, "ICT", emptyList(), 10, "", openedAtMillis = 1000),
        )
        val candles = Analytics.tradeCandles(trades, 1_000.0)
        assertEquals(2, candles.size)
        assertEquals(1_000.0, candles[0].o, 0.001)
        assertEquals(1_010.0, candles[0].c, 0.001)
        assertEquals(1_010.0, candles[1].o, 0.001)
        assertEquals(1_005.0, candles[1].c, 0.001)
    }
}
