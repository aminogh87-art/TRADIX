package com.tradenex.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class JalaliCalendarTest {
    @Test
    fun convertsKnownNowruzDates() {
        assertEquals(JalaliDate(1405, 1, 1), JalaliCalendar.fromGregorian(LocalDate.of(2026, 3, 21)))
        assertEquals(LocalDate.of(2026, 3, 21), JalaliCalendar.toGregorian(1405, 1, 1))
        assertEquals(JalaliDate(1403, 1, 1), JalaliCalendar.fromGregorian(LocalDate.of(2024, 3, 20)))
    }

    @Test
    fun roundTripsMonthBoundaries() {
        for (month in 1..12) {
            val day = JalaliCalendar.daysInMonth(1405, month)
            val gregorian = JalaliCalendar.toGregorian(1405, month, day)
            assertEquals(JalaliDate(1405, month, day), JalaliCalendar.fromGregorian(gregorian))
        }
    }
}
