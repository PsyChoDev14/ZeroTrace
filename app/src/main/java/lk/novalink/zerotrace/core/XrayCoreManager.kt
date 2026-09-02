package lk.novalink.zerotrace.core

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import hev.sockstun.TProxyService
import libXray.DialerController
import libXray.LibXray
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.service.ZeroTraceVpnService
import java.io.File

object XrayCoreManager {

    private const val TAG = "ZeroTrace-Core"
    private const val SOCKS_PORT = 10808
    private const val HTTP_PORT = 10809

    @Volatile
    private var isXrayRunning = false

    fun startEngine(
        context: Context,
        vpnService: ZeroTraceVpnService,
        config: ProxyConfig,
        tunFd: Int,
        bypassLan: Boolean = true,
        primaryDns: String = "1.1.1.1",
        dpiBypassMode: lk.novalink.zerotrace.data.model.DpiBypassMode = lk.novalink.zerotrace.data.model.DpiBypassMode.SMART_FRAGMENT,
        utlsFingerprint: String = "chrome",
        muxEnabled: Boolean = false,
        fragmentPackets: String = "tlshello",
        fragmentLength: String = "10-30",
        fragmentInterval: String = "10-20",
        resolvedServerIp: String? = null
    ): Boolean {
        try {
            Log.d(TAG, "Starting Xray Core Engine for: ${config.name} (DPI Mode: ${dpiBypassMode.name}, Resolved IP: $resolvedServerIp)")

            // 1. Register Dialer Controller to protect outbound sockets from looping inside the VPN
            try {
                LibXray.registerDialerController(object : DialerController {
                    override fun protectFd(fd: Long): Boolean {
                        return try {
                            val ok = vpnService.protect(fd.toInt())
                            Log.d(TAG, "Protected socket fd: $fd -> $ok")
                            ok
                        } catch (e: Exception) {
                            Log.e(TAG, "Error protecting socket fd: $fd", e)
                            false
                        }
                    }
                })
            } catch (e: Throwable) {
                Log.w(TAG, "Could not register dialer controller", e)
            }

            // 2. Pre-resolve server domain if not provided and not a raw IP
            var finalServerIp = resolvedServerIp
            if (finalServerIp.isNullOrBlank() && config.server.isNotEmpty() && !config.server.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$"))) {
                try {
                    val future = java.util.concurrent.Executors.newSingleThreadExecutor().submit<String> {
                        java.net.InetAddress.getByName(config.server).hostAddress
                    }
                    finalServerIp = future.get(3, java.util.concurrent.TimeUnit.SECONDS)
                    Log.d(TAG, "Pre-resolved server: ${config.server} -> $finalServerIp")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not pre-resolve ${config.server}", e)
                }
            }

            // 3. Generate runtime Xray-Core JSON and save to file
            val configDir = File(context.filesDir, "xray").apply { mkdirs() }
            val configFile = File(configDir, "config.json")
            val xrayJson = XrayConfigGenerator.generateRuntimeJson(
                config = config,
                socksPort = SOCKS_PORT,
                httpPort = HTTP_PORT,
                bypassLan = bypassLan,
                primaryDns = primaryDns,
                dpiBypassMode = dpiBypassMode,
                utlsFingerprint = utlsFingerprint,
                muxEnabled = muxEnabled,
                fragmentPackets = fragmentPackets,
                fragmentLength = fragmentLength,
                fragmentInterval = fragmentInterval,
                resolvedServerIp = finalServerIp
            )
            configFile.writeText(xrayJson)
            Log.d(TAG, "Wrote runtime Xray JSON to: ${configFile.absolutePath}")

            // 3. Start Xray-core via libXray invoke API (apiVersion = 1)
            val runRequest = JsonObject().apply {
                addProperty("apiVersion", 1)
                addProperty("method", "runXray")
                add("payload", JsonObject().apply {
                    addProperty("configPath", configFile.absolutePath)
                })
            }

            val response = LibXray.invoke(runRequest.toString())
            Log.d(TAG, "LibXray run response: $response")

            val jsonResponse = try {
                JsonParser.parseString(response).asJsonObject
            } catch (e: Exception) {
                null
            }

            val success = jsonResponse?.get("success")?.asBoolean ?: true
            if (!success) {
                val errorMsg = jsonResponse?.get("error")?.asString ?: "Unknown Xray error"
                Log.e(TAG, "LibXray reported error: $errorMsg")
                if (errorMsg.contains("already running", ignoreCase = true)) {
                    stopEngine()
                    val retryResp = LibXray.invoke(runRequest.toString())
                    Log.d(TAG, "LibXray retry response: $retryResp")
                }
            }
            isXrayRunning = true

            // 4. Generate & write high-performance tun2socks YAML config
            val tunConfigFile = File(configDir, "tun.yml")
            val tunYaml = """
                tunnel:
                  name: tun0
                  mtu: 1400
                  ipv4: 10.233.233.2
                  ipv6: fd00:1:fd00:1::2

                socks5:
                  port: $SOCKS_PORT
                  address: 127.0.0.1
                  udp: udp
                  read-write-timeout: 60000
            """.trimIndent()
            tunConfigFile.writeText(tunYaml)

            // 5. Start hev-socks5-tunnel C-core with the TUN file descriptor
            try {
                TProxyService.TProxyStartService(tunConfigFile.absolutePath, tunFd)
                Log.d(TAG, "TProxyService started with fd: $tunFd")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start TProxyService", e)
            }

            return true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start Xray engine", e)
            e.printStackTrace()
            return false
        }
    }

    fun stopEngine() {
        try {
            Log.d(TAG, "Stopping Xray and Tun2socks engines...")

            // 1. Stop tun2socks
            try {
                TProxyService.TProxyStopService()
            } catch (e: Throwable) {
                Log.e(TAG, "Error stopping TProxyService", e)
            }

            // 2. Stop Xray core
            try {
                val stopRequest = JsonObject().apply {
                    addProperty("apiVersion", 1)
                    addProperty("method", "stopXray")
                }
                LibXray.invoke(stopRequest.toString())
            } catch (e: Throwable) {
                Log.e(TAG, "Error stopping LibXray", e)
            }

            isXrayRunning = false
        } catch (e: Throwable) {
            Log.e(TAG, "Error in stopEngine", e)
        }
    }
}
