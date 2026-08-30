package lk.novalink.zerotrace.data.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

enum class SplitTunnelMode {
    OFF,                // All device apps route through VPN
    EXCLUDE_SELECTED,   // Bypass selected apps (e.g. Banking, PickMe, Uber, Dialog MyAccount)
    INCLUDE_ONLY        // Only selected apps route through VPN
}

@Immutable
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val iconBitmap: ImageBitmap? = null,
    val isSystemApp: Boolean = false,
    val isSuggestedBankingOrLocal: Boolean = false
)
