package lk.novalink.zerotrace

import com.google.gson.Gson
import lk.novalink.zerotrace.data.model.AppUpdateInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateAbiTest {

    private val arm64 = "https://x/ZeroTrace-v1.4.1-arm64.apk"
    private val v7a = "https://x/ZeroTrace-v1.4.1-armeabi-v7a.apk"

    private fun info(urls: Map<String, String>? = mapOf("arm64-v8a" to arm64, "armeabi-v7a" to v7a)) =
        AppUpdateInfo(versionCode = 25, versionName = "1.4.1", downloadUrl = arm64, downloadUrls = urls)

    // ── choosing the APK for this phone ───────────────────────────────────

    @Test
    fun a64BitPhoneGetsTheArm64Build() {
        assertEquals(arm64, info().urlFor(listOf("arm64-v8a", "armeabi-v7a", "armeabi")))
    }

    @Test
    fun a32BitPhoneLikeTheGalaxyM02GetsTheV7aBuild() {
        assertEquals(v7a, info().urlFor(listOf("armeabi-v7a", "armeabi")))
    }

    @Test
    fun aFeedWithoutPerAbiUrlsFallsBackToTheDefaultUrl() {
        assertEquals(arm64, info(urls = null).urlFor(listOf("armeabi-v7a")))
        assertEquals(arm64, info(urls = emptyMap()).urlFor(listOf("armeabi-v7a")))
    }

    @Test
    fun anUnlistedAbiOrBlankEntryFallsBackToTheDefaultUrl() {
        assertEquals(arm64, info().urlFor(listOf("riscv64")))
        assertEquals(arm64, info(mapOf("armeabi-v7a" to "  ")).urlFor(listOf("armeabi-v7a")))
    }

    // ── both feed shapes must survive Gson (which skips Kotlin defaults) ──

    @Test
    fun theNewFeedShapeParses() {
        val json = """{"versionCode":25,"versionName":"1.4.1","downloadUrl":"$arm64",
            "downloadUrls":{"arm64-v8a":"$arm64","armeabi-v7a":"$v7a"},"changelog":"x"}"""
        val parsed = Gson().fromJson(json, AppUpdateInfo::class.java)
        assertEquals(v7a, parsed.urlFor(listOf("armeabi-v7a")))
    }

    @Test
    fun theOldFeedShapeWithoutDownloadUrlsStillParsesAndDoesNotCrash() {
        val json = """{"versionCode":24,"versionName":"1.4.0","downloadUrl":"$arm64","changelog":"x"}"""
        val parsed = Gson().fromJson(json, AppUpdateInfo::class.java)
        assertNull(parsed.downloadUrls)
        assertEquals(arm64, parsed.urlFor(listOf("armeabi-v7a")))
    }

    // ── reading GitHub release asset names ────────────────────────────────

    @Test
    fun assetNamesMapToTheirAbi() {
        assertEquals("arm64-v8a", AppUpdateInfo.abiForAssetName("ZeroTrace-v1.4.1-arm64.apk"))
        assertEquals("arm64-v8a", AppUpdateInfo.abiForAssetName("ZeroTrace-v1.3.1-arm64-v8a.apk"))
        assertEquals("armeabi-v7a", AppUpdateInfo.abiForAssetName("ZeroTrace-v1.4.1-armeabi-v7a.apk"))
        assertEquals("x86_64", AppUpdateInfo.abiForAssetName("ZeroTrace-v1.4.1-x86_64.apk"))
        assertEquals("x86", AppUpdateInfo.abiForAssetName("ZeroTrace-v1.4.1-x86.apk"))
        assertNull(AppUpdateInfo.abiForAssetName("ZeroTrace-v1.4.1.apk"))
    }
}
