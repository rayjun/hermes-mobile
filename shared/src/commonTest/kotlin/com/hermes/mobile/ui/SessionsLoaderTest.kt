package com.hermes.mobile.ui

import com.hermes.mobile.models.SessionStatus
import com.hermes.mobile.models.SessionSummary
import com.hermes.mobile.models.SessionTimeline
import com.hermes.mobile.models.TimelineItem
import com.hermes.mobile.models.TimelineItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant

class SessionsLoaderTest {
    @Test
    fun loadsSessionSummariesFromGateway() = runTest {
        val gateway = FakeSessionsGateway()
        val loader = SessionsLoader(gateway)

        val state = loader.load()

        assertIs<SessionsLoadState.Ready>(state)
        assertFalse(state.usingFallback)
        assertEquals(2, state.sessions.size)
        assertEquals("Recent work", state.sessions.first().title)
    }

    @Test
    fun returnsOfflineFallbackWhenGatewayFails() = runTest {
        val loader = SessionsLoader(FailingSessionsGateway())

        val state = loader.load()

        assertIs<SessionsLoadState.Ready>(state)
        assertEquals(true, state.usingFallback)
        assertEquals("Gateway unavailable · showing sample sessions", state.notice)
        assertEquals("Sample session", state.sessions.first().title)
    }

    @Test
    fun opensSessionDetailFromTimeline() = runTest {
        val gateway = FakeSessionsGateway()
        val loader = SessionsLoader(gateway)

        val detail = loader.openSession("sess_recent")

        assertEquals(SessionScreenMode.Detail, detail.mode)
        assertEquals("sess_recent", detail.timeline.sessionId)
        assertEquals("Recent work", detail.timeline.title)
        assertEquals(TimelineRowKind.UserGoal, detail.timeline.rows.first().kind)
    }
}

private class FakeSessionsGateway : SessionsGateway {
    override suspend fun sessions(): List<SessionSummary> = listOf(
        SessionSummary(
            id = "sess_recent",
            title = "Recent work",
            status = SessionStatus.Running,
            createdAt = Instant.parse("2026-06-24T10:00:00Z"),
            updatedAt = Instant.parse("2026-06-24T10:02:00Z"),
        ),
        SessionSummary(
            id = "sess_done",
            title = "Completed work",
            status = SessionStatus.Completed,
            createdAt = Instant.parse("2026-06-23T10:00:00Z"),
            updatedAt = Instant.parse("2026-06-23T10:02:00Z"),
        ),
    )

    override suspend fun sessionTimeline(sessionId: String): SessionTimeline = SessionTimeline(
        sessionId = sessionId,
        title = "Recent work",
        items = listOf(
            TimelineItem(
                type = TimelineItemType.UserGoal,
                id = "msg_1",
                createdAt = Instant.parse("2026-06-24T10:00:00Z"),
                text = "Continue implementation",
            ),
        ),
    )
}

private class FailingSessionsGateway : SessionsGateway {
    override suspend fun sessions(): List<SessionSummary> = error("offline")
    override suspend fun sessionTimeline(sessionId: String): SessionTimeline = error("offline")
}
