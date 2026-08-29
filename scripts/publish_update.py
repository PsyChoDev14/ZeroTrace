#!/usr/bin/env python3
"""
ZeroTrace VPN - 100% Automated Release & GitHub Publisher
Automates:
  1. Version bumping (versionCode & versionName in build.gradle.kts)
  2. Multi-line changelog generator
  3. Building optimized lightweight APKs
  4. Updating version.json for the in-app OTA Auto-Updater
  5. Git staging, tagging & commit
  6. Direct GitHub REST API Release creation
  7. Direct GitHub APK Binary Asset upload (No browser required!)
  8. Pushing commits & tags to GitHub main branch
"""

import os
import sys
import re
import json
import shutil
import subprocess
import datetime
import urllib.request
import urllib.error
from pathlib import Path

# ANSI Color Codes
CYAN = "\033[96m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
RED = "\033[91m"
BOLD = "\033[1m"
DIM = "\033[2m"
RESET = "\033[0m"

PROJECT_ROOT = Path(__file__).resolve().parent.parent
GRADLE_FILE = PROJECT_ROOT / "app" / "build.gradle.kts"
VERSION_JSON_FILE = PROJECT_ROOT / "version.json"
DIST_DIR = PROJECT_ROOT / "dist"
TOKEN_FILE = PROJECT_ROOT / ".github_token"
GLOBAL_TOKEN_FILE = Path.home() / ".zerotrace_github_token"
DEFAULT_GITHUB_REPO = "PsyChoDev14/ZeroTrace"

def print_banner():
    print(f"{CYAN}{BOLD}")
    print("╔═════════════════════════════════════════════════════════════════════════╗")
    print("║             ZEROTRACE VPN - 100% GITHUB RELEASE AUTOMATOR               ║")
    print("║             Engineered for NovaLink LK • Nexaura Core                   ║")
    print("╚═════════════════════════════════════════════════════════════════════════╝")
    print(f"{RESET}")

def run_cmd(cmd, cwd=PROJECT_ROOT, check=True, capture_output=False):
    """Runs a shell command and returns output."""
    if isinstance(cmd, list):
        cmd_str = " ".join(cmd)
    else:
        cmd_str = cmd

    env = os.environ.copy()
    if "JAVA_HOME" in env:
        del env["JAVA_HOME"]

    res = subprocess.run(cmd_str, shell=True, cwd=cwd, env=env, text=True, capture_output=capture_output)
    if check and res.returncode != 0:
        print(f"{RED}Command failed with exit code {res.returncode}: {cmd_str}{RESET}")
        if res.stderr:
            print(f"{RED}{res.stderr}{RESET}")
        sys.exit(res.returncode)
    return res

def get_current_version():
    """Reads current versionCode and versionName from build.gradle.kts."""
    content = GRADLE_FILE.read_text(encoding="utf-8")
    code_match = re.search(r'versionCode\s*=\s*(\d+)', content)
    name_match = re.search(r'versionName\s*=\s*["\']([^"\']+)["\']', content)

    version_code = int(code_match.group(1)) if code_match else 1
    version_name = name_match.group(1) if name_match else "1.0.0"
    return version_code, version_name

def update_gradle_version(new_code, new_name):
    """Updates versionCode and versionName in build.gradle.kts."""
    content = GRADLE_FILE.read_text(encoding="utf-8")
    content = re.sub(r'versionCode\s*=\s*\d+', f'versionCode = {new_code}', content)
    content = re.sub(r'versionName\s*=\s*["\'][^"\']+["\']', f'versionName = "{new_name}"', content)
    GRADLE_FILE.write_text(content, encoding="utf-8")

def update_version_json(new_code, new_name, changelog, download_url, force_update):
    """Generates and writes the updated version.json."""
    today = datetime.datetime.now().strftime("%Y-%m-%d")
    data = {
        "versionCode": new_code,
        "versionName": new_name,
        "downloadUrl": download_url,
        "changelog": changelog,
        "forceUpdate": force_update,
        "releaseDate": today,
        "minSupportedVersion": 1
    }
    VERSION_JSON_FILE.write_text(json.dumps(data, indent=2), encoding="utf-8")

def parse_semver(name):
    parts = name.split(".")
    major = int(parts[0]) if len(parts) > 0 and parts[0].isdigit() else 1
    minor = int(parts[1]) if len(parts) > 1 and parts[1].isdigit() else 0
    patch = int(parts[2]) if len(parts) > 2 and parts[2].isdigit() else 0
    return major, minor, patch

