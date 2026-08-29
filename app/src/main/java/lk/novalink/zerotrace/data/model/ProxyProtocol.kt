package lk.novalink.zerotrace.data.model

enum class ProxyProtocol(val displayName: String) {
    VLESS("VLESS"),
    VMESS("VMess"),
    TROJAN("Trojan"),
    SHADOWSOCKS("Shadowsocks"),
    CUSTOM_JSON("Custom JSON")
}
