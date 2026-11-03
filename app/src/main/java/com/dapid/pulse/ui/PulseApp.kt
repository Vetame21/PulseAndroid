package com.dapid.pulse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dapid.pulse.ui.screens.CalendarScreen
import com.dapid.pulse.ui.screens.MoneyScreen
import com.dapid.pulse.ui.screens.MoreScreen
import com.dapid.pulse.ui.screens.TodayScreen
import kotlinx.coroutines.launch

// Same four tabs as the web's bottom-nav.tsx, with the green + in the middle.
enum class Tab(val label: String, val icon: ImageVector) {
    Today("Today", PulseIcons.Home),
    Calendar("Calendar", PulseIcons.Calendar),
    Money("Money", PulseIcons.Wallet),
    More("More", PulseIcons.More),
}

// Lite's add choices: the web's menu minus Notice (web only).
private data class AddChoice(val label: String, val hint: String, val icon: ImageVector)

private val ADD_CHOICES = listOf(
    AddChoice("Expense", "Money you spent", PulseIcons.Expense),
    AddChoice("Event", "Class, exam, dinner…", PulseIcons.Event),
    AddChoice("Deadline", "Homework, submission", PulseIcons.Deadline),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseApp() {
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var adding by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Back from another tab goes to Today first, then leaves the app.
    BackHandler(enabled = tab != Tab.Today) { tab = Tab.Today }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            BottomBar(tab = tab, adding = adding, onTab = { tab = it }, onAdd = { adding = !adding })
        },
    ) { padding ->
        when (tab) {
            Tab.Today -> TodayScreen(padding)
            Tab.Calendar -> CalendarScreen(padding)
            Tab.Money -> MoneyScreen(padding)
            Tab.More -> MoreScreen(padding)
        }
    }

    if (adding) {
        ModalBottomSheet(
            onDismissRequest = { adding = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            AddSheet(onPick = { choice ->
                adding = false
                // The add forms arrive with sign-in and the API (next step).
                scope.launch { snackbar.showSnackbar("${choice.label}: coming in the next step") }
            })
        }
    }
}

@Composable
private fun BottomBar(tab: Tab, adding: Boolean, onTab: (Tab) -> Unit, onAdd: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            Tab.entries.forEachIndexed { i, t ->
                if (i == 2) AddButton(adding = adding, onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onAdd()
                })
                NavigationBarItem(
                    selected = t == tab,
                    onClick = { onTab(t) },
                    icon = { Icon(t.icon, contentDescription = null) },
                    label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                    colors = itemColors,
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AddButton(adding: Boolean, onClick: () -> Unit) {
    // The + turns into × while the sheet is open, like the web.
    val turn by animateFloatAsState(if (adding) 45f else 0f, label = "add-rotation")
    Box(
        modifier = Modifier
            .weight(1f)
            .semantics { contentDescription = if (adding) "Close add menu" else "Add" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PulseIcons.Plus, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.rotate(turn))
        }
    }
}

@Composable
private fun AddSheet(onPick: (AddChoice) -> Unit) {
    Column(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
        ADD_CHOICES.forEachIndexed { i, choice ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable { onPick(choice) }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(choice.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(choice.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(choice.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
