package com.hermes.mobile.ui

import com.hermes.mobile.models.HermesEvent
import com.hermes.mobile.models.HermesEventType
import com.hermes.mobile.models.SessionTimeline
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

class SessionLiveEventController(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun apply(current: SessionDetailState, event: HermesEvent): SessionDetailState {
        if (event.type != HermesEventType.SessionUpdated) return current
        if (event.sessionId != current.timeline.sessionId) return current
        val timelineElement = event.payload["timeline"] ?: return current
        val timeline = json.decodeFromJsonElement<SessionTimeline>(timelineElement)
        return current.copy(timeline = timeline.toState())
    }
}
