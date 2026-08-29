package lk.novalink.zerotrace.core

sealed class VpnState {
    data object Disconnected : VpnState()
    data object Connecting : VpnState()
    data class Connected(
        val serverName: String,
        val serverAddress: String,
        val connectedAt: Long = System.currentTimeMillis()
    ) : VpnState()
    data object Stopping : VpnState()
    data class Error(val message: String) : VpnState()

    val isConnected: Boolean get() = this is Connected
    val isConnecting: Boolean get() = this is Connecting
    val isDisconnected: Boolean get() = this is Disconnected
}
