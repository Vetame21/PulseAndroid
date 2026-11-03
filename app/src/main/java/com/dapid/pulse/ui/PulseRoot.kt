package com.dapid.pulse.ui

import androidx.compose.runtime.Composable
import com.dapid.pulse.data.Store
import com.dapid.pulse.ui.screens.SetupScreen

/** First run → Setup (region + budget); after that, the tabs. */
@Composable
fun PulseRoot(store: Store) {
    if (store.data.region == null) SetupScreen(store) else PulseApp(store)
}
