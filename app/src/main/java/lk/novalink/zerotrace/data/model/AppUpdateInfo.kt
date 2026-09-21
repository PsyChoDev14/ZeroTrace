package lk.novalink.zerotrace.data.model

import java.io.File

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    /** arm64 build. Kept for app versions that predate [downloadUrls]. */
    val downloadUrl: String,
    val changelog: String = "",
    val forceUpdate: Boolean = false,
    val releaseDate: String = "",
    val minSupportedVersion: Int = 1,
    /**
     * CPU ABI -> APK URL. Nullable on purpose: Gson builds this class without running Kotlin
     * defaults, so a feed without the field arrives as null, not as an empty map.
     */
    val downloadUrls: Map<String, String>? = null
) {
    /**
     * The APK matching this phone. [supportedAbis] is Build.SUPPORTED_ABIS (most preferred first);
     * a 32-bit phone such as the Galaxy M02 can't install the arm64 build ("App not installed").
     */
    fun urlFor(supportedAbis: List<String>): String {
        val urls = downloadUrls
        if (urls != null) {
            for (abi in supportedAbis) {
                val url = urls[abi]
                if (!url.isNullOrBlank()) return url
            }
        }
        return downloadUrl
    }

    companion object {
        /** Which ABI a release asset is for, judged from its file name; null if it doesn't say. */
        fun abiForAssetName(name: String): String? {
            val n = name.lowercase()
            return when {
                "arm64" in n -> "arm64-v8a"
                "armeabi" in n || "armv7" in n -> "armeabi-v7a"
                "x86_64" in n -> "x86_64"
                "x86" in n -> "x86"
                else -> null
            }
        }
    }
}

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
