package lk.novalink.zerotrace.data.model

import java.io.File

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String = "",
    val forceUpdate: Boolean = false,
    val releaseDate: String = "",
    val minSupportedVersion: Int = 1
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : UpdateState
    data class Downloading(
        val updateInfo: AppUpdateInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateState
    data class ReadyToInstall(
        val updateInfo: AppUpdateInfo,
        val apkFile: File
    ) : UpdateState
    data class Error(val message: String) : UpdateState
    data object UpToDate : UpdateState
}
