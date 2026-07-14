package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("DeviceInfo")
data class DeviceInfoPayload(val model: String, val osVersion: String, val appVersion: String) : Payload()
