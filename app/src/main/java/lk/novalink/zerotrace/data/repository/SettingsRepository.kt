package lk.novalink.zerotrace.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import lk.novalink.zerotrace.data.model.DpiBypassMode
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

    // DPI Bypass & Stealth Engine Settings
    private val initialDpiMode = try {
        DpiBypassMode.valueOf(prefs.getString(KEY_DPI_MODE, DpiBypassMode.SMART_FRAGMENT.name) ?: DpiBypassMode.SMART_FRAGMENT.name)
    } catch (e: Exception) {
        DpiBypassMode.SMART_FRAGMENT
    }
    private val _dpiBypassMode = MutableStateFlow(initialDpiMode)
    val dpiBypassMode: StateFlow<DpiBypassMode> = _dpiBypassMode.asStateFlow()

    private val _utlsFingerprint = MutableStateFlow(prefs.getString(KEY_UTLS_FP, "chrome") ?: "chrome")
    val utlsFingerprint: StateFlow<String> = _utlsFingerprint.asStateFlow()

    private val _muxEnabled = MutableStateFlow(prefs.getBoolean(KEY_MUX_ENABLED, false))
    val muxEnabled: StateFlow<Boolean> = _muxEnabled.asStateFlow()

    private val _fragmentPackets = MutableStateFlow(prefs.getString(KEY_FRAGMENT_PACKETS, "tlshello") ?: "tlshello")
    val fragmentPackets: StateFlow<String> = _fragmentPackets.asStateFlow()

    private val _fragmentLength = MutableStateFlow(prefs.getString(KEY_FRAGMENT_LENGTH, "10-30") ?: "10-30")
    val fragmentLength: StateFlow<String> = _fragmentLength.asStateFlow()

    private val _fragmentInterval = MutableStateFlow(prefs.getString(KEY_FRAGMENT_INTERVAL, "10-20") ?: "10-20")
    val fragmentInterval: StateFlow<String> = _fragmentInterval.asStateFlow()

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

    // Biometric Security Lock
    private val _biometricLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_LOCK, false))
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()

    fun setBiometricLockEnabled(enabled: Boolean) {
        _biometricLockEnabled.value = enabled
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
    }

    fun setDpiBypassMode(mode: DpiBypassMode) {
        _dpiBypassMode.value = mode
        prefs.edit().putString(KEY_DPI_MODE, mode.name).apply()
    }

    fun setUtlsFingerprint(fp: String) {
        _utlsFingerprint.value = fp
        prefs.edit().putString(KEY_UTLS_FP, fp).apply()
    }

    fun setMuxEnabled(enabled: Boolean) {
        _muxEnabled.value = enabled
        prefs.edit().putBoolean(KEY_MUX_ENABLED, enabled).apply()
    }

    fun setFragmentCustomParams(packets: String, length: String, interval: String) {
        _fragmentPackets.value = packets
        _fragmentLength.value = length
        _fragmentInterval.value = interval
        prefs.edit()
            .putString(KEY_FRAGMENT_PACKETS, packets)
            .putString(KEY_FRAGMENT_LENGTH, length)
            .putString(KEY_FRAGMENT_INTERVAL, interval)
            .apply()
    }

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
        private const val KEY_DPI_MODE = "setting_dpi_mode"
        private const val KEY_UTLS_FP = "setting_utls_fp"
        private const val KEY_MUX_ENABLED = "setting_mux_enabled"
        private const val KEY_FRAGMENT_PACKETS = "setting_fragment_packets"
        private const val KEY_FRAGMENT_LENGTH = "setting_fragment_length"
        private const val KEY_FRAGMENT_INTERVAL = "setting_fragment_interval"
        private const val KEY_SPLIT_MODE = "setting_split_mode"
        private const val KEY_SPLIT_APPS = "setting_split_apps"
        private const val KEY_BIOMETRIC_LOCK = "setting_biometric_lock"

        const val TELEGRAM_SUPPORT_URL = "https://t.me/novalink_lk"
        const val WHATSAPP_SUPPORT_URL = "https://wa.me/94770000000"
        const val WEBSITE_URL = "https://novalink.lk"
    }
}
