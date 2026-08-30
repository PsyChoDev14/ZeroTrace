package lk.novalink.zerotrace.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import lk.novalink.zerotrace.MainActivity
import lk.novalink.zerotrace.R
import lk.novalink.zerotrace.ZeroTraceApp
import lk.novalink.zerotrace.core.VpnState
import lk.novalink.zerotrace.core.VpnTunnelManager

class ZeroTraceTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var stateJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        stateJob?.cancel()
        stateJob = serviceScope.launch {
            VpnTunnelManager.vpnState.collectLatest { vpnState ->
                updateTileState(vpnState)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        stateJob?.cancel()
        stateJob = null
    }

    override fun onClick() {
        super.onClick()
        val currentState = VpnTunnelManager.vpnState.value

        when (currentState) {
            is VpnState.Connected, is VpnState.Connecting -> {
                // Disconnect VPN
                VpnTunnelManager.stopVpn(this)
            }
            is VpnState.Disconnected, is VpnState.Error, is VpnState.Stopping -> {
                // Check if VPN permission is granted
                val prepareIntent = VpnService.prepare(this)
                if (prepareIntent != null) {
                    launchMainActivity()
                    return
                }

                // Retrieve active / default config
                val app = applicationContext as? ZeroTraceApp ?: ZeroTraceApp.instance
                val configs = app.configRepository.configs.value
                val selectedId = app.configRepository.selectedConfigId.value
                val targetConfig = configs.find { it.id == selectedId } ?: configs.firstOrNull()

                if (targetConfig != null) {
                    VpnTunnelManager.startVpn(this, targetConfig)
                } else {
                    // No configs saved, open app so user can add one
                    launchMainActivity()
                }
            }
        }
    }

    private fun updateTileState(vpnState: VpnState) {
        val tile = qsTile ?: return

        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_tile)

        when (vpnState) {
            is VpnState.Connected -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "ZeroTrace"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = vpnState.serverName
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    tile.stateDescription = "Connected to ${vpnState.serverName}"
                }
            }
            is VpnState.Connecting -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "ZeroTrace"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Connecting..."
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    tile.stateDescription = "Connecting to VPN"
                }
            }
            is VpnState.Stopping -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "ZeroTrace"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Disconnecting..."
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    tile.stateDescription = "Disconnecting"
                }
            }
            is VpnState.Disconnected, is VpnState.Error -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "ZeroTrace"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Tap to connect"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    tile.stateDescription = "Disconnected"
                }
            }
        }

        try {
            tile.updateTile()
        } catch (e: Exception) {
            Log.e("ZeroTraceTileService", "Error updating QS tile", e)
        }
    }

    private fun launchMainActivity() {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                appIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(appIntent)
        }
    }
}
