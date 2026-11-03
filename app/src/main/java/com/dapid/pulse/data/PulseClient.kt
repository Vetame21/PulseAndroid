package com.dapid.pulse.data

import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

data class AccountSettings(
    val onboarded: Boolean,
    val region: Region,
    val homeCurrency: String, // CNY or HKD
    val timezone: String = "Asia/Shanghai",
    val categories: List<Category> = emptyList(),
)

/** The app's calls, with the token handled: a 401 refreshes once, then asks you to sign in again. */
class PulseClient(private val api: PulseApi, private val session: Session) {
    val signedIn: Boolean get() = session.signedIn

    suspend fun signIn(email: String, password: String): ApiResult<Unit> =
        when (val r = api.call("POST", "/auth/sign-in", body = JSONObject().put("email", email.trim()).put("password", password))) {
            is ApiResult.Ok -> ApiResult.Ok(saveTokens(r.value))
            is ApiResult.Error -> r
        }

    suspend fun signOut() {
        session.accessToken?.let { api.call("POST", "/auth/sign-out", token = it) }
        session.clear()
    }

    suspend fun settings(): ApiResult<AccountSettings> =
        when (val r = authed("GET", "/settings")) {
            is ApiResult.Ok -> ApiResult.Ok(
                AccountSettings(
                    onboarded = r.value.optBoolean("onboarded"),
                    region = Region.fromKey(r.value.optString("region")) ?: Region.Mainland,
                    homeCurrency = r.value.optString("homeCurrency", "CNY"),
                    timezone = r.value.optString("timezone", "Asia/Shanghai"),
                    categories = parseCategories(r.value.optJSONArray("categories")),
                ),
            )
            is ApiResult.Error -> r
        }

    suspend fun setup(region: Region, budget: String): ApiResult<Unit> =
        when (val r = authed("POST", "/setup", JSONObject().put("region", region.key).put("budget", budget.trim()))) {
            is ApiResult.Ok -> ApiResult.Ok(Unit)
            is ApiResult.Error -> r
        }

    suspend fun today(): ApiResult<Today> = authed("GET", "/today").map(::parseToday)

    suspend fun tick(date: LocalDate, index: Int, done: Boolean): ApiResult<Unit> =
        authed("POST", "/day-plan/tick", JSONObject().put("date", date.toString()).put("index", index).put("done", done)).map { }

    /** This month's budget, expenses and spending calendar (two calls, one screen). */
    suspend fun money(): ApiResult<Money> {
        val money = authed("GET", "/money")
        if (money !is ApiResult.Ok) return money as ApiResult.Error
        return authed("GET", "/money/calendar").map { parseMoney(money.value, it) }
    }

    /** [id] is made once per form, so tapping Save again after a timeout saves it once. */
    suspend fun addExpense(id: String, amount: String, category: String, date: LocalDate, note: String): ApiResult<String> =
        authed(
            "POST", "/expenses",
            JSONObject().put("id", id).put("amount", amount.trim()).put("category", category).put("date", date.toString()).put("note", note.trim()),
        ).map { it.optString("message", "Saved.") }

    suspend fun calendar(month: YearMonth): ApiResult<CalendarMonth> = authed("GET", "/calendar?month=$month").map(::parseCalendar)

    suspend fun addEvent(body: JSONObject): ApiResult<Unit> = authed("POST", "/events", body).map { }

    /** Public holidays (no sign-in): this year's, plus next year's so late December still has a "next". */
    suspend fun holidays(region: Region, today: LocalDate): ApiResult<List<HolidayDay>> {
        val days = mutableListOf<HolidayDay>()
        for (year in listOf(today.year, today.year + 1)) {
            when (val r = api.call("GET", "/holidays?region=${region.key}&year=$year")) {
                is ApiResult.Ok -> {
                    val list = r.value.optJSONArray("days") ?: continue
                    for (i in 0 until list.length()) {
                        val d = list.getJSONObject(i)
                        days += HolidayDay(LocalDate.parse(d.getString("date")), d.getString("name"), d.getBoolean("off"))
                    }
                }
                is ApiResult.Error -> if (year == today.year) return r
            }
        }
        return ApiResult.Ok(days)
    }

    private suspend fun authed(method: String, path: String, body: JSONObject? = null): ApiResult<JSONObject> {
        val first = api.call(method, path, session.accessToken, body)
        if (first !is ApiResult.Error || first.status != 401) return first
        val refresh = session.refreshToken ?: return first
        return when (val r = api.call("POST", "/auth/refresh", body = JSONObject().put("refreshToken", refresh))) {
            is ApiResult.Ok -> {
                saveTokens(r.value)
                api.call(method, path, session.accessToken, body)
            }
            is ApiResult.Error -> {
                // 400/401: the refresh token is used up or revoked. Offline (0/503): keep it for later.
                if (r.status != 401 && r.status != 400) return r
                session.clear()
                ApiResult.Error(401, "Sign in again.")
            }
        }
    }

    private fun saveTokens(json: JSONObject) = session.save(json.getString("accessToken"), json.getString("refreshToken"))
}

/** Turn an Ok JSON answer into a model; a reading bug becomes an error instead of a crash. */
private inline fun <T> ApiResult<JSONObject>.map(f: (JSONObject) -> T): ApiResult<T> = when (this) {
    is ApiResult.Ok -> runCatching { ApiResult.Ok(f(value)) }.getOrElse { ApiResult.Error(-1, "Pulse sent something this app can't read. Update the app.") }
    is ApiResult.Error -> this
}
