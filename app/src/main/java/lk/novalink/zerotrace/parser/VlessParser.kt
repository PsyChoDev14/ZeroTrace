package lk.novalink.zerotrace.parser

import android.net.Uri
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object VlessParser {

    fun parse(uriString: String): ProxyConfig? {
        val trimmed = uriString.trim()
        if (!trimmed.startsWith("vless://", ignoreCase = true)) return null

        return try {
            val uri = Uri.parse(trimmed)
            val uuid = uri.userInfo ?: ""
            val host = uri.host ?: ""
            val port = if (uri.port != -1) uri.port else 443

            if (uuid.isEmpty() || host.isEmpty()) return null

            val fragment = uri.fragment?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
            val name = if (!fragment.isNullOrBlank()) fragment else "NovaLink VLESS ($host)"

            val security = uri.getQueryParameter("security") ?: "none"
            val network = uri.getQueryParameter("type") ?: "tcp"
            val flow = uri.getQueryParameter("flow") ?: ""
            val sni = uri.getQueryParameter("sni") ?: uri.getQueryParameter("host") ?: ""
            val path = uri.getQueryParameter("path")?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) } ?: ""
            val pbk = uri.getQueryParameter("pbk") ?: ""
            val sid = uri.getQueryParameter("sid") ?: ""
            val fp = uri.getQueryParameter("fp") ?: "chrome"
            val serviceName = uri.getQueryParameter("serviceName") ?: ""

            ProxyConfig(
                name = name,
                protocol = ProxyProtocol.VLESS,
                server = host,
                port = port,
                uuid = uuid,
                security = security,
                network = network,
                sni = sni,
                path = path,
                flow = flow,
                publicKey = pbk,
                shortId = sid,
                fingerprint = fp,
                serviceName = serviceName,
                rawConfig = trimmed
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
