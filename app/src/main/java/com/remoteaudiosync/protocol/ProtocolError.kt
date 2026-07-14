package com.remoteaudiosync.protocol

sealed interface ProtocolError {
    val message: String

    data class InvalidVersion(override val message: String) : ProtocolError
    data class MissingPayload(override val message: String) : ProtocolError
    data class SerializationFailure(override val message: String, val cause: Throwable? = null) : ProtocolError
    data class InvalidPacketId(override val message: String) : ProtocolError
    data class InvalidTimestamp(override val message: String) : ProtocolError
    data class InvalidSender(override val message: String) : ProtocolError
    data class InvalidReceiver(override val message: String) : ProtocolError
    data class PayloadTypeMismatch(override val message: String) : ProtocolError
    data class MalformedPacket(override val message: String) : ProtocolError
}

sealed interface ProtocolResult<out T> {
    data class Success<T>(val data: T) : ProtocolResult<T>
    data class Failure(val error: ProtocolError) : ProtocolResult<Nothing>
    
    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
}
