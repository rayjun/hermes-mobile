package com.hermes.mobile.repositories

import com.hermes.mobile.api.HermesApi
import com.hermes.mobile.models.NodeStatus
import com.hermes.mobile.models.SessionTimeline

class SessionRepository(
    private val api: HermesApi,
) {
    suspend fun nodeStatus(): NodeStatus = api.status()

    suspend fun timeline(sessionId: String): SessionTimeline = api.sessionTimeline(sessionId)
}
