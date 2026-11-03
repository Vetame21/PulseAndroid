package com.dapid.pulse.data

import android.content.Context
import androidx.core.content.edit

// The signed-in token pair, in the app's private storage (other apps can't read it).
// Encrypting it with a device key comes with biometric unlock, later.
class Session(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    val accessToken: String? get() = prefs.getString(ACCESS, null)
    val refreshToken: String? get() = prefs.getString(REFRESH, null)
    val signedIn: Boolean get() = accessToken != null

    fun save(access: String, refresh: String) = prefs.edit { putString(ACCESS, access).putString(REFRESH, refresh) }

    fun clear() = prefs.edit { clear() }

    private companion object {
        const val ACCESS = "access_token"
        const val REFRESH = "refresh_token"
    }
}
