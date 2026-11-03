package com.dapid.pulse.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dapid.pulse.ui.components.PageHeader
import com.dapid.pulse.ui.components.PulseCard
import com.dapid.pulse.ui.components.ScreenColumn
import com.dapid.pulse.ui.components.SectionLabel

// More: settings and sign out.

@Composable
fun MoreScreen(padding: PaddingValues, onSignOut: () -> Unit) {
    ScreenColumn(padding) {
        PageHeader(title = "More")
        Placeholder("Settings", "Theme and budget.")
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Sign out") }
    }
}

@Composable
private fun Placeholder(label: String, text: String) {
    PulseCard {
        SectionLabel(label)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
