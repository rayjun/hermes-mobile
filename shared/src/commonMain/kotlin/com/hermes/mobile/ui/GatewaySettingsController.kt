package com.hermes.mobile.ui

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
)

sealed class GatewaySettingsError(message: String) : IllegalArgumentException(message) {
    data object InvalidUrl : GatewaySettingsError("invalid_gateway_url")
}
