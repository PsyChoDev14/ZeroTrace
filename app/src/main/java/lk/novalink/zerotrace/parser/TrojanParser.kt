package lk.novalink.zerotrace.parser

import android.net.Uri
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object TrojanParser {

    fun parse(uriString: String): ProxyConfig? {
        val trimmed = uriString.trim()
        if (!trimmed.startsWith("trojan://", ignoreCase = true)) return null

        return try {
            val uri = Uri.parse(trimmed)
            val password = uri.userInfo ?: ""
            val host = uri.host ?: ""
            val port = if (uri.port != -1) uri.port else 443

            if (password.isEmpty() || host.isEmpty()) return null

            val fragment = uri.fragment?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
            val name = if (!fragment.isNullOrBlank()) fragment else "NovaLink Trojan ($host)"

            val network = uri.getQueryParameter("type") ?: "tcp"
            val sni = uri.getQueryParameter("sni") ?: uri.getQueryParameter("peer") ?: host
            val path = uri.getQueryParameter("path")?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) } ?: ""
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
