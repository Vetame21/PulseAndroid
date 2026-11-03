package com.dapid.pulse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dapid.pulse.data.ApiResult
import com.dapid.pulse.data.PulseClient
import com.dapid.pulse.data.Region
import com.dapid.pulse.data.nextHoliday
import com.dapid.pulse.ui.components.PageHeader
import com.dapid.pulse.ui.components.PulseCard
import com.dapid.pulse.ui.components.ScreenColumn
import com.dapid.pulse.ui.components.SectionLabel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

@Composable
fun SignInScreen(client: PulseClient, onSignedIn: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var region by rememberSaveable { mutableStateOf(Region.Mainland) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            when (val r = client.signIn(email, password)) {
                is ApiResult.Ok -> onSignedIn()
                is ApiResult.Error -> error = r.message
            }
            busy = false
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        ScreenColumn(padding) {
            PageHeader(title = "Sign in", eyebrow = "Pulse")
            PulseCard {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = ::submit,
                    enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text(if (busy) "Signing in…" else "Sign in") }
            }
            HolidayPreview(client, region, onRegion = { region = it })
        }
    }
}

/** Pick 🇨🇳 or 🇭🇰 to see that region's next public holiday. A preview only: nothing is saved. */
@Composable
private fun HolidayPreview(client: PulseClient, region: Region, onRegion: (Region) -> Unit) {
    var line by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(region) {
        line = null
        val today = LocalDate.now()
        line = when (val r = client.holidays(region, today)) {
            is ApiResult.Ok -> nextHoliday(r.value, today)?.let { "${it.name} · ${it.date.format(DAY)}" } ?: "No holidays published yet."
            is ApiResult.Error -> r.message
        }
    }
    PulseCard {
        SectionLabel("Next public holiday")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Region.entries.forEach { r ->
                FilterChip(selected = r == region, onClick = { onRegion(r) }, label = { Text("${r.flag} ${r.label}") })
            }
        }
        Text(line ?: "Loading…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
