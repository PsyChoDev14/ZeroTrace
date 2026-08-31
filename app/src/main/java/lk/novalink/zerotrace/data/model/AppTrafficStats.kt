package lk.novalink.zerotrace.data.model

data class AppTrafficStats(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val downloadSpeed: Long = 0L,         // Bytes / second
    val uploadSpeed: Long = 0L,           // Bytes / second
    val sessionDownloadBytes: Long = 0L,  // Cumulative bytes received this VPN session
    val sessionUploadBytes: Long = 0L,    // Cumulative bytes sent this VPN session
    val isTunneled: Boolean = true,       // true: routing via ZeroTrace VPN; false: Bypassed
    val isSystemApp: Boolean = false,
    val lastActiveTimestamp: Long = 0L
) {
    val totalSpeed: Long get() = downloadSpeed + uploadSpeed
    val totalSessionBytes: Long get() = sessionDownloadBytes + sessionUploadBytes
    val isActiveNow: Boolean get() = totalSpeed > 0
}
