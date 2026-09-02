package lk.novalink.zerotrace.core

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import lk.novalink.zerotrace.data.model.DnsProviders
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
        dpiBypassMode: DpiBypassMode = DpiBypassMode.OFF,
        utlsFingerprint: String = "chrome",
        muxEnabled: Boolean = false,
        fragmentPackets: String = "tlshello",
        fragmentLength: String = "100-200",
        fragmentInterval: String = "10-20",
        resolvedServerIp: String? = null
    ): String {
        // If raw custom JSON was provided, return it directly
        if (config.protocol == ProxyProtocol.CUSTOM_JSON && config.rawConfig.startsWith("{")) {
            return config.rawConfig
        }

        val root = JsonObject()

        // 1. Log configuration
        val log = JsonObject().apply {
            addProperty("loglevel", "debug")
        }
        root.add("log", log)

        // 2. Core Policy
        val policy = JsonObject().apply {
            val levels = JsonObject().apply {
                val level0 = JsonObject().apply {
                    addProperty("handshake", 4)
                    addProperty("connIdle", 300)
                }
                add("0", level0)
            }
            add("levels", levels)
        }
        root.add("policy", policy)

        // 3. Inbounds (Local SOCKS & HTTP with destOverride sniffing)
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
                    addProperty("routeOnly", true)
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
            DpiBypassMode.DEEP_STEALTH -> "5-15"
            DpiBypassMode.CUSTOM -> fragmentLength
            DpiBypassMode.OFF -> ""
        }
        val effectiveInterval = when (dpiBypassMode) {
            DpiBypassMode.SMART_FRAGMENT -> "10-20"
            DpiBypassMode.DEEP_STEALTH -> "5-10"
            DpiBypassMode.CUSTOM -> fragmentInterval
            DpiBypassMode.OFF -> ""
        }

        val outbounds = JsonArray().apply {
            // 1. Primary Proxy outbound
            add(buildProxyOutbound(config, isDpiFragmentActive, isMuxActive, utlsFingerprint, resolvedServerIp))

            // 2. DPI Bypass Fragment Outbound (if active)
            if (isDpiFragmentActive) {
                add(JsonObject().apply {
                    addProperty("tag", "fragment")
                    addProperty("protocol", "freedom")
                    add("settings", JsonObject().apply {
                        addProperty("domainStrategy", "AsIs")
                        val fragment = JsonObject().apply {
                            addProperty("packets", effectivePackets)
                            addProperty("length", effectiveLength)
                            addProperty("interval", effectiveInterval)
                        }
                        add("fragment", fragment)
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

            // Direct outbound (Freedom)
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

            // DNS outbound — Xray handles DNS queries internally via protected sockets (bypasses VPN TUN)
            add(JsonObject().apply {
                addProperty("tag", "dns-out")
                addProperty("protocol", "dns")
            })

            // Block outbound (Blackhole with type none for clean packet drop)
            add(JsonObject().apply {
                addProperty("tag", "block")
                addProperty("protocol", "blackhole")
                add("settings", JsonObject().apply {
                    val response = JsonObject().apply {
                        addProperty("type", "none")
                    }
                    add("response", response)
                })
            })
        }
        root.add("outbounds", outbounds)

        // 5. DNS Settings
        val dns = JsonObject().apply {
            val hosts = JsonObject().apply {
                addProperty("domain:googleapis.cn", "googleapis.com")
            }
            if (!resolvedServerIp.isNullOrBlank() && config.server.isNotEmpty()) {
                hosts.addProperty(config.server, resolvedServerIp)
            }
            add("hosts", hosts)

            add("servers", JsonArray().apply {
                val dnsProfile = DnsProviders.findByPrimaryIp(primaryDns)
                val primary = if (primaryDns.isNotEmpty()) primaryDns else "1.1.1.1"
                val secondary = dnsProfile?.secondaryIp ?: "8.8.8.8"
                add(primary)
                add(secondary)
                if (primary != "1.1.1.1" && secondary != "1.1.1.1") {
                    add("1.1.1.1")
                }
                if (primary != "8.8.8.8" && secondary != "8.8.8.8") {
                    add("8.8.8.8")
                }
            })
            addProperty("queryStrategy", "UseIPv4")
        }
        root.add("dns", dns)

        // 6. Routing Rules
        val routing = JsonObject().apply {
            addProperty("domainStrategy", "AsIs")
            val rules = JsonArray().apply {
                // 1. Intercept client port 53 DNS queries coming from tun2socks (socks-in)
                //    Route through Xray DNS engine (which uses protected sockets, bypassing the TUN)
                add(JsonObject().apply {
                    addProperty("type", "field")
                    add("inboundTag", JsonArray().apply { add("socks-in") })
                    addProperty("port", "53")
                    addProperty("outboundTag", "dns-out")
                })

                // NOTE: Removed generic "port 53 -> proxy" rule that caused DNS deadlocks.
                // Xray's own DNS outbound uses protected DialerController sockets (bypass VPN TUN),
                // so it does NOT need a routing rule and must NOT be routed to proxy (causes loop).

                // 2. Block Android Private DNS over TLS (port 853) — force plain port 53 DNS
                add(JsonObject().apply {
                    addProperty("type", "field")
                    addProperty("port", "853")
                    addProperty("network", "tcp")
                    addProperty("outboundTag", "block")
                })

                // 3. Block QUIC / HTTP3 (UDP 443) — YouTube, Chrome instantly fall back to TCP HTTPS
                add(JsonObject().apply {
                    addProperty("type", "field")
                    addProperty("port", "443")
                    addProperty("network", "udp")
                    addProperty("outboundTag", "block")
                })

                // 4. Bypass LAN if enabled
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

                // 5. Route the VPN server IP/domain direct so Xray's TLS handshake doesn't re-enter TUN
                if (!resolvedServerIp.isNullOrBlank()) {
                    add(JsonObject().apply {
                        addProperty("type", "field")
                        addProperty("outboundTag", "direct")
                        add("ip", JsonArray().apply { add(resolvedServerIp) })
                    })
                } else if (config.server.isNotEmpty()) {
                    add(JsonObject().apply {
                        addProperty("type", "field")
                        addProperty("outboundTag", "direct")
                        add("domain", JsonArray().apply { add(config.server) })
                    })
                }

                // 6. Route everything else (TCP + UDP) through proxy tunnel
                add(JsonObject().apply {
                    addProperty("type", "field")
                    addProperty("outboundTag", "proxy")
                    addProperty("network", "tcp,udp")
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
        utlsFingerprint: String,
        resolvedServerIp: String? = null
    ): JsonObject {
        val targetServer = if (!resolvedServerIp.isNullOrBlank()) resolvedServerIp else config.server
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
                        addProperty("address", targetServer)
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
                        addProperty("address", targetServer)
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
                        addProperty("address", targetServer)
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
                        addProperty("address", targetServer)
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

            ProxyProtocol.CUSTOM_JSON -> {
                return JsonParser.parseString(config.rawConfig).asJsonObject
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
                val sniHost = if (config.sni.isNotEmpty()) config.sni else config.server
                addProperty("serverName", sniHost)
                addProperty("allowInsecure", true)
                addProperty("fingerprint", effectiveFp)
                val alpn = JsonArray().apply {
                    add("http/1.1")
                    add("h2")
                }
                add("alpn", alpn)
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

        // Kernel Socket Options
        stream.add("sockopt", buildSockopt(isDirect = false, isDpiFragmentActive = isDpiFragmentActive))

        return stream
    }

    private fun buildSockopt(isDirect: Boolean, isDpiFragmentActive: Boolean): JsonObject {
        return JsonObject().apply {
            addProperty("tcpNoDelay", true) // Disable Nagle's algorithm for minimum latency/ping
            addProperty("tcpKeepAlivePeriod", 15) // Keep connections alive without stalling
            if (!isDirect && isDpiFragmentActive) {
                addProperty("dialerProxy", "fragment") // Routes handshake through fragment freedom dialer
            }
        }
    }
}
