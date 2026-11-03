package com.dapid.pulse.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// The monthly budget: spending, safe-to-spend and status. Same rules as the full
// product's budget.ts (pure, unit tested). Amounts are Long cents, never floats.

enum class BudgetStatus(val text: String) {
    OnTrack("On track"),
    AbovePace("Spending faster than planned"),
    OverBudget("Over budget"),
    NoBudget("Tracking only (no budget)"),
}

data class MonthBudget(
    val month: YearMonth,
    val budgetFen: Long,
    val trackedDays: Int,
    val elapsedDays: Int,
    val spentFen: Long,
    val remainingFen: Long, // negative when over budget
    val progress: Double, // spent ÷ budget, 0 without a budget
    val spentTodayFen: Long?, // the next three only for the current month
    val safeDailyFen: Long?, // stays the same all day
    val remainingDays: Int?,
    val status: BudgetStatus,
)

/** Divide, rounding half away from zero. */
fun roundDiv(n: Long, d: Long): Long {
    require(d != 0L) { "Division by zero" }
    val sign = java.lang.Long.signum(n) * java.lang.Long.signum(d)
    return sign * ((2 * kotlin.math.abs(n) + kotlin.math.abs(d)) / (2 * kotlin.math.abs(d)))
}

private fun daysInclusive(a: LocalDate, b: LocalDate) = ChronoUnit.DAYS.between(a, b).toInt() + 1

/** Spending in a month (before [before], if given). */
fun spentInMonth(expenses: List<LocalExpense>, month: YearMonth, before: LocalDate? = null): Long =
    expenses.filter { YearMonth.from(it.date) == month && (before == null || it.date < before) }.sumOf { it.amountFen }

/**
 * 🟢 on track: at or below the pace line (budget × elapsed ÷ tracked days) ·
 * 🟡 above pace: over the line but within budget · 🔴 over budget.
 */
fun budgetStatus(budgetFen: Long, spentFen: Long, elapsedDays: Int, trackedDays: Int): BudgetStatus = when {
    budgetFen <= 0 -> BudgetStatus.NoBudget
    spentFen > budgetFen -> BudgetStatus.OverBudget
    trackedDays == 0 -> BudgetStatus.OnTrack
    spentFen * trackedDays <= budgetFen * elapsedDays -> BudgetStatus.OnTrack
    else -> BudgetStatus.AbovePace
}

fun computeMonthBudget(month: YearMonth, budgetFen: Long, today: LocalDate, trackingStart: LocalDate, expenses: List<LocalExpense>): MonthBudget {
    val start = month.atDay(1)
    val end = month.atEndOfMonth()
    val firstTracked = maxOf(start, trackingStart)
    val trackedDays = if (firstTracked > end) 0 else daysInclusive(firstTracked, end)
    val current = today in start..end
    val elapsedDays = when {
        today > end -> trackedDays
        today < start || today < firstTracked -> 0
        else -> daysInclusive(firstTracked, today)
    }
    val spent = spentInMonth(expenses, month)

    var spentToday: Long? = null
    var safeDaily: Long? = null
    var remainingDays: Int? = null
    if (current && elapsedDays > 0) {
        remainingDays = daysInclusive(today, end)
        val beforeToday = spentInMonth(expenses, month, today)
        spentToday = spent - beforeToday
        // Rounded down so following it can never push you over.
        safeDaily = maxOf(0L, budgetFen - beforeToday) / remainingDays
    }
    return MonthBudget(
        month = month,
        budgetFen = budgetFen,
        trackedDays = trackedDays,
        elapsedDays = elapsedDays,
        spentFen = spent,
        remainingFen = budgetFen - spent,
        progress = if (budgetFen > 0) spent.toDouble() / budgetFen else 0.0,
        spentTodayFen = spentToday,
        safeDailyFen = safeDaily,
        remainingDays = remainingDays,
        status = budgetStatus(budgetFen, spent, elapsedDays, trackedDays),
    )
}

/** Daily allowance for the spending calendar: the budget over the month's tracked days. */
fun dailyAllowanceFen(month: YearMonth, budgetFen: Long, trackingStart: LocalDate): Long {
    val first = maxOf(month.atDay(1), trackingStart)
    val tracked = if (first > month.atEndOfMonth()) 0 else daysInclusive(first, month.atEndOfMonth())
    return if (budgetFen > 0 && tracked > 0) budgetFen / tracked else 0
}

/** 0 nothing spent · 1 ≤ half the allowance · 2 ≤ allowance · 3 ≤ 1.5× · 4 ≤ 2× · 5 more. */
fun heatLevel(spentFen: Long, allowanceFen: Long): Int {
    if (spentFen <= 0) return 0
    if (allowanceFen <= 0) return 2
    val r = spentFen.toDouble() / allowanceFen
    return when {
        r <= 0.5 -> 1
        r <= 1.0 -> 2
        r <= 1.5 -> 3
        r <= 2.0 -> 4
        else -> 5
    }
}
