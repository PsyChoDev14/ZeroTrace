package lk.novalink.zerotrace.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import lk.novalink.zerotrace.data.model.AppUpdateInfo
import lk.novalink.zerotrace.data.model.UpdateState
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val TAG = "ZeroTrace-Update"
    
    // Default GitHub raw / Server endpoint where version.json is hosted
    const val DEFAULT_UPDATE_URL = "https://raw.githubusercontent.com/PsyChoDev14/ZeroTrace/main/version.json"

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

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

    suspend fun checkForUpdates(
        context: Context,
        endpointUrl: String = DEFAULT_UPDATE_URL,
        isManualCheck: Boolean = false
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            _updateState.value = UpdateState.Checking

            val url = URL(endpointUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ZeroTrace-Android-Client")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val updateInfo = gson.fromJson(jsonString, AppUpdateInfo::class.java)

                val currentVersion = getCurrentVersionCode(context)
                Log.d(TAG, "Current versionCode: $currentVersion, Remote versionCode: ${updateInfo.versionCode}")

                if (updateInfo.versionCode > currentVersion) {
                    _updateState.value = UpdateState.UpdateAvailable(updateInfo)
                    return@withContext updateInfo
                } else {
                    _updateState.value = if (isManualCheck) UpdateState.UpToDate else UpdateState.Idle
                    return@withContext null
                }
            } else {
                Log.e(TAG, "Failed to check update. HTTP response: ${connection.responseCode}")
                _updateState.value = if (isManualCheck) {
                    UpdateState.Error("Server returned code ${connection.responseCode}")
                } else {
                    UpdateState.Idle
                }
                return@withContext null
            }
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

    suspend fun downloadUpdate(
        context: Context,
        updateInfo: AppUpdateInfo
    ): File? = withContext(Dispatchers.IO) {
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
            val apkFile = File(updateDir, "ZeroTrace-v${updateInfo.versionName}.apk")

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
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

            _updateState.value = UpdateState.ReadyToInstall(updateInfo, apkFile)
            return@withContext apkFile

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update APK", e)
            _updateState.value = UpdateState.Error(e.localizedMessage ?: "Download failed")
            return@withContext null
        }
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
