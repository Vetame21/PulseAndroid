package com.dapid.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dapid.pulse.data.PulseApi
import com.dapid.pulse.data.PulseClient
import com.dapid.pulse.data.Session
import com.dapid.pulse.ui.PulseRoot
import com.dapid.pulse.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The app only knows Pulse's address (debug: your laptop's dev server; release: the live site).
        val client = PulseClient(PulseApi(BuildConfig.API_BASE), Session(applicationContext))
        setContent {
            PulseTheme {
                PulseRoot(client)
            }
        }
    }
}
