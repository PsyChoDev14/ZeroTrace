package lk.novalink.zerotrace.core

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import lk.novalink.zerotrace.data.model.DpiBypassMode
import lk.novalink.zerotrace.data.model.ProxyConfig
import lk.novalink.zerotrace.data.model.ProxyProtocol

object XrayConfigGenerator {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun generateRuntimeJson(
        config: ProxyConfig,
        socksPort: Int = 10808,
        httpPort: Int = 10809,
        bypassLan: Boolean = true,
        primaryDns: String = "1.1.1.1",
        dpiBypassMode: DpiBypassMode = DpiBypassMode.SMART_FRAGMENT,
        utlsFingerprint: String = "chrome",
        muxEnabled: Boolean = false,
        fragmentPackets: String = "tlshello",
        fragmentLength: String = "10-30",
        fragmentInterval: String = "10-20"
    ): String {
        // If raw custom JSON was provided, return it directly
        if (config.protocol == ProxyProtocol.CUSTOM_JSON && config.rawConfig.startsWith("{")) {
            return config.rawConfig
        }

        val root = JsonObject()

        // 1. Log configuration (warning level minimizes log overhead for max I/O throughput)
        val log = JsonObject().apply {
            addProperty("loglevel", "warning")
        }
        root.add("log", log)

        // 2. High-Performance Core Policy (Enlarged buffer, Fast Handshake, Low Jitter)
        val policy = JsonObject().apply {
            val levels = JsonObject().apply {
                val level0 = JsonObject().apply {
                    addProperty("handshake", 4) // 4s fast handshake
                    addProperty("connIdle", 300) // 300s keep-alive
                    addProperty("uplinkOnly", 2)
                    addProperty("downlinkOnly", 5)
                    addProperty("statsUserUplink", false)
                    addProperty("statsUserDownlink", false)
                    addProperty("bufferSize", 2048) // 2048 KB buffer per connection for gigabit throughput
                }
                add("0", level0)
            }
            add("levels", levels)
            val system = JsonObject().apply {
                addProperty("statsInboundUplink", false)
                addProperty("statsInboundDownlink", false)
                addProperty("statsOutboundUplink", false)
                addProperty("statsOutboundDownlink", false)
            }
            add("system", system)
        }
        root.add("policy", policy)

        // 3. Inbounds (Local SOCKS & HTTP with routeOnly sniffing for zero payload copy overhead)
        val inbounds = JsonArray().apply {
            // SOCKS inbound (used by tun2socks)
            add(JsonObject().apply {
                addProperty("tag", "socks-in")
                addProperty("port", socksPort)
                addProperty("listen", "127.0.0.1")
                addProperty("protocol", "socks")
                add("settings", JsonObject().apply {
                    addProperty("auth", "noauth")
                    addProperty("udp", true)
                })
                add("sniffing", JsonObject().apply {
                    addProperty("enabled", true)
                    addProperty("routeOnly", true) // Ultra-fast: only route, no payload re-buffering
                    add("destOverride", JsonArray().apply {
                        add("http")
                        add("tls")
                        add("quic")
                    })
                })
            })

            // HTTP inbound
            add(JsonObject().apply {
                addProperty("tag", "http-in")
                addProperty("port", httpPort)
                addProperty("listen", "127.0.0.1")
                addProperty("protocol", "http")
            })
        }
        root.add("inbounds", inbounds)

        // 4. Outbounds (Primary Proxy, Direct, Fragment for DPI bypass, and Block)
        val isDpiFragmentActive = dpiBypassMode != DpiBypassMode.OFF
        val isMuxActive = muxEnabled || dpiBypassMode == DpiBypassMode.DEEP_STEALTH

        val effectivePackets = when (dpiBypassMode) {
            DpiBypassMode.SMART_FRAGMENT -> "tlshello"
            DpiBypassMode.DEEP_STEALTH -> "1-3"
            DpiBypassMode.CUSTOM -> fragmentPackets
            DpiBypassMode.OFF -> ""
        }
        val effectiveLength = when (dpiBypassMode) {
            DpiBypassMode.SMART_FRAGMENT -> "10-30"
            DpiBypassMode.DEEP_STEALTH -> "5-20"
            DpiBypassMode.CUSTOM -> fragmentLength
            DpiBypassMode.OFF -> ""
        }
        val effectiveInterval = when (dpiBypassMode) {
            DpiBypassMode.SMART_FRAGMENT -> "10-20"
            DpiBypassMode.DEEP_STEALTH -> "15-30"
            DpiBypassMode.CUSTOM -> fragmentInterval
            DpiBypassMode.OFF -> ""
        }

        val outbounds = JsonArray().apply {
            // Primary Proxy Outbound
            add(buildProxyOutbound(
                config = config,
                isDpiFragmentActive = isDpiFragmentActive,
                isMuxActive = isMuxActive,
                utlsFingerprint = utlsFingerprint
            ))

            // DPI Bypass Fragment Outbound (Freedom dialer with packet fragmentation)
            if (isDpiFragmentActive) {
                add(JsonObject().apply {
                    addProperty("tag", "fragment")
                    addProperty("protocol", "freedom")
                    add("settings", JsonObject().apply {
                        addProperty("domainStrategy", "AsIs")
                        val fragmentObj = JsonObject().apply {
                            addProperty("packets", effectivePackets)
                            addProperty("length", effectiveLength)
                            addProperty("interval", effectiveInterval)
                        }
                        add("fragment", fragmentObj)
                    })
                    add("streamSettings", JsonObject().apply {
                        val sockopt = JsonObject().apply {
                            addProperty("tcpNoDelay", true)
                            addProperty("tcpFastOpen", true)
                            addProperty("tcpKeepAlivePeriod", 15)
                            addProperty("tcpCongestion", "bbr")
                        }
                        add("sockopt", sockopt)
                    })
                })
            }

            // Direct outbound (Freedom with BBR & fast open)
            add(JsonObject().apply {
                addProperty("tag", "direct")
                addProperty("protocol", "freedom")
                add("settings", JsonObject().apply {
                    addProperty("domainStrategy", "UseIPv4")
                })
                add("streamSettings", JsonObject().apply {
                    add("sockopt", buildSockopt(isDirect = true, isDpiFragmentActive = false))
                })
            })

            // Block outbound (Blackhole)
            add(JsonObject().apply {
                addProperty("tag", "block")
                addProperty("protocol", "blackhole")
                add("settings", JsonObject().apply {
                    val response = JsonObject().apply {
                        addProperty("type", "http")
                    }
                    add("response", response)
                })
            })
        }
        root.add("outbounds", outbounds)

        // 5. DNS Settings (Strict IPv4 prioritization, 0ms fast path)
        val profile = lk.novalink.zerotrace.data.model.DnsProviders.findByPrimaryIp(primaryDns)
        val secondaryDns = profile?.secondaryIp ?: "1.0.0.1"

        val dns = JsonObject().apply {
            add("servers", JsonArray().apply {
                add("localhost")
                add(primaryDns)
                if (secondaryDns != primaryDns) {
                    add(secondaryDns)
                }
            })
            addProperty("queryStrategy", "UseIPv4")
        }
        root.add("dns", dns)

        // 6. Routing Rules
        val routing = JsonObject().apply {
            addProperty("domainStrategy", "AsIs")
            val rules = JsonArray().apply {
                // Route all DNS (port 53) traffic direct to prevent DNS loops
                add(JsonObject().apply {
                    addProperty("type", "field")
                    addProperty("outboundTag", "direct")
                    addProperty("port", "53")
                    add("network", JsonArray().apply {
                        add("udp")
                        add("tcp")
                    })
                })

                // Bypass LAN if enabled (using direct CIDR ranges; 0 lookup overhead)
                if (bypassLan) {
                    add(JsonObject().apply {
                        addProperty("type", "field")
                        addProperty("outboundTag", "direct")
                        add("ip", JsonArray().apply {
                            add("10.0.0.0/8")
                            add("172.16.0.0/12")
                            add("192.168.0.0/16")
                            add("127.0.0.0/8")
                            add("100.64.0.0/10")
                            add("::1/128")
                            add("fc00::/7")
                        })
                    })
                }

                // Default all user traffic to proxy
                add(JsonObject().apply {
                    addProperty("type", "field")
                    addProperty("outboundTag", "proxy")
                    add("network", JsonArray().apply {
                        add("tcp")
                        add("udp")
                    })
                })
            }
            add("rules", rules)
        }
        root.add("routing", routing)

        return gson.toJson(root)
    }

