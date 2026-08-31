package lk.novalink.zerotrace.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
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

    // Live Telemetry Endpoint (Vercel / Cloudflare Worker)
    @Volatile
    var telemetryEndpoint: String = "https://server-omega-blue.vercel.app/api/heartbeat"

    private fun getOrCreateClientId(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var clientId = prefs.getString(KEY_CLIENT_ID, null)
        if (clientId == null) {
            clientId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_CLIENT_ID, clientId).apply()
        }
        return clientId
    }

    private fun getDeviceModel(): String {
        val manu = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
        val model = Build.MODEL ?: "Device"
        return if (model.startsWith(manu, ignoreCase = true)) model else "$manu $model"
    }

    private fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
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
                    put("configRemark", config.name)
                    put("serverAddress", "${config.server}:${config.port}")
                    put("deviceModel", getDeviceModel())
                    put("androidVersion", getAndroidVersion())
                    put("timestamp", System.currentTimeMillis())
                }

                sendHeartbeat(payload.toString())
            } catch (e: Exception) {
                Log.d(TAG, "Telemetry ping skipped: ${e.localizedMessage}")
            }
        }
    }

    fun recordActiveAppsHeartbeat(
        context: Context,
        protocol: String,
        configRemark: String = "",
        serverAddress: String = "",
        durationSeconds: Long = 0L,
        downloadSpeed: Long = 0L,
        uploadSpeed: Long = 0L,
        activeApps: List<String>
    ) {
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
                    put("configRemark", configRemark)
                    put("serverAddress", serverAddress)
                    put("durationSeconds", durationSeconds)
                    put("downloadSpeed", downloadSpeed)
                    put("uploadSpeed", uploadSpeed)
                    put("deviceModel", getDeviceModel())
                    put("androidVersion", getAndroidVersion())
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
                    put("deviceModel", getDeviceModel())
                    put("androidVersion", getAndroidVersion())
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
            Log.d(TAG, "Heartbeat failed: ${e.message}")
        }
    }
}
