package com.hermes.mobile.ui

import com.hermes.mobile.models.GoalRequest
import com.hermes.mobile.models.GoalResponse
import com.hermes.mobile.models.SessionTimeline
import com.hermes.mobile.models.TimelineItem
import com.hermes.mobile.models.TimelineItemType

interface GoalGateway {
    suspend fun startSession(request: GoalRequest): GoalResponse
    suspend fun appendGoal(sessionId: String, request: GoalRequest): GoalResponse
}

sealed class GoalError(message: String) : RuntimeException(message) {
    data object BlankGoal : GoalError("blank_goal")
}

class GoalController(
    private val gateway: GoalGateway,
) {
    suspend fun startGoal(goal: String): SessionTimelineState {
        val cleaned = goal.trim()
        if (cleaned.isEmpty()) throw GoalError.BlankGoal
        return gateway.startSession(GoalRequest(cleaned)).toTimelineState()
    }

    suspend fun appendGoal(sessionId: String, goal: String): SessionTimelineState {
        val cleaned = goal.trim()
        if (cleaned.isEmpty()) throw GoalError.BlankGoal
        return gateway.appendGoal(sessionId, GoalRequest(cleaned)).toTimelineState()
    }
}

data class SessionTimelineState(
    val sessionId: String,
    val title: String,
    val rows: List<TimelineRowState>,
)

data class TimelineRowState(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: TimelineRowKind,
)

enum class TimelineRowKind {
    UserGoal,
    Thinking,
    Result,
}

private fun GoalResponse.toTimelineState(): SessionTimelineState = timeline.toState()

private fun SessionTimeline.toState(): SessionTimelineState = SessionTimelineState(
    sessionId = sessionId,
    title = title,
    rows = items.map(TimelineItem::toRowState),
)

private fun TimelineItem.toRowState(): TimelineRowState = when (type) {
    TimelineItemType.UserGoal -> TimelineRowState(
        id = id,
        title = text.orEmpty(),
        subtitle = "Goal",
        kind = TimelineRowKind.UserGoal,
    )
    TimelineItemType.ThinkingBlock -> TimelineRowState(
        id = id,
        title = title ?: "Thinking",
        subtitle = toolCalls?.joinToString { it.summary } ?: "Queued",
        kind = TimelineRowKind.Thinking,
    )
    TimelineItemType.AssistantResult -> TimelineRowState(
        id = id,
        title = markdown.orEmpty().lineSequence().firstOrNull().orEmpty(),
        subtitle = "Result",
        kind = TimelineRowKind.Result,
    )
}
