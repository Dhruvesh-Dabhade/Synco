import os

payloads = [
    ("PairRequestPayload", "val deviceName: String, val publicKey: String"),
    ("PairResponsePayload", "val status: String, val publicKey: String"),
    ("AuthRequestPayload", "val token: String"),
    ("AuthSuccessPayload", "val sessionId: String"),
    ("AckPayload", "val originalPacketId: String"),
    ("ErrorPayload", "val code: String, val message: String"),
    ("DeviceInfoPayload", "val model: String, val osVersion: String, val appVersion: String"),
    ("RoleStatePayload", "val isAudioOwner: Boolean"),
    ("RoleChangeRequestPayload", "val requestAudioOwner: Boolean"),
    ("MediaCommandPayload", "val command: String"),
    ("MediaStatePayload", "val title: String, val artist: String, val isPlaying: Boolean, val position: Long, val duration: Long"),
    ("CallStatePayload", "val state: String, val callerId: String? = null"),
    ("CallCommandPayload", "val command: String"),
    ("BatteryStatePayload", "val percentage: Int, val isCharging: Boolean"),
    ("PermissionStatePayload", "val grantedPermissions: List<String>, val missingPermissions: List<String>"),
    ("NotificationStatePayload", "val id: String, val title: String, val text: String"),
    ("ArtworkRequestPayload", "val mediaId: String"),
    ("ArtworkResponsePayload", "val mediaId: String, val artworkBase64: String"),
    ("HeartbeatPayload", "val uptimeMillis: Long"),
    ("WakeRequestPayload", "val reason: String"),
    ("LimitedModeStatePayload", "val isLimited: Boolean, val reason: String")
]

base_dir = "app/src/main/java/com/remoteaudiosync/protocol"

# Create Payload.kt
with open(os.path.join(base_dir, "Payload.kt"), "w") as f:
    f.write("package com.remoteaudiosync.protocol\n\n")
    f.write("import kotlinx.serialization.Serializable\n\n")
    f.write("@Serializable\n")
    f.write("sealed class Payload\n")

for name, params in payloads:
    with open(os.path.join(base_dir, f"{name}.kt"), "w") as f:
        f.write("package com.remoteaudiosync.protocol\n\n")
        f.write("import kotlinx.serialization.SerialName\n")
        f.write("import kotlinx.serialization.Serializable\n\n")
        f.write("@Serializable\n")
        serial_name = name.replace("Payload", "")
        f.write(f'@SerialName("{serial_name}")\n')
        f.write(f"data class {name}({params}) : Payload()\n")

print("Generated files")
