package com.dapid.pulse.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// Pure rules for the screens (unit tested): regions, holidays, the month grid,
// deadlines and "up next".

enum class Region(val key: String, val flag: String, val label: String, val currency: String, val zone: String) {
    Mainland("mainland", "🇨🇳", "Mainland China", "CNY", "Asia/Shanghai"),
    HongKong("hk", "🇭🇰", "Hong Kong", "HKD", "Asia/Hong_Kong");

    fun today(): LocalDate = LocalDate.now(java.time.ZoneId.of(zone))
    fun nowTime(): String = java.time.LocalTime.now(java.time.ZoneId.of(zone)).toString().take(5)

    companion object {
        fun fromKey(key: String?): Region? = entries.firstOrNull { it.key == key }
    }
}

/** off = false is a 调休 make-up workday (Mainland only). */
data class HolidayDay(val date: LocalDate, val name: String, val off: Boolean)

fun holidayFor(date: LocalDate, days: List<HolidayDay>): HolidayDay? = days.firstOrNull { it.date == date }

/** First day of a run of days off (the name is shown only there). */
fun holidayStart(date: LocalDate, days: List<HolidayDay>): Boolean {
    val h = holidayFor(date, days) ?: return false
    val before = holidayFor(date.minusDays(1), days)
    return h.off && !(before?.off == true && before.name == h.name)
}

/** The next day off on or after today. */
fun nextHoliday(days: List<HolidayDay>, today: LocalDate): HolidayDay? =
    days.filter { it.off && !it.date.isBefore(today) }.minByOrNull { it.date }

/** The first holiday or 调休 workday in the coming week (for Today's briefing). */
fun holidayThisWeek(days: List<HolidayDay>, today: LocalDate): HolidayDay? =
    (0L..7L).map { today.plusDays(it) }.firstNotNullOfOrNull { holidayFor(it, days) }

/** Weeks of the month, Monday first; null pads days outside the month. */
fun monthGrid(month: YearMonth): List<List<LocalDate?>> {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    val cells: MutableList<LocalDate?> = MutableList(lead) { null }
    for (d in 1..month.lengthOfMonth()) cells += month.atDay(d)
    while (cells.size % 7 != 0) cells += null
    return cells.chunked(7)
}

private const val DEFAULT_LEAD_DAYS = 7

data class ActiveDeadline(val event: LocalEvent, val daysLeft: Int)

/** Deadlines to work on today: from leadDays before the due date until done (overdue ones stay). */
fun activeDeadlines(events: List<LocalEvent>, today: LocalDate): List<ActiveDeadline> =
    events
        .filter { it.deadline && !it.done && !it.date.minusDays((it.leadDays ?: DEFAULT_LEAD_DAYS).toLong()).isAfter(today) }
        .map { ActiveDeadline(it, ChronoUnit.DAYS.between(today, it.date).toInt()) }
        .sortedWith(compareBy({ it.event.date }, { it.event.startTime ?: "" }))

/** Events on a day: all-day ones first, then by start time. */
fun eventsOn(events: List<LocalEvent>, date: LocalDate): List<LocalEvent> =
    events.filter { it.date == date }.sortedBy { it.startTime ?: "" }

/** What's next over the coming 7 days, grouped by day; today's events that already ended are left out. */
fun upNext(events: List<LocalEvent>, today: LocalDate, nowTime: String, limit: Int = 6): List<Pair<LocalDate, List<LocalEvent>>> {
    val result = mutableListOf<Pair<LocalDate, List<LocalEvent>>>()
    var count = 0
    for (offset in 0L..7L) {
        if (count >= limit) break
        val day = today.plusDays(offset)
        val items = eventsOn(events.filter { !it.deadline }, day)
            .filter { day != today || it.startTime == null || (it.endTime ?: it.startTime) > nowTime }
            .take(limit - count)
        if (items.isNotEmpty()) {
            result += day to items
            count += items.size
        }
    }
    return result
}

private val AMOUNT = Regex("""^\d{1,9}([.,]\d{1,2})?$""")

/** "32.50" (or "32,50") → 3250 cents; null if it isn't a positive amount with up to 2 decimals. */
fun parseAmount(text: String): Long? {
    val t = text.trim()
    if (!AMOUNT.matches(t)) return null
    val parts = t.replace(',', '.').split('.')
    val cents = parts[0].toLong() * 100 + (parts.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0L)
    return cents.takeIf { it > 0 }
}

/** The message for a monthly budget that can't be used, or null. */
fun budgetProblem(text: String): String? = if (parseAmount(text) == null) "Enter your monthly budget, like 1500." else null

private val TIME = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")

fun isTime(text: String) = TIME.matches(text)
