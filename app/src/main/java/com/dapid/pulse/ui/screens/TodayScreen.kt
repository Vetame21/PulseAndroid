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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dapid.pulse.data.AccountSettings
import com.dapid.pulse.data.ApiResult
import com.dapid.pulse.data.PulseClient
import com.dapid.pulse.data.Today
import com.dapid.pulse.data.dayLabel
import com.dapid.pulse.data.deadlineText
import com.dapid.pulse.data.formatMoney
import com.dapid.pulse.data.longDay
import com.dapid.pulse.data.timeRange
import com.dapid.pulse.ui.components.BudgetBar
import com.dapid.pulse.ui.components.Loading
import com.dapid.pulse.ui.components.PageHeader
import com.dapid.pulse.ui.components.PulseCard
import com.dapid.pulse.ui.components.SectionLabel
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(padding: PaddingValues, client: PulseClient, settings: AccountSettings, reloadKey: Int, onSignedOut: () -> Unit) {
    Loading(padding, reloadKey, load = client::today, onSignedOut = onSignedOut) { t, _ ->
        PageHeader(title = longDay(t.date), eyebrow = t.greeting)
        Briefing(t)
        Plan(t, client)
        MoneyCard(t, settings.homeCurrency)
    }
}

@Composable
private fun Briefing(t: Today) {
    val summary = buildString {
        append(if (t.classesToday == 0) "No classes today" else "${t.classesToday} class${if (t.classesToday > 1) "es" else ""} today")
        if (t.deadlines.isNotEmpty()) append(" · ${t.deadlines.size} deadline${if (t.deadlines.size > 1) "s" else ""} to work on")
    }
    PulseCard {
        SectionLabel("Briefing")
        Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        t.birthdays.forEach { b ->
            Note(
                if (b.inMemory) "🕯️ ${dayLabel(b.date, t.date)}: remembering ${b.name}"
                else "🎂 ${dayLabel(b.date, t.date)}: ${b.name}'s birthday${b.turns?.let { " ($it)" } ?: ""}${if (b.inDays == 0) " 🎉" else ""}",
                tone = if (b.inDays == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        t.holiday?.let { h ->
            Note(
                if (h.off) "📅 ${dayLabel(h.date, t.date)}: ${h.name} holiday" else "💼 ${dayLabel(h.date, t.date)}: make-up workday for ${h.name} (调休)",
                tone = if (h.off) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        if (t.deadlines.isNotEmpty()) {
            SectionLabel("Work on")
            t.deadlines.forEach { d ->
                Column {
                    Text(d.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        deadlineText(d.date, d.daysLeft, d.startTime, t.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (d.daysLeft < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        SectionLabel("Up next")
        if (t.upNext.isEmpty()) {
            Text("Nothing planned for the next 7 days.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        t.upNext.forEach { (day, items) ->
            Text(dayLabel(day, t.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            items.forEach { e ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${if (e.id.startsWith("class:")) "🎓" else "📌"} ${e.title}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        e.location?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Text(timeRange(e.startTime, e.endTime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Note(text: String, tone: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().background(tone, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Tap to tick: the box changes at once, and goes back if the save fails. */
@Composable
private fun Plan(t: Today, client: PulseClient) {
    val items = remember(t) { mutableStateListOf(*t.plan.toTypedArray()) }
    val scope = rememberCoroutineScope()
    PulseCard {
        SectionLabel("Today's plan")
        if (items.isEmpty()) {
            Text("No plan for today yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items.forEachIndexed { i, item ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().clickable {
                    val done = !item.done
                    items[i] = item.copy(done = done)
                    scope.launch { if (client.tick(t.date, i, done) !is ApiResult.Ok) items[i] = item }
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = item.done, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun MoneyCard(t: Today, currency: String) {
    val b = t.budget
    PulseCard {
        SectionLabel("Money")
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                formatMoney(kotlin.math.abs(b.remainingFen), currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (b.remainingFen < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(if (b.remainingFen < 0) "over budget" else "left this month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BudgetBar(b)
        Text(
            "${formatMoney(b.safeDailyFen ?: 0, currency)}/day safe · ${formatMoney(b.spentTodayFen ?: 0, currency)} spent today",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
