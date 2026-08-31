package lk.novalink.zerotrace.data.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class AppTrafficStats(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val icon: Drawable? = null,
    val iconBitmap: ImageBitmap? = null,
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

    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024f * 1024f))
            bytesPerSec >= 1024 -> String.format("%.0f KB/s", bytesPerSec / 1024f)
            bytesPerSec > 0 -> "$bytesPerSec B/s"
            else -> "0 B/s"
        }
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024f * 1024f * 1024f))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format("%.0f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }
}
