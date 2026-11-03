package com.dapid.pulse.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

// Everything Pulse Lite knows lives on the phone, in one private JSON file
// (other apps can't read it; uninstalling deletes it). No server, no account.

data class Category(val key: String, val label: String, val emoji: String)

val CATEGORIES = listOf(
    Category("food", "Food", "🍜"),
    Category("transport", "Transport", "🚇"),
    Category("household", "Household", "🪑"),
    Category("toys", "Toys", "🧸"),
    Category("entertainment", "Fun", "🎮"),
    Category("shopping", "Shopping", "🛍"),
    Category("travel", "Travel", "✈️"),
    Category("other", "Other", "📦"),
)

fun category(key: String) = CATEGORIES.firstOrNull { it.key == key } ?: CATEGORIES.last()

data class LocalExpense(val id: String, val date: LocalDate, val amountFen: Long, val category: String, val note: String?)

data class LocalEvent(
    val id: String,
    val title: String,
    val date: LocalDate,
    val startTime: String?, // "HH:MM", null = all day
    val endTime: String?,
    val location: String?,
    val deadline: Boolean,
    val leadDays: Int?, // deadlines: start reminding this many days before
    val done: Boolean,
)

data class PlanEntry(val text: String, val done: Boolean)

data class AppData(
    val region: Region? = null, // null until setup
    val budgetFen: Long = 0,
    val trackingStart: LocalDate = LocalDate.now(),
    val expenses: List<LocalExpense> = emptyList(),
    val events: List<LocalEvent> = emptyList(),
    val plans: Map<LocalDate, List<PlanEntry>> = emptyMap(),
) {
    val currency: String get() = region?.currency ?: "CNY"
}

/** The app's data, observable by Compose: every [update] redraws and saves. */
class Store(private val file: File) {
    var data by mutableStateOf(load())
        private set

    fun update(change: (AppData) -> AppData) {
        data = change(data)
        // Write to a temp file first, so a crash mid-save can't leave half a file.
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(toJson(data).toString())
        tmp.renameTo(file)
    }

    fun reset() = update { AppData() }

    private fun load(): AppData = runCatching { fromJson(JSONObject(file.readText())) }.getOrDefault(AppData())

    companion object {
        fun open(context: Context) = Store(File(context.filesDir, "pulse.json"))
    }
}

private fun JSONObject.str(key: String): String? = if (isNull(key) || !has(key)) null else optString(key).takeIf { it.isNotEmpty() }
private fun <T> JSONArray?.list(f: (JSONObject) -> T): List<T> = if (this == null) emptyList() else (0 until length()).map { f(getJSONObject(it)) }

private fun toJson(d: AppData): JSONObject = JSONObject()
    .put("version", 1)
    .put("region", d.region?.key)
    .put("budgetFen", d.budgetFen)
    .put("trackingStart", d.trackingStart.toString())
    .put("expenses", JSONArray(d.expenses.map {
        JSONObject().put("id", it.id).put("date", it.date.toString()).put("amountFen", it.amountFen).put("category", it.category).put("note", it.note)
    }))
    .put("events", JSONArray(d.events.map {
        JSONObject().put("id", it.id).put("title", it.title).put("date", it.date.toString()).put("startTime", it.startTime).put("endTime", it.endTime)
            .put("location", it.location).put("deadline", it.deadline).put("leadDays", it.leadDays).put("done", it.done)
    }))
    .put("plans", JSONObject().apply {
        d.plans.forEach { (date, items) -> put(date.toString(), JSONArray(items.map { JSONObject().put("text", it.text).put("done", it.done) })) }
    })

private fun fromJson(o: JSONObject): AppData = AppData(
    region = Region.fromKey(o.str("region")),
    budgetFen = o.optLong("budgetFen"),
    trackingStart = o.str("trackingStart")?.let(LocalDate::parse) ?: LocalDate.now(),
    expenses = o.optJSONArray("expenses").list { LocalExpense(it.getString("id"), LocalDate.parse(it.getString("date")), it.getLong("amountFen"), it.getString("category"), it.str("note")) },
    events = o.optJSONArray("events").list {
        LocalEvent(
            id = it.getString("id"),
            title = it.getString("title"),
            date = LocalDate.parse(it.getString("date")),
            startTime = it.str("startTime"),
            endTime = it.str("endTime"),
            location = it.str("location"),
            deadline = it.optBoolean("deadline"),
            leadDays = if (it.isNull("leadDays") || !it.has("leadDays")) null else it.getInt("leadDays"),
            done = it.optBoolean("done"),
        )
    },
    plans = o.optJSONObject("plans")?.let { p ->
        p.keys().asSequence().associate { key -> LocalDate.parse(key) to p.optJSONArray(key).list { PlanEntry(it.getString("text"), it.getBoolean("done")) } }
    } ?: emptyMap(),
)

/** Official holidays built into the app (assets/holidays/<region>-<year>.json). */
object Holidays {
    private val cache = mutableMapOf<String, List<HolidayDay>>()

    fun load(context: Context, region: Region, years: Collection<Int>): List<HolidayDay> = years.flatMap { year ->
        cache.getOrPut("${region.key}-$year") {
            runCatching {
                val text = context.assets.open("holidays/${region.key}-$year.json").bufferedReader().use { it.readText() }
                JSONArray(text).list { HolidayDay(LocalDate.parse(it.getString("date")), it.getString("name"), it.getBoolean("off")) }
            }.getOrDefault(emptyList()) // that year isn't bundled yet
        }
    }
}
