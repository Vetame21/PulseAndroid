package com.dapid.pulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dapid.pulse.data.AccountSettings
import com.dapid.pulse.data.ApiResult
import com.dapid.pulse.data.OFFLINE_MESSAGE
import com.dapid.pulse.data.PulseClient
import com.dapid.pulse.data.Stage
import com.dapid.pulse.data.stageFor
import com.dapid.pulse.ui.screens.SetupScreen
import com.dapid.pulse.ui.screens.SignInScreen
import kotlinx.coroutines.launch

/** Signed out → Sign in · not set up → Setup · otherwise the tabs. */
@Composable
fun PulseRoot(client: PulseClient) {
    var stage by remember { mutableStateOf(if (client.signedIn) Stage.Loading else Stage.SignIn) }
    var offlineMessage by remember { mutableStateOf(OFFLINE_MESSAGE) }
    var account by remember { mutableStateOf<AccountSettings?>(null) }
    var check by remember { mutableIntStateOf(0) } // bump to look again (after sign-in, setup or Retry)
    val scope = rememberCoroutineScope()

    LaunchedEffect(check) {
        if (!client.signedIn) {
            stage = Stage.SignIn
            return@LaunchedEffect
        }
        stage = Stage.Loading
        val settings = client.settings()
        if (settings is ApiResult.Error) offlineMessage = settings.message
        if (settings is ApiResult.Ok) account = settings.value
        stage = stageFor(client.signedIn, settings)
    }

    when (stage) {
        Stage.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        Stage.SignIn -> SignInScreen(client, onSignedIn = { check++ })
        Stage.Setup -> SetupScreen(client, onDone = { check++ })
        Stage.Home -> account?.let {
            PulseApp(client, it, onSignOut = { scope.launch { client.signOut(); check++ } }, onSignedOut = { check++ })
        }
        Stage.Offline -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(offlineMessage, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { check++ }) { Text("Try again") }
        }
    }
}
