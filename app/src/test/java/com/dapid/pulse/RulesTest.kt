package com.dapid.pulse

import com.dapid.pulse.data.AccountSettings
import com.dapid.pulse.data.ApiResult
import com.dapid.pulse.data.HolidayDay
import com.dapid.pulse.data.Region
import com.dapid.pulse.data.Stage
import com.dapid.pulse.data.budgetProblem
import com.dapid.pulse.data.nextHoliday
import com.dapid.pulse.data.stageFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RulesTest {
    private val today = LocalDate.of(2026, 9, 30)
    private val days = listOf(
        HolidayDay(LocalDate.of(2026, 9, 27), "National Day", off = false), // 调休 workday
        HolidayDay(LocalDate.of(2026, 10, 1), "National Day", off = true),
        HolidayDay(LocalDate.of(2026, 10, 2), "National Day", off = true),
        HolidayDay(LocalDate.of(2026, 10, 10), "National Day", off = false),
    )

    @Test
    fun nextHolidaySkipsWorkdaysAndPastDays() {
        assertEquals(LocalDate.of(2026, 10, 1), nextHoliday(days, today)?.date)
        assertEquals(LocalDate.of(2026, 10, 2), nextHoliday(days, LocalDate.of(2026, 10, 2))?.date)
        assertNull(nextHoliday(days, LocalDate.of(2026, 10, 3)))
    }

    @Test
    fun budgetAcceptsPlainAmounts() {
        listOf("1500", "1500.50", "1500,5", " 800 ").forEach { assertNull(it, budgetProblem(it)) }
    }

    @Test
    fun budgetRejectsEmptyZeroAndJunk() {
        listOf("", "0", "0.00", "-5", "abc", "1.234", "1,500.00").forEach {
            assertEquals(it, "Enter your monthly budget, like 1500.", budgetProblem(it))
        }
    }

    @Test
    fun regionKeysMatchTheApi() {
        assertEquals(Region.HongKong, Region.fromKey("hk"))
        assertEquals(Region.Mainland, Region.fromKey("mainland"))
        assertNull(Region.fromKey("HK"))
    }

    @Test
    fun stageFollowsSignInAndSetup() {
        val setUp = ApiResult.Ok(AccountSettings(onboarded = true, region = Region.Mainland, homeCurrency = "CNY"))
        val fresh = ApiResult.Ok(AccountSettings(onboarded = false, region = Region.Mainland, homeCurrency = "CNY"))
        assertEquals(Stage.SignIn, stageFor(signedIn = false, settings = null))
        assertEquals(Stage.Loading, stageFor(signedIn = true, settings = null))
        assertEquals(Stage.Home, stageFor(true, setUp))
        assertEquals(Stage.Setup, stageFor(true, fresh))
        assertEquals(Stage.SignIn, stageFor(true, ApiResult.Error(401, "Sign in again.")))
        assertEquals(Stage.Offline, stageFor(true, ApiResult.Error(0, "offline")))
    }
}
