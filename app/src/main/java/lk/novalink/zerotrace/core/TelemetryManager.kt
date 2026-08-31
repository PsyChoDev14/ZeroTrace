package lk.novalink.zerotrace.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lk.novalink.zerotrace.data.model.ProxyConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object TelemetryManager {

    private const val TAG = "ZeroTrace-Telemetry"
    private const val PREFS_NAME = "zerotrace_telemetry_prefs"
    private const val KEY_CLIENT_ID = "anonymous_client_id"

    // Telemetry Endpoint (Swappable with custom Cloudflare worker or API)
    @Volatile
    var telemetryEndpoint: String = "https://zerotrace-telemetry.nexauracore.workers.dev/api/heartbeat"

    private fun getOrCreateClientId(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var clientId = prefs.getString(KEY_CLIENT_ID, null)
        if (clientId == null) {
            clientId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_CLIENT_ID, clientId).apply()
        }
        return clientId
    }

    fun recordVpnConnected(context: Context, config: ProxyConfig) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val clientId = getOrCreateClientId(appContext)
                val versionName = UpdateManager.getCurrentVersionName(appContext)
                val payload = JSONObject().apply {
                    put("clientId", clientId)
                    put("version", versionName)
                    put("event", "vpn_connected")
                    put("protocol", config.protocol.name.lowercase())
                    put("timestamp", System.currentTimeMillis())
                }

                sendHeartbeat(payload.toString())
            } catch (e: Exception) {
                // Silently ignore - telemetry should never disrupt user experience
                Log.d(TAG, "Telemetry ping skipped: ${e.localizedMessage}")
            }
        }
    }

    fun recordActiveAppsHeartbeat(context: Context, protocol: String, activeApps: List<String>) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val clientId = getOrCreateClientId(appContext)
                val versionName = UpdateManager.getCurrentVersionName(appContext)
                val payload = JSONObject().apply {
                    put("clientId", clientId)
                    put("version", versionName)
                    put("event", "heartbeat")
                    put("protocol", protocol)
                    put("activeApps", JSONArray(activeApps))
                    put("timestamp", System.currentTimeMillis())
                }

                sendHeartbeat(payload.toString())
            } catch (e: Exception) {
                Log.d(TAG, "Heartbeat ping skipped: ${e.localizedMessage}")
            }
        }
    }

    fun recordAppOpen(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val clientId = getOrCreateClientId(appContext)
                val versionName = UpdateManager.getCurrentVersionName(appContext)
                val payload = JSONObject().apply {
                    put("clientId", clientId)
                    put("version", versionName)
                    put("event", "app_open")
                    put("timestamp", System.currentTimeMillis())
                }

                sendHeartbeat(payload.toString())
            } catch (e: Exception) {
                Log.d(TAG, "App open ping skipped: ${e.localizedMessage}")
            }
        }
    }

    private fun sendHeartbeat(jsonPayload: String) {
        try {
            val url = URL(telemetryEndpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "ZeroTrace-Android-Client")
            }

            conn.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            Log.d(TAG, "Telemetry heartbeat response: $responseCode")
            conn.disconnect()
        } catch (e: Exception) {
            Log.d(TAG, "Heartbeat failed (expected if endpoint is offline): ${e.message}")
        }
    }
}
