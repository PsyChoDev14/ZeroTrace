package lk.novalink.zerotrace.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _primaryDns = MutableStateFlow(prefs.getString(KEY_DNS, "1.1.1.1") ?: "1.1.1.1")
    val primaryDns: StateFlow<String> = _primaryDns.asStateFlow()

    private val _bypassLan = MutableStateFlow(prefs.getBoolean(KEY_BYPASS_LAN, true))
    val bypassLan: StateFlow<Boolean> = _bypassLan.asStateFlow()

    private val _sriLankaSniTweak = MutableStateFlow(prefs.getString(KEY_SL_SNI, "") ?: "")
    val sriLankaSniTweak: StateFlow<String> = _sriLankaSniTweak.asStateFlow()

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

    companion object {
        private const val PREFS_NAME = "zerotrace_settings_pref"
        private const val KEY_DNS = "setting_primary_dns"
        private const val KEY_BYPASS_LAN = "setting_bypass_lan"
        private const val KEY_SL_SNI = "setting_sl_sni"

        const val TELEGRAM_SUPPORT_URL = "https://t.me/novalink_lk"
        const val WHATSAPP_SUPPORT_URL = "https://wa.me/94770000000"
        const val WEBSITE_URL = "https://novalink.lk"
    }
}
