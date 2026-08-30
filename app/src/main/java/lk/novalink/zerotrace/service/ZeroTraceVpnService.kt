package lk.novalink.zerotrace.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import hev.sockstun.TProxyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lk.novalink.zerotrace.MainActivity
import lk.novalink.zerotrace.R
import lk.novalink.zerotrace.ZeroTraceApp
import lk.novalink.zerotrace.core.VpnState
import lk.novalink.zerotrace.core.VpnTunnelManager
import lk.novalink.zerotrace.core.XrayCoreManager
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.parser.ConfigParser

class ZeroTraceVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var monitorJob: Job? = null

    private var currentConfig: ProxyConfig? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START -> {
                val app = application as? ZeroTraceApp
                val configRepo = app?.configRepository
                val settingsRepo = app?.settingsRepository

                val configId = intent.getStringExtra(EXTRA_CONFIG_ID)
                currentConfig = if (configId != null) {
                    configRepo?.configs?.value?.find { it.id == configId }
                } else {
                    configRepo?.getSelectedConfig()
                }

                if (currentConfig == null) {
                    val raw = intent.getStringExtra(EXTRA_RAW_CONFIG) ?: ""
                    currentConfig = ConfigParser.parseSingle(raw)
                }

                val bypassLan = settingsRepo?.bypassLan?.value ?: true
                val primaryDns = settingsRepo?.primaryDns?.value ?: "1.1.1.1"

                startVpnTunnel(bypassLan, primaryDns)
                return START_STICKY
            }

            ACTION_STOP -> {
                stopVpnTunnel()
                return START_NOT_STICKY
            }

            else -> return START_NOT_STICKY
        }
    }

    private fun startVpnTunnel(bypassLan: Boolean, primaryDns: String) {
        val config = currentConfig
        if (config == null) {
            VpnTunnelManager.updateState(VpnState.Error("No valid configuration found"))
            stopSelf()
            return
        }

        try {
            startForeground(NOTIFICATION_ID, buildNotification("Connecting to ${config.name}..."))
            VpnTunnelManager.updateState(VpnState.Connecting)

            val app = application as? ZeroTraceApp ?: ZeroTraceApp.instance
            val settingsRepo = app.settingsRepository
            val splitMode = settingsRepo.splitTunnelMode.value
            val splitApps = settingsRepo.splitTunnelApps.value

            val dnsProfile = lk.novalink.zerotrace.data.model.DnsProviders.findByPrimaryIp(primaryDns)
            val secondaryDns = dnsProfile?.secondaryIp ?: "8.8.8.8"

            // Configure High-Performance TUN Interface (MTU 1400 for zero 4G/5G carrier fragmentation)
            val builder = Builder()
                .setSession("ZeroTrace - ${config.name}")
                .setMtu(1400)
                .addAddress("10.233.233.2", 24)
                .addDnsServer(primaryDns)
                .addDnsServer(secondaryDns)
                .addRoute("0.0.0.0", 0)

            // Split Tunneling Application Routing
            when (splitMode) {
                lk.novalink.zerotrace.data.model.SplitTunnelMode.EXCLUDE_SELECTED -> {
                    for (pkg in splitApps) {
                        try {
                            builder.addDisallowedApplication(pkg)
                        } catch (e: Exception) {
                            Log.w("ZeroTraceVpnService", "Could not disallow package: $pkg", e)
                        }
                    }
                }
                lk.novalink.zerotrace.data.model.SplitTunnelMode.INCLUDE_ONLY -> {
                    val appsToInclude = HashSet(splitApps).apply { add(packageName) }
                    for (pkg in appsToInclude) {
                        try {
                            builder.addAllowedApplication(pkg)
                        } catch (e: Exception) {
                            Log.w("ZeroTraceVpnService", "Could not allow package: $pkg", e)
                        }
                    }
                }
                lk.novalink.zerotrace.data.model.SplitTunnelMode.OFF -> {
                    // Full device tunneling
                }
            }

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                val tunFd = vpnInterface!!.fd

                // Start native Xray-Core and Tun2socks Bridge
                val success = XrayCoreManager.startEngine(
                    context = this,
                    vpnService = this,
                    config = config,
                    tunFd = tunFd,
                    bypassLan = bypassLan,
                    primaryDns = primaryDns
                )

                if (success) {
                    VpnTunnelManager.updateState(
                        VpnState.Connected(
                            serverName = config.name,
                            serverAddress = "${config.server}:${config.port}"
                        )
                    )
                    startForeground(NOTIFICATION_ID, buildNotification("Connected to ${config.name} • NovaLink LK"))
                    startTrafficMonitor()
                    registerNetworkCallback()
                    lk.novalink.zerotrace.core.TelemetryManager.recordVpnConnected(this, config)
                } else {
                    VpnTunnelManager.updateState(VpnState.Error("Failed to start Xray core"))
                    stopVpnTunnel()
                }
            } else {
                VpnTunnelManager.updateState(VpnState.Error("Failed to establish TUN interface"))
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e("ZeroTraceVpnService", "VPN start failed", e)
            VpnTunnelManager.updateState(VpnState.Error(e.localizedMessage ?: "VPN start failed"))
            stopSelf()
        }
    }

    private fun startTrafficMonitor() {
        monitorJob?.cancel()
        val app = application as? ZeroTraceApp
        val statsRepo = app?.trafficStatsRepository

        monitorJob = serviceScope.launch {
            var lastRx = 0L
            var lastTx = 0L

            while (isActive) {
                delay(1000)
                try {
                    val stats = TProxyService.TProxyGetStats()
                    if (stats != null && stats.size >= 2) {
                        val currentRx = stats[0]
                        val currentTx = stats[1]

                        val downSpeed = if (lastRx > 0 && currentRx >= lastRx) currentRx - lastRx else 0L
                        val upSpeed = if (lastTx > 0 && currentTx >= lastTx) currentTx - lastTx else 0L

                        lastRx = currentRx
                        lastTx = currentTx

                        VpnTunnelManager.updateSpeed(downSpeed, upSpeed)
                        statsRepo?.addTraffic(downSpeed, upSpeed)
                        statsRepo?.addUptimeSecond()
                    }
                } catch (e: Throwable) {
                    // Fallback
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d("ZeroTraceVpnService", "Underlying network changed/available: $network")
                }
            }
            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.w("ZeroTraceVpnService", "Failed to register network callback", e)
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
            networkCallback = null
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun stopVpnTunnel() {
        unregisterNetworkCallback()
        monitorJob?.cancel()
        monitorJob = null

        // Stop native Xray and tun2socks
        XrayCoreManager.stopEngine()

        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        VpnTunnelManager.updateSpeed(0, 0)
        VpnTunnelManager.updateState(VpnState.Disconnected)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpnTunnel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpnTunnel()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, ZeroTraceVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_zerotrace_logo)
            .setContentTitle("ZeroTrace • NovaLink LK")
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_disconnect),
                disconnectPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START = "lk.novalink.zerotrace.action.START_VPN"
        const val ACTION_STOP = "lk.novalink.zerotrace.action.STOP_VPN"

        const val EXTRA_CONFIG_ID = "extra_config_id"
        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_SERVER_HOST = "extra_server_host"
        const val EXTRA_SERVER_PORT = "extra_server_port"
        const val EXTRA_RAW_CONFIG = "extra_raw_config"

        private const val CHANNEL_ID = "zerotrace_vpn_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
