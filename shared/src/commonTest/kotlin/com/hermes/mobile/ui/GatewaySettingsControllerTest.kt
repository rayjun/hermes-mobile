package com.hermes.mobile.ui

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
}
