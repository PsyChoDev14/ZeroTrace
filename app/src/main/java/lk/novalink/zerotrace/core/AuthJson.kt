package lk.novalink.zerotrace.core

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import lk.novalink.zerotrace.data.model.SubscriptionExpiry
import lk.novalink.zerotrace.data.model.SubscriptionInfo
import lk.novalink.zerotrace.data.model.SubscriptionUsage
import lk.novalink.zerotrace.data.model.UserProfile

/**
 * Tolerant parsing of the NetchSuite API. The backend sends explicit `null` for fields that don't
 * apply (an expiry date on a plan that never expires, a limit on an unlimited plan) and uses two
 * different error shapes, so nothing here trusts a field to be present or the right type. Pure
 * JVM code (no android.*) so it is unit-tested.
 */
object AuthJson {
    const val API_BASE = "https://dash.novalink.lk"

    // The API guide doesn't name the profile-picture field, so accept the common ones.
    private val AVATAR_KEYS = listOf(
        "avatar_url", "avatar", "picture", "profile_picture", "profile_image", "image", "photo", "photo_url"
    )

    class TokenResult(
        val access: String?,
        val refresh: String?,
        val avatarUrl: String,
        /** Backend's own explanation when tokens are absent; empty if the body wasn't JSON at all. */
        val error: String
    )

    private fun parseObject(body: String): JsonObject? = try {
        JsonParser.parseString(body).takeIf { it.isJsonObject }?.asJsonObject
    } catch (e: Exception) {
        null
    }

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.long(key: String): Long? = get(key)?.takeIf { it.isJsonPrimitive }?.let {
        try { it.asBigDecimal.toLong() } catch (e: Exception) { null }
    }

    private fun JsonObject.double(key: String): Double? = get(key)?.takeIf { it.isJsonPrimitive }?.let {
        try { it.asBigDecimal.toDouble() } catch (e: Exception) { null }
    }

    private fun JsonObject.bool(key: String): Boolean? = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean

    private fun JsonObject.obj(key: String): JsonObject? = get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    /**
     * Success responses carry access_token/refresh_token (plus `user` on the code exchange); errors
     * are OAuth-style {error, error_description} with no `success` key, so presence of the tokens is
     * what decides success.
     */
    fun parseTokens(body: String): TokenResult {
        val json = parseObject(body) ?: return TokenResult(null, null, "", "")
        return TokenResult(
            access = json.str("access_token")?.takeIf { it.isNotEmpty() },
            refresh = json.str("refresh_token")?.takeIf { it.isNotEmpty() },
            avatarUrl = avatarFrom(json.obj("user")),
            error = json.str("error_description") ?: json.str("message") ?: json.str("error") ?: ""
        )
    }

    /**
     * First non-empty picture field. Only https URLs (or dashboard-relative paths) are accepted so a
     * bad value can never make the app open a file:// or plain-http address.
     */
    fun avatarFrom(user: JsonObject?): String {
        if (user == null) return ""
        for (key in AVATAR_KEYS) {
            val value = user.str(key)?.trim().orEmpty()
            if (value.isEmpty()) continue
            return when {
                value.startsWith("https://") -> value
                value.startsWith("/") -> API_BASE + value
                else -> continue
            }
        }
        return ""
    }

    fun parseUser(body: String): UserProfile? {
        val data = parseObject(body)?.obj("data") ?: return null
        return UserProfile(
            id = data.long("id") ?: 0L,
            name = data.str("name").orEmpty(),
            email = data.str("email").orEmpty(),
            balance = data.double("balance") ?: 0.0,
            avatarUrl = avatarFrom(data)
        )
    }

    /** Null means "not a subscriptions response" (missing `data`), never "no subscriptions". */
    fun parseSubscriptions(body: String): List<SubscriptionInfo>? {
        val data = parseObject(body)?.get("data")?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
        return data.mapNotNull { element ->
            val s = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val id = s.long("id") ?: return@mapNotNull null
            val expiry = s.obj("expiry")
            val usage = s.obj("usage")
            SubscriptionInfo(
                id = id,
                status = s.str("status").orEmpty(),
                planName = s.str("plan_name").orEmpty(),
                packageName = s.str("package_name").orEmpty(),
                serverLocation = s.str("server_location").orEmpty(),
                configUrl = s.str("config_url").orEmpty(),
                expiry = SubscriptionExpiry(
                    date = expiry?.str("date").orEmpty(),
                    daysRemaining = expiry?.long("days_remaining") ?: 0L,
                    isExpired = expiry?.bool("is_expired") ?: false,
                    neverExpires = expiry?.bool("never_expires") ?: false
                ),
                usage = SubscriptionUsage(
                    downloadGb = usage?.double("download_gb") ?: 0.0,
                    uploadGb = usage?.double("upload_gb") ?: 0.0,
                    totalGb = usage?.double("total_gb") ?: 0.0,
                    limitGb = usage?.double("limit_gb") ?: 0.0,
                    usedPercentage = usage?.double("used_percentage") ?: 0.0
                )
            )
        }
    }

    /** Status plus a body snippet, for when a response isn't the JSON we expected (WAF page, etc). */
    fun describeBadResponse(status: Int, body: String): String =
        "HTTP $status: ${body.trim().take(200).ifEmpty { "empty response" }}"
}
