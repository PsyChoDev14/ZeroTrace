package lk.novalink.zerotrace.data.model

import android.graphics.drawable.Drawable

enum class SplitTunnelMode {
    OFF,                // All device apps route through VPN
    EXCLUDE_SELECTED,   // Bypass selected apps (e.g. Banking, PickMe, Uber, Dialog MyAccount)
    INCLUDE_ONLY        // Only selected apps route through VPN
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
    val isSuggestedBankingOrLocal: Boolean = false
)
