package com.dapid.pulse

import com.dapid.pulse.data.BudgetStatus
import com.dapid.pulse.data.LocalExpense
import com.dapid.pulse.data.budgetStatus
import com.dapid.pulse.data.computeMonthBudget
import com.dapid.pulse.data.dailyAllowanceFen
import com.dapid.pulse.data.heatLevel
import com.dapid.pulse.data.roundDiv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

// Same cases as the full product's budget.test.ts, where they apply.
class BudgetTest {
    private val nov = YearMonth.of(2026, 11)
    private fun spend(day: Int, fen: Long) = LocalExpense("e$day$fen", nov.atDay(day), fen, "food", null)

    @Test
    fun safeDailyIsWhatsLeftBeforeTodaySpreadOverTheDaysLeft() {
        val b = computeMonthBudget(nov, 150000, LocalDate.of(2026, 11, 3), nov.atDay(1), listOf(spend(1, 3000), spend(2, 2000), spend(3, 500)))
        assertEquals(5500L, b.spentFen)
        assertEquals(144500L, b.remainingFen)
        assertEquals(500L, b.spentTodayFen)
        assertEquals(28, b.remainingDays)
        assertEquals((150000L - 5000) / 28, b.safeDailyFen) // rounded down: following it never goes over
        assertEquals(BudgetStatus.OnTrack, b.status)
    }

    @Test
    fun paceAndOverBudget() {
        assertEquals(BudgetStatus.AbovePace, budgetStatus(150000, 20000, elapsedDays = 3, trackedDays = 30))
        assertEquals(BudgetStatus.OnTrack, budgetStatus(150000, 15000, elapsedDays = 3, trackedDays = 30))
        assertEquals(BudgetStatus.OverBudget, budgetStatus(150000, 150001, elapsedDays = 30, trackedDays = 30))
        assertEquals(BudgetStatus.NoBudget, budgetStatus(0, 100, elapsedDays = 3, trackedDays = 30))
    }

    @Test
    fun pastMonthsHaveNoDailyNumbers() {
        val b = computeMonthBudget(YearMonth.of(2026, 10), 150000, LocalDate.of(2026, 11, 3), LocalDate.of(2026, 10, 1), emptyList())
        assertEquals(31, b.elapsedDays)
        assertNull(b.safeDailyFen)
    }

    @Test
    fun trackingThatStartsMidMonthCountsOnlyTrackedDays() {
        val b = computeMonthBudget(nov, 150000, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 16), emptyList())
        assertEquals(15, b.trackedDays)
        assertEquals(5, b.elapsedDays)
        assertEquals(10000L, dailyAllowanceFen(nov, 150000, LocalDate.of(2026, 11, 16)))
    }

    @Test
    fun heatComparesWithTheDailyAllowance() {
        assertEquals(0, heatLevel(0, 4838))
        assertEquals(1, heatLevel(2000, 4838))
        assertEquals(2, heatLevel(4838, 4838))
        assertEquals(3, heatLevel(7000, 4838))
        assertEquals(4, heatLevel(9000, 4838))
        assertEquals(5, heatLevel(20000, 4838))
        assertEquals(2, heatLevel(100, 0))
    }

    @Test
    fun roundDivRoundsHalfAwayFromZero() {
        assertEquals(3L, roundDiv(5, 2))
        assertEquals(-3L, roundDiv(-5, 2))
        assertEquals(2L, roundDiv(7, 4))
    }
}
