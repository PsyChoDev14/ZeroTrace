<div align="center">

# ⚡ ZeroTrace VPN
### *Ultra-Fast • Anti-Censorship • Privacy-First Android Xray Client*

[![Latest Release](https://img.shields.io/github/v/release/PsyChoDev14/ZeroTrace?color=5468FF&label=Release&logo=github&style=for-the-badge)](https://github.com/PsyChoDev14/ZeroTrace/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2024--34)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/PsyChoDev14/ZeroTrace)
[![Core Engine](https://img.shields.io/badge/Engine-Xray--Core%20v1.8.11-7C4DFF?style=for-the-badge&logo=shield&logoColor=white)](https://github.com/XTLS/Xray-core)
[![License](https://img.shields.io/badge/License-MIT-FFA502?style=for-the-badge)](LICENSE)
[![Status](https://img.shields.io/badge/Build-Passing%20(R8%20Optimized)-35C77B?style=for-the-badge&logo=checkmarx)](https://github.com/PsyChoDev14/ZeroTrace/releases)

<br/>

[📥 **Download Latest APK (v1.0.6)**](https://github.com/PsyChoDev14/ZeroTrace/releases/latest/download/ZeroTrace-v1.0.6-arm64.apk) • [✨ **Features**](#-key-features) • [🚀 **Getting Started**](#-getting-started) • [💻 **Tech Stack**](#-tech-stack--architecture)

---

</div>

## 🌟 Overview

**ZeroTrace** is a next-generation, high-performance Android VPN and proxy client powered by **Xray-Core** and native **tun2socks** bridging. Engineered from the ground up with **Jetpack Compose** and **Kotlin Coroutines**, ZeroTrace delivers unmatched connection speed, anti-censorship DPI bypassing (VLESS Reality), per-app split tunneling, phone-wide ad-blocking, and seamless background OTA auto-updates.

---

## ✨ Key Features

### 🛡️ 1. Advanced Anti-Censorship & Encryption
* **VLESS XTLS Reality & Vision:** Eliminates traditional VPN signatures. Traffic is indistinguishable from standard TLS connections to foreign CDN domains (Apple, Microsoft, Cloudflare).
* **Deep Packet Inspection (DPI) Resistance:** Resists active probing and throttling on restricted mobile carriers (Dialog, Mobitel, SLT, Airtel).
* **Multi-Protocol Powerhouse:** Full support for `VLESS`, `VMess`, `Trojan`, `Shadowsocks (2022/SIP002)`, and raw custom `Xray JSON`.

### 🔀 2. Per-App Split Tunneling
* **Bypass Mode (Recommended):** Route sensitive local apps (*Commercial Bank, BOC, Sampath, HNB, PickMe, Uber, Daraz, Dialog MyAccount*) directly through your regular ISP while the rest of your device stays protected under the VPN.
* **VPN-Only Mode:** Restrict VPN tunneling strictly to selected apps (e.g., YouTube, Telegram, Zoom, Chrome).
* **Smart Local App Detection:** Automatically highlights Sri Lankan banking and delivery apps with `🏦 LOCAL/BANK` badges.

### 🛡️ 3. Built-in AdGuard Ad-Blocker & Security DNS
* **AdGuard Ad-Blocking DNS (`94.140.14.14`):** Blocks annoying popups, banner ads, and tracking telemetry phone-wide at the DNS level.
* **Cloudflare Speed DNS (`1.1.1.1`):** Ultra-low latency DNS resolver optimized for gaming and 4K streaming.
* **Cloudflare Security (`1.1.1.2`):** Real-time blocking of phishing, scam, and malware hosts.
* **Quad9 Threat Defense (`9.9.9.9`):** Malicious domain and botnet filtering.

### 🔘 4. Android Quick Settings Notification Tile
* **1-Tap Connect / Disconnect:** Swipe down from your phone’s status bar to toggle ZeroTrace instantly without opening the app.
* **Live Server Feedback:** Displays the active server name (e.g. `ZeroTrace • SG - Singapore 01`) directly on the tile.
* **Native 1-Tap Add (Android 13+):** Add the tile directly from the in-app guide with a single tap.

### 🔄 5. In-App OTA Auto-Updater
* **Zero-Data Package Support:** Updates download directly through the active encrypted VPN tunnel—even if you have 0 MB regular data on a bug host package!
* **Interactive Changelogs:** Categorized release notes with glowing badges (`⚡ SPEED`, `📦 COMPACT`, `📷 QR CODE`, `🐛 FIX`).
* **Active Download Cancel:** 1-tap cancel button to abort downloads at any time.

### 📷 6. QR Code Scanner & Instant Sharing
* **CameraX + ML Kit Vision:** Super-fast 1-tap QR code scanning from your camera or photo gallery.
* **ZXing Share Generator:** Generate high-contrast QR codes and share configs with friends via WhatsApp, Telegram, or system share sheet.

### ⚡ 7. Gigabit Performance Engine
* **Google BBR Congestion Control:** Maximum bandwidth utilization and lowest jitter.
* **TCP NoDelay & Fast Open:** Disables Nagle's algorithm for minimum gaming ping.
* **MTU 1400 Zero-Fragmentation Tuning:** Eliminates cellular GTP packet drops on 4G/5G mobile towers.
* **2048 KB I/O Streaming Buffer:** Doubled internal buffer size for lag-free 4K/8K video streaming.

---

## 📋 Protocol & Feature Matrix

| Feature | Supported Methods / Capabilities |
| :--- | :--- |
| **VLESS** | `XTLS Reality`, `Vision (xtls-rprx-vision)`, `WebSocket`, `gRPC`, `TCP`, `HTTPUpgrade` |
| **VMess** | `WebSocket`, `gRPC`, `TCP`, `TLS / Non-TLS`, `AEAD Encryption` |
| **Trojan** | `TLS`, `gRPC`, `WebSocket`, `Native TCP` |
| **Shadowsocks** | `AEAD 2022`, `aes-128-gcm`, `aes-256-gcm`, `chacha20-poly1305` |
| **Split Tunneling** | `All Apps`, `Bypass Selected (Exclude)`, `VPN Only (Include)` |
| **DNS Resolvers** | `AdGuard Ad-Blocker`, `Cloudflare 1.1.1.1`, `Cloudflare Security`, `Quad9`, `Google DNS` |
| **Quick Settings** | `Android Quick Settings Tile` (Android 7.0 - 14.0+) |
| **Architecture** | `arm64-v8a`, `armeabi-v7a`, `x86_64` |

---

## 🚀 Getting Started

### 1. Installation
1. Download the latest release: [**ZeroTrace-v1.0.6-arm64.apk**](https://github.com/PsyChoDev14/ZeroTrace/releases/latest/download/ZeroTrace-v1.0.6-arm64.apk)
2. Open the file on your Android device and tap **Install**.

### 2. Adding a Server Configuration
ZeroTrace supports three fast ways to import servers:
* **📋 Clipboard Paste:** Copy any `vless://`, `vmess://`, `trojan://`, or `ss://` link and tap **"Paste from Clipboard"**.
* **📷 Scan QR Code:** Tap **"Scan QR"** to scan a server QR code via camera or gallery screenshot.
* **⚙️ Custom JSON:** Paste raw full Xray JSON configuration.

### 3. Connect!
* Tap the glowing central **Connect** button on the Home screen.
* Watch real-time Download/Upload speed meters and ping latency respond live!

---

## 🤖 Release Automation

ZeroTrace includes an automated release publishing pipeline:

```bash
# 1-Command Automated Build & Release
./release.sh
```
* **What it does automatically:**
  1. Bumps `versionCode` and `versionName` in `build.gradle.kts`.
  2. Compiles optimized, R8-shrunk Release APK (56.6 MB).
  3. Updates `version.json` with changelogs and date.
  4. Creates GitHub Release tag and uploads the APK binary via GitHub REST API.
  5. Pushes updated code to GitHub `main` branch.

---

## 💻 Tech Stack & Architecture

ZeroTrace is built with modern native Android and low-level networking technologies:

```
┌─────────────────────────────────────────────────────────────┐
│                    ZeroTrace UI Layer                       │
│     Jetpack Compose • Material 3 • Kotlin Coroutines Flow   │
├──────────────────────────────┬──────────────────────────────┤
│      Android System Layer    │       Camera & Vision AI     │
│  VpnService • QS TileService │   CameraX • ML Kit • ZXing   │
├──────────────────────────────┴──────────────────────────────┤
│                    Native Routing Bridge                    │
│     hev-socks5-tunnel (C / Rust) • TUN Interface (MTU 1400) │
├─────────────────────────────────────────────────────────────┤
│                      Xray-Core Engine                       │
│   VLESS Reality • Vision • VMess • Trojan • Shadowsocks 2022│
└─────────────────────────────────────────────────────────────┘
```

### 📱 Android & UI Architecture
* **Language:** 100% Modern Idiomatic **Kotlin**
* **UI Toolkit:** **Jetpack Compose** (Declarative Reactive UI)
* **Design System:** Custom Cyberpunk Dark Theme grounded in Apple HIG & Google Material 3
* **Concurrency:** **Kotlin Coroutines** & **StateFlow / SharedFlow**
* **Architecture Pattern:** Clean MVI / Unidirectional Data Flow with Repository Pattern
* **System Integration:** Android `VpnService`, `TileService` (Quick Settings), `StatusBarManager`, `NotificationManager`

### 🛡️ VPN Core & Native Networking
* **Core Proxy Engine:** **[Xray-Core v1.8.11](https://github.com/XTLS/Xray-core)** (Compiled with Go / JNI bindings via `LibXray`)
* **TUN Interface Bridge:** **[hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel)** (High-performance native C / Rust daemon)
* **Congestion Control:** **Google BBR** (Bottleneck Bandwidth and RTT)
* **Mobile Packet Tuning:** Clamped **MTU 1400** & **TCP MSS 1360** (Zero fragmentation on Sri Lankan mobile carriers)
* **Buffer Pipeline:** 2048 KB I/O Streaming Ring Buffers for lag-free 4K/8K playback

### 👁️ Camera & Vision AI
* **Camera Capture:** **AndroidX CameraX** (`camera-view`, `camera-lifecycle`)
* **QR Detection Engine:** **Google ML Kit Barcode Scanning** (Sub-second on-device machine learning detection)
* **QR Code Rendering:** **ZXing Core** (`zxing:core:3.5.3`)

### ⚡ Compiler & Optimization
* **Bytecode Minification:** **Google R8** & **ProGuard** with custom JNI keep-rules
* **Asset Shrinking:** `isShrinkResources = true` (Reduced APK from 74.2 MB ➔ 56.6 MB)
* **Build System:** **Gradle 8.7** with Kotlin DSL (`build.gradle.kts`)
* **ABIs Supported:** `arm64-v8a`, `armeabi-v7a`, `x86_64`

### 📊 DevOps & Telemetry
* **Release Automation:** Python 3 + GitHub REST API v3
* **Live Dashboard:** Standalone HTML5 / CSS3 Glassmorphism Dashboard
* **Serverless Telemetry:** Cloudflare Workers (Edge DAU and active connection tracker)

---

## 🛠️ Project Structure

```text
ZeroTrace/
├── app/
│   ├── src/main/
│   │   ├── java/lk/novalink/zerotrace/
│   │   │   ├── MainActivity.kt               # Jetpack Compose navigation & VPN controller
│   │   │   ├── ZeroTraceApp.kt               # App singleton & repository lifecycle
│   │   │   ├── core/
│   │   │   │   ├── PingEngine.kt             # High-speed TCP socket latency tester
│   │   │   │   ├── TelemetryManager.kt       # Anonymous usage telemetry
│   │   │   │   ├── UpdateManager.kt          # In-app OTA streaming updater
│   │   │   │   ├── VpnTunnelManager.kt       # Real-time StateFlow bridge
│   │   │   │   ├── XrayConfigGenerator.kt    # Production Xray JSON generator
│   │   │   │   └── XrayCoreManager.kt        # libXray & hev-socks5-tunnel supervisor
│   │   │   ├── data/
│   │   │   │   ├── model/                    # ProxyConfig, SplitTunnelModel, DnsProfile
│   │   │   │   └── repository/               # ConfigRepository, SettingsRepository, TrafficStats
│   │   │   ├── parser/                       # VlessParser, VmessParser, TrojanParser, Shadowsocks
│   │   │   ├── service/
│   │   │   │   ├── ZeroTraceVpnService.kt    # Android VpnService, TUN & Split Tunnel router
│   │   │   │   └── ZeroTraceTileService.kt   # Android Quick Settings Notification Tile
│   │   │   ├── ui/
│   │   │   │   ├── components/               # UpdateDialog, QuickSettingsGuide, ShareQR, Scanner
│   │   │   │   ├── screens/                  # HomeScreen, ConfigsScreen, Settings, SplitTunneling
│   │   │   │   └── theme/                    # Cyberpunk colors, typography, shapes
│   │   │   └── util/                         # QrCodeGenerator (ZXing)
│   │   └── res/                              # Vector icons, QS tile icons, adaptive launcher
│   └── build.gradle.kts                      # R8 Minification, ABI splits, dependencies
├── dashboard/
│   └── index.html                            # Standalone live telemetry dashboard
├── scripts/
│   ├── analytics.py                          # Live GitHub release metrics script
│   └── publish_update.py                     # 1-click GitHub release automation engine
├── analytics.sh                              # CLI analytics executable
├── release.sh                                # CLI release publisher executable
├── version.json                              # Live OTA version feed for user devices
└── README.md
```

---

## 🔒 Security & Zero-Logs Privacy

* **Strict No-Logs Guarantee:** ZeroTrace does **not** log user IP addresses, visited URLs, DNS queries, or session histories.
* **Kernel Socket Protection:** Outbound socket file descriptors are protected via `VpnService.protect(fd)` at the OS kernel level.
* **App Sandboxing:** Server credentials and UUIDs are encrypted within Android's private app sandbox.
* **Leak Proofing:** Complete DNS and IPv6 leak prevention enforced by default.

---

## 👥 Credits & Brand

* **Engineered by:** [**Nexaura Core**](https://nexauracore.com)
* **Lead Developer:** **Nadun Gawesh**
* **Network & Community:** **NovaLink LK**

---

<div align="center">
  <sub>Built with ❤️ for privacy and uncensored internet freedom.</sub>
</div>
