package com.hermes.mobile.ui

import com.hermes.mobile.models.PairingCompleteResponse
import com.hermes.mobile.models.PairingStartResponse
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class PairingControllerTest {
    @Test
    fun startsPairingAndReturnsCodeState() = runTest {
        val controller = PairingController(RecordingPairingGateway())

        val state = controller.start()

        assertEquals("834912", state.code)
        assertEquals("pair_test", state.pairingId)
        assertEquals(PairingStatus.CodeReady, state.status)
        assertEquals("Pairing code 834912", state.message)
    }

    @Test
    fun completesPairingAndStoresDeviceIdentity() = runTest {
        val gateway = RecordingPairingGateway()
        val controller = PairingController(gateway)

        val state = controller.complete("834912", " Ray's Android ", "android")

        assertEquals("834912", gateway.completedCode)
        assertEquals("Ray's Android", gateway.completedDeviceName)
        assertEquals("dev_test", state.deviceId)
        assertEquals("hmob_test_token", state.deviceToken)
        assertEquals(PairingStatus.Paired, state.status)
    }

    @Test
    fun rejectsBlankPairingCodeBeforeCallingGateway() = runTest {
        val gateway = RecordingPairingGateway()
        val controller = PairingController(gateway)

        val state = controller.complete(" ", "Ray's Android", "android")

        assertEquals(null, gateway.completedCode)
        assertEquals(PairingStatus.Invalid, state.status)
        assertEquals("Enter pairing code", state.message)
    }
}

private class RecordingPairingGateway : PairingGateway {
    var completedCode: String? = null
    var completedDeviceName: String? = null

    override suspend fun startPairing(): PairingStartResponse = PairingStartResponse(
        pairingId = "pair_test",
        code = "834912",
        expiresAt = Instant.parse("2026-06-24T10:15:00Z"),
        qrPayload = "hermes://pair?url=http://test&code=834912&fingerprint=mock",
    )

    override suspend fun completePairing(
        code: String,
        deviceName: String,
        platform: String,
        publicKey: String?,
    ): PairingCompleteResponse {
        completedCode = code
        completedDeviceName = deviceName
        return PairingCompleteResponse(
            deviceId = "dev_test",
            deviceToken = "hmob_test_token",
            capabilities = mapOf("approvals" to true),
        )
    }
}
