package com.remoteaudiosync.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import java.security.SecureRandom
import javax.net.ssl.HostnameVerifier
import java.util.concurrent.TimeUnit

class WebSocketClient {
    private val client: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()
        }
    }
        
    private var webSocket: WebSocket? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _logs = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val logs = _logs.asSharedFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val messages = _messages.asSharedFlow()

    fun connect(ip: String, port: Int) {
        if (_connectionState.value is ConnectionState.Connecting || _connectionState.value is ConnectionState.Connected) {
            return
        }

        _connectionState.value = ConnectionState.Connecting
        val cleanIp = ip.trim()
        val url = if (cleanIp.startsWith("ws://") || cleanIp.startsWith("wss://")) {
            cleanIp
        } else if (cleanIp.startsWith("https://")) {
            cleanIp.replace("https://", "wss://")
        } else if (cleanIp.startsWith("http://")) {
            cleanIp.replace("http://", "ws://")
        } else if (cleanIp.contains("ngrok")) {
            "wss://$cleanIp"
        } else if (cleanIp.contains(".") && cleanIp.any { it.isLetter() }) {
            if (port == 443) "wss://$cleanIp" else "ws://$cleanIp:$port"
        } else {
            "ws://$cleanIp:$port"
        }

        addLog("Connecting to $url...")

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.Connected
                addLog("onOpen: Connected successfully")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.length > 65536) {
                    addLog("onMessage: Rejected oversized message of length ${text.length}")
                    return
                }
                _messages.tryEmit(text)
                addLog("onMessage: $text")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                addLog("onClosing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnected
                addLog("onClosed: $code $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (this@WebSocketClient.webSocket != webSocket) return
                val errorMsg = t.message ?: "Unknown error"
                _connectionState.value = ConnectionState.Failed(errorMsg)
                addLog("onFailure: $errorMsg")
            }
        })
    }

    fun disconnect() {
        addLog("Disconnecting...")
        webSocket?.cancel()
        webSocket?.close(1000, "User initiated disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }
    
    fun clearError() {
        if (_connectionState.value is ConnectionState.Failed) {
            _connectionState.value = ConnectionState.Disconnected
            addLog("Error cleared")
        }
    }

    fun clearLogs() {
        // SharedFlow doesn't have a clear mechanism, but we can emit a clear signal if needed.
        // For our UI, we'll maintain the log list in the ViewModel.
    }

    var customSender: ((String) -> Unit)? = null

    fun setServerConnected(connected: Boolean) {
        _connectionState.value = if (connected) ConnectionState.Connected else ConnectionState.Disconnected
    }

    fun feedIncomingMessage(message: String) {
        _messages.tryEmit(message)
        addLog("onMessage: $message")
    }

    fun sendMessage(message: String) {
        val sender = customSender
        if (sender != null) {
            sender(message)
            addLog("Sent: $message")
            return
        }
        if (_connectionState.value is ConnectionState.Connected) {
            webSocket?.send(message)
            addLog("Sent: $message")
        } else {
            addLog("Cannot send message: Not connected")
        }
    }

    private fun addLog(message: String) {
        _logs.tryEmit(message)
    }
}
