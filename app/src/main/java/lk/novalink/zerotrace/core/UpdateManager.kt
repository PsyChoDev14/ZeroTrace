package lk.novalink.zerotrace.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import lk.novalink.zerotrace.R
import lk.novalink.zerotrace.data.model.AppUpdateInfo
import lk.novalink.zerotrace.data.model.UpdateState
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

object UpdateManager {

    private const val TAG = "ZeroTrace-Update"
    private const val UPDATE_CHANNEL_ID = "zerotrace_app_updates"
    private const val UPDATE_NOTIFICATION_ID = 2001
    
    // Default GitHub raw endpoint where version.json is hosted
    const val DEFAULT_UPDATE_URL = "https://raw.githubusercontent.com/PsyChoDev14/ZeroTrace/main/version.json"

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    @Volatile
    private var isCancelled = false

    private val gson = Gson()

    fun getCurrentVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    const val GITHUB_RELEASES_API_URL = "https://api.github.com/repos/PsyChoDev14/ZeroTrace/releases/latest"

    suspend fun checkForUpdates(
        context: Context,
        endpointUrl: String = DEFAULT_UPDATE_URL,
        isManualCheck: Boolean = false
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            _updateState.value = UpdateState.Checking
            val currentVersionCode = getCurrentVersionCode(context)
            val currentVersionName = getCurrentVersionName(context)

            // 1. First attempt: version.json
            var updateInfo = fetchVersionJson(endpointUrl)

            // 2. Fallback / Complementary attempt: GitHub Releases API
            // Only use GH API if version.json didn't find anything newer
            if (updateInfo == null || updateInfo.versionCode <= currentVersionCode) {
                val ghUpdateInfo = fetchGitHubLatestRelease()
                if (ghUpdateInfo != null) {
                    // GitHub API fallback uses ONLY semantic version comparison
                    // (versionCode is set to -1 as placeholder, never used for comparison)
                    val isGenuinelyNewer = isVersionNewer(ghUpdateInfo.versionName, currentVersionName)
                    if (isGenuinelyNewer) {
                        updateInfo = ghUpdateInfo
                    }
                }
            }

            if (updateInfo != null) {
                // For version.json updates: compare versionCode; for GH API fallback (versionCode=-1): compare semver only
                val isNewer = if (updateInfo.versionCode > 0) {
                    updateInfo.versionCode > currentVersionCode
                } else {
                    isVersionNewer(updateInfo.versionName, currentVersionName)
                }

                if (isNewer) {
                    Log.d(TAG, "Update available! Current: $currentVersionName ($currentVersionCode), New: ${updateInfo.versionName}")
                    _updateState.value = UpdateState.UpdateAvailable(updateInfo)
                    postUpdateNotification(context, updateInfo)
                    return@withContext updateInfo
                }
            }

            Log.d(TAG, "App is up to date: $currentVersionName ($currentVersionCode)")
            _updateState.value = if (isManualCheck) UpdateState.UpToDate else UpdateState.Idle
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            _updateState.value = if (isManualCheck) {
                UpdateState.Error(e.localizedMessage ?: "Failed to connect to update server")
            } else {
                UpdateState.Idle
            }
            return@withContext null
        }
    }

    private fun fetchVersionJson(endpointUrl: String): AppUpdateInfo? {
        return try {
            val url = URL(endpointUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ZeroTrace-Android-Client")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                gson.fromJson(jsonString, AppUpdateInfo::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchGitHubLatestRelease(): AppUpdateInfo? {
        return try {
            val url = URL(GITHUB_RELEASES_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ZeroTrace-Android-Client")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val root = com.google.gson.JsonParser.parseString(jsonString).asJsonObject
                val tagName = root.get("tag_name")?.asString?.removePrefix("v") ?: ""
                val body = root.get("body")?.asString ?: "Bug fixes and performance improvements."
                val assets = root.getAsJsonArray("assets")

                var downloadUrl = ""
                if (assets != null) {
                    for (element in assets) {
                        val assetObj = element.asJsonObject
                        val name = assetObj.get("name")?.asString ?: ""
                        val browserUrl = assetObj.get("browser_download_url")?.asString ?: ""
                        if (name.contains("arm64", ignoreCase = true) || name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = browserUrl
                            break
                        }
                    }
                }

                if (tagName.isNotEmpty() && downloadUrl.isNotEmpty()) {
                    AppUpdateInfo(
                        versionCode = -1, // placeholder — GH API; compared via semver only
                        versionName = tagName,
                        downloadUrl = downloadUrl,
                        changelog = body,
                        forceUpdate = false,
                        releaseDate = ""
                    )
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        try {
            val remoteParts = remote.removePrefix("v").split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = current.removePrefix("v").split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = if (i < remoteParts.size) remoteParts[i] else 0
                val c = if (i < currentParts.size) currentParts[i] else 0
                if (r > c) return true
                if (r < c) return false
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return false
    }

    /**
     * Posts a high-priority system notification notifying the user of an available update.
     */
    fun postUpdateNotification(context: Context, updateInfo: AppUpdateInfo) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    UPDATE_CHANNEL_ID,
                    "App Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications when a new version of ZeroTrace is available"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_SHOW_UPDATE", true)
            }

            val pendingIntent = if (launchIntent != null) {
                PendingIntent.getActivity(
                    context,
                    UPDATE_NOTIFICATION_ID,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
            } else null

            val notificationBuilder = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_qs_tile)
                .setContentTitle("🚀 ZeroTrace v${updateInfo.versionName} Available!")
                .setContentText("A new version with performance improvements is ready.")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "ZeroTrace v${updateInfo.versionName} is now available!\n\n${updateInfo.changelog}\n\nTap to download & install the update."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            if (pendingIntent != null) {
                notificationBuilder.setContentIntent(pendingIntent)
                notificationBuilder.addAction(
                    android.R.drawable.stat_sys_download,
                    "Update Now",
                    pendingIntent
                )
            }

            notificationManager.notify(UPDATE_NOTIFICATION_ID, notificationBuilder.build())
            Log.d(TAG, "Update notification posted for v${updateInfo.versionName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post update notification", e)
        }
    }

    suspend fun downloadUpdate(
        context: Context,
        updateInfo: AppUpdateInfo
    ): File? = withContext(Dispatchers.IO) {
        isCancelled = false
        var apkFile: File? = null
        try {
            _updateState.value = UpdateState.Downloading(
                updateInfo = updateInfo,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = -1L
            )

            val url = URL(updateInfo.downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ZeroTrace-Android-Client")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                _updateState.value = UpdateState.Error("Download failed with HTTP ${connection.responseCode}")
                return@withContext null
            }

            val totalBytes = connection.contentLengthLong
            val updateDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
            apkFile = File(updateDir, "ZeroTrace-v${updateInfo.versionName}.apk")

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(64 * 1024) // 64 KB high-throughput streaming buffer
                    var bytesRead = 0
                    var totalRead = 0L

                    while (coroutineContext.isActive && !isCancelled && input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes.toFloat() else 0f
                        _updateState.value = UpdateState.Downloading(
                            updateInfo = updateInfo,
                            progress = progress,
                            downloadedBytes = totalRead,
                            totalBytes = totalBytes
                        )
                    }
                }
            }

            if (isCancelled || !coroutineContext.isActive) {
                apkFile.delete()
                _updateState.value = UpdateState.Idle
                return@withContext null
            }

            _updateState.value = UpdateState.ReadyToInstall(updateInfo, apkFile)
            return@withContext apkFile

        } catch (e: Exception) {
            if (isCancelled) {
                apkFile?.delete()
                _updateState.value = UpdateState.Idle
                return@withContext null
            }
            Log.e(TAG, "Error downloading update APK", e)
            _updateState.value = UpdateState.Error(e.localizedMessage ?: "Download failed")
            return@withContext null
        }
    }

    fun cancelDownload() {
        isCancelled = true
        _updateState.value = UpdateState.Idle
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                _updateState.value = UpdateState.Error("APK file not found")
                return
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting APK installation intent", e)
            _updateState.value = UpdateState.Error("Failed to open installer: ${e.localizedMessage}")
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }
}
