package lk.novalink.zerotrace.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lk.novalink.zerotrace.ZeroTraceApp
import lk.novalink.zerotrace.data.model.AppTrafficStats
import lk.novalink.zerotrace.data.model.SplitTunnelMode
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object LiveAppTrafficManager {

    private const val TAG = "LiveAppTrafficManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    private val _liveApps = MutableStateFlow<List<AppTrafficStats>>(emptyList())
    val liveApps: StateFlow<List<AppTrafficStats>> = _liveApps.asStateFlow()

    private val _activeAppsCount = MutableStateFlow(0)
    val activeAppsCount: StateFlow<Int> = _activeAppsCount.asStateFlow()

    private val _topActiveApps = MutableStateFlow<List<AppTrafficStats>>(emptyList())
    val topActiveApps: StateFlow<List<AppTrafficStats>> = _topActiveApps.asStateFlow()

    // Internal app cache
    private data class CachedApp(
        val packageName: String,
        val appName: String,
        val uid: Int,
        val isSystemApp: Boolean
    )

    private val installedAppsCache = mutableListOf<CachedApp>()
    private var isAppCacheLoaded = false

    // Per-UID tracking state
    private data class UidTracker(
        var lastRxBytes: Long = 0L,
        var lastTxBytes: Long = 0L,
        var sessionRxBytes: Long = 0L,
        var sessionTxBytes: Long = 0L,
        var lastActiveTime: Long = 0L
    )

    private val uidTrackers = ConcurrentHashMap<Int, UidTracker>()

    fun startMonitoring(context: Context) {
        if (monitorJob?.isActive == true) return

        monitorJob = scope.launch {
            ensureAppsLoaded(context)

            while (isActive) {
                try {
                    val app = (context.applicationContext as? ZeroTraceApp) ?: ZeroTraceApp.instance
                    val splitMode = app.settingsRepository.splitTunnelMode.value
                    val splitApps = app.settingsRepository.splitTunnelApps.value

                    val updatedStats = mutableListOf<AppTrafficStats>()
                    var activeCount = 0

                    for (cached in installedAppsCache) {
                        val uid = cached.uid
                        val currentRx = TrafficStats.getUidRxBytes(uid)
                        val currentTx = TrafficStats.getUidTxBytes(uid)

                        if (currentRx == TrafficStats.UNSUPPORTED.toLong() || currentTx == TrafficStats.UNSUPPORTED.toLong()) {
                            continue
                        }

                        val tracker = uidTrackers.getOrPut(uid) {
                            UidTracker(lastRxBytes = currentRx, lastTxBytes = currentTx)
                        }

                        val deltaRx = if (tracker.lastRxBytes > 0 && currentRx >= tracker.lastRxBytes) currentRx - tracker.lastRxBytes else 0L
                        val deltaTx = if (tracker.lastTxBytes > 0 && currentTx >= tracker.lastTxBytes) currentTx - tracker.lastTxBytes else 0L

                        tracker.sessionRxBytes += deltaRx
                        tracker.sessionTxBytes += deltaTx
                        tracker.lastRxBytes = currentRx
                        tracker.lastTxBytes = currentTx

                        val isAct = (deltaRx > 0 || deltaTx > 0)
                        if (isAct) {
                            tracker.lastActiveTime = System.currentTimeMillis()
                            activeCount++
                        }

                        val isTunneled = when (splitMode) {
                            SplitTunnelMode.OFF -> true
                            SplitTunnelMode.EXCLUDE_SELECTED -> !splitApps.contains(cached.packageName)
                            SplitTunnelMode.INCLUDE_ONLY -> splitApps.contains(cached.packageName) || cached.packageName == context.packageName
                        }

                        updatedStats.add(
                            AppTrafficStats(
                                packageName = cached.packageName,
                                appName = cached.appName,
                                uid = uid,
                                downloadSpeed = deltaRx,
                                uploadSpeed = deltaTx,
                                sessionDownloadBytes = tracker.sessionRxBytes,
                                sessionUploadBytes = tracker.sessionTxBytes,
                                isTunneled = isTunneled,
                                isSystemApp = cached.isSystemApp,
                                lastActiveTimestamp = tracker.lastActiveTime
                            )
                        )
                    }

                    // Sort: Active apps first (by speed desc), then by session usage desc, then app name
                    val sorted = updatedStats.sortedWith(
                        compareByDescending<AppTrafficStats> { it.isActiveNow }
                            .thenByDescending { it.totalSpeed }
                            .thenByDescending { it.totalSessionBytes }
                            .thenBy { it.appName.lowercase(Locale.ROOT) }
                    )

                    _liveApps.value = sorted
                    _activeAppsCount.value = activeCount
                    _topActiveApps.value = sorted.filter { it.isActiveNow || it.totalSessionBytes > 0 }.take(5)

                } catch (e: Exception) {
                    Log.e(TAG, "Error polling live app traffic", e)
                }

                delay(1000)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null

        // Clear active speeds to 0 while keeping historical session usage
        _liveApps.value = _liveApps.value.map { it.copy(downloadSpeed = 0L, uploadSpeed = 0L) }
        _activeAppsCount.value = 0
        _topActiveApps.value = emptyList()
    }

    fun resetSession() {
        uidTrackers.clear()
        _liveApps.value = _liveApps.value.map {
            it.copy(
                downloadSpeed = 0L,
                uploadSpeed = 0L,
                sessionDownloadBytes = 0L,
                sessionUploadBytes = 0L,
                lastActiveTimestamp = 0L
            )
        }
        _activeAppsCount.value = 0
        _topActiveApps.value = emptyList()
    }

    private suspend fun ensureAppsLoaded(context: Context) {
        if (isAppCacheLoaded && installedAppsCache.isNotEmpty()) return

        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val list = mutableListOf<CachedApp>()

                for (pkg in packages) {
                    val isSystem = (pkg.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val appName = try {
                        pm.getApplicationLabel(pkg).toString()
                    } catch (e: Exception) {
                        pkg.packageName
                    }

                    list.add(
                        CachedApp(
                            packageName = pkg.packageName,
                            appName = appName,
                            uid = pkg.uid,
                            isSystemApp = isSystem
                        )
                    )
                }

                list.sortBy { it.appName.lowercase(Locale.ROOT) }
                installedAppsCache.clear()
                installedAppsCache.addAll(list)
                isAppCacheLoaded = true
                Log.d(TAG, "Cached ${list.size} installed apps for background telemetry monitoring")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load installed apps", e)
            }
        }
    }
}
