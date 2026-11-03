package com.dapid.pulse.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dapid.pulse.data.Region
import com.dapid.pulse.data.Store
import com.dapid.pulse.data.budgetProblem
import com.dapid.pulse.data.currencySymbol
import com.dapid.pulse.data.parseAmount
import com.dapid.pulse.ui.components.PageHeader
import com.dapid.pulse.ui.components.PulseCard
import com.dapid.pulse.ui.components.ScreenColumn
import com.dapid.pulse.ui.components.SectionLabel

/** First run: where you live (fixes ¥ or HK$, the timezone and the holidays) and your monthly budget. */
@Composable
fun SetupScreen(store: Store) {
    var region by rememberSaveable { mutableStateOf<Region?>(null) }
    var budget by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val picked = region ?: return run { error = "Pick where you live." }
        budgetProblem(budget)?.let { error = it; return }
        // Track from the 1st of this month, so the first month has a full budget.
        store.update { it.copy(region = picked, budgetFen = parseAmount(budget)!!, trackingStart = picked.today().withDayOfMonth(1)) }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        ScreenColumn(padding) {
            PageHeader(title = "Set up Pulse", eyebrow = "Welcome")
            PulseCard {
                SectionLabel("Where do you live?")
                Region.entries.forEach { r -> RegionOption(r, selected = r == region, onClick = { region = r }) }
                Text(
                    "Your money is kept in this currency, so it can't change later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PulseCard {
                SectionLabel("Monthly budget")
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    prefix = { Text(region?.let { currencySymbol(it.currency) } ?: "") },
                    placeholder = { Text("1500") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("What you plan to spend each month. You can change it in More.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            Button(onClick = ::submit, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Start") }
        }
    }
}

@Composable
private fun RegionOption(region: Region, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(region.flag, style = MaterialTheme.typography.headlineSmall)
            Column {
                Text(region.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    if (region == Region.Mainland) "¥ RMB · holidays with 调休 workdays" else "HK$ · Hong Kong public holidays",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
