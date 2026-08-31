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

TAG_NAME = "v1.0.8"
VERSION_CODE = 9
VERSION_NAME = "1.0.8"
CHANGELOG = """• Ultra-fast 120 FPS list scrolling & pre-cached image engine
• High-priority system push notifications for new updates
• Compose UI responsiveness & memory footprint optimizations
• Enhanced stability & background telemetry pipeline"""

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
        # Fallback check
        apks = list((PROJECT_ROOT / "app" / "build" / "outputs" / "apk" / "release").glob("*.apk"))
        print(f"Available APKs: {[a.name for a in apks]}")
        for a in apks:
            if "arm64" in a.name:
                src_apk = a
                break

    shutil.copyfile(src_apk, dest_apk)
    size_mb = dest_apk.stat().st_size / (1024 * 1024)
    print(f"✅ Built Release APK: {dest_apk.name} ({size_mb:.1f} MB)")

    # 2. Git Commit & Tag
    token = TOKEN_FILE.read_text().strip() if TOKEN_FILE.exists() else ""
    run("git add app/build.gradle.kts version.json scripts/ server/")
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
            upload_url = data.get("upload_url", "").split("{")[0]
            html_url = data.get("html_url", "")
            print(f"✅ Created Release: {html_url}")

            # Upload APK
            upload_apk_url = f"{upload_url}?name={dest_apk.name}"
            apk_bytes = dest_apk.read_bytes()
            upload_headers = {
                "User-Agent": "ZeroTrace-Automator",
                "Authorization": f"token {token}",
                "Content-Type": "application/vnd.android.package-archive",
                "Content-Length": str(len(apk_bytes))
            }
            upload_req = urllib.request.Request(upload_apk_url, data=apk_bytes, headers=upload_headers, method="POST")
            with urllib.request.urlopen(upload_req, timeout=300) as up_resp:
                up_data = json.loads(up_resp.read().decode("utf-8"))
                download_url = up_data.get("browser_download_url", "")
                print(f"🎉 Asset Uploaded Successfully: {download_url}")

    except urllib.error.HTTPError as e:
        print(f"GitHub API Error: {e.code} - {e.read().decode('utf-8')}")

    print(f"\n🎉 ZERO-TOUCH RELEASE {TAG_NAME} COMPLETE!")

if __name__ == "__main__":
    main()
