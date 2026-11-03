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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapid.pulse.data.HolidayDay
import com.dapid.pulse.data.Holidays
import com.dapid.pulse.data.LocalEvent
import com.dapid.pulse.data.Region
import com.dapid.pulse.data.Store
import com.dapid.pulse.data.eventsOn
import com.dapid.pulse.data.holidayFor
import com.dapid.pulse.data.holidayStart
import com.dapid.pulse.data.longDay
import com.dapid.pulse.data.lunarOf
import com.dapid.pulse.data.monthGrid
import com.dapid.pulse.data.monthTitle
import com.dapid.pulse.data.timeRange
import com.dapid.pulse.ui.PulseIcons
import com.dapid.pulse.ui.components.PulseCard
import com.dapid.pulse.ui.components.ScreenColumn
import com.dapid.pulse.ui.components.SectionLabel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(padding: PaddingValues, store: Store) {
    val region = store.data.region ?: Region.Mainland
    val today = region.today()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthText)
    var selected by remember(monthText) { mutableStateOf(if (month == YearMonth.from(today)) today else month.atDay(1)) }
    val holidays = Holidays.load(LocalContext.current, region, setOf(month.year))
    val events = store.data.events

    ScreenColumn(padding) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monthText = month.minusMonths(1).toString() }) { Icon(PulseIcons.ChevronLeft, contentDescription = "Previous month") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(monthTitle(month.atDay(1)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (month != YearMonth.from(today)) TextButton(onClick = { monthText = YearMonth.from(today).toString() }) { Text("Back to today") }
            }
            IconButton(onClick = { monthText = month.plusMonths(1).toString() }) { Icon(PulseIcons.ChevronRight, contentDescription = "Next month") }
        }
        PulseCard {
            WeekdayHeader()
            monthGrid(month).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEachIndexed { i, date ->
                        Box(Modifier.weight(1f).aspectRatio(0.8f)) {
                            if (date != null) DayCell(date, weekend = i >= 5, today = today, holidays = holidays, hasItems = eventsOn(events, date).isNotEmpty(), selected = date == selected) { selected = date }
                        }
                    }
                }
            }
            if (holidays.isEmpty()) {
                Text("Holidays for ${month.year} aren't in this version of the app yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DayDetails(selected, holidayFor(selected, holidays), eventsOn(events, selected), onDone = { e ->
            store.update { s -> s.copy(events = s.events.map { if (it.id == e.id) it.copy(done = !it.done) else it }) }
        }, onDelete = { e -> store.update { s -> s.copy(events = s.events.filter { it.id != e.id }) } })
    }
}

/** Red for official days off and weekends (unless it's a 调休 workday, marked 班), as on Chinese calendars. */
@Composable
private fun DayCell(date: LocalDate, weekend: Boolean, today: LocalDate, holidays: List<HolidayDay>, hasItems: Boolean, selected: Boolean, onClick: () -> Unit) {
    val h = holidayFor(date, holidays)
    val off = h?.off == true
    val workday = h?.off == false
    val red = off || (weekend && !workday)
    val lunar = lunarOf(date)
    val sub = when {
        holidayStart(date, holidays) -> h?.name
        workday -> "班"
        else -> lunar.festival ?: lunar.label
    }
    val isToday = date == today
    val numberColor = if (red) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
            Modifier.size(26.dp).clip(CircleShape).background(if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("${date.dayOfMonth}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isToday) MaterialTheme.colorScheme.onPrimary else numberColor)
        }
        Text(sub.orEmpty(), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Clip, textAlign = TextAlign.Center, color = if (workday || off) numberColor else MaterialTheme.colorScheme.onSurfaceVariant)
        if (hasItems) Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun DayDetails(date: LocalDate, holiday: HolidayDay?, events: List<LocalEvent>, onDone: (LocalEvent) -> Unit, onDelete: (LocalEvent) -> Unit) {
    val lunar = lunarOf(date)
    PulseCard {
        SectionLabel(longDay(date))
        Text("农历 ${lunar.full}${lunar.festival?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        holiday?.let {
            Text(
                if (it.off) "📅 ${it.name}: day off" else "💼 Make-up workday for ${it.name} (调休)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (it.off) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        events.forEach { e ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${if (e.deadline) (if (e.done) "✅" else "⏰") else "📌"} ${e.title}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        listOfNotNull(timeRange(e.startTime, e.endTime), e.location).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (e.deadline) TextButton(onClick = { onDone(e) }) { Text(if (e.done) "Undo" else "Done") }
                TextButton(onClick = { onDelete(e) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
        if (events.isEmpty() && holiday == null) Text("Nothing on this day. Tap + to add an event.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
