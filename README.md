# ZeroTrace - Android Xray VPN Client
**NovaLink LK** • Engineered by **Nexaura Core**

ZeroTrace is a modern, high-performance Android VPN client crafted for NovaLink LK customers. It enables users to paste, save, select, and connect their Xray configurations (`vless://`, `vmess://`, `trojan://`, `ss://`, or raw JSON).

---

## Key Features

- **Multi-Protocol Support**:
  - **VLESS**: Reality, Vision (`xtls-rprx-vision`), WebSocket, gRPC, TCP, TLS.
  - **VMess**: Standard Base64 JSON configurations (WS, TCP, TLS).
  - **Trojan**: TLS, gRPC, WebSocket.
  - **Shadowsocks**: Standard SIP002 & 2022 encryption methods.
  - **Custom JSON**: Full custom Xray-Core JSON config strings.
- **One-Tap Paste & Save**:
  - Instant clipboard auto-detection.
  - Automatic parameter extraction (SNI, Bug Host, Reality Public Key, Short ID, Path).
  - Custom naming and persistent local storage.
- **VPN Core & Routing**:
  - Android `VpnService` TUN integration (`10.233.233.2/24`, `1.1.1.1` DNS).
  - Bypass LAN / Local IP subnet routing.
  - Primary DNS selector (Cloudflare, Google, AdGuard Ad-blocking, Quad9).
- **Branded Modern UI (Jetpack Compose)**:
  - Dark Cyberpunk theme with animated glowing Connect button.
  - Real-time download/upload traffic telemetry.
  - Live TCP Ping latency tester for all nodes.
- **Customer Support Integration**:
  - One-tap access to NovaLink LK Telegram & WhatsApp technical support channels.

---

## Project Structure

```
ZeroTrace/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/lk/novalink/zerotrace/
│   │   │   │   ├── MainActivity.kt               # Main Jetpack Compose navigation & VPN controller
│   │   │   │   ├── ZeroTraceApp.kt               # Application entry & repository init
│   │   │   │   ├── core/
│   │   │   │   │   ├── PingEngine.kt             # TCP socket latency tester
│   │   │   │   │   ├── VpnState.kt               # State machine (Connecting, Connected, etc.)
│   │   │   │   │   ├── VpnTunnelManager.kt       # StateFlow singleton bridge
│   │   │   │   │   └── XrayConfigGenerator.kt    # Production Xray JSON generator
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── ProxyConfig.kt        # Configuration data entity
│   │   │   │   │   │   └── ProxyProtocol.kt      # Protocol enum
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── ConfigRepository.kt   # Persistent storage for configs
│   │   │   │   │       └── SettingsRepository.kt # Settings & Support links
│   │   │   │   ├── parser/
│   │   │   │   │   ├── ConfigParser.kt           # Master router
│   │   │   │   │   ├── VlessParser.kt            # VLESS Reality/Vision parser
│   │   │   │   │   ├── VmessParser.kt            # VMess Base64 parser
│   │   │   │   │   ├── TrojanParser.kt           # Trojan parser
│   │   │   │   │   └── ShadowsocksParser.kt      # Shadowsocks parser
│   │   │   │   ├── service/
│   │   │   │   │   └── ZeroTraceVpnService.kt    # Android VpnService & TUN manager
│   │   │   │   └── ui/
│   │   │   │       ├── components/               # ConnectButton, ServerCard, TopHeader, StatusIndicator
│   │   │   │       ├── screens/                  # HomeScreen, ConfigsScreen, AddConfigDialog, SettingsScreen
│   │   │   │       └── theme/                    # Color, Theme, Typography
│   │   │   └── res/                              # Vector icons, strings, colors, adaptive launcher
│   │   └── test/                                 # Parser and generator unit tests
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## How to Build the APK

### Requirements
- **JDK 17**
- **Android SDK** (API 34 / Build Tools 34.0.0)
- **Gradle 8.7+** (or Android Studio Ladybug / Iguana / Hedgehog)

### Build Command (Debug APK)
```bash
./gradlew assembleDebug
```
The APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Command (Release APK for Customers)
```bash
./gradlew assembleRelease
```

---

## Customer Usage Flow

1. Open **ZeroTrace**.
2. Tap the **"+"** button at the top right (or "Change Server" -> "+").
3. Tap **"Paste From Clipboard"** (or paste your `vless://`, `vmess://`, `trojan://` config link).
4. ZeroTrace automatically validates the protocol, extracts host/port/SNI/Reality keys, and saves it.
5. Tap the **Connect** button to establish the VPN tunnel.
