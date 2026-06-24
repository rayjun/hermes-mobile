package com.hermes.mobile.ui

import com.hermes.mobile.models.NodeStatus

class GatewaySettingsController {
    fun initialState(savedBaseUrl: String?): GatewaySettingsState {
        val cleaned = savedBaseUrl?.trim()?.takeIf { it.isNotEmpty() }
        return GatewaySettingsState(
            baseUrl = cleaned ?: DefaultGatewayBaseUrl,
            configured = cleaned != null,
        )
    }

    fun save(input: String): GatewaySettingsState {
        val baseUrl = normalize(input)
        return GatewaySettingsState(baseUrl = baseUrl, configured = true)
    }

    suspend fun testConnection(input: String, probe: GatewayStatusProbe): GatewayConnectionResult {
        val baseUrl = try {
            normalize(input)
        } catch (_: GatewaySettingsError.InvalidUrl) {
            return GatewayConnectionResult(GatewayConnectionState.Invalid, "Enter an http:// or https:// gateway URL")
        }
        return try {
            val status = probe.status(baseUrl)
            GatewayConnectionResult(
                state = if (status.gatewayReady) GatewayConnectionState.Online else GatewayConnectionState.Offline,
                message = "${status.nodeName} · ${status.profile} · ${status.model.provider}/${status.model.model}",
            )
        } catch (_: Throwable) {
            GatewayConnectionResult(GatewayConnectionState.Offline, "Gateway unreachable")
        }
    }

    private fun normalize(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isBlank()) throw GatewaySettingsError.InvalidUrl
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw GatewaySettingsError.InvalidUrl
        }
        val hostPart = trimmed.substringAfter("://").substringBefore('/').substringBefore('?')
        if (hostPart.isBlank()) throw GatewaySettingsError.InvalidUrl
        return trimmed
    }

    companion object {
        const val DefaultGatewayBaseUrl = "http://10.0.2.2:8765"
    }
}

data class GatewaySettingsState(
    val baseUrl: String,
    val configured: Boolean,
    val error: String? = null,
    val connection: GatewayConnectionResult? = null,
)

data class GatewayConnectionResult(
    val state: GatewayConnectionState,
    val message: String,
)

enum class GatewayConnectionState {
    Unknown,
    Testing,
    Online,
    Offline,
    Invalid,
}

fun interface GatewayStatusProbe {
    suspend fun status(baseUrl: String): NodeStatus
}

sealed class GatewaySettingsError(message: String) : IllegalArgumentException(message) {
    data object InvalidUrl : GatewaySettingsError("invalid_gateway_url")
}
