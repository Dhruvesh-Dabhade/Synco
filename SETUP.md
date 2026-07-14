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
git clone https://github.com/jyotideepak241988/Synco.git

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

Once started, the Javalin WebSocket server will start listening (typically on port `8080`). 
- Open your browser and navigate to: **`http://localhost:8080`**
- This opens the Web Control Panel, displaying connected device statuses, media controls, and real-time security log feeds.

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
# Compile and package the debug APK
gradle :app:assembleDebug
```

Once completed, the compiled APK will be located at:
`[Project-Root]/app/build/outputs/apk/debug/app-debug.apk`

- Transfer this `.apk` file to your Android phone via USB, wireless sharing, or cloud storage.
- Open the file on your device to install it (ensure you grant installation permissions for unknown sources if prompted).

---

## 🤝 5. Securing the Wireless Connection

Synco runs on a zero-trust model. Follow these steps to pair your device securely:

1. **Launch the Synco App** on your phone.
2. **Grant Permissions**:
   - **Notification Listening Access**: Allows Synco to stream media playback telemetry and notifications.
   - **Phone/Calls Access**: Allows Synco to notify you of incoming calls on your desktop terminal.
3. **Personalize Your Profile**:
   - Tap the glowing profile circle in the top right corner to enter the **Settings Center**.
   - Input your name (e.g., `John Doe`). The dashboard header will automatically parse your name and display a clean, high-contrast, perfectly circular avatar with your initials (`JD`).
4. **Connect to Host**:
   - Enter your Desktop's **IPv4 Address** (from Step 3) and **Port** (`8080`).
   - The system utilizes secure Diffie-Hellman Key Exchange to derive shared secrets automatically!
   - Tap **Sync & Connect**. Once paired, your notifications, media controls, and system events are synchronized seamlessly under full AES-GCM-256 encryption.

---

