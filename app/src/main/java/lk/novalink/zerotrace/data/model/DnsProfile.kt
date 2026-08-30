package lk.novalink.zerotrace.data.model

data class DnsProfile(
    val id: String,
    val name: String,
    val primaryIp: String,
    val secondaryIp: String,
    val description: String,
    val categoryTag: String,
    val isAdBlocker: Boolean = false,
    val isMalwareBlocker: Boolean = false
)

object DnsProviders {
    val ADGUARD_ADBLOCK = DnsProfile(
        id = "adguard",
        name = "AdGuard Ad-Blocker",
        primaryIp = "94.140.14.14",
        secondaryIp = "94.140.15.15",
        description = "Blocks popups, banner ads, and tracking domains phone-wide",
        categoryTag = "🛡️ AD-BLOCK",
        isAdBlocker = true,
        isMalwareBlocker = true
    )

    val CLOUDFLARE_SPEED = DnsProfile(
        id = "cloudflare",
        name = "Cloudflare 1.1.1.1",
        primaryIp = "1.1.1.1",
        secondaryIp = "1.0.0.1",
        description = "Lowest latency DNS resolver optimized for gaming & streaming",
        categoryTag = "⚡ SPEED"
    )

    val CLOUDFLARE_SECURITY = DnsProfile(
        id = "cloudflare_sec",
        name = "Cloudflare Security",
        primaryIp = "1.1.1.2",
        secondaryIp = "1.0.0.2",
        description = "Blocks malicious domains, phishing, and scam sites",
        categoryTag = "🔒 ANTI-MALWARE",
        isMalwareBlocker = true
    )

    val GOOGLE_DNS = DnsProfile(
        id = "google",
        name = "Google Public DNS",
        primaryIp = "8.8.8.8",
        secondaryIp = "8.8.4.4",
        description = "Highly reliable global resolution by Google",
        categoryTag = "🌐 RELIABLE"
    )

    val QUAD9_SECURITY = DnsProfile(
        id = "quad9",
        name = "Quad9 Threat Blocking",
        primaryIp = "9.9.9.9",
        secondaryIp = "149.112.112.112",
        description = "Blocks malicious domains and botnets in real-time",
        categoryTag = "🛡️ SECURITY",
        isMalwareBlocker = true
    )

    val ALL_PROFILES = listOf(
        ADGUARD_ADBLOCK,
        CLOUDFLARE_SPEED,
        CLOUDFLARE_SECURITY,
        GOOGLE_DNS,
        QUAD9_SECURITY
    )

    fun findByPrimaryIp(ip: String): DnsProfile? {
        return ALL_PROFILES.find { it.primaryIp == ip }
    }
}
