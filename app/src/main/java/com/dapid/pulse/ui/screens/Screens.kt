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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Placeholder screens: layout and look only. Real data arrives with sign-in and the /api/v1 routes.

@Composable
fun TodayScreen(padding: PaddingValues) {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))
    ScreenColumn(padding) {
        PageHeader(title = "Today", eyebrow = date)
        Placeholder("Briefing", "Classes, deadlines, events and birthdays for today.")
        Placeholder("Today's plan", "Write your day, then tick it off.")
        Placeholder("Up next", "What's coming in the next 7 days.")
    }
}

@Composable
fun CalendarScreen(padding: PaddingValues) {
    ScreenColumn(padding) {
        PageHeader(title = "Calendar")
        Placeholder("Month", "Holidays, events, deadlines and birthdays.")
    }
}

@Composable
fun MoneyScreen(padding: PaddingValues) {
    ScreenColumn(padding) {
        PageHeader(title = "Money")
        Placeholder("This month", "Budget left and what you spent.")
        Placeholder("Spending calendar", "Each day from green to red.")
        Placeholder("Transactions", "Everything you spent, newest first.")
    }
}

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
