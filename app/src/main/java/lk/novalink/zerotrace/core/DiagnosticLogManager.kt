package lk.novalink.zerotrace.core

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import lk.novalink.zerotrace.ZeroTraceApp
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLogManager {

    fun collectDiagnostics(context: Context): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timestamp = dateFormat.format(Date())
        val versionName = UpdateManager.getCurrentVersionName(context)
        val versionCode = UpdateManager.getCurrentVersionCode(context)

        sb.appendLine("========================================")
        sb.appendLine("   ZeroTrace VPN - Diagnostic Report    ")
        sb.appendLine("========================================")
        sb.appendLine("Generated: $timestamp")
        sb.appendLine()

        // 1. App Info
        sb.appendLine("--- APP INFORMATION ---")
        sb.appendLine("App Version: v$versionName (Build $versionCode)")
        sb.appendLine("Package: ${context.packageName}")
        sb.appendLine()

        // 2. Device & OS Info
        sb.appendLine("--- DEVICE INFORMATION ---")
        sb.appendLine("Manufacturer: ${Build.MANUFACTURER}")
        sb.appendLine("Brand / Model: ${Build.BRAND} ${Build.MODEL} (${Build.PRODUCT})")
        sb.appendLine("Android OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        sb.appendLine()

        // 3. Network Environment
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        sb.appendLine("--- NETWORK ENVIRONMENT ---")
        sb.appendLine("Transport: ${when {
            isWifi -> "Wi-Fi"
            isCellular -> "Cellular / Mobile Data"
            else -> "Unknown / Other"
        }}")
        sb.appendLine("Active VPN Transport: $isVpn")
        sb.appendLine("Validated Internet: ${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true}")
        sb.appendLine()

        // 4. VPN State & Config Settings
        val app = context.applicationContext as? ZeroTraceApp
        val settingsRepo = app?.settingsRepository
        val vpnState = VpnTunnelManager.vpnState.value

        sb.appendLine("--- VPN STATUS & CONFIGURATION ---")
        sb.appendLine("Tunnel State: ${vpnState::class.simpleName}")
        sb.appendLine("Primary DNS: ${settingsRepo?.primaryDns?.value ?: "N/A"}")
        sb.appendLine("Bypass LAN: ${settingsRepo?.bypassLan?.value ?: "N/A"}")
        sb.appendLine("DPI Mode: ${settingsRepo?.dpiBypassMode?.value?.name ?: "N/A"}")
        sb.appendLine("uTLS Fingerprint: ${settingsRepo?.utlsFingerprint?.value ?: "N/A"}")
        sb.appendLine("Mux Enabled: ${settingsRepo?.muxEnabled?.value ?: "N/A"}")
        sb.appendLine("Split Tunnel Mode: ${settingsRepo?.splitTunnelMode?.value?.name ?: "N/A"}")
        sb.appendLine("Split Tunnel Apps Count: ${settingsRepo?.splitTunnelApps?.value?.size ?: 0}")
        sb.appendLine()

        // 5. Recent System & Core Logs (logcat)
        sb.appendLine("--- RECENT SYSTEM & ENGINE LOGS ---")
        val logs = captureRecentLogs()
        if (logs.isNotEmpty()) {
            sb.appendLine(logs)
        } else {
            sb.appendLine("No recent core logs found.")
        }

        sb.appendLine()
        sb.appendLine("========================================")
        sb.appendLine("           END OF DIAGNOSTICS           ")
        sb.appendLine("========================================")

        return sb.toString()
    }

    private fun captureRecentLogs(): String {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "logcat",
                    "-d",
                    "-v",
                    "time",
                    "-t",
                    "150",
                    "ZeroTrace:V",
                    "ZeroTrace-Core:V",
                    "ZeroTraceVpnService:V",
                    "ZeroTrace-Telemetry:V",
                    "libXray:V",
                    "TProxyService:V",
                    "AndroidRuntime:E",
                    "*:S"
                )
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val logLines = mutableListOf<String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { logLines.add(it) }
            }
            reader.close()
            process.destroy()

            // If tag-filtered logcat returned few lines, fallback to general logcat tail
            if (logLines.size < 5) {
                val fallbackProcess = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "80"))
                val fbReader = BufferedReader(InputStreamReader(fallbackProcess.inputStream))
                var fbLine: String?
                while (fbReader.readLine().also { fbLine = it } != null) {
                    fbLine?.let {
                        if (it.contains("ZeroTrace", ignoreCase = true) || it.contains("xray", ignoreCase = true) || it.contains("tproxy", ignoreCase = true)) {
                            logLines.add(it)
                        }
                    }
                }
                fbReader.close()
                fallbackProcess.destroy()
            }

            logLines.joinToString("\n")
        } catch (e: Exception) {
            "Error capturing logcat: ${e.message}"
        }
    }

    fun shareDiagnostics(context: Context) {
        try {
            val report = collectDiagnostics(context)
            val cacheDir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
            val logFile = File(cacheDir, "ZeroTrace_Diagnostic_Report.txt")
            logFile.writeText(report)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "ZeroTrace VPN Diagnostic Report")
                putExtra(Intent.EXTRA_TEXT, "Attached is the ZeroTrace VPN diagnostic report for technical troubleshooting.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Send Diagnostic Logs to Developer").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share menu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendToWhatsApp(context: Context) {
        try {
            val report = collectDiagnostics(context)
            val versionName = UpdateManager.getCurrentVersionName(context)
            val summary = """
                *ZeroTrace Diagnostic Report*
                • Version: v$versionName
                • Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})
                • VPN State: ${VpnTunnelManager.vpnState.value::class.simpleName}
                
                Please find full logs below:
            """.trimIndent()

            val textToSend = summary + "\n\n" + report.take(2500) // WhatsApp URL length limit safe
            val encodedText = Uri.encode(textToSend)
            val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/94788385465?text=$encodedText")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            shareDiagnostics(context)
        }
    }

    fun copyToClipboard(context: Context) {
        val report = collectDiagnostics(context)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ZeroTrace Diagnostic Logs", report)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Diagnostic logs copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
