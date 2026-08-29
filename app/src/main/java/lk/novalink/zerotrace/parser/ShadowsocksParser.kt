package lk.novalink.zerotrace.parser

import android.net.Uri
import android.util.Base64
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ShadowsocksParser {

    fun parse(uriString: String): ProxyConfig? {
        val trimmed = uriString.trim()
        if (!trimmed.startsWith("ss://", ignoreCase = true)) return null

        return try {
            val withoutScheme = trimmed.substring(5)
            val parts = withoutScheme.split("#", limit = 2)
            val mainPart = parts[0]
            val fragment = if (parts.size > 1) {
                URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
            } else null

            var cipher = "aes-256-gcm"
            var password = ""
            var host = ""
            var port = 8388

            if (mainPart.contains("@")) {
                val atSplit = mainPart.split("@", limit = 2)
                val userPart = atSplit[0]
                val hostPort = atSplit[1]

                // userPart might be base64 or plaintext method:password
                val decodedUser = decodeBase64(userPart) ?: userPart
                if (decodedUser.contains(":")) {
                    val authSplit = decodedUser.split(":", limit = 2)
                    cipher = authSplit[0]
                    password = authSplit[1]
                } else {
                    password = decodedUser
                }

                val hpUri = Uri.parse("http://$hostPort")
                host = hpUri.host ?: ""
                port = if (hpUri.port != -1) hpUri.port else 8388
            } else {
                // Entire string might be Base64
                val decoded = decodeBase64(mainPart) ?: return null
                val uri = Uri.parse("ss://$decoded")
                val userInfo = uri.userInfo ?: ""
                if (userInfo.contains(":")) {
                    val authSplit = userInfo.split(":", limit = 2)
                    cipher = authSplit[0]
                    password = authSplit[1]
                }
                host = uri.host ?: ""
                port = if (uri.port != -1) uri.port else 8388
            }

            if (host.isEmpty()) return null
            val name = if (!fragment.isNullOrBlank()) fragment else "NovaLink Shadowsocks ($host)"

            ProxyConfig(
                name = name,
                protocol = ProxyProtocol.SHADOWSOCKS,
                server = host,
                port = port,
                uuid = password,
                cipher = cipher,
                rawConfig = trimmed
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeBase64(input: String): String? {
        val clean = input.replace("-", "+").replace("_", "/")
        val padLength = (4 - clean.length % 4) % 4
        val padded = clean + "=".repeat(padLength)

        return try {
            val bytes = Base64.decode(padded, Base64.DEFAULT or Base64.NO_WRAP)
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
