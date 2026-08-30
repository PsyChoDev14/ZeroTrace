package lk.novalink.zerotrace.parser

import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol

object TrojanParser {

    fun parse(uriString: String): ProxyConfig? {
        val trimmed = uriString.trim()
        if (!trimmed.startsWith("trojan://", ignoreCase = true)) return null

        return try {
            val uri = UriHelper.parse(trimmed) ?: return null
            val password = uri.userInfo
            val host = uri.host
            val port = uri.port

            if (password.isEmpty() || host.isEmpty()) return null

            val name = if (!uri.fragment.isNullOrBlank()) uri.fragment else "NovaLink Trojan ($host)"

            val network = uri.getQueryParameter("type") ?: "tcp"
            val sni = uri.getQueryParameter("sni") ?: uri.getQueryParameter("peer") ?: host
            val path = uri.getQueryParameter("path") ?: ""
            val security = uri.getQueryParameter("security") ?: "tls"

            ProxyConfig(
                name = name,
                protocol = ProxyProtocol.TROJAN,
                server = host,
                port = port,
                uuid = password,
                security = security,
                network = network,
                sni = sni,
                path = path,
                rawConfig = trimmed
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
