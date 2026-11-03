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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapid.pulse.data.CalendarDay
import com.dapid.pulse.data.CalendarMonth
import com.dapid.pulse.data.PulseClient
import com.dapid.pulse.data.longDay
import com.dapid.pulse.data.monthTitle
import com.dapid.pulse.data.timeRange
import com.dapid.pulse.ui.PulseIcons
import com.dapid.pulse.ui.components.Loading
import com.dapid.pulse.ui.components.PulseCard
import com.dapid.pulse.ui.components.SectionLabel
import java.time.YearMonth

@Composable
fun CalendarScreen(padding: PaddingValues, client: PulseClient, reloadKey: Int, onSignedOut: () -> Unit) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val ym = YearMonth.parse(month)
    Loading(padding, ym to reloadKey, load = { client.calendar(ym) }, onSignedOut = onSignedOut) { cal, _ ->
        MonthHeader(cal, onMove = { month = ym.plusMonths(it.toLong()).toString() }, onToday = { month = YearMonth.now().toString() })
        MonthGrid(cal)
    }
}

@Composable
private fun MonthHeader(cal: CalendarMonth, onMove: (Int) -> Unit, onToday: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onMove(-1) }) { Icon(PulseIcons.ChevronLeft, contentDescription = "Previous month") }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(monthTitle(cal.month), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (YearMonth.from(cal.month) != YearMonth.now()) {
                Text("Back to today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onToday))
            }
        }
        IconButton(onClick = { onMove(1) }) { Icon(PulseIcons.ChevronRight, contentDescription = "Next month") }
    }
}

@Composable
private fun MonthGrid(cal: CalendarMonth) {
    val days = cal.weeks.flatten().filterNotNull()
    var selected by remember(cal) { mutableStateOf(days.firstOrNull { it.isToday }?.date ?: days.firstOrNull()?.date) }
    PulseCard {
        WeekdayHeader()
        cal.weeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { d ->
                    Box(Modifier.weight(1f).aspectRatio(0.8f)) {
                        if (d != null) DayCell(d, d.date == selected) { selected = d.date }
                    }
                }
            }
        }
        if (!cal.holidaysPublished) {
            Text(
                "This year's holiday schedule isn't out yet. It'll appear here automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    days.firstOrNull { it.date == selected }?.let { DayDetails(it) }
}

/** Red for official days off and weekends (unless it's a 调休 workday), as on Chinese calendars. */
@Composable
private fun DayCell(d: CalendarDay, selected: Boolean, onClick: () -> Unit) {
    val off = d.holiday?.off == true
    val workday = d.holiday?.off == false
    val red = off || (d.weekend && !workday)
    val numberColor = if (red) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val sub = when {
        d.holidayStart -> d.holiday?.name
        workday -> "班"
        d.festival != null -> d.festival
        else -> d.lunar
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (off) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (d.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("${d.dayNumber}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (d.isToday) MaterialTheme.colorScheme.onPrimary else numberColor)
        }
        Text(
            sub.orEmpty(),
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            color = if (workday || off) numberColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val dots = d.events.size + d.classes.size + d.birthdays.size
        if (dots > 0) Box(Modifier.padding(top = 2.dp).size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun DayDetails(d: CalendarDay) {
    PulseCard {
        SectionLabel(longDay(d.date))
        Text(d.lunarFull(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        d.holiday?.let {
            Text(
                if (it.off) "📅 ${it.name}: day off" else "💼 Make-up workday for ${it.name} (调休)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (it.off) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        d.birthdays.forEach { b ->
            Text(if (b.inMemory) "🕯️ Remembering ${b.name}" else "🎂 ${b.name}'s birthday${b.turns?.let { " ($it)" } ?: ""}", style = MaterialTheme.typography.bodyMedium)
        }
        d.classes.forEach { c ->
            Line("🎓 ${c.title}${c.room?.let { " · $it" } ?: ""}", "${c.startTime}–${c.endTime}")
        }
        d.events.forEach { e ->
            Line("${if (e.deadline) (if (e.done) "✅" else "⏰") else "📌"} ${e.title}", timeRange(e.startTime, e.endTime))
        }
        if (d.events.isEmpty() && d.classes.isEmpty() && d.birthdays.isEmpty() && d.holiday == null) {
            Text("Nothing on this day.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Line(title: String, time: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun CalendarDay.lunarFull() = festival?.let { "$lunar · $it" } ?: lunar
