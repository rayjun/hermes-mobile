package com.hermes.mobile.ui

import com.hermes.mobile.models.SessionStatus
import com.hermes.mobile.models.SessionSummary
import com.hermes.mobile.models.SessionTimeline
import kotlinx.datetime.Instant

interface SessionsGateway {
    suspend fun sessions(): List<SessionSummary>
    suspend fun sessionTimeline(sessionId: String): SessionTimeline
}

sealed interface SessionsLoadState {
    data object Loading : SessionsLoadState

    data class Ready(
        val sessions: List<SessionSummary>,
        val notice: String? = null,
        val usingFallback: Boolean = false,
    ) : SessionsLoadState
}

class SessionsLoader(
    private val gateway: SessionsGateway,
) {
    suspend fun load(): SessionsLoadState.Ready = try {
        SessionsLoadState.Ready(sessions = gateway.sessions())
    } catch (_: Throwable) {
        SessionsLoadState.Ready(
            sessions = listOf(fallbackSession()),
            notice = "Gateway unavailable · showing sample sessions",
            usingFallback = true,
        )
    }

    suspend fun openSession(sessionId: String): SessionDetailState {
        val timeline = gateway.sessionTimeline(sessionId)
        return SessionDetailState(
            mode = SessionScreenMode.Detail,
            timeline = timeline.toState(),
        )
    }
}

private fun fallbackSession(): SessionSummary = SessionSummary(
    id = "sess_sample",
    title = "Sample session",
    status = SessionStatus.Completed,
    createdAt = Instant.parse("2026-06-24T12:00:00Z"),
    updatedAt = Instant.parse("2026-06-24T12:01:00Z"),
)
