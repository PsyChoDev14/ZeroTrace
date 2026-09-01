package lk.novalink.zerotrace.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lk.novalink.zerotrace.data.model.InstalledAppInfo
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// Known Sri Lankan Banking & Local Ride-sharing / Delivery packages and keywords
private val KNOWN_LOCAL_PACKAGES = setOf(
    "com.boc.mobile", "com.boc.itdiv.smartpassbook", "com.sampath.payapp", "lk.combank.mbanking",
    "combank.com.combankdigital", "com.euronetindia.combankqpluscustomer", "lk.hnb.mobile",
    "com.peoplesbank.mobile", "lk.ndb.mobile", "lk.nsb.mobile", "lk.nsbpay.user", "com.seylan.mobile",
    "com.nationstrust.mobilebanking", "com.pickme.passenger", "com.ubercab", "com.daraz.android",
    "net.omobio.dialogsc", "lk.dialog.myaccount", "lk.mobitel.selfcare", "lk.slt.myslt",
    "com.airtel.srilanka", "lk.payhere", "com.dialog.ezcash", "lk.mobitel.mcash", "com.frimi",
    "lk.qplus", "lk.genie.app", "lk.geniebiz", "lk.ipay.consumer", "lk.flash.account",
    "com.ceb.lk.cebcare", "com.SadeepDhananjana.NWSDBSelfCare", "com.sc.bancassurance", "com.hsbc.mobile"
)

private val KNOWN_LOCAL_KEYWORDS = listOf(
    "combank", "boc mobile", "smartpassbook", "sampath", "hnb", "peoples bank", "seylan",
    "pickme", "daraz", "dialog", "mydialog", "mobitel", "myslt", "airtel", "frimi",
    "genie", "ipay", "flash", "qplus", "ezcash", "mcash", "cebcare", "nwsdb"
)

class InstalledAppsRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == Intent.ACTION_PACKAGE_ADDED ||
                action == Intent.ACTION_PACKAGE_REMOVED ||
                action == Intent.ACTION_PACKAGE_REPLACED
            ) {
                val pkgUri = intent.data?.schemeSpecificPart
                if (pkgUri != null && action == Intent.ACTION_PACKAGE_REMOVED) {
                    iconCache.remove(pkgUri)
                }
                preloadInstalledApps()
            }
        }
    }

    init {
        // 1. Pre-warm and index apps in the background on app startup
        preloadInstalledApps()

        // 2. Register dynamic broadcast listener for app installs/uninstalls
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            context.registerReceiver(packageChangeReceiver, filter)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun refreshInstalledApps() {
        preloadInstalledApps()
    }

    fun preloadInstalledApps() {
        scope.launch {
            try {
                loadAppsInternal()
            } catch (e: Throwable) {
                _isLoading.value = false
            }
        }
    }

    private fun loadAppsInternal() {
        val pm = context.packageManager
        val myPkg = context.packageName

        // 1. Get all installed applications
        val packages = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList()
        }

        val launchIntentSet = try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            pm.queryIntentActivities(mainIntent, 0).mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (e: Exception) {
            emptySet()
        }

        val appList = ArrayList<InstalledAppInfo>(packages.size)

        for (pkg in packages) {
            val pkgName = pkg.packageName
            if (pkgName == myPkg) continue

            val isUserInstalled = (pkg.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            val isUpdatedSystem = (pkg.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val hasLaunchIntent = launchIntentSet.contains(pkgName)

            val isSystem = !isUserInstalled && !isUpdatedSystem && !hasLaunchIntent
            val isLaunchable = isUserInstalled || isUpdatedSystem || hasLaunchIntent

            val rawLabel = try {
                pm.getApplicationLabel(pkg).toString().trim()
            } catch (e: Exception) {
                ""
            }

            val finalAppName = if (rawLabel.isBlank() || rawLabel == pkgName) {
                cleanPackageLabel(pkgName)
            } else {
                rawLabel
            }

            val isLocalOrBank = isBankingOrLocalApp(finalAppName, pkgName, isSystem = isSystem, isLaunchable = isLaunchable)

            // Fast icon retrieval (from cache or fast decode)
            val iconBitmap = iconCache.getOrPut(pkgName) {
                try {
                    val d = pm.getApplicationIcon(pkg)
                    drawableToBitmap(d).asImageBitmap()
                } catch (e: Exception) {
                    null
                } ?: return@getOrPut null
            }

            appList.add(
                InstalledAppInfo(
                    packageName = pkgName,
                    appName = finalAppName,
                    iconBitmap = iconBitmap,
                    isSystemApp = isSystem,
                    isLaunchable = isLaunchable,
                    isSuggestedBankingOrLocal = isLocalOrBank
                )
            )
        }

        // Sort: User/Launchable apps first, then alphabetically
        appList.sortWith(
            compareBy<InstalledAppInfo> { it.isSystemApp }
                .thenBy { it.appName.lowercase(Locale.ROOT) }
        )

        _installedApps.value = appList
        _isLoading.value = false
    }

    private fun cleanPackageLabel(pkgName: String): String {
        val lastSegment = pkgName.substringAfterLast('.')
        val words = lastSegment
            .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
            .replace('_', ' ')
            .replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .map { it.replaceFirstChar { c -> c.uppercase() } }

        val base = if (words.isNotEmpty()) words.joinToString(" ") else pkgName
        return if (pkgName.startsWith("com.samsung.") && !base.startsWith("Samsung", ignoreCase = true)) {
            "Samsung $base"
        } else if (pkgName.startsWith("com.google.") && !base.startsWith("Google", ignoreCase = true)) {
            "Google $base"
        } else {
            base
        }
    }

    private fun isBankingOrLocalApp(name: String, pkg: String, isSystem: Boolean, isLaunchable: Boolean): Boolean {
        if (isSystem && !isLaunchable) return false
        val lowerName = name.lowercase(Locale.ROOT)
        val lowerPkg = pkg.lowercase(Locale.ROOT)

        if (KNOWN_LOCAL_PACKAGES.contains(lowerPkg)) return true
        if (KNOWN_LOCAL_KEYWORDS.any { lowerName.contains(it) || lowerPkg.contains(it) }) return true
        if (lowerName.contains("bank") && !lowerName.contains("sandbox")) return true
        return false
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val bmp = drawable.bitmap
            if (!bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
                return bmp
            }
        }
        val width = (if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72).coerceIn(36, 96)
        val height = (if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72).coerceIn(36, 96)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }
}
