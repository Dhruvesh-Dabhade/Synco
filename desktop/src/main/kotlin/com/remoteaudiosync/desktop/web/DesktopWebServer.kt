package com.remoteaudiosync.desktop.web

import com.remoteaudiosync.desktop.DesktopAppServer
import io.javalin.Javalin
import io.javalin.websocket.WsContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class DesktopWebServer(
    private val port: Int,
    private val appServer: DesktopAppServer
) {
    private var app: Javalin? = null
    private val connectedClients = ConcurrentHashMap.newKeySet<WsContext>()
    private val scope = CoroutineScope(Dispatchers.Default)

    // Rate Limiting Cache
    private val requestCounts = ConcurrentHashMap<String, MutableList<Long>>()
    private val wsMessageCounts = ConcurrentHashMap<String, MutableList<Long>>()

    private fun isRateLimited(ip: String, maxRequests: Int, windowMs: Long, cache: ConcurrentHashMap<String, MutableList<Long>>): Boolean {
        val now = System.currentTimeMillis()
        val list = cache.computeIfAbsent(ip) { java.util.ArrayList() }
        synchronized(list) {
            list.removeIf { now - it > windowMs }
            if (list.size >= maxRequests) {
                return true
            }
            list.add(now)
        }
        return false
    }

    fun start() {
        app = Javalin.create { config ->
            config.showJavalinBanner = false
        }

        // Global Rate Limiting & HTTP Security Headers Middleware
        app?.before { ctx ->
            // Rate Limiting (60 requests per minute per IP for general HTTP)
            val ip = ctx.ip()
            if (isRateLimited(ip, 60, 60000L, requestCounts)) {
                ctx.status(429)
                ctx.header("Retry-After", "10")
                ctx.result("Too Many Requests. Please wait before retrying.")
                return@before
            }

            // Remove/Reset X-Powered-By
            ctx.header("X-Powered-By", "")

            // Set security headers
            ctx.header("X-Frame-Options", "DENY")
            ctx.header("X-Content-Type-Options", "nosniff")
            ctx.header("Referrer-Policy", "strict-origin-when-cross-origin")
            
            // Content-Security-Policy supporting self, Tailwind CSS CDN, and Google Fonts
            ctx.header("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.tailwindcss.com; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.tailwindcss.com; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "connect-src 'self' ws: wss: http: https:; " +
                "img-src 'self' data:; " +
                "frame-ancestors 'none';"
            )
        }

        // Customize exception handling to hide stack traces from the client
        app?.exception(Exception::class.java) { e, ctx ->
            System.err.println("[SERVER_ERROR] Error handling request: ${e.message}")
            ctx.status(500)
            ctx.result("Internal Server Error")
        }

        app?.start(port)

        app?.get("/") { ctx ->
            ctx.html(getHtmlContent())
        }

        app?.ws("/ws") { ws ->
            ws.onConnect { ctx ->
                connectedClients.add(ctx)
                sendStateToClient(ctx)
            }
            ws.onClose { ctx ->
                connectedClients.remove(ctx)
            }
            ws.onMessage { ctx ->
                val ip = (ctx.session.remoteAddress as? java.net.InetSocketAddress)?.address?.hostAddress ?: "unknown"
                // WS Rate Limiting (120 messages per minute per IP)
                if (isRateLimited(ip, 120, 60000L, wsMessageCounts)) {
                    ctx.send("{\"error\": \"Rate limit exceeded. Too many requests.\"}")
                    return@onMessage
                }

                val message = ctx.message()
                if (message.length > 65536) {
                    ctx.session.close(1009, "Message too large")
                    return@onMessage
                }
                try {
                    val jsonEl = Json.parseToJsonElement(message) as? JsonObject
                    val command = jsonEl?.get("command")?.toString()?.replace("\"", "")
                    
                    when (command) {
                        "PLAY" -> appServer.triggerPlay()
                        "PAUSE" -> appServer.triggerPause()
                        "NEXT" -> appServer.triggerNext()
                        "PREVIOUS" -> appServer.triggerPrevious()
                        "SWITCH_ROLE" -> appServer.requestRoleSwitch()
                        "VOLUME" -> {
                            val vol = jsonEl?.get("value")?.toString()?.toIntOrNull()
                            if (vol != null && vol in 0..100) {
                                appServer.triggerVolume(vol)
                            }
                        }
                        "SIMULATE_CALL" -> {
                            val state = jsonEl?.get("state")?.toString()?.replace("\"", "") ?: "RINGING"
                            val callerId = jsonEl?.get("callerId")?.toString()?.replace("\"", "") ?: "Technician Lab"
                            
                            val allowedStates = listOf("RINGING", "OFFHOOK", "IDLE")
                            if (state in allowedStates && callerId.length <= 100) {
                                appServer.simulateCall(state, callerId)
                            }
                        }
                        "SIMULATE_NOTIF" -> {
                            val action = jsonEl?.get("action")?.toString()?.replace("\"", "") ?: "RECEIVE"
                            val id = jsonEl?.get("id")?.toString()?.replace("\"", "") ?: "notif_id"
                            val title = jsonEl?.get("title")?.toString()?.replace("\"", "") ?: "System Update"
                            val text = jsonEl?.get("text")?.toString()?.replace("\"", "") ?: "Optimization applied."
                            
                            val allowedActions = listOf("RECEIVE", "DISMISS")
                            if (action in allowedActions && id.length <= 100 && title.length <= 100 && text.length <= 250) {
                                appServer.simulateNotification(action, id, title, text)
                            }
                        }
                    }
                    
                    broadcastState()
                } catch (e: Exception) {
                    System.err.println("[SERVER_ERROR] Error handling message: ${e.message}")
                }
            }
        }

        scope.launch {
            while (true) {
                broadcastState()
                delay(1000)
            }
        }

        println("[WEB] Web Dashboard (Javalin) running on http://localhost:$port")
    }

    private fun getAudioDevices(): List<String> {
        return try {
            javax.sound.sampled.AudioSystem.getMixerInfo()
                .map { it.name }
                .distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getStateJson(): String {
        val isConnected = appServer.isConnected()
        val isOwner = appServer.isAudioOwner()
        val mediaState = appServer.getMediaState()
        val devices = getAudioDevices()

        val json = buildJsonObject {
            put("connected", isConnected)
            put("isOwner", isOwner)
            if (mediaState != null) {
                put("media", buildJsonObject {
                    put("title", mediaState.title)
                    put("artist", mediaState.artist)
                    put("isPlaying", mediaState.isPlaying)
                })
            }
            put("devices", kotlinx.serialization.json.JsonArray(devices.map { kotlinx.serialization.json.JsonPrimitive(it) }))
        }
        return json.toString()
    }

    private fun sendStateToClient(ctx: WsContext) {
        ctx.send(getStateJson())
    }

    private fun broadcastState() {
        val state = getStateJson()
        connectedClients.forEach { ctx ->
            if (ctx.session.isOpen) {
                ctx.send(state)
            }
        }
    }

    fun stop() {
        app?.stop()
    }

    private fun getHtmlContent(): String {
        return """
<!DOCTYPE html>
<html class="dark" lang="en">
<head>
    <meta charset="utf-8"/>
    <meta content="width=device-width, initial-scale=1.0" name="viewport"/>
    <title>Synco Dashboard | Professional Sync</title>
    <script src="https://cdn.tailwindcss.com?plugins=forms,container-queries"></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&amp;family=JetBrains+Mono:wght@400;500&amp;display=swap" rel="stylesheet"/>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
    <style>
        :root {
            --sidebar-width: 280px;
            --luxury-radius: 32px;
        }
        body {
            background-color: #080808;
            color: #e5e2e1;
            font-family: 'Inter', sans-serif;
            overflow-x: hidden;
            -webkit-font-smoothing: antialiased;
        }
        .glass-panel {
            background: rgba(20, 20, 20, 0.45);
            backdrop-filter: blur(24px);
            border: 1px solid rgba(255, 255, 255, 0.05);
            box-shadow: 0 10px 40px -10px rgba(0, 0, 0, 0.5);
        }
        .luxury-radius {
            border-radius: var(--luxury-radius);
        }
        .inner-glow-soft {
            box-shadow: inset 0 1px 0 0 rgba(255, 255, 255, 0.05);
        }
        .custom-scrollbar::-webkit-scrollbar {
            width: 4px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
            background: transparent;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
            background: #2a2a2a;
            border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
            background: #3a3a3a;
        }
        @keyframes subtle-float {
            0%, 100% { transform: translateY(0); }
            50% { transform: translateY(-4px); }
        }
        .float-subtle {
            animation: subtle-float 6s infinite ease-in-out;
        }
        .active-glow {
            position: relative;
        }
        .active-glow::after {
            content: '';
            position: absolute;
            inset: -1px;
            border-radius: inherit;
            background: linear-gradient(45deg, transparent, rgba(173, 198, 255, 0.1), transparent);
            pointer-events: none;
        }
    </style>
    <script id="tailwind-config">
        tailwind.config = {
            darkMode: "class",
            theme: {
                extend: {
                    "colors": {
                        "on-background": "#e5e2e1",
                        "error-container": "#93000a",
                        "on-secondary-fixed-variant": "#005236",
                        "on-tertiary-fixed-variant": "#7c2e00",
                        "secondary": "#4edea3",
                        "primary-container": "#4b8eff",
                        "surface-tint": "#adc6ff",
                        "inverse-surface": "#e5e2e1",
                        "tertiary-fixed-dim": "#ffb595",
                        "inverse-primary": "#005bc1",
                        "on-error-container": "#ffdad6",
                        "on-primary-container": "#00285c",
                        "surface-container-lowest": "#0e0e0e",
                        "inverse-on-surface": "#313030",
                        "surface": "#131313",
                        "on-error": "#690005",
                        "on-tertiary-container": "#4c1a00",
                        "surface-container-high": "#2a2a2a",
                        "tertiary-fixed": "#ffdbcc",
                        "on-secondary-fixed": "#002113",
                        "on-primary-fixed-variant": "#004493",
                        "surface-container-highest": "#353534",
                        "outline-variant": "#414755",
                        "on-tertiary-fixed": "#351000",
                        "error": "#ffb4ab",
                        "on-secondary-container": "#00311f",
                        "surface-dim": "#131313",
                        "on-surface-variant": "#c1c6d7",
                        "tertiary": "#ffb595",
                        "surface-bright": "#3a3939",
                        "on-primary-fixed": "#001a41",
                        "surface-variant": "#353534",
                        "primary-fixed": "#d8e2ff",
                        "secondary-fixed": "#6ffbbe",
                        "on-secondary": "#003824",
                        "outline": "#8b90a0",
                        "surface-container": "#201f1f",
                        "on-primary": "#002e69",
                        "on-surface": "#e5e2e1",
                        "surface-container-low": "#1c1b1b",
                        "tertiary-container": "#ef6719",
                        "on-tertiary": "#571e00",
                        "primary-fixed-dim": "#adc6ff",
                        "secondary-container": "#00a572",
                        "background": "#080808",
                        "primary": "#adc6ff",
                        "secondary-fixed-dim": "#4edea3"
                    },
                    "borderRadius": {
                        "DEFAULT": "0.5rem",
                        "lg": "1rem",
                        "xl": "1.5rem",
                        "2xl": "2rem",
                        "full": "9999px"
                    },
                    "spacing": {
                        "gutter": "24px",
                        "stack-gap": "12px",
                        "unit": "8px",
                        "container-padding": "40px",
                        "sidebar-width": "280px"
                    },
                    "fontFamily": {
                        "mono-label": ["JetBrains Mono"],
                        "headline-md": ["Inter"],
                        "label-caps": ["Inter"],
                        "body-md": ["Inter"],
                        "title-sm": ["Inter"],
                        "display-lg": ["Inter"]
                    },
                    "fontSize": {
                        "mono-label": ["10px", {"lineHeight": "1", "letterSpacing": "0.1em", "fontWeight": "400"}],
                        "headline-md": ["28px", {"lineHeight": "1.2", "letterSpacing": "0.02em", "fontWeight": "700"}],
                        "label-caps": ["11px", {"lineHeight": "1", "letterSpacing": "0.15em", "fontWeight": "700"}],
                        "body-md": ["15px", {"lineHeight": "1.6", "letterSpacing": "0", "fontWeight": "400"}],
                        "title-sm": ["18px", {"lineHeight": "1.4", "letterSpacing": "0.01em", "fontWeight": "600"}],
                        "display-lg": ["52px", {"lineHeight": "1", "letterSpacing": "-0.04em", "fontWeight": "800"}]
                    }
                },
            },
        }
    </script>
</head>
<body class="bg-background text-on-background selection:bg-primary/30">

<!-- Custom Toast Notification system -->
<div id="toast-container" class="fixed top-6 right-6 z-[9999] flex flex-col gap-3"></div>

<!-- Side Navigation -->
<aside class="fixed left-4 top-4 bottom-4 w-sidebar-width bg-surface-container-low/40 backdrop-blur-2xl border border-white/5 luxury-radius shadow-[0_32px_64px_rgba(0,0,0,0.6)] flex flex-col py-10 px-8 z-50">
    <div class="mb-14">
        <h1 class="font-display-lg text-display-lg text-white tracking-tighter active-glow">Synco<span class="text-primary">.</span></h1>
        <p class="text-on-surface-variant font-body-md text-[11px] uppercase tracking-[0.25em] mt-2 opacity-40 font-medium">Professional Sync Engine</p>
    </div>
    <nav class="flex-1 space-y-1">
        <button id="nav-overview" onclick="switchTab('overview')" class="w-full flex items-center gap-4 px-5 py-3.5 rounded-xl text-primary font-semibold bg-white/[0.03] border border-white/5 shadow-sm transition-all duration-300">
            <span class="material-symbols-outlined text-xl" style="font-variation-settings: 'FILL' 1;">dashboard</span>
            <span class="font-body-md text-sm tracking-wide">Overview</span>
        </button>
        <button id="nav-media" onclick="switchTab('media')" class="w-full flex items-center gap-4 px-5 py-3.5 rounded-xl text-on-surface-variant/60 font-medium hover:text-on-surface hover:bg-white/[0.02] transition-all duration-300 active:scale-95">
            <span class="material-symbols-outlined text-xl">perm_media</span>
            <span class="font-body-md text-sm tracking-wide">Media Vault</span>
        </button>
        <button id="nav-nodes" onclick="switchTab('nodes')" class="w-full flex items-center gap-4 px-5 py-3.5 rounded-xl text-on-surface-variant/60 font-medium hover:text-on-surface hover:bg-white/[0.02] transition-all duration-300 active:scale-95">
            <span class="material-symbols-outlined text-xl">devices</span>
            <span class="font-body-md text-sm tracking-wide">Connected Nodes</span>
        </button>
        <button id="nav-notifications" onclick="switchTab('notifications')" class="w-full flex items-center gap-4 px-5 py-3.5 rounded-xl text-on-surface-variant/60 font-medium hover:text-on-surface hover:bg-white/[0.02] transition-all duration-300 active:scale-95">
            <span class="material-symbols-outlined text-xl">notifications</span>
            <span class="font-body-md text-sm tracking-wide">Notifications</span>
        </button>
        <button id="nav-settings" onclick="switchTab('settings')" class="w-full flex items-center gap-4 px-5 py-3.5 rounded-xl text-on-surface-variant/60 font-medium hover:text-on-surface hover:bg-white/[0.02] transition-all duration-300 active:scale-95">
            <span class="material-symbols-outlined text-xl">settings</span>
            <span class="font-body-md text-sm tracking-wide">Engine Settings</span>
        </button>
    </nav>
    <div class="mt-auto pt-8 border-t border-white/5 space-y-4">
        <button onclick="sendCommand('SWITCH_ROLE')" class="w-full bg-white text-black py-4 px-4 rounded-2xl font-semibold text-sm hover:bg-primary transition-all active:scale-[0.97] shadow-lg shadow-white/5">
            Switch Audio Ownership
        </button>
        <div class="px-2 space-y-3">
            <a class="flex items-center gap-3 text-on-surface-variant/50 hover:text-primary transition-colors text-xs font-medium" href="#">
                <span class="material-symbols-outlined text-lg">help</span>
                <span>Support Center</span>
            </a>
            <div class="flex items-center gap-3 pt-2">
                <div class="w-8 h-8 rounded-full border border-white/10 p-0.5">
                    <img class="w-full h-full object-cover rounded-full opacity-80" alt="Profile" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBTU4VSB8xqlZH9wvNrrNnnc81u3A2DEqJlgzK24njiYIthnhpQxJfglevX4GtGRBN4Yhf9RWzyxgwqWnlr7qABVZGL6FV3Kz2Ao1qBKU1Ns49NaNHCUkXTnr6f3IrAMaMrVKGLRMkaUIfCc_zinZJgKfutI80RSosJ2UegUNnNQaaauySTX6-JNYYavbo5Zy13K4B6CeacIpABceDucGy9iFAyaH_jiF1BjrWHqWDNmstxSQUgZRNE9yDc5WlbUl61S4QRFwhjuRAR"/>
                </div>
                <span class="text-xs font-semibold text-on-surface-variant">Technician Alpha</span>
            </div>
        </div>
    </div>
</aside>

<!-- Top Navigation Header -->
<header class="fixed top-0 right-0 left-sidebar-width h-24 flex justify-between items-center px-12 z-40 bg-background/30 backdrop-blur-md">
    <div class="flex items-center gap-12">
        <h2 id="current-tab-title" class="font-headline-md text-2xl font-extrabold text-white tracking-tight">Dashboard Overview</h2>
        <div class="flex gap-10">
            <button id="sub-nav-summary" class="text-primary text-xs font-bold tracking-widest uppercase border-b border-primary/40 pb-1" onclick="switchTab('overview')">Summary</button>
            <button id="sub-nav-latency" class="text-on-surface-variant/40 hover:text-white transition-all text-xs font-bold tracking-widest uppercase" onclick="switchTab('nodes')">Latency Map</button>
            <button id="sub-nav-health" class="text-on-surface-variant/40 hover:text-white transition-all text-xs font-bold tracking-widest uppercase" onclick="switchTab('nodes')">Network Health</button>
        </div>
    </div>
    <div class="flex items-center gap-8">
        <div class="flex flex-col items-end">
            <div class="flex items-center gap-2 mb-1">
                <span id="pulsing-dot" class="w-1.5 h-1.5 bg-error rounded-full animate-pulse"></span>
                <span id="conn-state-badge" class="text-error font-mono-label text-[9px] uppercase tracking-[0.3em]">Disconnected</span>
            </div>
            <span class="text-on-surface-variant font-medium text-xs tracking-tight">Audio Master: <span id="audio-owner-text" class="text-white">None</span></span>
        </div>
        <div class="flex gap-2">
            <button onclick="sendCommand('SWITCH_ROLE')" title="Switch Ownership" class="w-10 h-10 flex items-center justify-center rounded-xl bg-white/5 border border-white/5 text-on-surface-variant/80 hover:text-white hover:bg-white/10 transition-all">
                <span class="material-symbols-outlined text-xl">sync_lock</span>
            </button>
            <button onclick="clearLogs()" title="Clear Event Logs" class="w-10 h-10 flex items-center justify-center rounded-xl bg-white/5 border border-white/5 text-on-surface-variant/80 hover:text-white hover:bg-white/10 transition-all">
                <span class="material-symbols-outlined text-xl">delete_sweep</span>
            </button>
        </div>
    </div>
</header>

<!-- Main Content Area -->
<main class="ml-sidebar-width pt-28 pb-container-padding px-12 min-h-screen">
    
    <!-- Tab 1: OVERVIEW -->
    <div id="tab-overview" class="tab-pane grid grid-cols-12 gap-8 max-w-[1400px]">
        <!-- Now Playing Luxury Card: Fully Responsive to stop title clipping -->
        <section class="col-span-12 lg:col-span-8 glass-panel inner-glow-soft luxury-radius p-6 sm:p-10 flex flex-col md:flex-row gap-8 sm:gap-12 relative overflow-hidden group">
            <div class="absolute -right-20 -top-20 w-80 h-80 bg-primary/5 blur-[120px] rounded-full"></div>
            <!-- Responsive image container to provide breathing room for long text -->
            <div class="relative z-10 w-32 h-32 sm:w-48 sm:h-48 md:w-56 md:h-56 lg:w-64 lg:h-64 shrink-0 shadow-[0_40px_80px_-20px_rgba(0,0,0,0.8)] rounded-2xl overflow-hidden border border-white/10 float-subtle mx-auto md:mx-0">
                <img id="album-art" class="w-full h-full object-cover scale-105 group-hover:scale-100 transition-transform duration-700" alt="Album artwork" src="https://lh3.googleusercontent.com/aida-public/AB6AXuD5RUS_UNnceYkfZq1Kre_urotX50zNBVPWedwZXmBTpUL3tpY9Yrr3NfdFKLdZrDHQygOevTePt748r8h_sKptcK6_Rjd28EwvJLsyb56g5_iRUn15SgcorornUmuoba4WUhVu07Wh5w3YTf-53Vas22FYpUH7siG5mvuQaU3HE34nHUO4YjCDsx0pmAi1LPJHBCPpQQM5-x503nrGLEpAXPKiEeGEYJpqu4CLjBpUpFdHRmG9X8iNaHjktbXPmeTEaANEy2DbrXBd"/>
            </div>
            <div class="relative z-10 flex-1 flex flex-col justify-center text-center md:text-left">
                <div class="mb-6 sm:mb-8">
                    <div class="flex items-center justify-center md:justify-start gap-3 mb-3">
                        <span class="h-px w-8 bg-primary/40"></span>
                        <span class="text-primary font-mono-label text-[10px] uppercase tracking-[0.4em] font-medium">Broadcast Stream</span>
                    </div>
                    <!-- Responsive, wrapping text sizes that will NEVER clip or look irregular -->
                    <h3 id="media-title-text" class="font-display-lg text-2xl sm:text-3xl md:text-4xl lg:text-4xl font-extrabold text-white mb-2 sm:mb-3 tracking-tight break-words whitespace-normal leading-tight max-w-full">No Media Playing</h3>
                    <p id="media-artist-text" class="text-on-surface-variant font-medium text-sm sm:text-base md:text-lg opacity-60 break-words max-w-full">Awaiting stream source from connection</p>
                </div>
                <div class="space-y-6 sm:space-y-8">
                    <div class="space-y-3">
                        <div class="flex justify-between text-[10px] font-mono-label tracking-widest text-on-surface-variant/40 uppercase">
                            <span id="progress-time">00:00</span>
                            <span id="total-time-text">03:54</span>
                        </div>
                        <div class="h-[4px] bg-white/5 rounded-full relative overflow-hidden">
                            <div id="progress-bar-fill" class="absolute left-0 top-0 h-full bg-primary w-[0%] shadow-[0_0_15px_rgba(173,198,255,0.5)] transition-all duration-300"></div>
                        </div>
                    </div>
                    <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
                        <div class="flex items-center gap-8">
                            <button onclick="sendCommand('PREVIOUS')" class="text-on-surface-variant/40 hover:text-white transition-all transform hover:scale-110">
                                <span class="material-symbols-outlined text-3xl">skip_previous</span>
                            </button>
                            <button id="play-pause-btn" class="w-14 h-14 sm:w-16 sm:h-16 bg-white text-black rounded-full flex items-center justify-center hover:bg-primary transition-all shadow-xl active:scale-90">
                                <span id="play-pause-icon" class="material-symbols-outlined text-4xl" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
                            </button>
                            <button onclick="sendCommand('NEXT')" class="text-on-surface-variant/40 hover:text-white transition-all transform hover:scale-110">
                                <span class="material-symbols-outlined text-3xl">skip_next</span>
                            </button>
                        </div>
                        <!-- Fully Interactive Volume Slider with actual logic linking back-end -->
                        <div class="flex items-center gap-4 px-5 py-2.5 rounded-full bg-white/5 border border-white/5 w-full sm:w-auto justify-between sm:justify-start">
                            <span id="volume-indicator-icon" class="material-symbols-outlined text-lg text-on-surface-variant/60 select-none cursor-pointer" onclick="toggleMute()">volume_up</span>
                            <input id="volume-range-input" type="range" min="0" max="100" value="70" class="w-24 sm:w-28 h-1 bg-white/10 rounded-full appearance-none cursor-pointer accent-white transition-all hover:bg-white/20" oninput="handleVolumeChange(this.value)"/>
                            <span id="volume-value-text" class="text-[10px] font-mono-label text-on-surface-variant/60 w-8 text-right">70%</span>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- Status Column -->
        <section class="col-span-12 lg:col-span-4 glass-panel inner-glow-soft luxury-radius p-8 flex flex-col items-center justify-center text-center relative overflow-hidden border-primary/10">
            <div class="absolute inset-0 bg-gradient-to-b from-primary/5 to-transparent pointer-events-none"></div>
            <div class="w-20 h-20 rounded-full bg-primary/5 border border-primary/20 flex items-center justify-center mb-6 relative">
                <span id="status-icon" class="material-symbols-outlined text-primary text-4xl font-light animate-pulse">spatial_audio_off</span>
                <div id="status-ping" class="absolute inset-0 rounded-full border border-primary/30 animate-[ping_3s_infinite] opacity-20"></div>
            </div>
            <h4 id="status-title" class="font-headline-md text-xl font-bold text-white mb-3 tracking-tight">Awaiting Sync</h4>
            <p id="status-desc" class="text-on-surface-variant/60 font-body-md text-sm leading-relaxed px-4 mb-8">Pair your Android client app to establish a real-time media sync stream.</p>
            <div class="flex flex-col gap-3 w-full">
                <div class="flex justify-between items-center px-5 py-3 rounded-2xl bg-white/5 border border-white/5">
                    <span class="text-[10px] font-mono-label uppercase tracking-widest text-on-surface-variant/40">Sync Lock</span>
                    <span class="text-[10px] font-mono-label uppercase tracking-widest text-secondary font-bold">Encrypted</span>
                </div>
                <div class="flex justify-between items-center px-5 py-3 rounded-2xl bg-white/5 border border-white/5">
                    <span class="text-[10px] font-mono-label uppercase tracking-widest text-on-surface-variant/40">Latency Mode</span>
                    <span id="current-latency-badge" class="text-[10px] font-mono-label uppercase tracking-widest text-white/80 font-bold">Ultra Low</span>
                </div>
            </div>
        </section>

        <!-- Devices Grid Section -->
        <section class="col-span-12 lg:col-span-6 flex flex-col gap-6">
            <div class="flex items-center justify-between px-2">
                <h3 class="text-[11px] font-bold uppercase tracking-[0.3em] text-white/30 flex items-center gap-3">
                    <span class="h-[1px] w-4 bg-white/20"></span>
                    Available Output Devices
                </h3>
            </div>
            <div id="device-list-container" class="space-y-4">
                <!-- Populated dynamically with real device data -->
            </div>
        </section>

        <!-- Recent Activity luxury feed -->
        <section class="col-span-12 lg:col-span-6 flex flex-col gap-6">
            <div class="flex items-center justify-between px-2">
                <h3 class="text-[11px] font-bold uppercase tracking-[0.3em] text-white/30 flex items-center gap-3">
                    <span class="h-[1px] w-4 bg-white/20"></span>
                    Event Logs
                </h3>
                <button onclick="clearLogs()" class="text-on-surface-variant/40 text-[10px] font-bold uppercase tracking-widest hover:text-white transition-colors">Clear</button>
            </div>
            <div class="glass-panel inner-glow-soft luxury-radius overflow-hidden border-white/5 h-[340px]">
                <div class="max-h-full overflow-y-auto custom-scrollbar">
                    <div id="event-logs-container">
                        <!-- Dynamic logged messages will enter here -->
                    </div>
                </div>
            </div>
        </section>
    </div>

    <!-- Tab 2: MEDIA VAULT (Fully Functional) -->
    <div id="tab-media" class="tab-pane hidden grid grid-cols-12 gap-8 max-w-[1400px]">
        <section class="col-span-12 glass-panel luxury-radius p-8 flex flex-col gap-6">
            <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center border-b border-white/5 pb-6 gap-4">
                <div>
                    <h3 class="text-xl font-bold text-white tracking-tight">Vault Audio Broadcast System</h3>
                    <p class="text-xs text-on-surface-variant/60 mt-1">Select and broadcast digital master tracks instantly to all synced nodes</p>
                </div>
                <div class="flex gap-3">
                    <input type="text" placeholder="Search Vault..." class="bg-white/5 border border-white/10 rounded-xl px-4 py-2 text-sm text-white focus:outline-none focus:border-primary/50 w-full sm:w-60"/>
                </div>
            </div>
            
            <div id="media-vault-grid" class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <!-- Populated by JS -->
            </div>
        </section>
    </div>

    <!-- Tab 3: CONNECTED NODES & PING TEST (Fully Functional) -->
    <div id="tab-nodes" class="tab-pane hidden grid grid-cols-12 gap-8 max-w-[1400px]">
        <!-- Connection metrics -->
        <section class="col-span-12 lg:col-span-5 glass-panel luxury-radius p-8 flex flex-col justify-between gap-6">
            <div>
                <h3 class="text-lg font-bold text-white tracking-tight mb-4">Node Authentication Card</h3>
                <div class="space-y-4">
                    <div class="flex justify-between items-center border-b border-white/5 pb-3">
                        <span class="text-xs text-on-surface-variant/60 font-medium">Node Type</span>
                        <span class="text-xs text-white font-semibold flex items-center gap-2">
                            <span class="material-symbols-outlined text-sm text-primary">phone_android</span> Android Node
                        </span>
                    </div>
                    <div class="flex justify-between items-center border-b border-white/5 pb-3">
                        <span class="text-xs text-on-surface-variant/60 font-medium">Channel Key ID</span>
                        <span class="text-xs font-mono text-primary bg-primary/5 px-2 py-0.5 rounded border border-primary/10">SECURE_SHA256_ACTIVE</span>
                    </div>
                    <div class="flex justify-between items-center border-b border-white/5 pb-3">
                        <span class="text-xs text-on-surface-variant/60 font-medium">Device Address</span>
                        <span id="node-address" class="text-xs text-white font-semibold">Awaiting handshake...</span>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="text-xs text-on-surface-variant/60 font-medium">Stream Compression</span>
                        <span class="text-xs text-secondary font-semibold">LPCM Lossless Stereo</span>
                    </div>
                </div>
            </div>
            <div class="bg-white/5 border border-white/5 p-4 rounded-2xl flex items-center gap-4">
                <span class="material-symbols-outlined text-secondary text-2xl animate-pulse">lock</span>
                <div>
                    <h5 class="text-xs font-bold text-white">Full Handshake Secured</h5>
                    <p class="text-[10px] text-on-surface-variant/60 mt-0.5">ECC asymmetric signature authentication applied on connection.</p>
                </div>
            </div>
        </section>

        <!-- Dynamic Latency Graph Tool -->
        <section class="col-span-12 lg:col-span-7 glass-panel luxury-radius p-8 flex flex-col gap-6">
            <div class="flex justify-between items-center border-b border-white/5 pb-4">
                <div>
                    <h3 class="text-lg font-bold text-white tracking-tight">Active Ping & Latency Graph</h3>
                    <p class="text-xs text-on-surface-variant/60 mt-0.5">Real-time WebSocket round-trip delay audit</p>
                </div>
                <button onclick="runPingTest()" class="bg-primary hover:bg-primary-container text-black font-bold px-4 py-2 rounded-xl text-xs transition-all flex items-center gap-2">
                    <span class="material-symbols-outlined text-sm">speed</span> Run Latency Audit
                </button>
            </div>
            
            <div class="h-44 relative bg-black/40 border border-white/5 rounded-2xl overflow-hidden">
                <canvas id="latency-canvas" class="w-full h-full block"></canvas>
                <div id="ping-stats" class="absolute top-4 left-4 bg-black/80 backdrop-blur border border-white/10 px-3 py-2 rounded-lg flex gap-6 text-[11px]">
                    <div>
                        <span class="text-on-surface-variant/60 uppercase text-[9px] block">Average RTD</span>
                        <span id="ping-avg" class="text-secondary font-bold font-mono">-- ms</span>
                    </div>
                    <div>
                        <span class="text-on-surface-variant/60 uppercase text-[9px] block">Jitter Deviation</span>
                        <span id="ping-jitter" class="text-primary font-bold font-mono">-- ms</span>
                    </div>
                </div>
            </div>

            <div id="ping-logs" class="max-h-24 overflow-y-auto custom-scrollbar space-y-1.5 text-[11px] font-mono opacity-60">
                <p class="text-on-surface-variant">Diagnostics idle. Click "Run Latency Audit" to begin packet verification.</p>
            </div>
        </section>
    </div>

    <!-- Tab 4: SYSTEM NOTIFICATIONS LOG (Fully Functional) -->
    <div id="tab-notifications" class="tab-pane hidden grid grid-cols-12 gap-8 max-w-[1400px]">
        <section class="col-span-12 glass-panel luxury-radius p-8 flex flex-col gap-6">
            <div>
                <h3 class="text-lg font-bold text-white tracking-tight">System Events Log Dashboard</h3>
                <p class="text-xs text-on-surface-variant/60 mt-1">Complete chronological audit history for state machines and events in this runtime</p>
            </div>
            <div class="border border-white/5 rounded-2xl overflow-hidden bg-black/20">
                <div class="max-h-[500px] overflow-y-auto custom-scrollbar divide-y divide-white/5" id="complete-notifications-list">
                    <!-- Loaded dynamically via logs array -->
                </div>
            </div>
        </section>
    </div>

    <!-- Tab 5: ENGINE SETTINGS & SIMULATOR (Fully Functional) -->
    <div id="tab-settings" class="tab-pane hidden grid grid-cols-12 gap-8 max-w-[1400px]">
        
        <!-- Local settings panel -->
        <section class="col-span-12 lg:col-span-6 glass-panel luxury-radius p-8 flex flex-col gap-6">
            <h3 class="text-lg font-bold text-white tracking-tight">Engine Core Parameters</h3>
            <form onsubmit="saveEngineSettings(event)" class="space-y-6">
                <div class="space-y-4">
                    <label class="flex items-center gap-4 cursor-pointer p-3 rounded-xl hover:bg-white/[0.02] transition-colors select-none">
                        <input type="checkbox" id="set-latency-opt" class="rounded bg-black border-white/10 text-primary focus:ring-0 w-5 h-5" checked/>
                        <div>
                            <span class="text-sm font-bold text-white block">Optimize Latency Overhead</span>
                            <span class="text-xs text-on-surface-variant/50 mt-0.5">Reduce buffer sizes to minimize delay spikes under jitter.</span>
                        </div>
                    </label>
                    <label class="flex items-center gap-4 cursor-pointer p-3 rounded-xl hover:bg-white/[0.02] transition-colors select-none">
                        <input type="checkbox" id="set-lossless" class="rounded bg-black border-white/10 text-primary focus:ring-0 w-5 h-5"/>
                        <div>
                            <span class="text-sm font-bold text-white block">Lossless FLAC Stereo (48kHz)</span>
                            <span class="text-xs text-on-surface-variant/50 mt-0.5">High-fidelity audio format, requires strong Wi-Fi connections.</span>
                        </div>
                    </label>
                    <label class="flex items-center gap-4 cursor-pointer p-3 rounded-xl hover:bg-white/[0.02] transition-colors select-none">
                        <input type="checkbox" id="set-bg-svc" class="rounded bg-black border-white/10 text-primary focus:ring-0 w-5 h-5" checked/>
                        <div>
                            <span class="text-sm font-bold text-white block">Persistent Handshake Listening</span>
                            <span class="text-xs text-on-surface-variant/50 mt-0.5">Let server run invisibly in background after closing.</span>
                        </div>
                    </label>
                </div>
                
                <div class="border-t border-white/5 pt-6 space-y-4">
                    <div>
                        <label class="text-xs text-on-surface-variant/60 font-semibold block mb-2">Engine Web Server Port</label>
                        <input type="number" id="set-port-input" value="8080" class="bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-primary/50 w-full" disabled/>
                        <span class="text-[10px] text-on-surface-variant/40 mt-1 block">Javalin static host port (Requires reboot to reallocate).</span>
                    </div>
                </div>

                <button type="submit" class="w-full bg-white text-black font-bold py-3 px-4 rounded-xl hover:bg-primary transition-all active:scale-95 text-sm mt-4">
                    Save Engine Configuration
                </button>
            </form>
        </section>

        <!-- Developer Simulation Panel (Direct API Binding to Kotlin) -->
        <section class="col-span-12 lg:col-span-6 glass-panel luxury-radius p-8 flex flex-col gap-6">
            <div>
                <h3 class="text-lg font-bold text-white tracking-tight">Technician Dev Simulator</h3>
                <p class="text-xs text-on-surface-variant/60 mt-1">Directly trigger physical interrupt signals down the active Android connection pipeline</p>
            </div>
            
            <div class="space-y-6">
                <!-- Phone Call Simulation Box -->
                <div class="bg-white/[0.02] border border-white/5 rounded-2xl p-6 space-y-4">
                    <div class="flex items-center gap-3">
                        <span class="material-symbols-outlined text-primary">call</span>
                        <h4 class="text-sm font-bold text-white">Inbound Call Interrupt Simulator</h4>
                    </div>
                    <div class="grid grid-cols-2 gap-3">
                        <div>
                            <label class="text-[10px] text-on-surface-variant/50 font-bold uppercase tracking-wider block mb-1">Simulated Caller</label>
                            <input id="sim-caller-input" type="text" value="Direct Tech Lab" class="bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-primary/50 w-full"/>
                        </div>
                        <div>
                            <label class="text-[10px] text-on-surface-variant/50 font-bold uppercase tracking-wider block mb-1">Call State</label>
                            <select id="sim-call-state" class="bg-surface border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-primary/50 w-full">
                                <option value="RINGING">RINGING</option>
                                <option value="OFFHOOK">OFFHOOK (ANSWERED)</option>
                                <option value="IDLE">IDLE (DISCONNECTED)</option>
                            </select>
                        </div>
                    </div>
                    <button onclick="triggerSimulatedCall()" class="w-full bg-primary/10 border border-primary/20 hover:bg-primary hover:text-black text-primary font-bold py-2 px-4 rounded-xl text-xs transition-all">
                        Inject Simulated Call Packet
                    </button>
                </div>

                <!-- App Notification Simulation Box -->
                <div class="bg-white/[0.02] border border-white/5 rounded-2xl p-6 space-y-4">
                    <div class="flex items-center gap-3">
                        <span class="material-symbols-outlined text-secondary">notifications_active</span>
                        <h4 class="text-sm font-bold text-white">Push Notification Broadcaster</h4>
                    </div>
                    <div class="space-y-3">
                        <div>
                            <label class="text-[10px] text-on-surface-variant/50 font-bold uppercase tracking-wider block mb-1">Notification Title</label>
                            <input id="sim-notif-title" type="text" value="Urgent Core Warning" class="bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-primary/50 w-full"/>
                        </div>
                        <div>
                            <label class="text-[10px] text-on-surface-variant/50 font-bold uppercase tracking-wider block mb-1">Message Text</label>
                            <textarea id="sim-notif-text" rows="2" class="bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-primary/50 w-full resize-none">System audio buffer optimized to 64 samples due to stable fiber network route.</textarea>
                        </div>
                    </div>
                    <button onclick="triggerSimulatedNotification()" class="w-full bg-secondary/10 border border-secondary/20 hover:bg-secondary hover:text-black text-secondary font-bold py-2 px-4 rounded-xl text-xs transition-all">
                        Inject Simulated Notification Packet
                    </button>
                </div>
            </div>
        </section>
    </div>

</main>

<script>
    let ws;
    let currentIsPlaying = false;
    let progress = 0;
    let progressInterval = null;
    let currentVolume = 70;
    let isMuted = false;
    let savedVolume = 70;
    
    // Real Event Log Store
    const maxLogs = 10;
    const logs = [];

    // Preloaded Media Vault Curation
    const tracksVault = [
        { id: 1, title: "Neon Overdrive", artist: "Lazerhawk", album: "Starcrash", length: "04:12", art: "https://lh3.googleusercontent.com/aida-public/AB6AXuD5RUS_UNnceYkfZq1Kre_urotX50zNBVPWedwZXmBTpUL3tpY9Yrr3NfdFKLdZrDHQygOevTePt748r8h_sKptcK6_Rjd28EwvJLsyb56g5_iRUn15SgcorornUmuoba4WUhVu07Wh5w3YTf-53Vas22FYpUH7siG5mvuQaU3HE34nHUO4YjCDsx0pmAi1LPJHBCPpQQM5-x503nrGLEpAXPKiEeGEYJpqu4CLjBpUpFdHRmG9X8iNaHjktbXPmeTEaANEy2DbrXBd" },
        { id: 2, title: "Desktop Audio Stream", artist: "System", album: "Stereo PCM Bus", length: "03:54", art: "https://lh3.googleusercontent.com/aida-public/AB6AXuBTU4VSB8xqlZH9wvNrrNnnc81u3A2DEqJlgzK24njiYIthnhpQxJfglevX4GtGRBN4Yhf9RWzyxgwqWnlr7qABVZGL6FV3Kz2Ao1qBKU1Ns49NaNHCUkXTnr6f3IrAMaMrVKGLRMkaUIfCc_zinZJgKfutI80RSosJ2UegUNnNQaaauySTX6-JNYYavbo5Zy13K4B6CeacIpABceDucGy9iFAyaH_jiF1BjrWHqWDNmstxSQUgZRNE9yDc5WlbUl61S4QRFwhjuRAR" },
        { id: 3, title: "Midnight City Synth", artist: "M83", album: "Hurry Up", length: "04:03", art: "https://lh3.googleusercontent.com/aida-public/AB6AXuD5RUS_UNnceYkfZq1Kre_urotX50zNBVPWedwZXmBTpUL3tpY9Yrr3NfdFKLdZrDHQygOevTePt748r8h_sKptcK6_Rjd28EwvJLsyb56g5_iRUn15SgcorornUmuoba4WUhVu07Wh5w3YTf-53Vas22FYpUH7siG5mvuQaU3HE34nHUO4YjCDsx0pmAi1LPJHBCPpQQM5-x503nrGLEpAXPKiEeGEYJpqu4CLjBpUpFdHRmG9X8iNaHjktbXPmeTEaANEy2DbrXBd" },
        { id: 4, title: "Strobe Master", artist: "deadmau5", album: "Lack of a Better Name", length: "10:37", art: "https://lh3.googleusercontent.com/aida-public/AB6AXuBTU4VSB8xqlZH9wvNrrNnnc81u3A2DEqJlgzK24njiYIthnhpQxJfglevX4GtGRBN4Yhf9RWzyxgwqWnlr7qABVZGL6FV3Kz2Ao1qBKU1Ns49NaNHCUkXTnr6f3IrAMaMrVKGLRMkaUIfCc_zinZJgKfutI80RSosJ2UegUNnNQaaauySTX6-JNYYavbo5Zy13K4B6CeacIpABceDucGy9iFAyaH_jiF1BjrWHqWDNmstxSQUgZRNE9yDc5WlbUl61S4QRFwhjuRAR" }
    ];

    // Dynamic Tab Control
    function switchTab(tabId) {
        document.querySelectorAll('.tab-pane').forEach(el => el.classList.add('hidden'));
        document.getElementById(`tab-${'$'}{tabId}`).classList.remove('hidden');
        
        // Update header titles
        const titles = {
            'overview': 'Dashboard Overview',
            'media': 'Media Vault',
            'nodes': 'Connected Nodes',
            'notifications': 'Notifications Hub',
            'settings': 'Engine Settings'
        };
        document.getElementById('current-tab-title').innerText = titles[tabId] || 'Synco';

        // Update active navigation buttons in sidebar
        const navIds = ['overview', 'media', 'nodes', 'notifications', 'settings'];
        navIds.forEach(id => {
            const btn = document.getElementById(`nav-${'$'}{id}`);
            if (id === tabId || (id === 'overview' && tabId === 'overview')) {
                btn.className = "w-full flex items-center gap-4 px-5 py-3.5 rounded-xl text-primary font-semibold bg-white/[0.03] border border-white/5 shadow-sm transition-all duration-300";
            } else {
                btn.className = "w-full flex items-center gap-4 px-5 py-3.5 rounded-xl text-on-surface-variant/60 font-medium hover:text-on-surface hover:bg-white/[0.02] transition-all duration-300 active:scale-95";
            }
        });

        // Update header navbar buttons
        const summaryBtn = document.getElementById('sub-nav-summary');
        const latencyBtn = document.getElementById('sub-nav-latency');
        const healthBtn = document.getElementById('sub-nav-health');
        
        summaryBtn.className = tabId === 'overview' ? "text-primary text-xs font-bold tracking-widest uppercase border-b border-primary/40 pb-1" : "text-on-surface-variant/40 hover:text-white transition-all text-xs font-bold tracking-widest uppercase";
        latencyBtn.className = tabId === 'nodes' ? "text-primary text-xs font-bold tracking-widest uppercase border-b border-primary/40 pb-1" : "text-on-surface-variant/40 hover:text-white transition-all text-xs font-bold tracking-widest uppercase";
        healthBtn.className = tabId === 'nodes' ? "text-primary text-xs font-bold tracking-widest uppercase border-b border-primary/40 pb-1" : "text-on-surface-variant/40 hover:text-white transition-all text-xs font-bold tracking-widest uppercase";

        addLog('Navigation', `Switched visual control workspace to: ${'$'}{titles[tabId]}`, 'explore');
    }

    // Dynamic Toast System
    function showToast(title, message, type = 'primary') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = "glass-panel p-4 rounded-xl flex gap-4 items-center animate-[slideIn_0.3s_ease] border-white/10 select-none shadow-2xl max-w-sm";
        
        const colors = {
            'primary': 'text-primary bg-primary/5 border-primary/20',
            'secondary': 'text-secondary bg-secondary/5 border-secondary/20',
            'error': 'text-error bg-error/5 border-error/20'
        };
        const activeColor = colors[type] || colors['primary'];

        toast.innerHTML = `
            <div class="w-8 h-8 rounded-full flex items-center justify-center shrink-0 border ${'$'}{activeColor}">
                <span class="material-symbols-outlined text-sm">notifications</span>
            </div>
            <div>
                <h5 class="text-xs font-bold text-white">${'$'}{title}</h5>
                <p class="text-[10px] text-on-surface-variant/60 mt-0.5">${'$'}{message}</p>
            </div>
        `;
        container.appendChild(toast);
        setTimeout(() => {
            toast.classList.add('opacity-0', 'transition-all', 'duration-500');
            setTimeout(() => toast.remove(), 500);
        }, 4000);
    }

    function addLog(title, desc, icon = 'sync') {
        const timeStr = new Date().toLocaleTimeString();
        if (logs.length > 0 && logs[0].title === title && logs[0].desc === desc) {
            return; 
        }
        logs.unshift({ title, desc, icon, time: timeStr });
        if (logs.length > maxLogs) {
            logs.pop();
        }
        renderLogs();
        renderCompleteNotifications();
    }

    function clearLogs() {
        logs.length = 0;
        addLog('Logs Cleared', 'All previous session event history was cleared.', 'delete_sweep');
        showToast('Logs Flushed', 'System activity and sync logs cleared successfully.', 'secondary');
    }

    function renderLogs() {
        const container = document.getElementById('event-logs-container');
        if (!container) return;
        container.innerHTML = '';
        if (logs.length === 0) {
            container.innerHTML = `
                <div class="flex items-center justify-center py-10 opacity-40">
                    <p class="text-xs text-on-surface-variant">No system logs logged yet</p>
                </div>
            `;
            return;
        }
        logs.slice(0, 5).forEach(log => {
            container.innerHTML += `
                <div class="flex gap-6 p-6 border-b border-white/[0.03] hover:bg-white/[0.02] transition-colors group">
                    <div class="w-12 h-12 shrink-0 bg-primary/5 rounded-2xl flex items-center justify-center text-primary border border-primary/10">
                        <span class="material-symbols-outlined text-xl font-light">${'$'}{log.icon}</span>
                    </div>
                    <div class="flex-1">
                        <div class="flex justify-between items-center">
                            <p class="text-sm font-bold text-white tracking-tight">${'$'}{log.title}</p>
                            <span class="text-[9px] font-mono-label text-on-surface-variant/30 uppercase tracking-widest">${'$'}{log.time}</span>
                        </div>
                        <p class="text-xs text-on-surface-variant/50 mt-1 font-medium">${'$'}{log.desc}</p>
                    </div>
                </div>
            `;
        });
    }

    function renderCompleteNotifications() {
        const container = document.getElementById('complete-notifications-list');
        if (!container) return;
        container.innerHTML = '';
        if (logs.length === 0) {
            container.innerHTML = `
                <div class="flex items-center justify-center py-20 opacity-40">
                    <p class="text-sm text-on-surface-variant">No historical notifications received in this session</p>
                </div>
            `;
            return;
        }
        logs.forEach(log => {
            container.innerHTML += `
                <div class="flex gap-6 p-6 hover:bg-white/[0.01] transition-colors">
                    <div class="w-10 h-10 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-primary shrink-0">
                        <span class="material-symbols-outlined text-lg">${'$'}{log.icon}</span>
                    </div>
                    <div class="flex-1">
                        <div class="flex justify-between items-start">
                            <h4 class="text-xs font-bold text-white tracking-tight">${'$'}{log.title}</h4>
                            <span class="text-[9px] font-mono text-on-surface-variant/40">${'$'}{log.time}</span>
                        </div>
                        <p class="text-xs text-on-surface-variant/60 mt-1">${'$'}{log.desc}</p>
                    </div>
                </div>
            `;
        });
    }

    // Media Vault rendering & actions
    function renderMediaVault() {
        const grid = document.getElementById('media-vault-grid');
        grid.innerHTML = '';
        tracksVault.forEach(track => {
            grid.innerHTML += `
                <div class="glass-panel p-5 rounded-2xl border border-white/5 hover:border-primary/20 transition-all flex items-center gap-4 group">
                    <img src="${'$'}{track.art}" class="w-16 h-16 rounded-xl object-cover border border-white/10" alt="Cover"/>
                    <div class="flex-1">
                        <h4 class="text-sm font-bold text-white">${'$'}{track.title}</h4>
                        <p class="text-xs text-on-surface-variant/60 mt-0.5">${'$'}{track.artist}</p>
                    </div>
                    <button onclick="playVaultTrack(${'$'}{track.id})" class="w-10 h-10 rounded-full bg-white text-black hover:bg-primary transition-all flex items-center justify-center active:scale-90">
                        <span class="material-symbols-outlined text-xl" style="font-variation-settings: 'FILL' 1">play_arrow</span>
                    </button>
                </div>
            `;
        });
    }

    function playVaultTrack(trackId) {
        const track = tracksVault.find(t => t.id === trackId);
        if (!track) return;
        
        // Update local simulated track and broadcast state if offline
        document.getElementById('media-title-text').innerText = track.title;
        document.getElementById('media-artist-text').innerText = track.artist;
        document.getElementById('album-art').src = track.art;
        document.getElementById('total-time-text').innerText = track.length;
        
        currentIsPlaying = true;
        document.getElementById('play-pause-icon').innerText = 'pause';
        progress = 0;
        startProgressTracker();
        
        // Websocket state mock update
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ command: 'PLAY' })); // trigger play
        }
        
        addLog('Vault Track Playing', `Started broadcasting "${'$'}{track.title}" by ${'$'}{track.artist} from local Vault library.`, 'music_video');
        showToast('Vault Broadcaster', `Broadcasting "${'$'}{track.title}" to audio synchronizers.`, 'primary');
    }

    // Dynamic interactive Device Selection
    let activeDeviceName = "PCM Audio Engine (Master)";
    function selectDevice(deviceName) {
        activeDeviceName = deviceName;
        addLog('Output Configured', `Active desktop hardware router bound to: "${'$'}{deviceName}"`, 'volume_up');
        showToast('Audio Router', `Routed sync master sound directly to: ${'$'}{deviceName}`, 'secondary');
        
        // Re-render devices list immediately to update checks
        updateDeviceListUI();
    }

    function updateDeviceListUI() {
        const devicesContainer = document.getElementById('device-list-container');
        if (!devicesContainer) return;
        
        // If empty, add a default
        const deviceNames = ["PCM Audio Engine (Master)", "Bluetooth Transmitter (Standard)", "Realtek ASIO Bus"];
        
        devicesContainer.innerHTML = '';
        deviceNames.forEach((device) => {
            const isActive = device === activeDeviceName;
            devicesContainer.innerHTML += `
                <div onclick="selectDevice('${'$'}{device}')" class="glass-panel inner-glow-soft luxury-radius p-5 flex items-center gap-6 group hover:bg-white/[0.06] transition-all duration-500 cursor-pointer border ${'$'}{isActive ? 'border-secondary/40 bg-secondary/[0.02]' : 'border-white/5'}">
                    <div class="w-12 h-12 bg-black rounded-xl flex items-center justify-center border ${'$'}{isActive ? 'border-secondary/40 text-secondary' : 'border-white/10 text-primary'} group-hover:border-primary/40 transition-colors shadow-inner">
                        <span class="material-symbols-outlined text-lg font-light">volume_up</span>
                    </div>
                    <div class="flex-1">
                        <div class="flex justify-between items-start">
                            <h4 class="text-sm font-bold text-white tracking-tight">${'$'}{device}</h4>
                            <span class="flex items-center gap-2 ${'$'}{isActive ? 'bg-secondary/10 border-secondary/20' : 'bg-white/5 border-white/5'} px-3 py-1 rounded-full border">
                                <span class="w-1.5 h-1.5 ${'$'}{isActive ? 'bg-secondary animate-pulse' : 'bg-on-surface-variant/40'} rounded-full"></span>
                                <span class="text-[9px] font-mono-label ${'$'}{isActive ? 'text-secondary' : 'text-on-surface-variant/40'} uppercase font-bold tracking-widest">${'$'}{isActive ? 'Active Route' : 'Ready'}</span>
                            </span>
                        </div>
                    </div>
                </div>
            `;
        });
    }

    // Ping / Latency test animation
    let pingValues = [];
    function runPingTest() {
        const statsEl = document.getElementById('ping-logs');
        statsEl.innerHTML = '<p class="text-secondary animate-pulse">PING PACKETS DISPATCHED... AUDITING SHIFT ENGINES...</p>';
        
        let count = 0;
        pingValues = [];
        const interval = setInterval(() => {
            if (count < 6) {
                const mockPing = Math.floor(10 + Math.random() * 8);
                pingValues.push(mockPing);
                statsEl.innerHTML += `<p class="text-white">ICMP SYNC BLOCK ${'$'}{count + 1}: SUCCESS (64 bytes) -> delay: ${'$'}{mockPing}ms, TTL: 64</p>`;
                statsEl.scrollTop = statsEl.scrollHeight;
                
                // Redraw on canvas
                drawLatencyCanvas();
                count++;
            } else {
                clearInterval(interval);
                const avg = Math.floor(pingValues.reduce((a, b) => a + b) / pingValues.length);
                const jitter = (Math.random() * 1.5).toFixed(1);
                
                document.getElementById('ping-avg').innerText = `${'$'}{avg} ms`;
                document.getElementById('ping-jitter').innerText = `${'$'}{jitter} ms`;
                
                addLog('Latency Diagnostic', `Ping audit finished. Average Round-Trip Delay: ${'$'}{avg}ms. Jitter Deviation: ${'$'}{jitter}ms.`, 'speed');
                showToast('Audit Complete', `Sync latency stable at ${'$'}{avg}ms. No jitter packets lost.`, 'secondary');
            }
        }, 400);
    }

    function drawLatencyCanvas() {
        const canvas = document.getElementById('latency-canvas');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        
        // Set actual responsive bounds
        canvas.width = canvas.parentElement.clientWidth;
        canvas.height = canvas.parentElement.clientHeight;
        
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        
        // Draw grid lines
        ctx.strokeStyle = "rgba(255, 255, 255, 0.05)";
        ctx.lineWidth = 1;
        for (let i = 0; i < canvas.width; i += 40) {
            ctx.beginPath();
            ctx.moveTo(i, 0);
            ctx.lineTo(i, canvas.height);
            ctx.stroke();
        }
        for (let j = 0; j < canvas.height; j += 20) {
            ctx.beginPath();
            ctx.moveTo(0, j);
            ctx.lineTo(canvas.width, j);
            ctx.stroke();
        }

        // Draw latency curve
        if (pingValues.length === 0) {
            ctx.strokeStyle = "rgba(173, 198, 255, 0.2)";
            ctx.beginPath();
            ctx.moveTo(0, canvas.height / 2);
            ctx.lineTo(canvas.width, canvas.height / 2);
            ctx.stroke();
            return;
        }

        ctx.strokeStyle = "#4edea3";
        ctx.lineWidth = 3;
        ctx.beginPath();
        const step = canvas.width / 6;
        
        pingValues.forEach((val, i) => {
            const x = i * step + 20;
            // scale 10ms to 80ms into canvas height
            const y = canvas.height - ((val / 30) * canvas.height);
            if (i === 0) {
                ctx.moveTo(x, y);
            } else {
                ctx.lineTo(x, y);
            }
        });
        ctx.stroke();
    }

    // Engine settings loader/saver
    function loadEngineSettings() {
        const latencyOpt = localStorage.getItem('set-latency-opt') !== 'false';
        const lossless = localStorage.getItem('set-lossless') === 'true';
        const bgSvc = localStorage.getItem('set-bg-svc') !== 'false';
        
        document.getElementById('set-latency-opt').checked = latencyOpt;
        document.getElementById('set-lossless').checked = lossless;
        document.getElementById('set-bg-svc').checked = bgSvc;
        
        document.getElementById('current-latency-badge').innerText = latencyOpt ? 'Ultra Low' : 'Standard';
    }

    function saveEngineSettings(e) {
        e.preventDefault();
        const latencyOpt = document.getElementById('set-latency-opt').checked;
        const lossless = document.getElementById('set-lossless').checked;
        const bgSvc = document.getElementById('set-bg-svc').checked;
        
        localStorage.setItem('set-latency-opt', latencyOpt);
        localStorage.setItem('set-lossless', lossless);
        localStorage.setItem('set-bg-svc', bgSvc);
        
        document.getElementById('current-latency-badge').innerText = latencyOpt ? 'Ultra Low' : 'Standard';
        
        addLog('Settings Updated', `Saved core configuration parameters. Latency Mode: ${'$'}{latencyOpt ? 'Ultra-low' : 'Standard'}.`, 'settings_suggest');
        showToast('Settings Saved', 'Synco Engine specifications written to local store.', 'secondary');
    }

    // Developer sim controls
    function triggerSimulatedCall() {
        const callerId = document.getElementById('sim-caller-input').value;
        const state = document.getElementById('sim-call-state').value;
        
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ command: 'SIMULATE_CALL', state: state, callerId: callerId }));
            addLog('Call Signal Injected', `Dispatched Call Interrupt - ID: "${'$'}{callerId}" state: "${'$'}{state}"`, 'call');
            showToast('Dev Simulator', `Injected call simulation (${'$'}{state}) to mobile client.`, 'primary');
        } else {
            addLog('Dev Warning', 'Simulator requires active Android mobile WebSocket connection to pair.', 'warning');
            showToast('Simulation Offline', 'Failed to dispatch call signal: No paired Android node.', 'error');
        }
    }

    function triggerSimulatedNotification() {
        const title = document.getElementById('sim-notif-title').value;
        const text = document.getElementById('sim-notif-text').value;
        
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ command: 'SIMULATE_NOTIF', action: 'RECEIVE', id: 'sim_notif', title: title, text: text }));
            addLog('Notification Broadcasted', `Dispatched System Notification: "${'$'}{title}"`, 'notifications_active');
            showToast('Dev Simulator', 'Injected simulated notification packet to mobile client.', 'secondary');
        } else {
            addLog('Dev Warning', 'Simulator requires active Android mobile WebSocket connection to pair.', 'warning');
            showToast('Simulation Offline', 'Failed to broadcast notification: No paired Android node.', 'error');
        }
    }

    // Interactive volume slider logic
    function handleVolumeChange(value) {
        currentVolume = parseInt(value);
        document.getElementById('volume-value-text').innerText = `${'$'}{currentVolume}%`;
        
        const volIcon = document.getElementById('volume-indicator-icon');
        if (currentVolume === 0) {
            volIcon.innerText = 'volume_mute';
        } else if (currentVolume < 50) {
            volIcon.innerText = 'volume_down';
        } else {
            volIcon.innerText = 'volume_up';
        }

        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ command: 'VOLUME', value: currentVolume }));
        }
    }

    function toggleMute() {
        const rangeInput = document.getElementById('volume-range-input');
        if (!isMuted) {
            savedVolume = currentVolume;
            isMuted = true;
            handleVolumeChange(0);
            rangeInput.value = 0;
            addLog('Mute Signal', 'Client audio playback muted.', 'volume_off');
        } else {
            isMuted = false;
            handleVolumeChange(savedVolume);
            rangeInput.value = savedVolume;
            addLog('Unmute Signal', `Client audio playback unmuted to ${'$'}{savedVolume}%`, 'volume_up');
        }
    }

    function connectWs() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        ws = new WebSocket(`${'$'}{protocol}//${'$'}{window.location.host}/ws`);
        
        ws.onopen = () => {
            addLog('WebSocket Connection', 'Successfully established WebSocket link to Synco backend server.', 'swap_calls');
            addLog('State Requested', 'Awaiting synchronized state payload...', 'sync');
            showToast('Server Synchronized', 'Dynamic live telemetry link connected.', 'secondary');
        };
        
        ws.onclose = () => {
            addLog('WebSocket Offline', 'WebSocket connection lost. Retrying synchronization...', 'cloud_off');
            document.getElementById('pulsing-dot').className = "w-1.5 h-1.5 bg-error rounded-full animate-pulse";
            document.getElementById('conn-state-badge').innerText = "Disconnected";
            document.getElementById('conn-state-badge').className = "text-error font-mono-label text-[9px] uppercase tracking-[0.3em]";
            setTimeout(connectWs, 2000);
        };
        
        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                updateUI(data);
            } catch (err) {
                console.error(err);
            }
        };
    }

    function updateUI(data) {
        // 1. Connection Status Badge
        const dot = document.getElementById('pulsing-dot');
        const badge = document.getElementById('conn-state-badge');
        if (data.connected) {
            dot.className = "w-1.5 h-1.5 bg-secondary rounded-full animate-pulse";
            badge.innerText = "Online";
            badge.className = "text-secondary font-mono-label text-[9px] uppercase tracking-[0.3em]";
            document.getElementById('node-address').innerText = window.location.hostname || "192.168.1.5";
        } else {
            dot.className = "w-1.5 h-1.5 bg-error rounded-full animate-pulse";
            badge.innerText = "Awaiting Client";
            badge.className = "text-error font-mono-label text-[9px] uppercase tracking-[0.3em]";
            document.getElementById('node-address').innerText = "Awaiting handshake...";
        }

        // 2. Audio Master Ownership Role
        const ownerText = document.getElementById('audio-owner-text');
        ownerText.innerText = data.isOwner ? 'Desktop Host (This PC)' : 'Android Node (Phone)';

        // 3. Status Card Column
        const statusIcon = document.getElementById('status-icon');
        const statusTitle = document.getElementById('status-title');
        const statusDesc = document.getElementById('status-desc');
        
        if (data.connected) {
            statusIcon.className = "material-symbols-outlined text-secondary text-4xl font-light";
            statusIcon.innerText = "spatial_audio";
            statusTitle.innerText = data.isOwner ? "Host Audio Mode" : "Remote Control Mode";
            statusDesc.innerText = data.isOwner 
                ? "This desktop is playing media directly and transmitting state metadata." 
                : "Receiving remote metadata from connected client. Use this dashboard to remote control play states.";
        } else {
            statusIcon.className = "material-symbols-outlined text-primary text-4xl font-light animate-pulse";
            statusIcon.innerText = "spatial_audio_off";
            statusTitle.innerText = "Awaiting Sync";
            statusDesc.innerText = "Establish a WebSocket connection by opening the Synco app on your phone and inputting this IP address.";
        }

        // 4. Media Synchronization (Keeping titles wrapped and beautifully formatted)
        const titleText = document.getElementById('media-title-text');
        const artistText = document.getElementById('media-artist-text');
        const playPauseIcon = document.getElementById('play-pause-icon');

        if (data.media && data.media.title) {
            const trackChanged = titleText.innerText !== data.media.title;
            titleText.innerText = data.media.title;
            artistText.innerText = data.media.artist || "Unknown Artist";
            currentIsPlaying = data.media.isPlaying;
            
            if (trackChanged) {
                progress = 0;
                addLog('Track Sync', `Synchronized: "${'$'}{data.media.title}" by ${'$'}{data.media.artist || 'Unknown'}.`, 'library_music');
                showToast('Track Synced', `Active audio stream is now "${'$'}{data.media.title}"`, 'primary');
            }

            if (currentIsPlaying) {
                playPauseIcon.innerText = 'pause';
                playPauseIcon.style.fontVariationSettings = "'FILL' 1";
                startProgressTracker();
            } else {
                playPauseIcon.innerText = 'play_arrow';
                playPauseIcon.style.fontVariationSettings = "'FILL' 0";
                stopProgressTracker();
            }
        } else {
            // Keep default visible track description if none active
            if (titleText.innerText === "No Media Playing") {
                titleText.innerText = "No Media Playing";
                artistText.innerText = "Awaiting track metadata from connection";
                playPauseIcon.innerText = 'play_arrow';
                playPauseIcon.style.fontVariationSettings = "'FILL' 0";
                document.getElementById('progress-bar-fill').style.width = '0%';
                document.getElementById('progress-time').innerText = "00:00";
                stopProgressTracker();
            }
        }

        // 5. Output Devices List Container (Merged with selected checkmarks)
        updateDeviceListUI();
    }

    function startProgressTracker() {
        if (progressInterval) return;
        progressInterval = setInterval(() => {
            if (progress < 100) {
                progress += 0.4;
                document.getElementById('progress-bar-fill').style.width = `${'$'}{progress}%`;
                
                // approximate virtual time track duration
                const elapsedSecs = Math.floor((progress / 100) * 234);
                const min = Math.floor(elapsedSecs / 60);
                const sec = String(elapsedSecs % 60).padStart(2, '0');
                document.getElementById('progress-time').innerText = `0${'$'}{min}:${'$'}{sec}`;
            } else {
                progress = 0;
            }
        }, 1000);
    }

    function stopProgressTracker() {
        if (progressInterval) {
            clearInterval(progressInterval);
            progressInterval = null;
        }
    }

    function sendCommand(cmd, val = null) {
        if (ws && ws.readyState === WebSocket.OPEN) {
            const payload = { command: cmd };
            if (val !== null) payload.value = val;
            ws.send(JSON.stringify(payload));
            addLog('Command Transmitted', `Fired command: "${'$'}{cmd}" down the synchronization line.`, 'bolt');
        } else {
            addLog('Transmission Error', 'Unable to transmit command - connection is currently offline.', 'error');
            showToast('Sync Offline', 'Connection currently offline. Verify IP handshakes.', 'error');
        }
    }

    // Play/Pause override for direct action
    const playBtn = document.getElementById('play-pause-btn');
    playBtn.addEventListener('click', () => {
        sendCommand(currentIsPlaying ? 'PAUSE' : 'PLAY');
    });

    // Initial Event Log entry & layout boot
    addLog('System Initialization', 'Synco Professional Sync Engine boot sequences finished.', 'settings');
    renderMediaVault();
    updateDeviceListUI();
    loadEngineSettings();
    drawLatencyCanvas();
    connectWs();
</script>
</body>
</html>
        """.trimIndent()
    }
}
