package com.dapid.pulse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dapid.pulse.data.CATEGORIES
import com.dapid.pulse.data.LocalEvent
import com.dapid.pulse.data.LocalExpense
import com.dapid.pulse.data.Region
import com.dapid.pulse.data.Store
import com.dapid.pulse.data.category
import com.dapid.pulse.data.currencySymbol
import com.dapid.pulse.data.dayLabel
import com.dapid.pulse.data.formatMoney
import com.dapid.pulse.data.isTime
import com.dapid.pulse.data.parseAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

private const val MAX_AMOUNT_FEN = 100_000_000L // ¥1,000,000: anything larger is surely a typo

/** Quick add: amount first (number pad), a category chip, optional note. Today by default. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseForm(store: Store, onSaved: (String) -> Unit) {
    val currency = store.data.currency
    val today = remember { (store.data.region ?: Region.Mainland).today() }
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryKey by rememberSaveable { mutableStateOf(CATEGORIES.first().key) }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(today.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    FormColumn("New expense") {
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            prefix = { Text(currencySymbol(currency)) },
            placeholder = { Text("32.50") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CATEGORIES.forEach { c ->
                FilterChip(selected = c.key == categoryKey, onClick = { categoryKey = c.key }, label = { Text("${c.emoji} ${c.label}") })
            }
        }
        OutlinedTextField(value = note, onValueChange = { note = it.take(200) }, label = { Text("Note (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        DateField(LocalDate.parse(date), today, maxToday = true) { date = it.toString() }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        Button(
            onClick = {
                val cents = parseAmount(amount)
                if (cents == null || cents > MAX_AMOUNT_FEN) {
                    error = "Enter an amount like 32 or 32.50."
                    return@Button
                }
                val day = LocalDate.parse(date)
                store.update { it.copy(expenses = it.expenses + LocalExpense(UUID.randomUUID().toString(), day, cents, categoryKey, note.trim().ifEmpty { null })) }
                val c = category(categoryKey)
                onSaved("Saved ${formatMoney(cents, currency)} · ${c.emoji} ${c.label} · ${dayLabel(day, today)}")
            },
            enabled = amount.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Save") }
    }
}

/** A new event (class, exam, dinner…) or deadline (homework, submission). */
@Composable
fun EventForm(store: Store, deadline: Boolean, onSaved: (String) -> Unit) {
    val today = remember { (store.data.region ?: Region.Mainland).today() }
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(today.toString()) }
    var allDay by rememberSaveable { mutableStateOf(deadline) }
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var leadDays by rememberSaveable { mutableIntStateOf(7) }
    var error by remember { mutableStateOf<String?>(null) }

    fun problem(): String? = when {
        title.isBlank() -> "Give it a title."
        !allDay && !isTime(start) -> "Use a time like 14:30."
        !allDay && !deadline && end.isNotBlank() && (!isTime(end) || end <= start) -> "The end time should be after the start, like 16:00."
        else -> null
    }

    FormColumn(if (deadline) "New deadline" else "New event") {
        OutlinedTextField(value = title, onValueChange = { title = it.take(200) }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        DateField(LocalDate.parse(date), today, maxToday = false) { date = it.toString() }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(checked = !allDay, onCheckedChange = { allDay = !it })
            Text(if (deadline) "Due at a time" else "At a time", style = MaterialTheme.typography.bodyMedium)
        }
        if (!allDay) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField(start, if (deadline) "Due" else "Start", Modifier.weight(1f)) { start = it }
                if (!deadline) TimeField(end, "End (optional)", Modifier.weight(1f)) { end = it }
            }
        }
        if (deadline) {
            Text("Start reminding me", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 7, 14).forEach { n ->
                    FilterChip(selected = leadDays == n, onClick = { leadDays = n }, label = { Text(if (n == 1) "1 day before" else "$n days") })
                }
            }
        } else {
            OutlinedTextField(value = location, onValueChange = { location = it.take(200) }, label = { Text("Place (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        Button(
            onClick = {
                problem()?.let { error = it; return@Button }
                val event = LocalEvent(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    date = LocalDate.parse(date),
                    startTime = if (allDay) null else start,
                    endTime = if (allDay || deadline || end.isBlank()) null else end,
                    location = if (deadline) null else location.trim().ifEmpty { null },
                    deadline = deadline,
                    leadDays = if (deadline) leadDays else null,
                    done = false,
                )
                store.update { it.copy(events = it.events + event) }
                onSaved(if (deadline) "Deadline added." else "Event added.")
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Save") }
    }
}

@Composable
private fun FormColumn(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun TimeField(value: String, label: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text("14:30") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/** The date as a button ("Today", "Tomorrow", "Wed 4 Nov"); tap to pick another. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(date: LocalDate, today: LocalDate, maxToday: Boolean, onPick: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("📅 ${dayLabel(date, today)}") }
    if (!open) return
    val toMillis = { d: LocalDate -> d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = toMillis(date),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !maxToday || utcTimeMillis <= toMillis(today)
        },
    )
    DatePickerDialog(
        onDismissRequest = { open = false },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                open = false
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
    ) { DatePicker(state) }
}