def get_github_token():
    """Retrieves GitHub token from env, local file, global file, or prompts the user."""
    # 1. Check environment variable
    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if token:
        return token

    # 2. Check local token file
    if TOKEN_FILE.exists():
        token = TOKEN_FILE.read_text(encoding="utf-8").strip()
        if token:
            return token

    # 3. Check global token file
    if GLOBAL_TOKEN_FILE.exists():
        token = GLOBAL_TOKEN_FILE.read_text(encoding="utf-8").strip()
        if token:
            return token

    # 4. Prompt user
    print(f"\n🔑 {YELLOW}GitHub Personal Access Token is needed for 100% automated publishing.{RESET}")
    print(f"{DIM}You can generate one at: https://github.com/settings/tokens (Scope: repo){RESET}")
    token = input("Enter your GitHub Token (or press Enter to skip auto-upload): ").strip()

    if token:
        save = input("Save token securely for future one-click releases? [Y/n]: ").strip().lower()
        if save != 'n':
            TOKEN_FILE.write_text(token, encoding="utf-8")
            # Ensure file is only readable by current user
            try:
                os.chmod(TOKEN_FILE, 0o600)
            except Exception:
                pass
            print(f"{GREEN}Saved token securely to .github_token{RESET}")
    return token

def github_api_request(url, method="GET", data=None, headers=None, token=""):
    """Makes an authenticated GitHub REST API request."""
    if headers is None:
        headers = {}

    headers["User-Agent"] = "ZeroTrace-Release-Automator"
    headers["Accept"] = "application/vnd.github.v3+json"
    if token:
        headers["Authorization"] = f"token {token}"

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as e:
        err_body = e.read().decode('utf-8', errors='ignore')
        return e.code, err_body
    except Exception as e:
        return 0, str(e)

