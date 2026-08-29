package lk.novalink.zerotrace.data.model

import java.util.UUID

data class ProxyConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val protocol: ProxyProtocol,
    val server: String,
    val port: Int,
    val uuid: String = "",           // UUID for VLESS/VMess, or Password for Trojan/SS
    val security: String = "none",   // reality, tls, none
    val network: String = "tcp",     // tcp, ws, grpc, httpupgrade
    val sni: String = "",            // SNI / Server Name / Bug Host
    val path: String = "",           // WS path or HTTP path
    val flow: String = "",           // e.g. xtls-rprx-vision
    val publicKey: String = "",      // Reality Public Key (pbk)
    val shortId: String = "",        // Reality Short ID (sid)
    val fingerprint: String = "chrome",
    val serviceName: String = "",    // gRPC service name
    val alterId: Int = 0,            // VMess alterId
    val cipher: String = "auto",     // Shadowsocks method or VMess cipher
    val rawConfig: String = "",      // Original URI or custom JSON
    val pingMs: Long = -1,           // Latency in milliseconds (-1 = untested)
    val createdAt: Long = System.currentTimeMillis()
) {
    val displaySubtitle: String
        get() = "$server:$port • ${network.uppercase()}" + if (security.isNotEmpty() && security != "none") " • ${security.uppercase()}" else ""
}
