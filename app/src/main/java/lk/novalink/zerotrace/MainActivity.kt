package lk.novalink.zerotrace

import android.app.Activity
import android.content.Context
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import lk.novalink.zerotrace.core.PingEngine
import lk.novalink.zerotrace.core.UpdateManager
import lk.novalink.zerotrace.core.VpnState
import lk.novalink.zerotrace.core.VpnTunnelManager
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.UpdateState
import lk.novalink.zerotrace.ui.components.BottomNav
import lk.novalink.zerotrace.ui.components.NavTab
import lk.novalink.zerotrace.ui.components.UpdateDialog
import lk.novalink.zerotrace.ui.screens.AddConfigDialog
import lk.novalink.zerotrace.ui.screens.ConfigsScreen
import lk.novalink.zerotrace.ui.screens.EditConfigDialog
import lk.novalink.zerotrace.ui.screens.HomeScreen
import lk.novalink.zerotrace.ui.screens.OnboardingScreen
import lk.novalink.zerotrace.ui.screens.SettingsScreen
import lk.novalink.zerotrace.ui.screens.StatisticsScreen
import lk.novalink.zerotrace.ui.theme.ZtBg
import lk.novalink.zerotrace.ui.theme.ZeroTraceTheme

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnInternal()
        } else {
            Toast.makeText(this, "VPN permission is required to connect", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Record anonymous telemetry check-in
        lk.novalink.zerotrace.core.TelemetryManager.recordAppOpen(this)

        val app = application as ZeroTraceApp
        val configRepo = app.configRepository
        val settingsRepo = app.settingsRepository

        setContent {
            ZeroTraceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ZtBg
                ) {
                    val onboardingPrefs = remember { getSharedPreferences("zerotrace_onboarding", Context.MODE_PRIVATE) }
                    var hasCompletedOnboarding by remember {
                        mutableStateOf(onboardingPrefs.getBoolean("onboarding_completed", false))
                    }

                    if (!hasCompletedOnboarding) {
                        OnboardingScreen(
                            onDone = {
                                onboardingPrefs.edit().putBoolean("onboarding_completed", true).apply()
                                hasCompletedOnboarding = true
                            }
                        )
                    } else {
                        val vpnState by VpnTunnelManager.vpnState.collectAsState()
                        val downloadSpeed by VpnTunnelManager.downloadSpeed.collectAsState()
                        val uploadSpeed by VpnTunnelManager.uploadSpeed.collectAsState()

                        val configs by configRepo.configs.collectAsState()
                        val selectedConfigId by configRepo.selectedConfigId.collectAsState()
                        val selectedConfig = configs.find { it.id == selectedConfigId }

                        val primaryDns by settingsRepo.primaryDns.collectAsState()
                        val bypassLan by settingsRepo.bypassLan.collectAsState()
                        val sriLankaSni by settingsRepo.sriLankaSniTweak.collectAsState()

                        val updateState by UpdateManager.updateState.collectAsState()

                        var currentTab by remember { mutableStateOf(NavTab.HOME) }
                        var showAddDialog by remember { mutableStateOf(false) }
                        var configToEdit by remember { mutableStateOf<ProxyConfig?>(null) }

                        val coroutineScope = rememberCoroutineScope()

                        // Silent background update check on app startup
                        LaunchedEffect(Unit) {
                            UpdateManager.checkForUpdates(this@MainActivity, isManualCheck = false)
                        }

                        // Handle UpToDate feedback
                        LaunchedEffect(updateState) {
                            if (updateState is UpdateState.UpToDate) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "ZeroTrace is up to date! (v${UpdateManager.getCurrentVersionName(this@MainActivity)})",
                                    Toast.LENGTH_SHORT
                                ).show()
                                UpdateManager.resetState()
                            }
                        }

                        fun handlePing(config: ProxyConfig) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val latency = PingEngine.testLatency(config.server, config.port)
                                configRepo.updatePing(config.id, latency)
                            }
                        }

                        fun handlePingAll() {
                            coroutineScope.launch(Dispatchers.IO) {
                                configs.map { cfg ->
                                    async {
                                        val latency = PingEngine.testLatency(cfg.server, cfg.port)
                                        configRepo.updatePing(cfg.id, latency)
                                    }
                                }.awaitAll()
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            // Main Screen Content based on Active Bottom Tab
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "tab_transition"
                            ) { tab ->
                                when (tab) {
                                    NavTab.HOME -> HomeScreen(
                                        vpnState = vpnState,
                                        selectedConfig = selectedConfig,
                                        downloadSpeed = downloadSpeed,
                                        uploadSpeed = uploadSpeed,
                                        onConnectToggle = { toggleVpn(selectedConfig) { showAddDialog = true } },
                                        onNavigateToConfigs = { currentTab = NavTab.CONFIGS },
                                        onNavigateToSettings = { currentTab = NavTab.SETTINGS },
                                        onAddConfigClick = { showAddDialog = true },
                                        onEditActiveConfig = { selectedConfig?.let { configToEdit = it } },
                                        onPingTest = { handlePing(it) }
                                    )

                                    NavTab.CONFIGS -> ConfigsScreen(
                                        configs = configs,
                                        selectedConfigId = selectedConfigId,
                                        onSelectConfig = { configId ->
                                            configRepo.setSelectedConfig(configId)
                                            // If already connected, reconnect to the new config
                                            if (vpnState is VpnState.Connected) {
                                                val newConfig = configs.find { it.id == configId }
                                                if (newConfig != null) {
                                                    VpnTunnelManager.stopVpn(this@MainActivity)
                                                    VpnTunnelManager.startVpn(this@MainActivity, newConfig)
                                                }
                                            }
                                        },
                                        onEditConfig = { config ->
                                            configToEdit = config
                                        },
                                        onDeleteConfig = { configId ->
                                            configRepo.deleteConfig(configId)
                                        },
                                        onPingTest = { handlePing(it) },
                                        onPingAll = { handlePingAll() },
                                        onAddConfigClick = { showAddDialog = true },
                                        onBackClick = null
                                    )

                                    NavTab.STATS -> StatisticsScreen()

                                    NavTab.SETTINGS -> SettingsScreen(
                                        primaryDns = primaryDns,
                                        bypassLan = bypassLan,
                                        sriLankaSni = sriLankaSni,
                                        onDnsChange = { settingsRepo.setPrimaryDns(it) },
                                        onBypassLanChange = { settingsRepo.setBypassLan(it) },
                                        onSriLankaSniChange = { settingsRepo.setSriLankaSniTweak(it) },
                                        onCheckUpdatesClick = {
                                            coroutineScope.launch {
                                                Toast.makeText(this@MainActivity, "Checking for updates...", Toast.LENGTH_SHORT).show()
                                                UpdateManager.checkForUpdates(this@MainActivity, isManualCheck = true)
                                            }
                                        },
                                        onShowOnboarding = { hasCompletedOnboarding = false },
                                        onBackClick = null
                                    )
                                }
                            }

                            // Bottom Navigation Bar from React Design
                            BottomNav(
                                activeTab = currentTab,
                                onTabSelected = { currentTab = it },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        // Add Config Dialog
                        if (showAddDialog) {
                            AddConfigDialog(
                                onDismiss = { showAddDialog = false },
                                onSaveConfig = { newConfig ->
                                    configRepo.addConfig(newConfig, setAsSelected = true)
                                    showAddDialog = false
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Config \"${newConfig.name}\" saved!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }

                        // Edit Config Dialog
                        if (configToEdit != null) {
                            EditConfigDialog(
                                config = configToEdit!!,
                                onDismiss = { configToEdit = null },
                                onSaveConfig = { updated ->
                                    val wasConnected = vpnState is VpnState.Connected && selectedConfigId == updated.id
                                    configRepo.addConfig(updated, setAsSelected = (selectedConfigId == updated.id))
                                    configToEdit = null
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Config \"${updated.name}\" updated!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    if (wasConnected) {
                                        VpnTunnelManager.stopVpn(this@MainActivity)
                                        VpnTunnelManager.startVpn(this@MainActivity, updated)
                                    }
                                }
                            )
                        }

                        // In-App OTA Update Dialog
                        if (updateState is UpdateState.UpdateAvailable ||
                            updateState is UpdateState.Downloading ||
                            updateState is UpdateState.ReadyToInstall ||
                            updateState is UpdateState.Error
                        ) {
                            UpdateDialog(
                                updateState = updateState,
                                onDismiss = { UpdateManager.resetState() },
                                onStartDownload = { info ->
                                    coroutineScope.launch {
                                        UpdateManager.downloadUpdate(this@MainActivity, info)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun toggleVpn(selectedConfig: ProxyConfig?, onShowAddDialog: () -> Unit) {
        val currentState = VpnTunnelManager.vpnState.value
        if (currentState is VpnState.Connected || currentState is VpnState.Connecting) {
            VpnTunnelManager.stopVpn(this)
        } else {
            if (selectedConfig == null) {
                Toast.makeText(this, "Please add or paste an Xray config first", Toast.LENGTH_SHORT).show()
                onShowAddDialog()
                return
            }
            prepareAndStartVpn()
        }
    }

    private fun prepareAndStartVpn() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            startVpnInternal()
        }
    }

    private fun startVpnInternal() {
        val app = application as ZeroTraceApp
        val activeConfig = app.configRepository.getSelectedConfig()
        if (activeConfig != null) {
            VpnTunnelManager.startVpn(this, activeConfig)
        } else {
            Toast.makeText(this, "No active config found to connect", Toast.LENGTH_SHORT).show()
        }
    }
}
