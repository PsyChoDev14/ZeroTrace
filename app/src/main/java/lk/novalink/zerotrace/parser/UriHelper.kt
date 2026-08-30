package lk.novalink.zerotrace.parser

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object UriHelper {

    data class ParsedUri(
        val scheme: String,
        val userInfo: String,
        val host: String,
        val port: Int,
        val queryParams: Map<String, String>,
        val fragment: String?
    ) {
        fun getQueryParameter(key: String): String? = queryParams[key]
    }

    fun parse(uriString: String): ParsedUri? {
        try {
            var raw = uriString.trim()
            val schemeIndex = raw.indexOf("://")
            if (schemeIndex == -1) return null
            val scheme = raw.substring(0, schemeIndex).lowercase()
            raw = raw.substring(schemeIndex + 3)

            // Extract fragment (#Name)
            val fragmentIndex = raw.indexOf("#")
            val fragment = if (fragmentIndex != -1) {
                val f = raw.substring(fragmentIndex + 1)
                raw = raw.substring(0, fragmentIndex)
                try {
                    URLDecoder.decode(f, StandardCharsets.UTF_8.name())
                } catch (e: Exception) {
                    f
                }
            } else null

            // Extract Query parameters (?key=value)
            val queryIndex = raw.indexOf("?")
            val queryMap = mutableMapOf<String, String>()
            if (queryIndex != -1) {
                val queryString = raw.substring(queryIndex + 1)
                raw = raw.substring(0, queryIndex)
                for (pair in queryString.split("&")) {
                    if (pair.isNotBlank()) {
                        val eq = pair.indexOf("=")
                        if (eq != -1) {
                            val key = pair.substring(0, eq)
                            val value = pair.substring(eq + 1)
                            queryMap[key] = try {
                                URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                            } catch (e: Exception) {
                                value
                            }
                        } else {
                            queryMap[pair] = ""
                        }
                    }
                }
            }

            // Extract userInfo (@user)
            val atIndex = raw.lastIndexOf("@")
            val userInfo = if (atIndex != -1) {
                val u = raw.substring(0, atIndex)
                raw = raw.substring(atIndex + 1)
                u
            } else ""

            // Extract host and port
            val colonIndex = raw.lastIndexOf(":")
            val host: String
            val port: Int
            if (colonIndex != -1 && colonIndex > raw.lastIndexOf("]")) {
                host = raw.substring(0, colonIndex).removePrefix("[").removeSuffix("]")
                port = raw.substring(colonIndex + 1).toIntOrNull() ?: 443
            } else {
                host = raw.removePrefix("[").removeSuffix("]")
                port = 443
            }

            return ParsedUri(
                scheme = scheme,
                userInfo = userInfo,
                host = host,
                port = port,
                queryParams = queryMap,
                fragment = fragment
            )
        } catch (e: Exception) {
            return null
        }
    }
}
