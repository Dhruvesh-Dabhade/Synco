package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PermissionState")
data class PermissionStatePayload(val grantedPermissions: List<String>, val missingPermissions: List<String>) : Payload()
