# ZeroTrace VPN - Complete Auto-Updater Guide

ZeroTrace includes an in-app **Over-The-Air (OTA) Auto-Updater**. When you push a new release, all installed apps will automatically notify users and allow them to download and install the update in-place with 1 tap.

---

## 🚀 How to Publish a New Update (3 Methods)

### Method 1: The All-In-One Terminal Script (Fastest & Recommended)

Simply run this command in your terminal from the project root:

```bash
./release.sh
```

#### What this script automatically does from A to Z:
1. **Reads current version** (e.g. `1.0.0`, `versionCode: 1`).
2. **Prompts you for the next version** (Patch: `1.0.1`, Minor: `1.1.0`, or Major: `2.0.0`).
3. **Prompts you for the changelog bullet points** to display in the user's update popup.
4. **Automatically updates `app/build.gradle.kts`** with the new `versionCode` and `versionName`.
5. **Automatically generates/updates `version.json`** with the correct download URL.
6. **Compiles the APK** into `dist/ZeroTrace-v{VERSION}-arm64.apk`.
7. **Creates a Git commit & Git tag** (`v1.0.1`).
8. If GitHub CLI (`gh`) is installed, it **creates the GitHub Release and uploads the APK automatically**.

---

### Method 2: One-Command Git Push (Cloud Automation via GitHub Actions)

If you push a version tag to your GitHub repository:

```bash
git tag v1.0.1
git push origin main --tags
```

The included **GitHub Actions workflow** (`.github/workflows/release_updater.yml`) will automatically:
1. Compile the APK on GitHub's cloud servers.
2. Create the release `v1.0.1` on your GitHub repository.
3. Attach the built APK to the release.
4. Make it immediately downloadable by all app users!

---

### Method 3: Manual Web Upload (GitHub Releases)

If you prefer using the GitHub website:
1. Run `./release.sh` to build the APK and generate `version.json`.
2. Push your `version.json` to GitHub:
   ```bash
   git add version.json
   git commit -m "Update version.json to v1.0.1"
   git push origin main
   ```
3. Open GitHub in your browser: `https://github.com/nadungawesh/ZeroTrace/releases/new`
4. Enter tag: `v1.0.1`, Title: `ZeroTrace v1.0.1`, and drag-and-drop the APK from `dist/` or `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`.
5. Click **Publish Release**.

---

## 📱 How It Works on the User's Phone:
1. When a user opens ZeroTrace, the app silently checks `https://raw.githubusercontent.com/nadungawesh/ZeroTrace/main/version.json`.
2. If `versionCode` on the server is higher than the installed version, a modern **"New Version Available"** popup appears showing your changelog.
3. Tapping **"Download & Update"** streams the APK with a live progress bar.
4. Upon completion, Android prompts the user: *"Do you want to update this app?"* and installs it seamlessly without losing saved configs or settings!
