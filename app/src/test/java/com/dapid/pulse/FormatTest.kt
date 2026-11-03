package com.dapid.pulse

import com.dapid.pulse.data.dayLabel
import com.dapid.pulse.data.deadlineText
import com.dapid.pulse.data.formatMoney
import com.dapid.pulse.data.timeRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FormatTest {
    private val today = LocalDate.of(2026, 11, 3) // a Tuesday

    @Test
    fun moneyLooksLikeTheWeb() {
        assertEquals("¥1,550", formatMoney(155000, "CNY"))
        assertEquals("¥32.50", formatMoney(3250, "CNY"))
        assertEquals("HK$0.05", formatMoney(5, "HKD"))
        assertEquals("-¥5", formatMoney(-500, "CNY"))
        assertEquals("+¥12", formatMoney(1200, "CNY", sign = true))
        assertEquals("¥1,000,000", formatMoney(100_000_000, "CNY"))
    }

    @Test
    fun daysReadAsTodayTomorrowOrShortDate() {
        assertEquals("Today", dayLabel(today, today))
        assertEquals("Tomorrow", dayLabel(today.plusDays(1), today))
        assertEquals("Thu 5 Nov", dayLabel(today.plusDays(2), today))
    }

    @Test
    fun deadlinesSayHowLongIsLeft() {
        assertEquals("Overdue by 1 day", deadlineText(today.minusDays(1), -1, null, today))
        assertEquals("Overdue by 3 days", deadlineText(today.minusDays(3), -3, null, today))
        assertEquals("Due today at 23:59", deadlineText(today, 0, "23:59", today))
        assertEquals("Due Tomorrow · 1 day left", deadlineText(today.plusDays(1), 1, null, today))
        assertEquals("Due Fri 6 Nov · 3 days left", deadlineText(today.plusDays(3), 3, null, today))
    }

    @Test
    fun timesShowARangeOrAllDay() {
        assertEquals("All day", timeRange(null, null))
        assertEquals("18:00", timeRange("18:00", null))
        assertEquals("18:00–20:00", timeRange("18:00", "20:00"))
    }
}
