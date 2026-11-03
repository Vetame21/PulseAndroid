package com.dapid.pulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dapid.pulse.data.AppData
import com.dapid.pulse.data.Holidays
import com.dapid.pulse.data.PlanEntry
import com.dapid.pulse.data.Region
import com.dapid.pulse.data.Store
import com.dapid.pulse.data.activeDeadlines
import com.dapid.pulse.data.computeMonthBudget
import com.dapid.pulse.data.dayLabel
import com.dapid.pulse.data.deadlineText
import com.dapid.pulse.data.formatMoney
import com.dapid.pulse.data.holidayThisWeek
import com.dapid.pulse.data.longDay
import com.dapid.pulse.data.timeRange
import com.dapid.pulse.data.upNext
import com.dapid.pulse.ui.components.BudgetBar
import com.dapid.pulse.ui.components.PageHeader
import com.dapid.pulse.ui.components.PulseCard
import com.dapid.pulse.ui.components.ScreenColumn
import com.dapid.pulse.ui.components.SectionLabel
import java.time.LocalDate
import java.time.YearMonth

private fun greeting(hour: Int) = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    in 18..23 -> "Good evening"
    else -> "Still up?"
}

@Composable
fun TodayScreen(padding: PaddingValues, store: Store) {
    val d = store.data
    val region = d.region ?: Region.Mainland
    val today = region.today()
    val now = region.nowTime()
    ScreenColumn(padding) {
        PageHeader(title = longDay(today), eyebrow = greeting(now.take(2).toInt()))
        Briefing(store, d, region, today, now)
        Plan(store, d.plans[today].orEmpty(), today)
        MoneyCard(d, today)
    }
}

@Composable
private fun Briefing(store: Store, d: AppData, region: Region, today: LocalDate, now: String) {
    val holidays = Holidays.load(LocalContext.current, region, setOf(today.year, today.plusDays(7).year))
    val deadlines = activeDeadlines(d.events, today)
    val next = upNext(d.events, today, now)
    PulseCard {
        SectionLabel("Briefing")
        holidayThisWeek(holidays, today)?.let { h ->
            Note(
                if (h.off) "📅 ${dayLabel(h.date, today)}: ${h.name} holiday" else "💼 ${dayLabel(h.date, today)}: make-up workday for ${h.name} (调休)",
                if (h.off) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        if (deadlines.isNotEmpty()) {
            SectionLabel("Work on")
            deadlines.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = false, onCheckedChange = {
                        store.update { s -> s.copy(events = s.events.map { if (it.id == a.event.id) it.copy(done = true) else it }) }
                    })
                    Column {
                        Text(a.event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            deadlineText(a.event.date, a.daysLeft, a.event.startTime, today),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (a.daysLeft < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        SectionLabel("Up next")
        if (next.isEmpty()) Text("Nothing planned for the next 7 days. Tap + to add an event.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        next.forEach { (day, items) ->
            Text(dayLabel(day, today), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            items.forEach { e ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("📌 ${e.title}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        e.location?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Text(timeRange(e.startTime, e.endTime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Note(text: String, tone: Color) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().background(tone, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
}

/** Write your day, then tick it off. Tap an item to tick it; × removes it. */
@Composable
private fun Plan(store: Store, items: List<PlanEntry>, today: LocalDate) {
    var draft by rememberSaveable { mutableStateOf("") }
    fun setItems(change: (List<PlanEntry>) -> List<PlanEntry>) = store.update { s -> s.copy(plans = s.plans + (today to change(s.plans[today].orEmpty()))) }
    fun add() {
        val text = draft.trim()
        if (text.isEmpty()) return
        setItems { it + PlanEntry(text, false) }
        draft = ""
    }
    PulseCard {
        SectionLabel("Today's plan")
        items.forEachIndexed { i, item ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().clickable { setItems { list -> list.mapIndexed { j, e -> if (j == i) e.copy(done = !e.done) else e } } }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.done, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { setItems { list -> list.filterIndexed { j, _ -> j != i } } }) { Text("×") }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text(if (items.isEmpty()) "Write your day, then tick it off" else "Add another") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            trailingIcon = { TextButton(onClick = ::add, enabled = draft.isNotBlank()) { Text("Add") } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MoneyCard(d: AppData, today: LocalDate) {
    val b = computeMonthBudget(YearMonth.from(today), d.budgetFen, today, d.trackingStart, d.expenses)
    PulseCard {
        SectionLabel("Money")
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                formatMoney(kotlin.math.abs(b.remainingFen), d.currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (b.remainingFen < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(if (b.remainingFen < 0) "over budget" else "left this month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BudgetBar(b)
        Text(
            "${formatMoney(b.safeDailyFen ?: 0, d.currency)}/day safe · ${formatMoney(b.spentTodayFen ?: 0, d.currency)} spent today",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
