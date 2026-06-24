package com.hermes.mobile.ui

import com.hermes.mobile.models.HermesEvent
import com.hermes.mobile.models.HermesEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class SessionLiveEventControllerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun appliesSessionUpdatedEventToMatchingDetail() {
        val controller = SessionLiveEventController()
        val current = SessionDetailState(
            mode = SessionScreenMode.Detail,
            timeline = SessionTimelineState(
                sessionId = "sess_mobile",
                title = "Initial goal",
                rows = emptyList(),
            ),
        )
        val event = json.decodeFromString<HermesEvent>(sessionUpdatedEventJson("sess_mobile"))

        val next = controller.apply(current, event)

        assertEquals("sess_mobile", next.timeline.sessionId)
        assertEquals(listOf("Initial goal", "Queued", "Done"), next.timeline.rows.map { it.title })
    }

    @Test
    fun ignoresSessionUpdatedEventForDifferentSession() {
        val controller = SessionLiveEventController()
        val current = SessionDetailState(
            mode = SessionScreenMode.Detail,
            timeline = SessionTimelineState(
                sessionId = "sess_mobile",
                title = "Initial goal",
                rows = emptyList(),
            ),
        )
        val event = json.decodeFromString<HermesEvent>(sessionUpdatedEventJson("other_session"))

        val next = controller.apply(current, event)

        assertEquals(current, next)
    }

    @Test
    fun onlySessionUpdatedEventsChangeDetail() {
        val controller = SessionLiveEventController()
        val current = SessionDetailState(
            mode = SessionScreenMode.Detail,
            timeline = SessionTimelineState(
                sessionId = "sess_mobile",
                title = "Initial goal",
                rows = emptyList(),
            ),
        )
        val event = HermesEvent(
            id = "evt_approval",
            type = HermesEventType.ApprovalRequested,
            sessionId = "sess_mobile",
            createdAt = Instant.parse("2026-06-24T10:00:00Z"),
            payload = json.parseToJsonElement("{\"approval_id\":\"appr\"}").jsonObject,
        )

        val next = controller.apply(current, event)

        assertEquals(current, next)
    }
}

private fun sessionUpdatedEventJson(sessionId: String): String = """
{
  "id": "evt_session_updated",
  "type": "session.updated",
  "session_id": "$sessionId",
  "created_at": "2026-06-24T10:00:02Z",
  "payload": {
    "timeline": {
      "session_id": "$sessionId",
      "title": "Initial goal",
      "items": [
        {
          "id": "goal_1",
          "type": "user_goal",
          "created_at": "2026-06-24T10:00:00Z",
          "text": "Initial goal"
        },
        {
          "id": "thinking_1",
          "type": "thinking_block",
          "created_at": "2026-06-24T10:00:01Z",
          "title": "Queued"
        },
        {
          "id": "result_1",
          "type": "assistant_result",
          "created_at": "2026-06-24T10:00:03Z",
          "markdown": "Done\n\nAll set."
        }
      ]
    }
  }
}
"""