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
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant

class GoalControllerTest {
    @Test
    fun rejectsBlankGoalBeforeCallingGateway() = runTest {
        val gateway = RecordingGoalGateway()
        val controller = GoalController(gateway)

        assertFailsWith<GoalError.BlankGoal> {
            controller.startGoal("   ")
        }

        assertEquals(0, gateway.startCalls)
    }

    @Test
    fun startsGoalAndReturnsTimelineState() = runTest {
        val gateway = RecordingGoalGateway()
        val controller = GoalController(gateway)

        val state = controller.startGoal("Review pending approvals")

        assertEquals(1, gateway.startCalls)
        assertEquals("Review pending approvals", gateway.lastGoal?.goal)
        assertEquals("sess_goal_1", state.sessionId)
        assertEquals("Review pending approvals", state.title)
        assertEquals(listOf("Review pending approvals", "Queued"), state.rows.map { it.title })
    }
}

private class RecordingGoalGateway : GoalGateway {
    var startCalls = 0
    var lastGoal: GoalRequest? = null

    override suspend fun startSession(request: GoalRequest): GoalResponse {
        startCalls += 1
        lastGoal = request
        return goalResponse(request.goal)
    }

    override suspend fun appendGoal(sessionId: String, request: GoalRequest): GoalResponse = goalResponse(request.goal)
}

private fun goalResponse(goal: String): GoalResponse = GoalResponse(
    session = SessionSummary(
        id = "sess_goal_1",
        title = goal,
        status = SessionStatus.Running,
        source = "mobile",
        createdAt = Instant.parse("2026-06-24T14:00:00Z"),
        updatedAt = Instant.parse("2026-06-24T14:00:00Z"),
    ),
    timeline = SessionTimeline(
        sessionId = "sess_goal_1",
        title = goal,
        items = listOf(
            TimelineItem(
                type = TimelineItemType.UserGoal,
                id = "goal_1",
                createdAt = Instant.parse("2026-06-24T14:00:00Z"),
                text = goal,
            ),
            TimelineItem(
                type = TimelineItemType.ThinkingBlock,
                id = "think_1",
                createdAt = Instant.parse("2026-06-24T14:00:01Z"),
                title = "Queued",
            ),
        ),
    ),
)
