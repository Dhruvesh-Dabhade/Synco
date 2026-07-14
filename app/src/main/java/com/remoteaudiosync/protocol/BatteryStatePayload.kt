package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("BatteryState")
data class BatteryStatePayload(val percentage: Int, val isCharging: Boolean) : Payload()
