package com.remoteaudiosync.network

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object WaitingForAck : ConnectionState()
    object Reconnecting : ConnectionState()
    object Lost : ConnectionState()
    data class Failed(val error: String) : ConnectionState()
}
