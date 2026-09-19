package lk.novalink.zerotrace

import lk.novalink.zerotrace.core.AuthJson
import lk.novalink.zerotrace.core.Pkce
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol
import lk.novalink.zerotrace.data.repository.ConfigRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthTest {

    // ── PKCE ──────────────────────────────────────────────────────────────

    @Test
    fun pkceMatchesRfc7636AppendixBVector() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challengeFor("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        )
    }

    @Test
    fun base64UrlHandlesEveryPaddingCase() {
        assertEquals("", Pkce.base64Url(ByteArray(0)))
        assertEquals("_w", Pkce.base64Url(byteArrayOf(0xFF.toByte())))
        assertEquals("__8", Pkce.base64Url(byteArrayOf(0xFF.toByte(), 0xFF.toByte())))
        assertEquals("-_8", Pkce.base64Url(byteArrayOf(0xFB.toByte(), 0xFF.toByte())))
        assertEquals("Zm9vYmFy", Pkce.base64Url("foobar".toByteArray()))
    }

    @Test
    fun generatedVerifierAndStateAreValid() {
        val verifier = Pkce.generateVerifier()
        assertTrue(verifier.length in 43..128)
        assertTrue(verifier.all { it.isLetterOrDigit() || it in "-._~" })
        assertEquals(32, Pkce.generateState().length)
        assertNotEquals(Pkce.generateVerifier(), Pkce.generateVerifier())
    }

    // ── Token responses ───────────────────────────────────────────────────

    @Test
    fun tokenSuccessCarriesTokensAndAvatarFromUser() {
        val body = """{"access_token":"a","refresh_token":"r","user":{"id":1,"name":"N",
            "picture":"https://lh3.googleusercontent.com/x=s96","avatar_url":"https://lh3.googleusercontent.com/x=s96"}}"""
        val r = AuthJson.parseTokens(body)
        assertEquals("a", r.access)
        assertEquals("r", r.refresh)
        assertEquals("https://lh3.googleusercontent.com/x=s96", r.avatarUrl)
    }

    @Test
    fun tokenErrorsInBothBackendShapesAreReadable() {
        val oauth = AuthJson.parseTokens("""{"error":"invalid_grant","error_description":"Invalid authorization code"}""")
        assertNull(oauth.access)
        assertEquals("Invalid authorization code", oauth.error)
        assertEquals("nope", AuthJson.parseTokens("""{"success":false,"message":"nope"}""").error)
    }

    @Test
    fun nonJsonTokenBodyYieldsNoTokensAndNoError() {
        val r = AuthJson.parseTokens("<html>403 Forbidden</html>")
        assertNull(r.access)
        assertEquals("", r.error)
        assertEquals("HTTP 403: <html>403 Forbidden</html>", AuthJson.describeBadResponse(403, "<html>403 Forbidden</html>"))
    }

    // ── Avatar ────────────────────────────────────────────────────────────

    private fun user(json: String) = com.google.gson.JsonParser.parseString(json).asJsonObject

    @Test
    fun avatarPicksFirstUsableFieldAndResolvesRelativePaths() {
        assertEquals("https://p/x.png", AuthJson.avatarFrom(user("""{"picture":"https://p/x.png"}""")))
        assertEquals("https://a/1.png", AuthJson.avatarFrom(user("""{"avatar_url":"https://a/1.png","picture":"https://p/2.png"}""")))
        assertEquals("https://dash.novalink.lk/static/a.png", AuthJson.avatarFrom(user("""{"avatar_url":"/static/a.png"}""")))
        assertEquals("", AuthJson.avatarFrom(user("""{"avatar_url":null,"picture":""}""")))
        assertEquals("", AuthJson.avatarFrom(null))
    }

    @Test
    fun avatarRejectsNonHttpsSchemes() {
        assertEquals("", AuthJson.avatarFrom(user("""{"avatar_url":"http://insecure/x.png"}""")))
        assertEquals("", AuthJson.avatarFrom(user("""{"avatar_url":"file:///data/x.png"}""")))
        assertEquals("", AuthJson.avatarFrom(user("""{"avatar_url":"content://x/y"}""")))
        // A bad first field must not hide a good later one.
        assertEquals("https://p/ok.png", AuthJson.avatarFrom(user("""{"avatar_url":"file:///x","picture":"https://p/ok.png"}""")))
    }

    // ── User & subscriptions ──────────────────────────────────────────────

    @Test
    fun userToleratesNullsAndMissingFields() {
        val u = AuthJson.parseUser("""{"success":true,"data":{"id":104,"name":null,"email":"a@b.c","balance":380.0}}""")!!
        assertEquals(104L, u.id)
        assertEquals("", u.name)
        assertEquals(380.0, u.balance, 0.0)
        assertNull(AuthJson.parseUser("""{"error":"x"}"""))
    }

    @Test
    fun subscriptionsToleratNullFieldsLikeTheRealBackend() {
        val body = """{"count":2,"data":[
            {"id":7,"status":"active","plan_name":null,"package_name":"Pkg","server_location":null,
             "config_url":"vless://x@h:443","expiry":{"date":null,"days_remaining":null,"is_expired":false,"never_expires":true},
             "usage":{"download_gb":1.5,"upload_gb":null,"total_gb":1.5,"limit_gb":null,"used_percentage":null}},
            {"status":"active"},
            {"id":9,"status":"expired","expiry":{"is_expired":true}}
        ]}"""
        val subs = AuthJson.parseSubscriptions(body)!!
        assertEquals(listOf(7L, 9L), subs.map { it.id })   // the id-less entry is skipped
        val first = subs[0]
        assertEquals("", first.planName)
        assertEquals("Pkg", first.packageName)
        assertTrue(first.expiry.neverExpires)
        assertEquals(0.0, first.usage.limitGb, 0.0)
        assertTrue(first.isActive)
        assertFalse(subs[1].isActive)
    }

    @Test
    fun errorBodyIsNotMistakenForAnEmptySubscriptionList() {
        assertNull(AuthJson.parseSubscriptions("""{"error":"server_error"}"""))
        assertNull(AuthJson.parseSubscriptions("<html>502</html>"))
        assertEquals(0, AuthJson.parseSubscriptions("""{"data":[]}""")!!.size)
    }

    @Test
    fun expiredByStatusAloneIsNotActive() {
        val sub = AuthJson.parseSubscriptions("""{"data":[{"id":1,"status":"Expired","expiry":{"is_expired":false}}]}""")!!.single()
        assertFalse(sub.isActive)
    }

    // ── Sync rules ────────────────────────────────────────────────────────

    private fun cfg(name: String, subId: Long? = null, ping: Long = -1) = ProxyConfig(
        name = name, protocol = ProxyProtocol.VLESS, server = "$name.host", port = 443, pingMs = ping, subscriptionId = subId
    )

    @Test
    fun syncNeverTouchesManualConfigs() {
        val manual = cfg("manual")
        val out = ConfigRepository.planSync(listOf(manual), emptySet(), emptyList())
        assertEquals(listOf(manual), out)
    }

    @Test
    fun syncDropsOtherAccountsAndExpiredPlans() {
        val mine = cfg("mine", 1)
        val expired = cfg("expired", 2)
        val otherAccount = cfg("theirs", 99)
        val out = ConfigRepository.planSync(listOf(mine, expired, otherAccount), setOf(1L), listOf(cfg("mine", 1)))
        assertEquals(listOf(1L), out.map { it.subscriptionId })
    }

    @Test
    fun syncUpdatesInPlaceKeepingIdentityAndPing() {
        val existing = cfg("old name", 1, ping = 88)
        val out = ConfigRepository.planSync(listOf(existing), setOf(1L), listOf(cfg("new name", 1)))
        assertEquals(1, out.size)
        assertEquals("new name", out[0].name)
        assertEquals(existing.id, out[0].id)
        assertEquals(88L, out[0].pingMs)
    }

    @Test
    fun syncAddsNewSubscriptionsAtTheFront() {
        val manual = cfg("manual")
        val out = ConfigRepository.planSync(listOf(manual), setOf(5L), listOf(cfg("fresh", 5)))
        assertEquals(listOf("fresh", "manual"), out.map { it.name })
    }

    @Test
    fun aLinkThatFailedToParseDoesNotDeleteItsExistingConfig() {
        val existing = cfg("kept", 3)
        // Subscription 3 is active (in activeIds) but produced no parsed config this time.
        val out = ConfigRepository.planSync(listOf(existing), setOf(3L), emptyList())
        assertNotNull(out.singleOrNull { it.subscriptionId == 3L })
    }
}
