package com.dapid.pulse.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dapid.pulse.data.ApiResult

/**
 * A tab that loads from the API: spinner first, pull down to refresh, and a
 * "Try again" card when Pulse can't be reached. Changing [key] (a month, a save
 * elsewhere) loads again. A 401 means the sign-in ran out: [onSignedOut].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Loading(
    padding: PaddingValues,
    key: Any?,
    load: suspend () -> ApiResult<T>,
    onSignedOut: () -> Unit,
    content: @Composable ColumnScope.(data: T, reload: () -> Unit) -> Unit,
) {
    var data by remember { mutableStateOf<T?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var pulls by remember { mutableIntStateOf(0) }

    LaunchedEffect(key, pulls) {
        error = null
        when (val r = load()) {
            is ApiResult.Ok -> data = r.value
            is ApiResult.Error -> if (r.status == 401) onSignedOut() else error = r.message
        }
        refreshing = false
    }
    val reload: () -> Unit = { refreshing = true; pulls++ }

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = reload, modifier = Modifier.fillMaxSize()) {
        val shown = data
        val problem = error
        when {
            shown != null -> ScreenColumn(padding) {
                // Keep the last data on screen; just say the refresh didn't work.
                problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                content(shown, reload)
            }
            problem != null -> ScreenColumn(padding) {
                PulseCard {
                    Text(problem, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = reload, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
                }
            }
            else -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.padding(24.dp)) }
        }
    }
}
