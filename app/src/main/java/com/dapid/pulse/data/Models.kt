package com.dapid.pulse.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

// What the screens show, read from /api/v1 answers (shapes: docs/api-v1.md in the web repo).
// Money is always Long cents of the home currency; dates are LocalDate.

data class Category(val key: String, val label: String, val emoji: String)

data class MonthBudget(
    val budgetFen: Long,
    val spentFen: Long,
    val remainingFen: Long,
    val progress: Double,
    val status: String, // on_track · above_pace · over_budget · no_budget
    val safeDailyFen: Long?,
    val spentTodayFen: Long?,
)

data class Event(
    val id: String,
    val title: String,
    val date: LocalDate,
    val startTime: String?, // null = all day
    val endTime: String?,
    val location: String?,
    val deadline: Boolean,
    val done: Boolean,
)

data class Deadline(val id: String, val title: String, val date: LocalDate, val startTime: String?, val daysLeft: Int)
data class Birthday(val name: String, val date: LocalDate, val inDays: Int, val turns: Int?, val inMemory: Boolean)
data class HolidayNote(val date: LocalDate, val name: String, val off: Boolean)
data class PlanItem(val text: String, val done: Boolean)

data class Today(
    val date: LocalDate,
    val greeting: String,
    val classesToday: Int,
    val upNext: List<Pair<LocalDate, List<Event>>>,
    val deadlines: List<Deadline>,
    val birthdays: List<Birthday>,
    val holiday: HolidayNote?,
    val plan: List<PlanItem>,
    val budget: MonthBudget,
)

data class Expense(
    val id: String,
    val date: LocalDate,
    val amountFen: Long,
    val refund: Boolean,
    val category: String,
    val fromBudget: Boolean,
    val note: String?,
)

data class SpendingDay(val date: LocalDate, val state: String, val spentFen: Long, val level: Int)
data class Money(val month: LocalDate, val budget: MonthBudget, val expenses: List<Expense>, val allowanceFen: Long, val weeks: List<List<SpendingDay?>>)

data class ClassItem(val title: String, val startTime: String, val endTime: String, val room: String?)
data class DayBirthday(val name: String, val inMemory: Boolean, val turns: Int?)

data class CalendarDay(
    val date: LocalDate,
    val dayNumber: Int,
    val weekend: Boolean,
    val isToday: Boolean,
    val lunar: String,
    val festival: String?,
    val holiday: HolidayNote?,
    val holidayStart: Boolean,
    val events: List<Event>,
    val classes: List<ClassItem>,
    val birthdays: List<DayBirthday>,
)

data class CalendarMonth(val month: LocalDate, val holidaysPublished: Boolean, val weeks: List<List<CalendarDay?>>)

// --- reading the JSON -------------------------------------------------------

private fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else getLong(key)
private fun JSONObject.date(key: String): LocalDate = LocalDate.parse(getString(key))
private fun <T> JSONArray?.map(f: (JSONObject) -> T): List<T> = if (this == null) emptyList() else (0 until length()).map { f(getJSONObject(it)) }

private fun <T> JSONArray?.weeks(f: (JSONObject) -> T): List<List<T?>> =
    if (this == null) emptyList() else (0 until length()).map { w ->
        val week = getJSONArray(w)
        (0 until week.length()).map { d -> if (week.isNull(d)) null else f(week.getJSONObject(d)) }
    }

fun parseCategories(a: JSONArray?) = a.map { Category(it.getString("key"), it.getString("label"), it.getString("emoji")) }

fun parseBudget(o: JSONObject) = MonthBudget(
    budgetFen = o.getLong("budgetFen"),
    spentFen = o.getLong("spentFen"),
    remainingFen = o.getLong("remainingFen"),
    progress = o.optDouble("progress", 0.0),
    status = o.optString("status"),
    safeDailyFen = o.longOrNull("safeDailyFen"),
    spentTodayFen = o.longOrNull("spentTodayFen"),
)

fun parseEvent(o: JSONObject) = Event(
    id = o.getString("id"),
    title = o.getString("title"),
    date = o.date("date"),
    startTime = o.str("startTime"),
    endTime = o.str("endTime"),
    location = o.str("location"),
    deadline = o.optString("kind") == "deadline",
    done = o.optBoolean("done"),
)

private fun parseHoliday(o: JSONObject?) = o?.let { HolidayNote(it.date("date"), it.getString("name"), it.getBoolean("off")) }

fun parseToday(o: JSONObject): Today {
    val b = o.getJSONObject("briefing")
    return Today(
        date = o.date("date"),
        greeting = o.optString("greeting"),
        classesToday = b.optInt("classesToday"),
        upNext = b.optJSONArray("upNext").map { g -> LocalDate.parse(g.getString("day")) to g.optJSONArray("items").map(::parseEvent) },
        deadlines = b.optJSONArray("deadlines").map { Deadline(it.getString("id"), it.getString("title"), it.date("date"), it.str("startTime"), it.getInt("daysLeft")) },
        birthdays = b.optJSONArray("birthdays").map {
            Birthday(it.getString("name"), it.date("date"), it.getInt("inDays"), if (it.isNull("turns")) null else it.getInt("turns"), it.optBoolean("inMemory"))
        },
        holiday = parseHoliday(b.optJSONObject("holiday")),
        plan = o.getJSONObject("plan").optJSONArray("items").map { PlanItem(it.getString("text"), it.getBoolean("done")) },
        budget = parseBudget(o.getJSONObject("budget")),
    )
}

fun parseMoney(money: JSONObject, calendar: JSONObject) = Money(
    month = money.date("month"),
    budget = parseBudget(money.getJSONObject("budget")),
    expenses = money.optJSONArray("expenses").map {
        Expense(
            id = it.getString("id"),
            date = it.date("date"),
            amountFen = it.getLong("amountFen"),
            refund = it.optString("kind") == "refund",
            category = it.optString("category"),
            fromBudget = it.optString("fundedBy") == "budget",
            note = it.str("note"),
        )
    },
    allowanceFen = calendar.optLong("allowanceFen"),
    weeks = calendar.optJSONArray("weeks").weeks { SpendingDay(it.date("date"), it.getString("state"), it.getLong("spentFen"), it.getInt("level")) },
)

fun parseCalendar(o: JSONObject) = CalendarMonth(
    month = o.date("month"),
    holidaysPublished = o.optBoolean("holidaysPublished", true),
    weeks = o.optJSONArray("weeks").weeks { d ->
        val date = d.date("date")
        CalendarDay(
            date = date,
            dayNumber = d.getInt("dayNumber"),
            weekend = d.optBoolean("weekend"),
            isToday = d.optBoolean("isToday"),
            lunar = d.optString("lunar"),
            festival = d.str("festival"),
            holiday = d.optJSONObject("holiday")?.let { HolidayNote(date, it.getString("name"), it.getBoolean("off")) },
            holidayStart = d.optBoolean("holidayStart"),
            events = d.optJSONArray("events").map(::parseEvent),
            classes = d.optJSONArray("classes").map { ClassItem(it.getString("title"), it.getString("startTime"), it.getString("endTime"), it.str("room")) },
            birthdays = d.optJSONArray("birthdays").map { DayBirthday(it.getString("name"), it.optBoolean("inMemory"), if (it.isNull("turns")) null else it.getInt("turns")) },
        )
    },
)