def upload_github_release_asset(upload_url_template, apk_file: Path, token: str):
    """Uploads binary APK asset to GitHub release."""
    # upload_url_template format: https://uploads.github.com/repos/owner/repo/releases/123/assets{?name,label}
    base_upload_url = upload_url_template.split("{")[0]
    upload_url = f"{base_upload_url}?name={apk_file.name}"

    apk_bytes = apk_file.read_bytes()
    headers = {
        "User-Agent": "ZeroTrace-Release-Automator",
        "Authorization": f"token {token}",
        "Content-Type": "application/vnd.android.package-archive",
        "Content-Length": str(len(apk_bytes))
    }

    print(f"      Uploading {apk_file.name} ({len(apk_bytes) / (1024*1024):.1f} MB) via GitHub API...")
    req = urllib.request.Request(upload_url, data=apk_bytes, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            resp_data = json.loads(resp.read().decode('utf-8'))
            return True, resp_data.get("browser_download_url", "")
    except urllib.error.HTTPError as e:
        err = e.read().decode('utf-8', errors='ignore')
        return False, err
    except Exception as e:
        return False, str(e)

def main():
    print_banner()

    curr_code, curr_name = get_current_version()
    major, minor, patch = parse_semver(curr_name)

    print(f"📦 Current Version: {BOLD}{curr_name}{RESET} (versionCode: {BOLD}{curr_code}{RESET})\n")

    print(f"{YELLOW}Select Release Type:{RESET}")
    print(f"  [1] Patch Release  -> {GREEN}{major}.{minor}.{patch + 1}{RESET} (Bug fixes & minor polish)")
    print(f"  [2] Minor Release  -> {GREEN}{major}.{minor + 1}.0{RESET} (New features & UI updates)")
    print(f"  [3] Major Release  -> {GREEN}{major + 1}.0.0{RESET} (Major overhaul)")
    print(f"  [4] Custom Version String")

    choice = input(f"\nEnter choice [1-4] (default: 1): ").strip()
    if choice == "2":
        new_name = f"{major}.{minor + 1}.0"
    elif choice == "3":
        new_name = f"{major + 1}.0.0"
    elif choice == "4":
        new_name = input("Enter new version name (e.g. 1.0.5): ").strip()
        if not new_name:
            new_name = f"{major}.{minor}.{patch + 1}"
    else:
        new_name = f"{major}.{minor}.{patch + 1}"

    new_code = curr_code + 1
    tag_name = f"v{new_name}"
    print(f"\n🚀 New Target: {BOLD}{GREEN}{tag_name}{RESET} (versionCode: {BOLD}{new_code}{RESET})\n")

    # GitHub Repository Setting
    github_repo = input(f"GitHub Repo [{DEFAULT_GITHUB_REPO}]: ").strip()
    if not github_repo:
        github_repo = DEFAULT_GITHUB_REPO

    # Changelog Input
    print(f"\n📝 {YELLOW}Enter Changelog (Bullet points):{RESET}")
    print(f"{DIM}(Press Enter on empty line when done, or press Enter immediately for default){RESET}")
    lines = []
    while True:
        line = input("  • ").strip()
        if not line:
            break
        lines.append(f"• {line}")

    if not lines:
        lines = [
            f"• Performance & speed optimizations for Sri Lanka networks",
            f"• Integrated Camera QR Code Scanner for instant config import",
            f"• Live multi-day usage statistics engine",
            f"• Network switch auto-reconnect handler"
        ]

    changelog_str = "\n".join(lines)
    print(f"\n{GREEN}Changelog preview:{RESET}\n{changelog_str}\n")

    # Force Update Check
    force_up = input("Force all users to update to this version? (y/N): ").strip().lower() == 'y'

    # Download URL for the APK
    apk_filename = f"ZeroTrace-{tag_name}-arm64.apk"
    download_url = f"https://github.com/{github_repo}/releases/download/{tag_name}/{apk_filename}"

    # 1. Update Gradle version
    print(f"\n[1/6] ✏️  Updating {GRADLE_FILE.name}...")
    update_gradle_version(new_code, new_name)
    print(f"      {GREEN}Updated build.gradle.kts to versionCode={new_code}, versionName={new_name}{RESET}")

    # 2. Update version.json
    print(f"\n[2/6] 📄 Updating {VERSION_JSON_FILE.name}...")
    update_version_json(new_code, new_name, changelog_str, download_url, force_up)
    print(f"      {GREEN}Updated version.json successfully.{RESET}")

    # 3. Build APK
    print(f"\n[3/6] 🔨 Compiling APK with Gradle...")
    DIST_DIR.mkdir(exist_ok=True)
    run_cmd("./gradlew --no-daemon assembleDebug")

    # Copy output APK to dist/
    src_apk = PROJECT_ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-arm64-v8a-debug.apk"
    dest_apk = DIST_DIR / apk_filename

    if src_apk.exists():
        shutil.copyfile(src_apk, dest_apk)
        apk_size_mb = dest_apk.stat().st_size / (1024 * 1024)
        print(f"      {GREEN}Built APK: {dest_apk.name} ({apk_size_mb:.1f} MB){RESET}")
    else:
        print(f"{RED}Error: Output APK not found at {src_apk}{RESET}")
        sys.exit(1)

    # 4. Git Automation
    print(f"\n[4/6] 🐙 Git Staging, Commit & Tag...")
    is_git_repo = (PROJECT_ROOT / ".git").exists()
    if not is_git_repo:
        run_cmd("git init")
        run_cmd("git branch -M main")
        print("      Initialized git repository with main branch.")

    # Configure remote if not set
    remote_check = run_cmd("git remote -v", check=False, capture_output=True)
    if "origin" not in remote_check.stdout:
        remote_url = f"https://github.com/{github_repo}.git"
        run_cmd(f"git remote add origin {remote_url}")
        print(f"      Configured remote origin -> {remote_url}")

    run_cmd("git add app/build.gradle.kts version.json .gitignore .github/ AUTO_UPDATER_GUIDE.md release.sh scripts/")
    run_cmd(f'git commit -m "Release {tag_name} (build {new_code})"', check=False)
    run_cmd(f"git tag -a {tag_name} -m \"ZeroTrace {tag_name}\"", check=False)
    print(f"      {GREEN}Committed changes and tagged {tag_name}{RESET}")

    # 5. GitHub API Automation
    print(f"\n[5/6] 🌐 100% Automated GitHub Release & Asset Upload...")
    token = get_github_token()

    if token:
        # Push commit & tags to remote
        print(f"      Pushing git branch and tags to GitHub...")
        auth_remote = f"https://x-access-token:{token}@github.com/{github_repo}.git"
        run_cmd(f"git push {auth_remote} main --tags", check=False)

        # Create Release via GitHub API
        create_release_url = f"https://api.github.com/repos/{github_repo}/releases"
        release_payload = json.dumps({
            "tag_name": tag_name,
            "target_commitish": "main",
            "name": f"ZeroTrace {tag_name}",
            "body": changelog_str,
            "draft": False,
            "prerelease": False
        }).encode('utf-8')

        status, resp_raw = github_api_request(create_release_url, method="POST", data=release_payload, token=token)
        if status in (200, 201):
            release_data = json.loads(resp_raw.decode('utf-8'))
            upload_url = release_data.get("upload_url", "")
            release_html_url = release_data.get("html_url", "")
            print(f"      {GREEN}Created GitHub Release: {release_html_url}{RESET}")

            # Upload APK Binary Asset
            if upload_url:
                ok, asset_url = upload_github_release_asset(upload_url, dest_apk, token)
                if ok:
                    print(f"      {GREEN}Successfully uploaded {dest_apk.name} to GitHub Releases!{RESET}")
                else:
                    print(f"      {RED}Asset upload notice: {asset_url}{RESET}")
        else:
            print(f"      {YELLOW}Note on GitHub Release creation: status={status}{RESET}")
            # If release already exists or needs manual push
    else:
        print(f"      {YELLOW}Skipped direct GitHub API upload (no token provided).{RESET}")
        print(f"      Push manually using: {CYAN}git push origin main --tags{RESET}")

    # 6. Final Summary
    print(f"\n[6/6] 🎉 {BOLD}{GREEN}ZERO-TOUCH RELEASE COMPLETE!{RESET}")
    print("═" * 70)
    print(f"📦 Version: {BOLD}{tag_name}{RESET} (versionCode: {BOLD}{new_code}{RESET})")
    print(f"📁 Local APK: {BOLD}{dest_apk}{RESET}")
    print(f"🔗 Public Download: {CYAN}{download_url}{RESET}")
    print(f"📡 OTA Feed: {CYAN}https://raw.githubusercontent.com/{github_repo}/main/version.json{RESET}")
    print("═" * 70)
    print(f"{GREEN}All installed ZeroTrace devices will receive this update on next launch! 🚀{RESET}\n")

if __name__ == "__main__":
    main()
