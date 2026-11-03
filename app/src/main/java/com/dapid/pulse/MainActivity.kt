package com.dapid.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dapid.pulse.data.Store
import com.dapid.pulse.ui.PulseRoot
import com.dapid.pulse.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Pulse Lite works on its own: everything is saved on the phone.
        val store = Store.open(applicationContext)
        setContent {
            PulseTheme {
                PulseRoot(store)
            }
        }
    }
}
