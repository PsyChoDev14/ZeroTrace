package lk.novalink.zerotrace.parser

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol

object ConfigParser {

    /**
     * Parses a single config line or JSON string.
     */
    fun parseSingle(input: String): ProxyConfig? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> VlessParser.parse(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> VmessParser.parse(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> TrojanParser.parse(trimmed)
            trimmed.startsWith("ss://", ignoreCase = true) -> ShadowsocksParser.parse(trimmed)
            trimmed.startsWith("{") && trimmed.endsWith("}") -> parseCustomJson(trimmed)
            else -> null
        }
    }

    /**
     * Parses multiple configs separated by newlines or subscription text.
     */
    fun parseMultiple(input: String): List<ProxyConfig> {
        val results = mutableListOf<ProxyConfig>()
        val lines = input.lines()

        for (line in lines) {
            val parsed = parseSingle(line.trim())
            if (parsed != null) {
                results.add(parsed)
            }
        }
        return results
    }

    private fun parseCustomJson(jsonString: String): ProxyConfig? {
        return try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            val outbounds = root.getAsJsonArray("outbounds")
            val firstOutbound = outbounds?.firstOrNull()?.asJsonObject

            val protocolStr = firstOutbound?.get("protocol")?.asString?.lowercase() ?: "custom"
            val tag = firstOutbound?.get("tag")?.asString ?: "NovaLink Custom Xray"

            val protocol = when (protocolStr) {
                "vless" -> ProxyProtocol.VLESS
                "vmess" -> ProxyProtocol.VMESS
                "trojan" -> ProxyProtocol.TROJAN
                "shadowsocks" -> ProxyProtocol.SHADOWSOCKS
                else -> ProxyProtocol.CUSTOM_JSON
            }

            ProxyConfig(
                name = tag,
                protocol = protocol,
                server = "Custom Endpoint",
                port = 443,
                rawConfig = jsonString
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
