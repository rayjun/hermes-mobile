package com.hermes.mobile.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PairingStartResponse(
    @SerialName("pairing_id") val pairingId: String,
    val code: String,
    @SerialName("expires_at") val expiresAt: Instant,
    @SerialName("qr_payload") val qrPayload: String,
)

@Serializable
data class PairingCompleteRequest(
    val code: String,
    @SerialName("device_name") val deviceName: String,
    val platform: String,
    @SerialName("public_key") val publicKey: String? = null,
)

@Serializable
data class PairingCompleteResponse(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_token") val deviceToken: String,
    val capabilities: Map<String, Boolean>,
)

@Serializable
data class DeviceInfo(
    val id: String,
    val name: String,
    val platform: String,
    @SerialName("created_at") val createdAt: Instant,
)

@Serializable
data class DevicesResponse(
    val devices: List<DeviceInfo>,
)
