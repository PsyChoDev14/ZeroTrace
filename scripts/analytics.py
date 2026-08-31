#!/usr/bin/env python3
"""
ZeroTrace VPN - Real-Time Download & Telemetry Analytics
Fetches and displays live GitHub APK download metrics and active usage statistics.
"""

import urllib.request
import json
import os
import sys
from pathlib import Path

# Terminal colors
CYAN = "\033[96m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
PURPLE = "\033[95m"
BOLD = "\033[1m"
DIM = "\033[2m"
RESET = "\033[0m"

PROJECT_ROOT = Path(__file__).parent.parent
TOKEN_FILE = PROJECT_ROOT / ".github_token"
REPO = "PsyChoDev14/ZeroTrace"
TELEMETRY_API = "https://server-omega-blue.vercel.app/api/stats"
ADMIN_KEY = os.environ.get("ADMIN_SECRET", "zerotrace_admin_secret_2026")

def print_banner():
    print(f"\n{CYAN}{BOLD}╔══════════════════════════════════════════════════════════════╗{RESET}")
    print(f"{CYAN}{BOLD}║         ZEROTRACE VPN • DEVELOPER LIVE ANALYTICS             ║{RESET}")
    print(f"{CYAN}{BOLD}╚══════════════════════════════════════════════════════════════╝{RESET}\n")

def get_github_token():
    if TOKEN_FILE.exists():
        token = TOKEN_FILE.read_text().strip()
        if token:
            return token
    return os.environ.get("GITHUB_TOKEN", "")

def fetch_github_release_analytics():
    token = get_github_token()
    headers = {
        "User-Agent": "ZeroTrace-Analytics",
        "Accept": "application/vnd.github.v3+json"
    }
    if token:
        headers["Authorization"] = f"token {token}"

    url = f"https://api.github.com/repos/{REPO}/releases"
    req = urllib.request.Request(url, headers=headers)
    
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        print(f"HTTP Error fetching GitHub stats: {e.code} {e.reason}")
        return None
    except Exception as e:
        print(f"Error fetching GitHub stats: {e}")
        return None

def fetch_live_telemetry():
    url = f"{TELEMETRY_API}?key={ADMIN_KEY}"
    req = urllib.request.Request(url, headers={"User-Agent": "ZeroTrace-CLI-Admin"})
    try:
        with urllib.request.urlopen(req, timeout=8) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception:
        return None

def main():
    print_banner()

    # 1. Query Live Private Telemetry
    telemetry = fetch_live_telemetry()
    if telemetry and not telemetry.get("error"):
        live_users = telemetry.get("liveUsers", 0)
        dau = telemetry.get("todayUsers", 0)
        total_conns = telemetry.get("totalConnections", 0)
        live_apps = telemetry.get("liveConnectedApps", [])

        print(f"{GREEN}{BOLD}════════════════════════════════════════════════════════════════{RESET}")
        print(f"  🟢 {BOLD}LIVE CONNECTED DEVICES:{RESET}   {GREEN}{BOLD}{live_users}{RESET} active right now")
        print(f"  📅 {BOLD}DAILY ACTIVE USERS (DAU):{RESET} {CYAN}{BOLD}{dau}{RESET} today")
        print(f"  ⚡ {BOLD}TOTAL SESSIONS:{RESET}           {PURPLE}{BOLD}{total_conns:,}{RESET} tunnel handshakes")
        print(f"{GREEN}{BOLD}════════════════════════════════════════════════════════════════{RESET}\n")

        if live_apps:
            print(f"{BOLD}📱 LIVE RUNNING APPS CONNECTED ACROSS DEVICES:{RESET}")
            print("─" * 60)
            print(f"{BOLD}{'APPLICATION NAME':<35} {'ACTIVE DEVICES':<15}{RESET}")
            print("─" * 60)
            for app in live_apps[:10]:
                print(f"{CYAN}{BOLD}{app['name']:<35}{RESET} {GREEN}{app['count']} online{RESET}")
            print("─" * 60 + "\n")
        else:
            print(f"{DIM}ℹ️  No client app traffic active at this instant.{RESET}\n")

    # 2. Query GitHub Releases
    print(f"📡 Querying repository releases: {BOLD}{REPO}{RESET}...\n")
    releases = fetch_github_release_analytics()
    if not releases:
        print("❌ Could not retrieve release statistics from GitHub.")
        sys.exit(1)

    total_downloads = 0
    release_stats = []

    for r in releases:
        tag = r.get("tag_name", "Unknown")
        name = r.get("name", tag)
        created_at = r.get("created_at", "")[:10]
        assets = r.get("assets", [])
        
        rel_downloads = sum(a.get("download_count", 0) for a in assets)
        total_downloads += rel_downloads
        
        release_stats.append({
            "tag": tag,
            "name": name,
            "date": created_at,
            "downloads": rel_downloads,
            "assets": assets
        })

    # Summary Cards
    print(f"{GREEN}{BOLD}════════════════════════════════════════════════════════════════{RESET}")
    print(f"  📥 {BOLD}TOTAL APK DOWNLOADS:{RESET}  {GREEN}{BOLD}{total_downloads:,}{RESET} across {len(releases)} releases")
    print(f"  🏷️ {BOLD}LATEST RELEASE:{RESET}       {CYAN}{BOLD}{releases[0].get('tag_name', 'N/A')}{RESET} ({releases[0].get('created_at', '')[:10]})")
    print(f"{GREEN}{BOLD}════════════════════════════════════════════════════════════════{RESET}\n")

    # Release Breakdown Table
    print(f"{BOLD}{'RELEASE':<10} {'DATE':<12} {'DOWNLOADS':<12} {'APK ASSET & SIZE':<35}{RESET}")
    print("─" * 70)

    for rel in release_stats:
        tag = rel["tag"]
        date = rel["date"]
        d_count = rel["downloads"]
        
        asset_info = "No assets"
        if rel["assets"]:
            a = rel["assets"][0]
            size_mb = a.get("size", 0) / (1024 * 1024)
            asset_info = f"{a.get('name', '')} ({size_mb:.1f} MB)"
        
        count_str = f"{GREEN}{BOLD}{d_count:>6}{RESET}" if d_count > 0 else f"{DIM}{d_count:>6}{RESET}"
        print(f"{CYAN}{BOLD}{tag:<10}{RESET} {date:<12} {count_str:<21} {asset_info:<35}")

    print("─" * 70)
    print(f"\n{PURPLE}💡 Tip:{RESET} Run {BOLD}./analytics.sh{RESET} anytime to monitor live usage & connected apps.")
    print(f"{PURPLE}🌐 Live Admin Web Dashboard:{RESET} https://server-omega-blue.vercel.app/admin?key={ADMIN_KEY}\n")

if __name__ == "__main__":
    main()
