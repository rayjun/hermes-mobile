package com.hermes.mobile.ui

import com.hermes.mobile.models.ModelInfo
import com.hermes.mobile.models.NodeStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GatewaySettingsControllerTest {
    @Test
    fun normalizesGatewayUrlByTrimmingAndRemovingTrailingSlash() {
        val controller = GatewaySettingsController()

        val config = controller.save(" http://100.64.1.2:8765/ ")

        assertEquals("http://100.64.1.2:8765", config.baseUrl)
    }

    @Test
    fun rejectsBlankGatewayUrl() {
        val controller = GatewaySettingsController()

        assertFailsWith<GatewaySettingsError.InvalidUrl> {
            controller.save("   ")
        }
    }

    @Test
    fun rejectsUnsupportedSchemes() {
        val controller = GatewaySettingsController()

        assertFailsWith<GatewaySettingsError.InvalidUrl> {
            controller.save("ftp://example.com:8765")
        }
    }

    @Test
    fun createsInitialStateFromSavedUrl() {
        val controller = GatewaySettingsController()

        val state = controller.initialState(savedBaseUrl = "http://192.168.1.10:8765")

        assertEquals("http://192.168.1.10:8765", state.baseUrl)
        assertEquals(true, state.configured)
    }

    @Test
    fun createsInitialStateFromDefaultWhenSavedUrlIsMissing() {
        val controller = GatewaySettingsController()

        val state = controller.initialState(savedBaseUrl = null)

        assertEquals(GatewaySettingsController.DefaultGatewayBaseUrl, state.baseUrl)
        assertEquals(false, state.configured)
    }

    @Test
    fun testConnectionReportsOnlineStatus() = runTest {
        val controller = GatewaySettingsController()
        val probe = RecordingGatewayStatusProbe(status = onlineStatus())

        val result = controller.testConnection(" http://100.64.1.2:8765/ ", probe)

        assertEquals("http://100.64.1.2:8765", probe.requestedBaseUrl)
        assertEquals(GatewayConnectionState.Online, result.state)
        assertEquals("Hermes Local · default · openai/gpt-5.5", result.message)
    }

    @Test
    fun testConnectionReportsOfflineWhenProbeFails() = runTest {
        val controller = GatewaySettingsController()
        val probe = RecordingGatewayStatusProbe(error = IllegalStateException("network down"))

        val result = controller.testConnection("http://100.64.1.2:8765", probe)

        assertEquals(GatewayConnectionState.Offline, result.state)
        assertEquals("Gateway unreachable", result.message)
    }

    @Test
    fun testConnectionRejectsInvalidUrlBeforeCallingProbe() = runTest {
        val controller = GatewaySettingsController()
        val probe = RecordingGatewayStatusProbe(status = onlineStatus())

        val result = controller.testConnection("ftp://100.64.1.2:8765", probe)

        assertEquals(null, probe.requestedBaseUrl)
        assertEquals(GatewayConnectionState.Invalid, result.state)
        assertEquals("Enter an http:// or https:// gateway URL", result.message)
    }
}

private class RecordingGatewayStatusProbe(
    private val status: NodeStatus? = null,
    private val error: Throwable? = null,
) : GatewayStatusProbe {
    var requestedBaseUrl: String? = null

    override suspend fun status(baseUrl: String): NodeStatus {
        requestedBaseUrl = baseUrl
        error?.let { throw it }
        return requireNotNull(status)
    }
}

private fun onlineStatus() = NodeStatus(
    nodeId = "node-local",
    nodeName = "Hermes Local",
    status = "online",
    gatewayReady = true,
    hermesVersion = "0.1.0",
    apiVersion = "mobile-v1",
    profile = "default",
    model = ModelInfo(provider = "openai", model = "gpt-5.5"),
    features = emptyMap(),
)
