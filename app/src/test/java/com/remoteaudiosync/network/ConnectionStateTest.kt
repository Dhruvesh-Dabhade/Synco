package com.remoteaudiosync.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionStateTest {
    @Test
    fun `test state progression`() {
        var state: ConnectionState = ConnectionState.Disconnected
        assertEquals(ConnectionState.Disconnected, state)
        
        state = ConnectionState.Connecting
        assertEquals(ConnectionState.Connecting, state)
        
        state = ConnectionState.Connected
        assertEquals(ConnectionState.Connected, state)
        
        state = ConnectionState.Failed("Timeout")
        assertEquals(true, state is ConnectionState.Failed)
        assertEquals("Timeout", (state as ConnectionState.Failed).error)
    }
}
