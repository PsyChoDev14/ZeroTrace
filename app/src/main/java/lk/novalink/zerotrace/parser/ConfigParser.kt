package lk.novalink.zerotrace.parser

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol

object ConfigParser {

    /**
     * Parses a single config line or JSON string with intelligent URI extraction.
     */
    fun parseSingle(input: String): ProxyConfig? {
        var clean = input.trim()
        if (clean.isEmpty()) return null

        // Strip surrounding quotes or markdown backticks
        if ((clean.startsWith("\"") && clean.endsWith("\"")) || (clean.startsWith("'") && clean.endsWith("'"))) {
            clean = clean.substring(1, clean.length - 1).trim()
        }
        if (clean.startsWith("```") && clean.endsWith("```")) {
            clean = clean.removeSurrounding("```").trim()
        }

        // 1. Direct protocol matching
        if (clean.startsWith("vless://", ignoreCase = true)) return VlessParser.parse(clean)
        if (clean.startsWith("vmess://", ignoreCase = true)) return VmessParser.parse(clean)
        if (clean.startsWith("trojan://", ignoreCase = true)) return TrojanParser.parse(clean)
        if (clean.startsWith("ss://", ignoreCase = true)) return ShadowsocksParser.parse(clean)
        if (clean.startsWith("{") && clean.endsWith("}")) return parseCustomJson(clean)

        // 2. Extract embedded URI if copied from chat captions (Telegram / WhatsApp)
        val schemes = listOf("vless://", "vmess://", "trojan://", "ss://")
        for (scheme in schemes) {
            val idx = clean.indexOf(scheme, ignoreCase = true)
            if (idx != -1) {
                val substring = clean.substring(idx)
                // Take until next whitespace or newline
                val candidate = substring.split(Regex("[\\s\\r\\n]+")).firstOrNull { it.startsWith(scheme, ignoreCase = true) } ?: ""
                val parsed = when {
                    candidate.startsWith("vless://", ignoreCase = true) -> VlessParser.parse(candidate)
                    candidate.startsWith("vmess://", ignoreCase = true) -> VmessParser.parse(candidate)
                    candidate.startsWith("trojan://", ignoreCase = true) -> TrojanParser.parse(candidate)
                    candidate.startsWith("ss://", ignoreCase = true) -> ShadowsocksParser.parse(candidate)
                    else -> null
                }
                if (parsed != null) return parsed
            }
        }

        // 3. Extract embedded JSON if present
        val firstBrace = clean.indexOf("{")
        val lastBrace = clean.lastIndexOf("}")
        if (firstBrace != -1 && lastBrace > firstBrace) {
            val jsonCandidate = clean.substring(firstBrace, lastBrace + 1)
            val parsed = parseCustomJson(jsonCandidate)
            if (parsed != null) return parsed
        }

        return null
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
