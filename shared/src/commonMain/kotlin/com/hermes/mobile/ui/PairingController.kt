package com.hermes.mobile.ui

import com.hermes.mobile.models.PairingCompleteResponse
import com.hermes.mobile.models.PairingStartResponse

class PairingController(
    private val gateway: PairingGateway,
) {
    suspend fun start(): PairingState = try {
        val response = gateway.startPairing()
        PairingState(
            status = PairingStatus.CodeReady,
            pairingId = response.pairingId,
            code = response.code,
            qrPayload = response.qrPayload,
            message = "Pairing code ${response.code}",
        )
    } catch (_: Throwable) {
        PairingState(status = PairingStatus.Failed, message = "Pairing unavailable")
    }

    suspend fun complete(code: String, deviceName: String, platform: String): PairingState {
        val cleanedCode = code.trim()
        val cleanedName = deviceName.trim()
        if (cleanedCode.isBlank()) {
            return PairingState(status = PairingStatus.Invalid, message = "Enter pairing code")
        }
        if (cleanedName.isBlank()) {
            return PairingState(status = PairingStatus.Invalid, message = "Enter device name")
        }
        return try {
            val response = gateway.completePairing(
                code = cleanedCode,
                deviceName = cleanedName,
                platform = platform,
                publicKey = null,
            )
            PairingState(
                status = PairingStatus.Paired,
                deviceId = response.deviceId,
                deviceToken = response.deviceToken,
                message = "Paired ${response.deviceId}",
            )
        } catch (_: Throwable) {
            PairingState(status = PairingStatus.Failed, message = "Pairing failed")
        }
    }
}

data class PairingState(
    val status: PairingStatus = PairingStatus.Idle,
    val pairingId: String? = null,
    val code: String? = null,
    val qrPayload: String? = null,
    val deviceId: String? = null,
    val deviceToken: String? = null,
    val message: String? = null,
)

enum class PairingStatus {
    Idle,
    CodeReady,
    Paired,
    Invalid,
    Failed,
}

interface PairingGateway {
    suspend fun startPairing(): PairingStartResponse

    suspend fun completePairing(
        code: String,
        deviceName: String,
        platform: String,
        publicKey: String? = null,
    ): PairingCompleteResponse
}
