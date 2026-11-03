package com.dapid.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dapid.pulse.ui.PulseApp
import com.dapid.pulse.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseTheme {
                PulseApp()
            }
        }
    }
}
