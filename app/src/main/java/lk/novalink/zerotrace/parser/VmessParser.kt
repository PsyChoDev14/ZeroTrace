package lk.novalink.zerotrace.parser

import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol
import java.nio.charset.StandardCharsets

object VmessParser {

    fun parse(uriString: String): ProxyConfig? {
        val trimmed = uriString.trim()
        if (!trimmed.startsWith("vmess://", ignoreCase = true)) return null

        return try {
            val base64Content = trimmed.substring(8).trim()
            val decodedJson = decodeBase64(base64Content) ?: return null
            val json = JsonParser.parseString(decodedJson).asJsonObject

            val host = json.getOptionalString("add") ?: return null
            val port = json.getOptionalInt("port") ?: 443
            val uuid = json.getOptionalString("id") ?: return null
            val alterId = json.getOptionalInt("aid") ?: 0
            val cipher = json.getOptionalString("scy") ?: "auto"
            val network = json.getOptionalString("net") ?: "tcp"
            val rawTls = json.getOptionalString("tls") ?: "none"
            val security = if (rawTls.equals("tls", ignoreCase = true)) "tls" else "none"
            val sni = json.getOptionalString("sni") ?: json.getOptionalString("host") ?: ""
            val path = json.getOptionalString("path") ?: ""
            val ps = json.getOptionalString("ps")
            val name = if (!ps.isNullOrBlank()) ps else "NovaLink VMess ($host)"

            ProxyConfig(
                name = name,
                protocol = ProxyProtocol.VMESS,
                server = host,
                port = port,
                uuid = uuid,
                alterId = alterId,
                cipher = cipher,
                network = network,
                security = security,
                sni = sni,
                path = path,
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

    private fun JsonObject.getOptionalString(key: String): String? {
        return if (has(key) && !get(key).isJsonNull) {
            get(key).asString.trim()
        } else null
    }

    private fun JsonObject.getOptionalInt(key: String): Int? {
        return if (has(key) && !get(key).isJsonNull) {
            try {
                get(key).asInt
            } catch (e: Exception) {
                get(key).asString.toIntOrNull()
            }
        } else null
    }
}
