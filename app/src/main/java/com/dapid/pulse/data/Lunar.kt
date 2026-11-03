package com.dapid.pulse.data

import android.icu.util.ChineseCalendar
import android.icu.util.GregorianCalendar
import android.icu.util.TimeZone
import java.time.LocalDate

// Lunar dates (农历) from Android's built-in Chinese calendar: no lookup table to keep up to date.

private val MONTHS = listOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
private val DAYS = listOf(
    "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
    "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
    "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十",
)

// Traditional festivals by lunar month-day (除夕, the last night of the year, is found separately).
private val FESTIVALS = mapOf(
    1 to 1 to "春节", 1 to 15 to "元宵节", 2 to 2 to "龙抬头", 5 to 5 to "端午节", 7 to 7 to "七夕",
    7 to 15 to "中元节", 8 to 15 to "中秋节", 9 to 9 to "重阳节", 12 to 8 to "腊八节", 12 to 23 to "小年",
)

data class Lunar(val label: String, val full: String, val festival: String?)

/** Pure naming (unit tested): month 1–12, day 1–30, leap month, and whether tomorrow is New Year. */
fun lunarName(month: Int, day: Int, leap: Boolean, tomorrowIsNewYear: Boolean): Lunar {
    val monthName = (if (leap) "闰" else "") + MONTHS[month - 1]
    val dayName = DAYS.getOrElse(day - 1) { day.toString() }
    val festival = if (tomorrowIsNewYear) "除夕" else if (leap) null else FESTIVALS[month to day]
    return Lunar(label = if (day == 1) monthName else dayName, full = monthName + dayName, festival = festival)
}

// Lazy, so unit tests of lunarName (on a laptop JVM, without Android's ICU) never touch it.
private val CHINA by lazy { TimeZone.getTimeZone("Asia/Shanghai") }

private fun raw(date: LocalDate): Triple<Int, Int, Boolean> {
    val g = GregorianCalendar(CHINA).apply { clear(); set(date.year, date.monthValue - 1, date.dayOfMonth, 12, 0) }
    val c = ChineseCalendar(CHINA).apply { timeInMillis = g.timeInMillis }
    return Triple(c.get(ChineseCalendar.MONTH) + 1, c.get(ChineseCalendar.DAY_OF_MONTH), c.get(ChineseCalendar.IS_LEAP_MONTH) == 1)
}

fun lunarOf(date: LocalDate): Lunar {
    val (m, d, leap) = raw(date)
    val (tm, td, tleap) = raw(date.plusDays(1))
    return lunarName(m, d, leap, tomorrowIsNewYear = tm == 1 && td == 1 && !tleap)
}