    private fun buildProxyOutbound(
        config: ProxyConfig,
        isDpiFragmentActive: Boolean,
        isMuxActive: Boolean,
        utlsFingerprint: String
    ): JsonObject {
        val outbound = JsonObject().apply {
            addProperty("tag", "proxy")
        }

        // Multiplexing (Mux.Cool) for anti-throttling & low connection overhead
        if (isMuxActive) {
            val muxObj = JsonObject().apply {
                addProperty("enabled", true)
                addProperty("concurrency", 8)
                addProperty("xudpConcurrency", 16)
                addProperty("xudpProxyUDP403", "reject")
            }
            outbound.add("mux", muxObj)
        }

        when (config.protocol) {
            ProxyProtocol.VLESS -> {
                outbound.addProperty("protocol", "vless")
                val settings = JsonObject()
                val vnext = JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("address", config.server)
                        addProperty("port", config.port)
                        val users = JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("id", config.uuid)
                                addProperty("encryption", "none")
                                if (config.flow.isNotEmpty()) {
                                    addProperty("flow", config.flow)
                                }
                            })
                        }
                        add("users", users)
                    })
                }
                settings.add("vnext", vnext)
                outbound.add("settings", settings)
                outbound.add("streamSettings", buildStreamSettings(config, isDpiFragmentActive, utlsFingerprint))
            }

            ProxyProtocol.VMESS -> {
                outbound.addProperty("protocol", "vmess")
                val settings = JsonObject()
                val vnext = JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("address", config.server)
                        addProperty("port", config.port)
                        val users = JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("id", config.uuid)
                                addProperty("alterId", config.alterId)
                                addProperty("security", if (config.cipher.isNotEmpty()) config.cipher else "auto")
                            })
                        }
                        add("users", users)
                    })
                }
                settings.add("vnext", vnext)
                outbound.add("settings", settings)
                outbound.add("streamSettings", buildStreamSettings(config, isDpiFragmentActive, utlsFingerprint))
            }

            ProxyProtocol.TROJAN -> {
                outbound.addProperty("protocol", "trojan")
                val settings = JsonObject()
                val servers = JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("address", config.server)
                        addProperty("port", config.port)
                        addProperty("password", config.uuid)
                    })
                }
                settings.add("servers", servers)
                outbound.add("settings", settings)
                outbound.add("streamSettings", buildStreamSettings(config, isDpiFragmentActive, utlsFingerprint))
            }

            ProxyProtocol.SHADOWSOCKS -> {
                outbound.addProperty("protocol", "shadowsocks")
                val settings = JsonObject()
                val servers = JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("address", config.server)
                        addProperty("port", config.port)
                        addProperty("method", if (config.cipher.isNotEmpty()) config.cipher else "aes-256-gcm")
                        addProperty("password", config.uuid)
                        addProperty("ota", false)
                    })
                }
                settings.add("servers", servers)
                outbound.add("settings", settings)
                outbound.add("streamSettings", buildStreamSettings(config, isDpiFragmentActive, utlsFingerprint))
            }

            else -> {
                outbound.addProperty("protocol", "freedom")
                outbound.add("settings", JsonObject())
            }
        }

        return outbound
    }

    private fun buildStreamSettings(
        config: ProxyConfig,
        isDpiFragmentActive: Boolean,
        utlsFingerprint: String
    ): JsonObject {
        val stream = JsonObject()
        val network = if (config.network.isNotEmpty()) config.network.lowercase() else "tcp"
        stream.addProperty("network", network)

        val security = if (config.security.isNotEmpty()) config.security.lowercase() else "none"
        stream.addProperty("security", security)

        val effectiveFp = when {
            config.fingerprint.isNotEmpty() -> config.fingerprint
            utlsFingerprint.isNotEmpty() -> utlsFingerprint
            else -> "chrome"
        }

        // Reality settings
        if (security == "reality") {
            val realitySettings = JsonObject().apply {
                addProperty("show", false)
                addProperty("fingerprint", effectiveFp)
                addProperty("serverName", if (config.sni.isNotEmpty()) config.sni else config.server)
                addProperty("publicKey", config.publicKey)
                addProperty("shortId", config.shortId)
                addProperty("spiderX", "")
            }
            stream.add("realitySettings", realitySettings)
        } else if (security == "tls") {
            val tlsSettings = JsonObject().apply {
                addProperty("serverName", if (config.sni.isNotEmpty()) config.sni else config.server)
                addProperty("verifyPeerCertByName", config.server)
                addProperty("fingerprint", effectiveFp)
            }
            stream.add("tlsSettings", tlsSettings)
        }

        // Transport settings
        when (network) {
            "ws" -> {
                val wsSettings = JsonObject().apply {
                    addProperty("path", if (config.path.isNotEmpty()) config.path else "/")
                    val headers = JsonObject()
                    if (config.sni.isNotEmpty()) {
                        headers.addProperty("Host", config.sni)
                    }
                    add("headers", headers)
                }
                stream.add("wsSettings", wsSettings)
            }
            "grpc" -> {
                val grpcSettings = JsonObject().apply {
                    addProperty("serviceName", if (config.serviceName.isNotEmpty()) config.serviceName else config.path)
                    addProperty("multiMode", true)
                }
                stream.add("grpcSettings", grpcSettings)
            }
            "httpupgrade" -> {
                val httpUpgradeSettings = JsonObject().apply {
                    addProperty("path", if (config.path.isNotEmpty()) config.path else "/")
                    if (config.sni.isNotEmpty()) {
                        addProperty("host", config.sni)
                    }
                }
                stream.add("httpUpgradeSettings", httpUpgradeSettings)
            }
        }

        // Optimized Kernel Socket Options (TCP Fast Open, BBR, DialerProxy for Fragment)
        stream.add("sockopt", buildSockopt(isDirect = false, isDpiFragmentActive = isDpiFragmentActive))

        return stream
    }

    private fun buildSockopt(isDirect: Boolean, isDpiFragmentActive: Boolean): JsonObject {
        return JsonObject().apply {
            addProperty("tcpFastOpen", true)
            addProperty("tcpNoDelay", true) // Disable Nagle's algorithm for minimum latency/ping
            addProperty("tcpKeepAlivePeriod", 15) // Keep connections alive without stalling
            addProperty("tcpCongestion", "bbr") // BBR bottleneck bandwidth & RTT congestion algorithm
            addProperty("tcpMaxSeg", 1360) // Ideal TCP Maximum Segment Size for MTU 1400 (Zero carrier GTP fragmentation)
            if (!isDirect && isDpiFragmentActive) {
                addProperty("dialerProxy", "fragment") // Routes handshake through fragment freedom dialer
            }
        }
    }
}
