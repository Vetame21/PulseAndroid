package com.dapid.pulse.data

import java.time.LocalDate

// Small pure rules for the sign-in and setup screens (unit tested). The server checks everything
// again; these only give instant feedback.

enum class Region(val key: String, val flag: String, val label: String, val currency: String) {
    Mainland("mainland", "🇨🇳", "Mainland China", "¥"),
    HongKong("hk", "🇭🇰", "Hong Kong", "HK$");

    companion object {
        fun fromKey(key: String): Region? = entries.firstOrNull { it.key == key }
    }
}

/** off = false is a 调休 make-up workday (Mainland only). */
data class HolidayDay(val date: LocalDate, val name: String, val off: Boolean)

/** The next day off on or after today; inside a run of days off, the run's first day still ahead. */
fun nextHoliday(days: List<HolidayDay>, today: LocalDate): HolidayDay? =
    days.filter { it.off && !it.date.isBefore(today) }.minByOrNull { it.date }

private val AMOUNT = Regex("""^\d{1,9}([.,]\d{1,2})?$""")

/** The message for a monthly budget that can't be used, or null. Accepts 1500, 1500.50 or 1500,50. */
fun budgetProblem(text: String): String? {
    val t = text.trim()
    if (!AMOUNT.matches(t) || t.replace(',', '.').toDouble() <= 0.0) return "Enter your monthly budget, like 1500."
    return null
}

/** Which screen to open once we know whether you're signed in and set up. */
enum class Stage { Loading, SignIn, Setup, Home, Offline }

fun stageFor(signedIn: Boolean, settings: ApiResult<AccountSettings>?): Stage = when {
    !signedIn -> Stage.SignIn
    settings == null -> Stage.Loading
    settings is ApiResult.Ok -> if (settings.value.onboarded) Stage.Home else Stage.Setup
    settings is ApiResult.Error && settings.status == 401 -> Stage.SignIn
    else -> Stage.Offline
}
