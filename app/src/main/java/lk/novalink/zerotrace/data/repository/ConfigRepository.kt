package lk.novalink.zerotrace.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol
import java.util.UUID

class ConfigRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _configs = MutableStateFlow<List<ProxyConfig>>(emptyList())
    val configs: StateFlow<List<ProxyConfig>> = _configs.asStateFlow()

    private val _selectedConfigId = MutableStateFlow<String?>(null)
    val selectedConfigId: StateFlow<String?> = _selectedConfigId.asStateFlow()

    init {
        loadConfigs()
    }

    private fun loadConfigs() {
        val json = prefs.getString(KEY_CONFIGS, null)
        val list: MutableList<ProxyConfig> = if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<ProxyConfig>>() {}.type
            val loaded: MutableList<ProxyConfig> = gson.fromJson(json, type) ?: mutableListOf()
            // Clean up any old dummy sample if present
            loaded.removeAll { it.uuid == "11111111-2222-3333-4444-555555555555" || it.server == "sg.novalink.lk" }
            loaded
        } else {
            mutableListOf()
        }

        _configs.value = list
        val savedSelectedId = prefs.getString(KEY_SELECTED_ID, null)
        _selectedConfigId.value = if (list.any { it.id == savedSelectedId }) savedSelectedId else list.firstOrNull()?.id

        saveConfigsInternal(list)
        if (_selectedConfigId.value != null) {
            prefs.edit().putString(KEY_SELECTED_ID, _selectedConfigId.value).apply()
        } else {
            prefs.edit().remove(KEY_SELECTED_ID).apply()
        }
    }

    fun addConfig(config: ProxyConfig, setAsSelected: Boolean = true) {
        val current = _configs.value.toMutableList()
        // If config with same id exists, replace, else prepend
        val index = current.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            current[index] = config
        } else {
            current.add(0, config)
        }
        saveConfigsInternal(current)

        if (setAsSelected || _selectedConfigId.value == null) {
            setSelectedConfig(config.id)
        }
    }

    fun addConfigs(newConfigs: List<ProxyConfig>) {
        if (newConfigs.isEmpty()) return
        val current = _configs.value.toMutableList()
        for (cfg in newConfigs.reversed()) {
            val idx = current.indexOfFirst { it.id == cfg.id }
            if (idx >= 0) {
                current[idx] = cfg
            } else {
                current.add(0, cfg)
            }
        }
        saveConfigsInternal(current)
        if (_selectedConfigId.value == null && current.isNotEmpty()) {
            setSelectedConfig(current.first().id)
        }
    }

    fun deleteConfig(configId: String) {
        val current = _configs.value.toMutableList()
        current.removeAll { it.id == configId }
        saveConfigsInternal(current)

        if (_selectedConfigId.value == configId) {
            val newSelected = current.firstOrNull()?.id
            setSelectedConfig(newSelected)
        }
    }

    fun updatePing(configId: String, pingMs: Long) {
        val current = _configs.value.toMutableList()
        val index = current.indexOfFirst { it.id == configId }
        if (index >= 0) {
            current[index] = current[index].copy(pingMs = pingMs)
            saveConfigsInternal(current)
        }
    }

    fun setSelectedConfig(configId: String?) {
        _selectedConfigId.value = configId
        prefs.edit().putString(KEY_SELECTED_ID, configId).apply()
    }

    fun getSelectedConfig(): ProxyConfig? {
        val selectedId = _selectedConfigId.value ?: return null
        return _configs.value.find { it.id == selectedId }
    }

    fun clearAllConfigs() {
        saveConfigsInternal(emptyList())
        setSelectedConfig(null)
    }

    private fun saveConfigsInternal(list: List<ProxyConfig>) {
        _configs.value = list
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_CONFIGS, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "zerotrace_configs_pref"
        private const val KEY_CONFIGS = "saved_configs_json"
        private const val KEY_SELECTED_ID = "selected_config_id"
    }
}
