package com.hermes.mobile.api

import com.hermes.mobile.models.HermesEvent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class HermesEventStream(
    private val baseUrl: String,
    private val client: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun events(): Flow<HermesEvent> = flow {
        val wsUrl = baseUrl.replaceFirst("http://", "ws://").replaceFirst("https://", "wss://")
        client.webSocket("$wsUrl/mobile/v1/events") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    emit(json.decodeFromString<HermesEvent>(frame.readText()))
                }
            }
        }
    }
}
