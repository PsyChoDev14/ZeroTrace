package lk.novalink.zerotrace.data.model

enum class DpiBypassMode(val title: String, val description: String) {
    OFF(
        title = "Disabled",
        description = "Standard direct connection with maximum throughput"
    ),
    SMART_FRAGMENT(
        title = "Smart TLS Fragment (Recommended)",
        description = "Splits TLS ClientHello into micro-packets to bypass ISP SNI firewalls"
    ),
    DEEP_STEALTH(
        title = "Deep Stealth Engine",
        description = "TLS Packet Fragmentation + Mux.Cool Multiplexing + uTLS Camouflage"
    ),
    CUSTOM(
        title = "Custom Fragment",
        description = "User-defined packet range, chunk size, and delay intervals"
    )
}
