package com.dapid.pulse.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dapid.pulse.data.Store
import com.dapid.pulse.data.budgetProblem
import com.dapid.pulse.data.currencySymbol
import com.dapid.pulse.data.formatMoney
import com.dapid.pulse.data.parseAmount
import com.dapid.pulse.ui.components.PageHeader
import com.dapid.pulse.ui.components.PulseCard
import com.dapid.pulse.ui.components.ScreenColumn
import com.dapid.pulse.ui.components.SectionLabel

// More: change the budget, or reset the app.

@Composable
fun MoreScreen(padding: PaddingValues, store: Store) {
    val d = store.data
    var budget by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    ScreenColumn(padding) {
        PageHeader(title = "More")
        PulseCard {
            SectionLabel("Monthly budget")
            Text("Now ${formatMoney(d.budgetFen, d.currency)} a month · ${d.region?.flag.orEmpty()} ${d.region?.label.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = budget,
                onValueChange = { budget = it },
                prefix = { Text(currencySymbol(d.currency)) },
                placeholder = { Text("New budget") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val cents = parseAmount(budget)
                    if (cents == null) {
                        message = budgetProblem(budget)
                    } else {
                        store.update { it.copy(budgetFen = cents) }
                        message = "Saved: ${formatMoney(cents, d.currency)} a month."
                        budget = ""
                    }
                },
                enabled = budget.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Change budget") }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        PulseCard {
            SectionLabel("About")
            Text("Pulse Lite keeps everything on this phone. Nothing is sent anywhere.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Reset app", color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset Pulse Lite?") },
            text = { Text("This deletes all your expenses, events and plans on this phone and starts setup again. It can't be undone.") },
            confirmButton = { TextButton(onClick = { store.reset(); confirmReset = false }) { Text("Reset", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}
