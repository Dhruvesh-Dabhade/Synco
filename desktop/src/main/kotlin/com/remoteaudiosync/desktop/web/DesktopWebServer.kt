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
import java.security.SecureRandom
import java.util.Base64

class DesktopWebServer(
    private val port: Int,
    private val appServer: DesktopAppServer
) {
    private var app: Javalin? = null
    private val connectedClients = ConcurrentHashMap.newKeySet<WsContext>()
    private val scope = CoroutineScope(Dispatchers.Default)

    private val requestCounts = ConcurrentHashMap<String, MutableList<Long>>()
    private val wsMessageCounts = ConcurrentHashMap<String, MutableList<Long>>()

    private val authToken: String = generateAuthToken()
    private val validTokens = ConcurrentHashMap.newKeySet<String>().also { it.add(authToken) }

    fun getAuthToken(): String = authToken

    private fun generateAuthToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

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

    private fun isAuthorized(ctx: io.javalin.http.Context): Boolean {
        val token = ctx.queryParam("token") ?: ctx.header("Authorization")?.removePrefix("Bearer ")
        return token != null && validTokens.contains(token)
    }

    private fun isWsAuthorized(ctx: WsContext): Boolean {
        val token = ctx.queryParam("token")
        return token != null && validTokens.contains(token)
    }

    fun start() {
        app = Javalin.create { config ->
            config.showJavalinBanner = false
        }

        app?.before { ctx ->
            val ip = ctx.ip()
            if (isRateLimited(ip, 60, 60000L, requestCounts)) {
                ctx.status(429)
                ctx.header("Retry-After", "10")
                ctx.result("Too Many Requests")
                return@before
            }

            ctx.header("X-Powered-By", "")
            ctx.header("X-Frame-Options", "DENY")
            ctx.header("X-Content-Type-Options", "nosniff")
            ctx.header("Referrer-Policy", "strict-origin-when-cross-origin")

            val csp = buildString {
                append("default-src 'self'; ")
                append("script-src 'self' 'unsafe-inline'; ")
                append("style-src 'self' 'unsafe-inline'; ")
                append("font-src 'self'; ")
                append("connect-src 'self'; ")
                append("img-src 'self' data:; ")
                append("frame-ancestors 'none';")
            }
            ctx.header("Content-Security-Policy", csp)
        }

        app?.exception(Exception::class.java) { e, ctx ->
            System.err.println("[SERVER_ERROR] Error handling request: ${e.message}")
            ctx.status(500)
            ctx.result("Internal Server Error")
        }

        app?.start(port)

        app?.get("/") { ctx ->
            if (!isAuthorized(ctx)) {
                ctx.status(401)
                ctx.result("Unauthorized. Use ?token= parameter or Authorization: Bearer header.")
                return@get
            }
            ctx.html(getHtmlContent())
        }

        app?.ws("/ws") { ws ->
            ws.onConnect { ctx ->
                if (!isWsAuthorized(ctx)) {
                    ctx.session.close(1008, "Unauthorized")
                    return@onConnect
                }
                connectedClients.add(ctx)
                sendStateToClient(ctx)
            }
            ws.onClose { ctx ->
                connectedClients.remove(ctx)
            }
            ws.onMessage { ctx ->
                val ip = (ctx.session.remoteAddress as? java.net.InetSocketAddress)?.address?.hostAddress ?: "unknown"
                if (isRateLimited(ip, 120, 60000L, wsMessageCounts)) {
                    ctx.send("{\"error\": \"Rate limit exceeded\"}")
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
                try {
                    broadcastState()
                } catch (e: Exception) {
                    System.err.println("[WEB] broadcastState error: ${e.message}")
                }
                delay(1000)
            }
        }

        println("[WEB] Web Dashboard running on http://localhost:$port")
        println("[WEB] Auth token: $authToken")
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
        try {
            ctx.send(getStateJson())
        } catch (e: Exception) {
            System.err.println("[WEB] sendStateToClient error: ${e.message}")
        }
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
        .custom-scrollbar::-webkit-scrollbar { width: 4px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: #2a2a2a; border-radius: 10px; }
        @keyframes subtle-float {
            0%, 100% { transform: translateY(0); }
            50% { transform: translateY(-4px); }
        }
        .float-subtle { animation: subtle-float 6s infinite ease-in-out; }
        @keyframes slideIn {
            from { opacity: 0; transform: translateX(20px); }
            to { opacity: 1; transform: translateX(0); }
        }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        input[type=range]::-webkit-slider-thumb {
            -webkit-appearance: none; appearance: none;
            width: 14px; height: 14px; border-radius: 50%;
            background: white; cursor: pointer;
        }
    </style>
</head>
<body style="padding:20px;font-family:'Inter',sans-serif;">

<div id="app" style="max-width:1200px;margin:0 auto;">
    <h1 style="font-size:32px;font-weight:800;color:white;margin-bottom:8px;">Synco<span style="color:#adc6ff;">.</span></h1>
    <p style="color:#8b90a0;font-size:12px;margin-bottom:32px;text-transform:uppercase;letter-spacing:0.25em;">Professional Sync Engine</p>

    <div id="status-indicator" style="display:flex;align-items:center;gap:12px;margin-bottom:24px;padding:16px 24px;background:rgba(20,20,20,0.45);border-radius:16px;border:1px solid rgba(255,255,255,0.05);">
        <span id="status-dot" style="width:12px;height:12px;border-radius:50%;background:#ffb4ab;"></span>
        <span id="status-text" style="color:#8b90a0;font-size:13px;">Disconnected</span>
    </div>

    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:24px;">
        <div class="glass-panel" style="padding:24px;border-radius:32px;">
            <h3 style="color:#8b90a0;font-size:10px;text-transform:uppercase;letter-spacing:0.2em;margin-bottom:12px;">Now Playing</h3>
            <p id="media-title" style="color:white;font-size:20px;font-weight:700;">No Media</p>
            <p id="media-artist" style="color:#8b90a0;font-size:13px;margin-top:4px;">Awaiting stream</p>
            <div style="display:flex;gap:12px;margin-top:16px;">
                <button onclick="sendCmd('PREVIOUS')" style="padding:8px 16px;border-radius:12px;border:1px solid rgba(255,255,255,0.1);background:transparent;color:white;cursor:pointer;">⏮</button>
                <button id="play-btn" onclick="togglePlay()" style="padding:8px 24px;border-radius:12px;background:white;color:black;border:none;cursor:pointer;font-weight:600;">▶</button>
                <button onclick="sendCmd('NEXT')" style="padding:8px 16px;border-radius:12px;border:1px solid rgba(255,255,255,0.1);background:transparent;color:white;cursor:pointer;">⏭</button>
            </div>
        </div>
        <div class="glass-panel" style="padding:24px;border-radius:32px;">
            <h3 style="color:#8b90a0;font-size:10px;text-transform:uppercase;letter-spacing:0.2em;margin-bottom:12px;">Volume</h3>
            <input type="range" min="0" max="100" value="70" onchange="setVolume(this.value)" style="width:100%;margin-top:8px;">
            <p style="color:white;font-size:13px;margin-top:8px;" id="vol-display">70%</p>
        </div>
    </div>

    <div class="glass-panel" style="padding:24px;border-radius:32px;margin-bottom:24px;">
        <h3 style="color:#8b90a0;font-size:10px;text-transform:uppercase;letter-spacing:0.2em;margin-bottom:12px;">Simulation Controls</h3>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
            <button onclick="sendCmd('SWITCH_ROLE')" style="padding:12px;border-radius:12px;border:1px solid rgba(255,255,255,0.1);background:transparent;color:white;cursor:pointer;font-size:12px;">Switch Audio Role</button>
            <button onclick="simulateCall()" style="padding:12px;border-radius:12px;border:1px solid rgba(255,255,255,0.1);background:transparent;color:white;cursor:pointer;font-size:12px;">Simulate Incoming Call</button>
            <button onclick="simulateNotification()" style="padding:12px;border-radius:12px;border:1px solid rgba(255,255,255,0.1);background:transparent;color:white;cursor:pointer;font-size:12px;">Simulate Notification</button>
            <button onclick="sendCmd('PAUSE')" style="padding:12px;border-radius:12px;border:1px solid rgba(255,255,255,0.1);background:transparent;color:white;cursor:pointer;font-size:12px;">Pause</button>
        </div>
    </div>

    <div class="glass-panel" style="padding:24px;border-radius:32px;">
        <h3 style="color:#8b90a0;font-size:10px;text-transform:uppercase;letter-spacing:0.2em;margin-bottom:12px;">Event Log</h3>
        <div id="event-log" style="max-height:200px;overflow-y:auto;font-size:11px;font-family:monospace;color:#8b90a0;"></div>
    </div>
</div>

<script>
    let ws;
    let isPlaying = false;
    let volume = 70;
    const logEl = document.getElementById('event-log');

    function addLog(msg) {
        const el = document.createElement('div');
        el.textContent = '> ' + msg;
        logEl.appendChild(el);
        logEl.scrollTop = logEl.scrollHeight;
    }

    function getWsUrl() {
        const token = new URLSearchParams(window.location.search).get('token');
        return 'ws://' + window.location.host + '/ws?token=' + token;
    }

    function connectWs() {
        const url = getWsUrl();
        ws = new WebSocket(url);
        ws.onopen = () => {
            addLog('WebSocket connected');
            document.getElementById('status-dot').style.background = '#4edea3';
            document.getElementById('status-text').textContent = 'Connected';
            document.getElementById('status-text').style.color = '#4edea3';
        };
        ws.onmessage = (e) => {
            try {
                const data = JSON.parse(e.data);
                if (data.media) {
                    document.getElementById('media-title').textContent = data.media.title || 'Unknown';
                    document.getElementById('media-artist').textContent = data.media.artist || 'Unknown';
                    isPlaying = data.media.isPlaying || false;
                    document.getElementById('play-btn').textContent = isPlaying ? '⏸' : '▶';
                }
                if (data.connected !== undefined) {
                    document.getElementById('status-dot').style.background = data.connected ? '#4edea3' : '#ffb4ab';
                    document.getElementById('status-text').textContent = data.connected ? 'Connected' : 'Disconnected';
                    document.getElementById('status-text').style.color = data.connected ? '#4edea3' : '#ffb4ab';
                }
            } catch(e) {}
        };
        ws.onclose = () => {
            addLog('WebSocket disconnected, reconnecting in 3s...');
            document.getElementById('status-dot').style.background = '#ffb4ab';
            document.getElementById('status-text').textContent = 'Disconnected';
            document.getElementById('status-text').style.color = '#ffb4ab';
            setTimeout(connectWs, 3000);
        };
        ws.onerror = () => {
            addLog('WebSocket error');
        };
    }

    function sendCmd(cmd) {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({command: cmd}));
            addLog('Sent: ' + cmd);
        }
    }

    function sendJson(data) {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify(data));
            addLog('Sent: ' + JSON.stringify(data));
        }
    }

    function togglePlay() {
        sendCmd(isPlaying ? 'PAUSE' : 'PLAY');
    }

    function setVolume(val) {
        volume = val;
        document.getElementById('vol-display').textContent = val + '%';
        sendJson({command: 'VOLUME', value: parseInt(val)});
    }

    function simulateCall() {
        sendJson({command: 'SIMULATE_CALL', state: 'RINGING', callerId: 'Simulated Call'});
    }

    function simulateNotification() {
        sendJson({command: 'SIMULATE_NOTIF', action: 'RECEIVE', id: 'sim_1', title: 'Test Notification', text: 'This is a simulated notification.'});
    }

    connectWs();
</script>
</body>
</html>
"""
    }
}
