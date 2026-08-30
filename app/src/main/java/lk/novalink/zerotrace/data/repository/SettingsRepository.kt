package lk.novalink.zerotrace.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import lk.novalink.zerotrace.data.model.SplitTunnelMode

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // DNS Settings (Default to AdGuard Ad-Blocker 94.140.14.14 or Cloudflare 1.1.1.1)
    private val _primaryDns = MutableStateFlow(prefs.getString(KEY_DNS, "94.140.14.14") ?: "94.140.14.14")
    val primaryDns: StateFlow<String> = _primaryDns.asStateFlow()

    private val _bypassLan = MutableStateFlow(prefs.getBoolean(KEY_BYPASS_LAN, true))
    val bypassLan: StateFlow<Boolean> = _bypassLan.asStateFlow()

    private val _sriLankaSniTweak = MutableStateFlow(prefs.getString(KEY_SL_SNI, "") ?: "")
    val sriLankaSniTweak: StateFlow<String> = _sriLankaSniTweak.asStateFlow()

    // Split Tunneling Settings
    private val initialSplitMode = try {
        SplitTunnelMode.valueOf(prefs.getString(KEY_SPLIT_MODE, SplitTunnelMode.OFF.name) ?: SplitTunnelMode.OFF.name)
    } catch (e: Exception) {
        SplitTunnelMode.OFF
    }
    private val _splitTunnelMode = MutableStateFlow(initialSplitMode)
    val splitTunnelMode: StateFlow<SplitTunnelMode> = _splitTunnelMode.asStateFlow()

    private val initialApps = prefs.getStringSet(KEY_SPLIT_APPS, emptySet()) ?: emptySet()
    private val _splitTunnelApps = MutableStateFlow<Set<String>>(HashSet(initialApps))
    val splitTunnelApps: StateFlow<Set<String>> = _splitTunnelApps.asStateFlow()

    fun setPrimaryDns(dns: String) {
        _primaryDns.value = dns
        prefs.edit().putString(KEY_DNS, dns).apply()
    }

    fun setBypassLan(enabled: Boolean) {
        _bypassLan.value = enabled
        prefs.edit().putBoolean(KEY_BYPASS_LAN, enabled).apply()
    }

    fun setSriLankaSniTweak(sni: String) {
        _sriLankaSniTweak.value = sni
        prefs.edit().putString(KEY_SL_SNI, sni).apply()
    }

    fun setSplitTunnelMode(mode: SplitTunnelMode) {
        _splitTunnelMode.value = mode
        prefs.edit().putString(KEY_SPLIT_MODE, mode.name).apply()
    }

    fun toggleApp(packageName: String) {
        val current = HashSet(_splitTunnelApps.value)
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _splitTunnelApps.value = current
        prefs.edit().putStringSet(KEY_SPLIT_APPS, current).apply()
    }

    fun setSplitTunnelApps(apps: Set<String>) {
        _splitTunnelApps.value = HashSet(apps)
        prefs.edit().putStringSet(KEY_SPLIT_APPS, apps).apply()
    }

    fun selectAll(packages: List<String>) {
        val updated = HashSet(_splitTunnelApps.value).apply { addAll(packages) }
        _splitTunnelApps.value = updated
        prefs.edit().putStringSet(KEY_SPLIT_APPS, updated).apply()
    }

    fun deselectAll() {
        _splitTunnelApps.value = emptySet()
        prefs.edit().putStringSet(KEY_SPLIT_APPS, emptySet()).apply()
    }

    companion object {
        private const val PREFS_NAME = "zerotrace_settings_pref"
        private const val KEY_DNS = "setting_primary_dns"
        private const val KEY_BYPASS_LAN = "setting_bypass_lan"
        private const val KEY_SL_SNI = "setting_sl_sni"
        private const val KEY_SPLIT_MODE = "setting_split_mode"
        private const val KEY_SPLIT_APPS = "setting_split_apps"

        const val TELEGRAM_SUPPORT_URL = "https://t.me/novalink_lk"
        const val WHATSAPP_SUPPORT_URL = "https://wa.me/94770000000"
        const val WEBSITE_URL = "https://novalink.lk"
    }
}
