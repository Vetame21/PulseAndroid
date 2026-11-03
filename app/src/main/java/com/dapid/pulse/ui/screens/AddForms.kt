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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dapid.pulse.data.AccountSettings
import com.dapid.pulse.data.ApiResult
import com.dapid.pulse.data.PulseClient
import com.dapid.pulse.data.currencySymbol
import com.dapid.pulse.data.dayLabel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

private fun todayIn(settings: AccountSettings): LocalDate = LocalDate.now(runCatching { ZoneId.of(settings.timezone) }.getOrDefault(ZoneId.systemDefault()))

/** Quick add: amount first (number pad), a category chip, optional note. Today by default. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseForm(client: PulseClient, settings: AccountSettings, onSaved: (String) -> Unit) {
    val id = rememberSaveable { UUID.randomUUID().toString() } // one id per form: a retry never saves twice
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(settings.categories.firstOrNull()?.key ?: "food") }
    var note by rememberSaveable { mutableStateOf("") }
    val today = remember { todayIn(settings) }
    var date by rememberSaveable { mutableStateOf(today.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    FormColumn("New expense") {
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            prefix = { Text(currencySymbol(settings.homeCurrency)) },
            placeholder = { Text("32.50") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            settings.categories.forEach { c ->
                FilterChip(selected = c.key == category, onClick = { category = c.key }, label = { Text("${c.emoji} ${c.label}") })
            }
        }
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        DateField(LocalDate.parse(date), today, maxToday = true) { date = it.toString() }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        Button(
            onClick = {
                busy = true
                error = null
                scope.launch {
                    when (val r = client.addExpense(id, amount, category, LocalDate.parse(date), note)) {
                        is ApiResult.Ok -> onSaved(r.value)
                        is ApiResult.Error -> error = r.message
                    }
                    busy = false
                }
            },
            enabled = !busy && amount.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (busy) "Saving…" else "Save") }
    }
}

/** A new event (class, exam, dinner…) or deadline (homework, submission). */
@Composable
fun EventForm(client: PulseClient, settings: AccountSettings, deadline: Boolean, onSaved: (String) -> Unit) {
    val id = rememberSaveable { UUID.randomUUID().toString() }
    val today = remember { todayIn(settings) }
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(today.toString()) }
    var allDay by rememberSaveable { mutableStateOf(deadline) }
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var leadDays by rememberSaveable { mutableStateOf(7) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    FormColumn(if (deadline) "New deadline" else "New event") {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Place (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        Button(
            onClick = {
                busy = true
                error = null
                val body = JSONObject()
                    .put("id", id)
                    .put("kind", if (deadline) "deadline" else "event")
                    .put("title", title.trim())
                    .put("date", date)
                    .put("allDay", allDay)
                    .put("startTime", if (allDay) "" else start.trim())
                    .put("endTime", if (allDay || deadline) "" else end.trim())
                    .put("location", if (deadline) "" else location.trim())
                if (deadline) body.put("leadDays", leadDays)
                scope.launch {
                    when (val r = client.addEvent(body)) {
                        is ApiResult.Ok -> onSaved(if (deadline) "Deadline added." else "Event added.")
                        is ApiResult.Error -> error = r.message
                    }
                    busy = false
                }
            },
            enabled = !busy && title.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (busy) "Saving…" else "Save") }
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
        selectableDates = object : androidx.compose.material3.SelectableDates {
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
