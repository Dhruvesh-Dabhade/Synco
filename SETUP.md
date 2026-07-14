# Synco 🎧 | Full System Integration & Setup Guide

This guide provides step-by-step instructions for cloning, compiling, deploying, and pairing the **Synco Client** (Android) and **Synco Companion** (Desktop).

---

## 📋 Prerequisites

Before starting, ensure your local development environment has the following installed:

- **Java Development Kit (JDK) 17 or 21**
- **Git** (installed and added to your system environment variables)
- **Active Local Network**: Both your Android device and Desktop computer **must be connected to the exact same Wi-Fi network / subnet** to communicate.

---

## 🚀 Step-by-Step Deployment

### 1. Clone the Repository
Open a terminal (Command Prompt, PowerShell, or Git Bash) on your computer and execute:

```bash
# Clone the repository locally
git clone https://github.com/Dhruvesh-Dabhade/Synco.git

# Navigate into the project root directory
cd Synco
```

---

### 💻 2. Compile and Start the Desktop Companion

The Desktop Companion is a fast Kotlin server. To install dependencies and run the daemon:

```bash
# Build and extract the companion binary distribution
gradle :desktop:installDist

# Launch the server companion
gradle :desktop:run
```

Once started, the terminal displays:
```
=================================================================
                     Synco - Desktop Server v1.0.0
=================================================================
[INFO] Booting up subproject environment...
[SERVER] WebSocket Server successfully launched on port 8765
[CONSOLE] Interactive Shell ready. Type 'help' to see controls.
[CONSOLE] Listening on Port: 8765 | PIN Code: 742189
```

#### Pairing PIN
- A new random **6-digit PIN** is generated every time the desktop starts.
- You must enter this PIN in the Android app to complete the **PAIR_REQUEST** handshake.
- If the PIN does not match, the server rejects the connection.

#### Web Dashboard (Auth Required)
The desktop also starts a web-based control panel on port **8080**:
- Open your browser and navigate to: **`http://localhost:8080?token=<auth-token>`**
- The **auth token** is a random 32-byte Base64URL string printed once at startup (scroll up if missed).
- **Without the token, the dashboard returns 401 Unauthorized.**
- The dashboard displays connected device status, media controls, and real-time security log feeds.

#### Desktop Identity Keys
- Persistent Ed25519 identity keys are stored in `desktop/desktop_identity.keys.enc`.
- The private key is **AES-256-GCM encrypted** with a key derived from `desktop_identity.key`.
- The raw `desktop_identity.key` is excluded from version control (listed in `.gitignore`).
- These keys enable persistent device recognition across sessions.

---

### 🔍 3. Retrieve Your Desktop's Local IP Address

To pair the Android app, you will need the local IPv4 address of your computer.

#### On Windows (PowerShell/Command Prompt):
```powershell
ipconfig
```
Look for the active adapter (e.g., *Wireless LAN adapter Wi-Fi*) and locate the **IPv4 Address** (e.g., `192.168.1.45`).

#### On Linux / macOS (Terminal):
```bash
ip a | grep inet
# or
ifconfig | grep "inet "
```
Look for your active wireless or ethernet adapter address (excluding `127.0.0.1`).

---

### 📱 4. Build and Install the Android Client

To compile the Android client application directly from the command line:

```bash
# Compile and package the debug APK (no obfuscation)
gradle :app:assembleDebug

# Compile and package the release APK (ProGuard minification enabled)
gradle :app:assembleRelease
```

Once completed, the compiled APKs will be located at:
- Debug: `[Project-Root]/app/build/outputs/apk/debug/app-debug.apk`
- Release: `[Project-Root]/app/build/outputs/apk/release/app-release.apk`

> **Note:** The release build has **ProGuard** enabled for code obfuscation and minification. Use the debug build for testing, the release build for production deployment.

- Transfer the `.apk` file to your Android phone via USB, wireless sharing, or cloud storage.
- Open the file on your device to install it (ensure you grant installation permissions for unknown sources if prompted).

---

## 🤝 5. Securing the Wireless Connection

Synco runs on a zero-trust model. Follow these steps to pair your device securely:

1. **Launch the Desktop Companion** (Step 2) and note the **6-digit PIN** and **WebSocket port (8765)** printed in the terminal.
2. **Launch the Synco App** on your phone.
3. **Grant Permissions**:
   - **Notification Listening Access**: Allows Synco to stream media playback telemetry and notifications.
   - **Phone/Calls Access**: Allows Synco to notify you of incoming calls on your desktop terminal.
   - **Bluetooth Connect** (Android 12+): Required for audio device monitoring.
4. **Personalize Your Profile**:
   - Tap the glowing profile circle in the top right corner to enter the **Settings Center**.
   - Input your name (e.g., `John Doe`). The dashboard header will automatically parse your name and display a clean, high-contrast, perfectly circular avatar with your initials (`JD`).
5. **Connect to Host**:
   - Enter your Desktop's **IPv4 Address** (from Step 3) and **Port** (`8765`).
   - When prompted, enter the **6-digit PIN** displayed on the desktop terminal.
   - The system performs a secure **X25519 Diffie-Hellman key exchange** with **Ed25519 digital signatures** to verify both identities and derive a shared AES-256-GCM session key.
   - Tap **Sync & Connect**. If the PIN is correct, the pairing succeeds and all subsequent packets are encrypted end-to-end with AAD-bound AES-256-GCM.
6. **Web Dashboard (Optional)**:
   - Once paired, open `http://localhost:8080?token=<auth-token>` on the desktop machine.
   - The dashboard shows live media telemetry, notification log, call states, and role switch controls — all served with a strict Content Security Policy and no external CDN dependencies.

---

