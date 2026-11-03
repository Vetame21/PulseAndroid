package com.dapid.pulse.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// The only thing the app talks to: Pulse's /api/v1 (see docs/api-v1.md in the web repo).
// Android's own HttpURLConnection + org.json, so no extra libraries.

/** A call's outcome. status 0 = the request never reached Pulse (offline, VPN, DNS). */
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Error(val status: Int, val message: String) : ApiResult<Nothing>
}

const val OFFLINE_MESSAGE = "Can't reach Pulse. Check your connection or VPN and try again."

class PulseApi(private val baseUrl: String) {
    /** One JSON request. Errors come back as the server's own { message }. */
    suspend fun call(method: String, path: String, token: String? = null, body: JSONObject? = null): ApiResult<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                val conn = URL("$baseUrl/api/v1$path").openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = method
                    conn.connectTimeout = 15_000
                    conn.readTimeout = 20_000
                    conn.setRequestProperty("Accept", "application/json")
                    if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
                    if (body != null) {
                        conn.doOutput = true
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    }
                    val status = conn.responseCode
                    val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                    val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    val json = runCatching { JSONObject(text) }.getOrNull()
                    if (status in 200..299 && json != null) {
                        ApiResult.Ok(json)
                    } else {
                        ApiResult.Error(status, json?.optString("message")?.takeIf { it.isNotBlank() } ?: "Something went wrong. Try again.")
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: IOException) {
                Log.w("PulseApi", "$method $path failed", e)
                ApiResult.Error(0, OFFLINE_MESSAGE)
            }
        }
}
