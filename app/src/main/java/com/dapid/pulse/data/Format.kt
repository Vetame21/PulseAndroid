package com.dapid.pulse.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Text for money and dates, same style as the web (pure, unit tested).

private val SYMBOL = mapOf("CNY" to "¥", "HKD" to "HK$")

/** Cents in the home currency: 155000 → "¥1,550", 3250 → "¥32.50", -500 → "-¥5". */
fun formatMoney(cents: Long, currency: String, sign: Boolean = false): String {
    val abs = kotlin.math.abs(cents)
    val whole = "%,d".format(Locale.US, abs / 100)
    val rest = abs % 100
    val decimals = if (rest != 0L) ".%02d".format(rest) else ""
    val prefix = if (cents < 0) "-" else if (sign && cents > 0) "+" else ""
    return "$prefix${SYMBOL[currency] ?: "¥"}$whole$decimals"
}

fun currencySymbol(currency: String) = SYMBOL[currency] ?: "¥"

private val SHORT_DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val LONG_DAY = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
private val MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/** "Today", "Tomorrow" or "Wed 4 Nov". */
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    else -> date.format(SHORT_DAY)
}

fun longDay(date: LocalDate): String = date.format(LONG_DAY)
fun monthTitle(date: LocalDate): String = date.format(MONTH)

/** "Due today", "Overdue by 2 days", "Due Fri 6 Nov · 3 days left". */
fun deadlineText(date: LocalDate, daysLeft: Int, startTime: String?, today: LocalDate): String = when {
    daysLeft < 0 -> "Overdue by ${-daysLeft} day${if (daysLeft == -1) "" else "s"}"
    daysLeft == 0 -> "Due today${startTime?.let { " at $it" } ?: ""}"
    else -> "Due ${dayLabel(date, today)} · $daysLeft day${if (daysLeft > 1) "s" else ""} left"
}

/** "18:00–20:00", "18:00" or "All day". */
fun timeRange(start: String?, end: String?): String = when {
    start == null -> "All day"
    end == null -> start
    else -> "$start–$end"
}
