package com.dapid.pulse

import com.dapid.pulse.data.HolidayDay
import com.dapid.pulse.data.LocalEvent
import com.dapid.pulse.data.Region
import com.dapid.pulse.data.activeDeadlines
import com.dapid.pulse.data.budgetProblem
import com.dapid.pulse.data.holidayStart
import com.dapid.pulse.data.holidayThisWeek
import com.dapid.pulse.data.isTime
import com.dapid.pulse.data.lunarName
import com.dapid.pulse.data.monthGrid
import com.dapid.pulse.data.nextHoliday
import com.dapid.pulse.data.parseAmount
import com.dapid.pulse.data.upNext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RulesTest {
    private val today = LocalDate.of(2026, 9, 30)
    private val days = listOf(
        HolidayDay(LocalDate.of(2026, 9, 27), "国庆节", off = false), // 调休 workday
        HolidayDay(LocalDate.of(2026, 10, 1), "国庆节", off = true),
        HolidayDay(LocalDate.of(2026, 10, 2), "国庆节", off = true),
        HolidayDay(LocalDate.of(2026, 10, 10), "国庆节", off = false),
    )

    private fun event(id: String, date: LocalDate, start: String? = null, end: String? = null, deadline: Boolean = false, lead: Int? = null, done: Boolean = false) =
        LocalEvent(id, id, date, start, end, null, deadline, lead, done)

    @Test
    fun holidaysSkipWorkdaysAndNameOnlyTheFirstDayOff() {
        assertEquals(LocalDate.of(2026, 10, 1), nextHoliday(days, today)?.date)
        assertNull(nextHoliday(days, LocalDate.of(2026, 10, 3)))
        assertTrue(holidayStart(LocalDate.of(2026, 10, 1), days))
        assertFalse(holidayStart(LocalDate.of(2026, 10, 2), days))
        assertFalse(holidayStart(LocalDate.of(2026, 9, 27), days))
        assertEquals(LocalDate.of(2026, 9, 27), holidayThisWeek(days, LocalDate.of(2026, 9, 25))?.date)
    }

    @Test
    fun monthGridStartsOnMonday() {
        val weeks = monthGrid(YearMonth.of(2026, 10)) // 1 Oct 2026 is a Thursday
        assertEquals(listOf(null, null, null, LocalDate.of(2026, 10, 1)), weeks[0].take(4))
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(31, weeks.flatten().filterNotNull().size)
    }

    @Test
    fun deadlinesShowFromTheirLeadTimeUntilDone() {
        val due = LocalDate.of(2026, 10, 6)
        val list = listOf(
            event("essay", due, deadline = true, lead = 7),
            event("later", due.plusDays(30), deadline = true, lead = 3),
            event("finished", due, deadline = true, lead = 7, done = true),
            event("late", today.minusDays(2), deadline = true, lead = 1),
        )
        val active = activeDeadlines(list, today)
        assertEquals(listOf("late", "essay"), active.map { it.event.id })
        assertEquals(listOf(-2, 6), active.map { it.daysLeft })
    }

    @Test
    fun upNextLeavesOutFinishedEventsAndDeadlines() {
        val list = listOf(
            event("breakfast", today, "07:00", "08:00"),
            event("dinner", today, "18:00"),
            event("trip", today.plusDays(1)),
            event("homework", today.plusDays(1), deadline = true),
            event("faraway", today.plusDays(9)),
        )
        val next = upNext(list, today, nowTime = "10:00")
        assertEquals(listOf(today to listOf("dinner"), today.plusDays(1) to listOf("trip")), next.map { (d, items) -> d to items.map { it.id } })
    }

    @Test
    fun amountsAreWholeCents() {
        assertEquals(150000L, parseAmount("1500"))
        assertEquals(3250L, parseAmount("32.50"))
        assertEquals(3250L, parseAmount("32,5"))
        listOf("", "0", "-5", "abc", "1.234", "1,500.00").forEach { assertNull(it, parseAmount(it)) }
        assertNull(budgetProblem("1500"))
        assertEquals("Enter your monthly budget, like 1500.", budgetProblem("0"))
    }

    @Test
    fun timesAreHoursAndMinutes() {
        assertTrue(isTime("08:50"))
        assertTrue(isTime("23:59"))
        assertFalse(isTime("8:50"))
        assertFalse(isTime("24:00"))
    }

    @Test
    fun regionsDecideCurrencyAndTimezone() {
        assertEquals("HKD", Region.fromKey("hk")?.currency)
        assertEquals("Asia/Shanghai", Region.Mainland.zone)
        assertNull(Region.fromKey("HK"))
    }

    @Test
    fun lunarNamesAndFestivals() {
        assertEquals("八月", lunarName(8, 1, leap = false, tomorrowIsNewYear = false).label)
        assertEquals("十五", lunarName(8, 15, leap = false, tomorrowIsNewYear = false).label)
        assertEquals("中秋节", lunarName(8, 15, leap = false, tomorrowIsNewYear = false).festival)
        assertEquals("闰六月初三", lunarName(6, 3, leap = true, tomorrowIsNewYear = false).full)
        assertEquals("除夕", lunarName(12, 29, leap = false, tomorrowIsNewYear = true).festival)
        assertNull(lunarName(5, 5, leap = true, tomorrowIsNewYear = false).festival) // no 端午 in a leap month
    }
}
