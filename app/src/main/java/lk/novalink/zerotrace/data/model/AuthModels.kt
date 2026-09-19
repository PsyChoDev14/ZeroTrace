package lk.novalink.zerotrace.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val id: Long,
    val name: String,
    val email: String,
    val balance: Double,
    val avatarUrl: String
)

@Immutable
data class SubscriptionExpiry(
    val date: String,
    val daysRemaining: Long,
    val isExpired: Boolean,
    val neverExpires: Boolean
)

@Immutable
data class SubscriptionUsage(
    val downloadGb: Double,
    val uploadGb: Double,
    val totalGb: Double,
    val limitGb: Double,
    val usedPercentage: Double
)

@Immutable
data class SubscriptionInfo(
    val id: Long,
    val status: String,
    val planName: String,
    val packageName: String,
    val serverLocation: String,
    val configUrl: String,
    val expiry: SubscriptionExpiry,
    val usage: SubscriptionUsage
) {
    /** Only active plans become server configs. */
    val isActive: Boolean
        get() = !expiry.isExpired && !status.equals("expired", ignoreCase = true)
}
