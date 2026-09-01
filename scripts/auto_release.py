#!/usr/bin/env python3
import os
import sys
import json
import shutil
import subprocess
from pathlib import Path
import urllib.request
import urllib.error

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DIST_DIR = PROJECT_ROOT / "dist"
TOKEN_FILE = PROJECT_ROOT / ".github_token"
REPO = "PsyChoDev14/ZeroTrace"

TAG_NAME = "v1.2.1"
VERSION_CODE = 13
VERSION_NAME = "1.2.1"
CHANGELOG = """• Clean, serene Home Screen with progressive disclosure
• Decluttered Settings with expandable Advanced Stealth Engine drawer
• Aggressive DPI Bypass & TLS ClientHello packet fragmentation
• Mux.Cool Stream Multiplexing & uTLS browser fingerprint camouflage
• 120 FPS Compose UI rendering & memory footprint optimizations"""

def run(cmd):
    print(f"==> {cmd}")
    env = os.environ.copy()
    if "JAVA_HOME" in env:
        del env["JAVA_HOME"]
    res = subprocess.run(cmd, shell=True, cwd=PROJECT_ROOT, env=env, text=True)
    if res.returncode != 0:
        print(f"FAILED: {cmd}")
        sys.exit(res.returncode)

def main():
    print(f"🚀 Starting Auto-Release for {TAG_NAME} (code {VERSION_CODE})...")
    
    # 1. Compile Release APK
    print("\n🔨 Building Release APK with Gradle...")
    DIST_DIR.mkdir(exist_ok=True)
    run("./gradlew --no-daemon assembleRelease")

    src_apk = PROJECT_ROOT / "app" / "build" / "outputs" / "apk" / "release" / "app-arm64-v8a-release.apk"
    dest_apk = DIST_DIR / f"ZeroTrace-{TAG_NAME}-arm64.apk"

    if not src_apk.exists():
        apks = list((PROJECT_ROOT / "app" / "build" / "outputs" / "apk" / "release").glob("*.apk"))
        for a in apks:
            if "arm64" in a.name:
                src_apk = a
                break

    shutil.copyfile(src_apk, dest_apk)
    size_mb = dest_apk.stat().st_size / (1024 * 1024)
    print(f"✅ Built Release APK: {dest_apk.name} ({size_mb:.1f} MB)")

    # 2. Git Commit & Tag
    token = TOKEN_FILE.read_text().strip() if TOKEN_FILE.exists() else ""
    run("git add app/ version.json scripts/ public/ api/ lib/ server/")
    subprocess.run(f'git commit -m "Release {TAG_NAME} (build {VERSION_CODE})"', shell=True, cwd=PROJECT_ROOT)
    subprocess.run(f'git tag -d {TAG_NAME} 2>/dev/null || true', shell=True, cwd=PROJECT_ROOT)
    run(f'git tag -a {TAG_NAME} -m "ZeroTrace {TAG_NAME}"')

    auth_remote = f"https://x-access-token:{token}@github.com/{REPO}.git" if token else "origin"
    run(f"git push {auth_remote} main --tags")

    # 3. GitHub Release Creation via API
    print(f"\n🌐 Creating GitHub Release {TAG_NAME}...")
    create_url = f"https://api.github.com/repos/{REPO}/releases"
    headers = {
        "User-Agent": "ZeroTrace-Automator",
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json"
    }
    payload = json.dumps({
        "tag_name": TAG_NAME,
        "target_commitish": "main",
        "name": f"ZeroTrace {TAG_NAME}",
        "body": CHANGELOG,
        "draft": False,
        "prerelease": False
    }).encode("utf-8")

    req = urllib.request.Request(create_url, data=payload, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            release_id = data.get("id")
            html_url = data.get("html_url", "")
            print(f"✅ Created Release: {html_url} (ID: {release_id})")

            # Upload APK binary with curl
            print(f"Uploading APK asset...")
            upload_cmd = f'curl -s -X POST -H "Authorization: token {token}" -H "Content-Type: application/vnd.android.package-archive" -H "Accept: application/vnd.github.v3+json" --data-binary "@{dest_apk}" "https://uploads.github.com/repos/{REPO}/releases/{release_id}/assets?name={dest_apk.name}"'
            upload_res = subprocess.run(upload_cmd, shell=True, text=True, capture_output=True)
            if upload_res.returncode == 0:
                print(f"🎉 Asset Uploaded Successfully!")
            else:
                print(f"Upload error: {upload_res.stderr}")

    except urllib.error.HTTPError as e:
        print(f"GitHub API Error: {e.code} - {e.read().decode('utf-8')}")

    print(f"\n🎉 ZERO-TOUCH RELEASE {TAG_NAME} COMPLETE!")

if __name__ == "__main__":
    main()
