# WALLACE Admin – Android companion

Kotlin + Jetpack Compose (Material 3) companion for the WALLACE Admin 3.4.0 desktop app. Talks only to the
**WALLACE Agent** (`..\wallace-admin-agent`) on the PC – never to AdGuard directly.

- Package `com.brandonwallace.wallaceadmin`, v1.0 (versionCode 1), min SDK 26, target 36.
- Screens: Dashboard (health ring, hotspot/AdGuard cards, 24 h query/blocked charts, live monitors, health tiles), WALLACE Secure,
  Live Status (uptime bars), Devices (search/filter, mode exempt/roku/filtered, pause with confirmation, lock reasons),
  Safe Guard (all toggles + categories, test an address, top blocked, locked Messaging badge, locked YouTube-restricted toggle),
  Query Log (live 5 s, blocked filter, allow/block from an entry), Admin (custom rules, maintenance), Blocklists, Reports (24 h–14 d),
  Alerts, Uptime Kuma, Updates (read-only, Windows Update check), Logs, Settings, About.
- Live sync: WebSocket `/v1/live` (status every 5 s) with polling fallback (5–60 s).
- Pairing: scan the PC's QR in-app (Google code scanner, no camera permission), with the Camera app (`wallace://pair` deep link), or paste the link.
- Token: AES-256-GCM with a key in the Android Keystore.
- Host: LAN address (default 192.0.2.13) + fallback (Tailscale 100.x / MagicDNS). Only private / 192.0.2.22/10 / *.ts.net hosts are accepted.

Build (PC): `set JAVA_HOME=C:\Program Files\Android\openjdk\jdk-21.0.8` then `gradlew assembleRelease`
→ `app\build\outputs\apk\release\app-release.apk` (debug-signed release build, sideload only).


---
WALLACE Admin for Android - remote control app for the WALLACE Admin agent.

## Build
Open in Android Studio (or run `./gradlew assembleDebug`).

