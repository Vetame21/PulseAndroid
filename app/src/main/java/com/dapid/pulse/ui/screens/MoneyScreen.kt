package com.dapid.pulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapid.pulse.data.AccountSettings
import com.dapid.pulse.data.Expense
import com.dapid.pulse.data.Money
import com.dapid.pulse.data.PulseClient
import com.dapid.pulse.data.SpendingDay
import com.dapid.pulse.data.dayLabel
import com.dapid.pulse.data.formatMoney
import com.dapid.pulse.data.monthTitle
import com.dapid.pulse.ui.components.BudgetBar
import com.dapid.pulse.ui.components.Loading
import com.dapid.pulse.ui.components.PageHeader
import com.dapid.pulse.ui.components.PulseCard
import com.dapid.pulse.ui.components.SectionLabel
import com.dapid.pulse.ui.theme.Heat
import java.time.LocalDate

private val STATUS = mapOf(
    "on_track" to "On track",
    "above_pace" to "Spending faster than planned",
    "over_budget" to "Over budget",
    "no_budget" to "Tracking only (no budget this month)",
)

@Composable
fun MoneyScreen(padding: PaddingValues, client: PulseClient, settings: AccountSettings, reloadKey: Int, onSignedOut: () -> Unit) {
    Loading(padding, reloadKey, load = client::money, onSignedOut = onSignedOut) { m, _ ->
        val cur = settings.homeCurrency
        PageHeader(title = "Money", eyebrow = monthTitle(m.month))
        ThisMonth(m, cur)
        SpendingCalendar(m, settings)
        Transactions(m.expenses, settings, today = m.weeks.flatten().filterNotNull().lastOrNull { it.state == "data" }?.date ?: m.month)
    }
}

@Composable
private fun ThisMonth(m: Money, cur: String) {
    val b = m.budget
    PulseCard {
        SectionLabel("This month")
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                formatMoney(kotlin.math.abs(b.remainingFen), cur),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (b.remainingFen < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(if (b.remainingFen < 0) "over budget" else "left", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BudgetBar(b)
        Text(
            "${STATUS[b.status] ?: ""} · ${formatMoney(b.spentFen, cur)} of ${formatMoney(b.budgetFen, cur)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Each day from green to red against the daily allowance; tap one to see what you spent. */
@Composable
private fun SpendingCalendar(m: Money, settings: AccountSettings) {
    var selected by remember(m) { mutableStateOf<LocalDate?>(null) }
    PulseCard {
        SectionLabel("Spending calendar")
        Text("Allowance ${formatMoney(m.allowanceFen, settings.homeCurrency)}/day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        WeekdayHeader()
        m.weeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { d ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (d != null) HeatCell(d, selected == d.date) { selected = if (selected == d.date) null else d.date }
                    }
                }
            }
        }
        selected?.let { day ->
            val list = m.expenses.filter { it.date == day }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(dayLabel(day, LocalDate.now()), style = MaterialTheme.typography.labelLarge)
            if (list.isEmpty()) Text("Nothing spent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            list.forEach { ExpenseRow(it, settings) }
        }
    }
}

@Composable
fun WeekdayHeader() {
    Row {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
            Text(it, Modifier.weight(1f), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun HeatCell(d: SpendingDay, selected: Boolean, onClick: () -> Unit) {
    val bg = when {
        d.state != "data" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        d.level == 0 -> MaterialTheme.colorScheme.surfaceVariant
        else -> Heat.levels[d.level - 1]
    }
    val ink = if (d.level >= 4) Heat.inkHigh else if (d.level >= 1) Heat.inkLow else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(enabled = d.state == "data", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("${d.date.dayOfMonth}", fontSize = 12.sp, color = ink)
    }
}

@Composable
private fun Transactions(list: List<Expense>, settings: AccountSettings, today: LocalDate) {
    PulseCard {
        SectionLabel("Transactions")
        if (list.isEmpty()) Text("No spending yet this month. Tap + to add one.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        list.forEachIndexed { i, e ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column {
                ExpenseRow(e, settings)
                Text(dayLabel(e.date, today), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExpenseRow(e: Expense, settings: AccountSettings) {
    val c = settings.categories.firstOrNull { it.key == e.category }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${c?.emoji ?: "📦"} ${e.note ?: c?.label ?: e.category}${if (!e.fromBudget) " · not in budget" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatMoney(if (e.refund) -e.amountFen else e.amountFen, settings.homeCurrency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (e.refund) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
