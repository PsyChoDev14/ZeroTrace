package lk.novalink.zerotrace.core

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.service.ZeroTraceVpnService

object VpnTunnelManager {

    private val _vpnState = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _downloadSpeed = MutableStateFlow(0L) // bytes per second
    val downloadSpeed: StateFlow<Long> = _downloadSpeed.asStateFlow()

    private val _uploadSpeed = MutableStateFlow(0L) // bytes per second
    val uploadSpeed: StateFlow<Long> = _uploadSpeed.asStateFlow()

    fun updateState(newState: VpnState) {
        _vpnState.value = newState
    }

    fun updateSpeed(downBytesPerSec: Long, upBytesPerSec: Long) {
        _downloadSpeed.value = downBytesPerSec
        _uploadSpeed.value = upBytesPerSec
    }

    fun startVpn(context: Context, config: ProxyConfig) {
        _vpnState.value = VpnState.Connecting
        val intent = Intent(context, ZeroTraceVpnService::class.java).apply {
            action = ZeroTraceVpnService.ACTION_START
            putExtra(ZeroTraceVpnService.EXTRA_CONFIG_ID, config.id)
            putExtra(ZeroTraceVpnService.EXTRA_SERVER_NAME, config.name)
            putExtra(ZeroTraceVpnService.EXTRA_SERVER_HOST, config.server)
            putExtra(ZeroTraceVpnService.EXTRA_SERVER_PORT, config.port)
            putExtra(ZeroTraceVpnService.EXTRA_RAW_CONFIG, config.rawConfig)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopVpn(context: Context) {
        _vpnState.value = VpnState.Stopping
        val intent = Intent(context, ZeroTraceVpnService::class.java).apply {
            action = ZeroTraceVpnService.ACTION_STOP
        }
        context.startService(intent)
    }
}
