package com.dapid.pulse.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapid.pulse.data.LocalExpense
import com.dapid.pulse.data.Region
import com.dapid.pulse.data.Store
import com.dapid.pulse.data.category
import com.dapid.pulse.data.computeMonthBudget
import com.dapid.pulse.data.dailyAllowanceFen
import com.dapid.pulse.data.dayLabel
import com.dapid.pulse.data.formatMoney
import com.dapid.pulse.data.heatLevel
import com.dapid.pulse.data.monthGrid
import com.dapid.pulse.data.monthTitle
import com.dapid.pulse.ui.components.BudgetBar
import com.dapid.pulse.ui.components.PageHeader
import com.dapid.pulse.ui.components.PulseCard
import com.dapid.pulse.ui.components.ScreenColumn
import com.dapid.pulse.ui.components.SectionLabel
import com.dapid.pulse.ui.theme.Heat
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MoneyScreen(padding: PaddingValues, store: Store) {
    val d = store.data
    val today = (d.region ?: Region.Mainland).today()
    val month = YearMonth.from(today)
    val monthExpenses = d.expenses.filter { YearMonth.from(it.date) == month }.sortedWith(compareByDescending<LocalExpense> { it.date })
    val b = computeMonthBudget(month, d.budgetFen, today, d.trackingStart, d.expenses)
    var deleting by remember { mutableStateOf<LocalExpense?>(null) }

    ScreenColumn(padding) {
        PageHeader(title = "Money", eyebrow = monthTitle(today))
        PulseCard {
            SectionLabel("This month")
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    formatMoney(kotlin.math.abs(b.remainingFen), d.currency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (b.remainingFen < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text(if (b.remainingFen < 0) "over budget" else "left", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BudgetBar(b)
            Text(
                "${b.status.text} · ${formatMoney(b.spentFen, d.currency)} of ${formatMoney(b.budgetFen, d.currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SpendingCalendar(month, today, d.trackingStart, dailyAllowanceFen(month, d.budgetFen, d.trackingStart), monthExpenses, d.currency, onLongPress = { deleting = it })
        PulseCard {
            SectionLabel("Transactions")
            if (monthExpenses.isEmpty()) Text("No spending yet this month. Tap + to add one.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            monthExpenses.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column {
                    ExpenseRow(e, d.currency, onLongPress = { deleting = e })
                    Text(dayLabel(e.date, today), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (monthExpenses.isNotEmpty()) Text("Press and hold one to delete it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    deleting?.let { e ->
        val c = category(e.category)
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this expense?") },
            text = { Text("${c.emoji} ${e.note ?: c.label} · ${formatMoney(e.amountFen, d.currency)} · ${dayLabel(e.date, today)}") },
            confirmButton = {
                TextButton(onClick = {
                    store.update { s -> s.copy(expenses = s.expenses.filter { it.id != e.id }) }
                    deleting = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } },
        )
    }
}

/** Each day from green to red against the daily allowance; tap one to see what you spent. */
@Composable
private fun SpendingCalendar(
    month: YearMonth,
    today: LocalDate,
    trackingStart: LocalDate,
    allowanceFen: Long,
    expenses: List<LocalExpense>,
    currency: String,
    onLongPress: (LocalExpense) -> Unit,
) {
    var selected by remember(month) { mutableStateOf<LocalDate?>(null) }
    PulseCard {
        SectionLabel("Spending calendar")
        Text("Allowance ${formatMoney(allowanceFen, currency)}/day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        WeekdayHeader()
        monthGrid(month).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (date != null) {
                            val tracked = date >= trackingStart && date <= today
                            val spent = expenses.filter { it.date == date }.sumOf { it.amountFen }
                            HeatCell(date.dayOfMonth, if (tracked) heatLevel(spent, allowanceFen) else -1, selected == date) {
                                if (tracked) selected = if (selected == date) null else date
                            }
                        }
                    }
                }
            }
        }
        selected?.let { day ->
            val list = expenses.filter { it.date == day }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("${dayLabel(day, today)} · ${formatMoney(list.sumOf { it.amountFen }, currency)}", style = MaterialTheme.typography.labelLarge)
            if (list.isEmpty()) Text("Nothing spent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            list.forEach { ExpenseRow(it, currency, onLongPress = { onLongPress(it) }) }
        }
    }
}

@Composable
fun WeekdayHeader() {
    Row {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
            Text(it, Modifier.weight(1f), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** level -1 = not tracked (before tracking started or in the future). */
@Composable
private fun HeatCell(day: Int, level: Int, selected: Boolean, onClick: () -> Unit) {
    val bg = when {
        level < 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        level == 0 -> MaterialTheme.colorScheme.surfaceVariant
        else -> Heat.levels[level - 1]
    }
    val ink = if (level >= 4) Heat.inkHigh else if (level >= 1) Heat.inkLow else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("$day", fontSize = 12.sp, color = ink) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpenseRow(e: LocalExpense, currency: String, onLongPress: () -> Unit) {
    val c = category(e.category)
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongPress).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${c.emoji} ${e.note ?: c.label}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(formatMoney(e.amountFen, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
