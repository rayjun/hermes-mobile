package com.hermes.mobile.ui

import com.hermes.mobile.models.GoalRequest
import com.hermes.mobile.models.GoalResponse
import com.hermes.mobile.models.SessionSummary
import com.hermes.mobile.models.SessionStatus
import com.hermes.mobile.models.SessionTimeline
import com.hermes.mobile.models.TimelineItem
import com.hermes.mobile.models.TimelineItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant

class SessionDetailControllerTest {
    @Test
    fun firstSubmittedGoalCreatesSessionDetail() = runTest {
        val gateway = RecordingSessionGoalGateway()
        val controller = SessionDetailController(GoalController(gateway))

        val detail = controller.submitGoal(current = null, goal = "Check mobile events")

        assertEquals(1, gateway.startCalls)
        assertEquals(0, gateway.appendCalls)
        assertEquals("sess_mobile", detail.timeline.sessionId)
        assertEquals(SessionScreenMode.Detail, detail.mode)
        assertEquals(listOf("Check mobile events", "Queued"), detail.timeline.rows.map { it.title })
    }

    @Test
    fun nextSubmittedGoalAppendsToExistingSessionDetail() = runTest {
        val gateway = RecordingSessionGoalGateway()
        val controller = SessionDetailController(GoalController(gateway))
        val first = controller.submitGoal(current = null, goal = "Check mobile events")

        val second = controller.submitGoal(current = first, goal = "Continue implementation")

        assertEquals(1, gateway.startCalls)
        assertEquals(1, gateway.appendCalls)
        assertEquals("sess_mobile", gateway.lastAppendSessionId)
        assertEquals("Continue implementation", gateway.lastAppendGoal?.goal)
        assertEquals("sess_mobile", second.timeline.sessionId)
        assertEquals(
            listOf("Check mobile events", "Queued", "Continue implementation", "Queued"),
            second.timeline.rows.map { it.title },
        )
    }
}

private class RecordingSessionGoalGateway : GoalGateway {
    var startCalls = 0
    var appendCalls = 0
    var lastAppendSessionId: String? = null
    var lastAppendGoal: GoalRequest? = null
    private val goals = mutableListOf<String>()

    override suspend fun startSession(request: GoalRequest): GoalResponse {
        startCalls += 1
        goals.clear()
        goals += request.goal
        return response(goals)
    }

    override suspend fun appendGoal(sessionId: String, request: GoalRequest): GoalResponse {
        appendCalls += 1
        lastAppendSessionId = sessionId
        lastAppendGoal = request
        goals += request.goal
        return response(goals)
    }
}

private fun response(goals: List<String>): GoalResponse = GoalResponse(
    session = SessionSummary(
        id = "sess_mobile",
        title = goals.first(),
        status = SessionStatus.Running,
        createdAt = Instant.parse("2026-06-24T14:00:00Z"),
        updatedAt = Instant.parse("2026-06-24T14:00:01Z"),
    ),
    timeline = SessionTimeline(
        sessionId = "sess_mobile",
        title = goals.first(),
        items = goals.flatMapIndexed { index, goal ->
            listOf(
                TimelineItem(
                    type = TimelineItemType.UserGoal,
                    id = "goal_$index",
                    createdAt = Instant.parse("2026-06-24T14:00:00Z"),
                    text = goal,
                ),
                TimelineItem(
                    type = TimelineItemType.ThinkingBlock,
                    id = "think_$index",
                    createdAt = Instant.parse("2026-06-24T14:00:01Z"),
                    title = "Queued",
                ),
            )
        },
    ),
)
