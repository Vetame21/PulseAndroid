package com.dapid.pulse.data

import org.json.JSONObject
import java.time.LocalDate

data class AccountSettings(val onboarded: Boolean, val region: Region, val homeCurrency: String)

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
                ),
            )
            is ApiResult.Error -> r
        }

    suspend fun setup(region: Region, budget: String): ApiResult<Unit> =
        when (val r = authed("POST", "/setup", JSONObject().put("region", region.key).put("budget", budget.trim()))) {
            is ApiResult.Ok -> ApiResult.Ok(Unit)
            is ApiResult.Error -> r
        }

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
