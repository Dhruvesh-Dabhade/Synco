# Synco 🎧 | Unified Remote Audio & System Sync

An advanced, security-hardened, and highly polished system bridging your **Android device** with a **Desktop host** via real-time WebSocket communication. **Synco** provides dynamic media telemetry, notification mirroring, phone state tracking, audio routing supervision, and cryptographic secure pairing.

Designed with an executive-class, cinema-inspired **Material 3 UI**, Synco utilizes a zero-trust handshake model to ensure all system sync metrics remain completely private, secure, and low-latency.

---

## 📱 Screenshots

<p align="center">
  <img src="assets/mobile-app.png" alt="Synco Android App" width="250"/>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="assets/desktop-app.png" alt="Synco Desktop Companion" width="500"/>
</p>

---

## 🏗️ System Architecture

Synco is built using a modern decoupled architecture consisting of two major modules:

1. **Android Client (`/app`)**: 
   - Powered by **Kotlin** and **Jetpack Compose**.
   - Implements native background services (**Foreground Service**, **NotificationListenerService**, and **InCallService**) to intercept media, alerts, and call states.
   - Designed with high-contrast Material 3 components, Glassmorphic layouts, and customizable user profile avatars.
2. **Desktop Companion (`/desktop`)**:
   - A lightweight Kotlin daemon.
   - Powered by a high-throughput **Javalin WebSocket Web Server** hosting an interactive web controller and system sync log terminal.
   - Bridges socket controls directly into your native OS media interfaces.

---

## 🔒 Hardened Cryptographic Pairing Protocol

Security is at the foundation of Synco. Communication uses a robust Zero-Trust handshake sequence:

```
[Android Client]                                   [Desktop Companion]
       │                                                    │
       ├────────────── 1. PAIR_REQUEST (DH Public Key) ────>│  (User verifies PIN)
       │                                                    │
       |<───────────── 2. PAIR_RESPONSE (DH Public Key) ────┤
       │                                                    │
       │           == Derived Secret Session Key ==         │
       │                                                    │
       ├────────────── 3. AUTH_REQUEST (Encrypted Token) ──>│
       │                                                    │
       |<───────────── 4. AUTH_SUCCESS (Session Active) ────┤
```

### Protocol Security Guards
- **AES-256-GCM Encryption**: Every packet following the successful pairing handshake is fully encrypted end-to-end with AAD (Additional Authenticated Data) bound to `packetType|senderId|receiverId`.
- **X25519 + Ed25519 Key Agreement**: Ephemeral X25519 Diffie-Hellman key exchange with Ed25519 identity signatures — session keys derived via HKDF-SHA256.
- **Random PIN Per Session**: A fresh 6-digit PIN is generated via `SecureRandom` on every desktop launch. No hardcoded default.
- **Nonce Reuse Protection**: AES-GCM nonces are tracked in a thread-safe set (capped at 100K) to prevent replay and nonce reuse attacks.
- **Clock Skew Mitigation**: Packet timestamps are validated against a 5-minute (300,000ms) maximum drift window to completely eliminate replay attacks.
- **Payload Length Restrictions**: Heavy payloads or oversized packets are blocked at the socket level. Handshake values, Sender/Receiver IDs, and packet identifiers have a strict limit of 100 characters. Single message sizes exceeding 65KB are automatically dropped.
- **WebSocket Rate-Limiting**: The desktop server rate-limits WebSocket messages to 120 per minute per connection. The web dashboard also rate-limits HTTP and WebSocket requests per IP.
- **TLS v1.2+ with Origin Validation**: All WebSocket connections enforce origin checks. Desktop identity keys are stored AES-256-GCM encrypted on disk.
- **Web Dashboard Auth Token**: The web control panel requires a random 32-byte Base64URL auth token printed at startup.

---

## ✨ Key Features

### 1. Dynamic Audio Routing & Device Monitoring
- Monitor current active output peripherals (Built-in Speakers, Wired Headphones, or Bluetooth headsets) dynamically.
- Gracefully request and surrender system audio ownership.
- View linked wireless devices directly from the UI.

### 2. Low-Latency Media Telemetry & Interception
- Stream high-fidelity album art, active player names, track durations, and playback progress indicators between the host and client.
- Universal media commands: Play, Pause, Skip, and Seek.

### 3. Native Phone Call & Notification Mirroring
- Intercept and securely forward notifications from selected apps to the desktop screen immediately.
- Real-time caller identity translation, active call state updates, and ring detection mapped straight to the desktop terminal.

### 4. Executive Material 3 Design
- **Perfect Circle Avatars**: Highly-polished circular profile avatars with dynamic linear-gradient glowing borders.
- **Custom Initials Parsing**: Automatically parses user initials (e.g., "John Doe" ➔ "JD", "Synco User" ➔ "SU") to display as custom initials on the dashboard header.
- **Glassmorphic Layouts**: Smooth transparent cards styled with Material Design 3 spacing guidelines (8dp grid spacing) and clean typography.
- **Unified Settings Center**: Toggle app preferences, background service lifecycles, and edit your user profile display name dynamically.

---

## 🚀 Getting Started & Setup

Deploying **Synco** is straightforward and requires zero complex configuration. 

For complete step-by-step instructions—including Git repository cloning, setting up the Kotlin Desktop Server, compiling the Android `.apk` client, retrieving your local IP addresses, and pairing your device securely—please refer to our dedicated guide:

👉 **[Synco Full Setup & Pairing Guide (SETUP.md)](./SETUP.md)**

### Quick Command Reference

```bash
# 1. Clone the repository
git clone https://github.com/Dhruvesh-Dabhade/Synco.git
cd Synco

# 2. Build and launch the Desktop Companion
gradle :desktop:installDist
gradle :desktop:run
```
Upon startup, the terminal displays:
```
Listening on Port: 8765 | PIN Code: 742189
Web Dashboard: http://localhost:8080?token=<32-char-base64url-token>
```

```bash
# 3. Compile the Android Client app (debug)
gradle :app:assembleDebug

#    Compile the Android Client app (release — ProGuard enabled)
gradle :app:assembleRelease
```

---

## 🛠️ Codebase Structure

```
├── app/                      # Android Client Application
│   ├── src/main/java/        # Clean MVVM Source Code
│   │   ├── app/              # Application Application Context
│   │   ├── artwork/          # Local album art caching & binary downscaling
│   │   ├── crypto/           # AES-GCM & Diffie-Hellman cryptographic providers
│   │   ├── manager/          # System managers (Call, Bluetooth, Media, Notifications)
│   │   ├── network/          # WebSocket clients and reliable transmission channels
│   │   ├── protocol/         # Packet encoders, decoders, and payload validators
│   │   └── ui/               # Composable Screens (Network, Calls, Settings, Diagnostics)
│   └── src/test/             # Robust Robolectric unit test suites
│
├── desktop/                  # Desktop Companion Daemon
│   ├── src/main/kotlin/      # Javalin Server, Web UI, and Host controller logic
│   └── build.gradle.kts      # Desktop subproject configuration
│
└── build.gradle.kts          # Root gradle configuration
```

---

## 🔒 Privacy Notice
Synco is an **offline-first local-network utility**. All WebSocket packets, call states, keystores, and notifications are sent directly between your Android device and your desktop host over your local private Wi-Fi network. **No remote servers, external APIs, or analytics trackers are used.** Persistent desktop identity keys are encrypted at rest with AES-256-GCM. Your data is entirely yours.
