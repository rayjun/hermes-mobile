package com.hermes.mobile.ui

class SessionDetailController(
    private val goalController: GoalController,
) {
    suspend fun submitGoal(current: SessionDetailState?, goal: String): SessionDetailState {
        val timeline = if (current == null) {
            goalController.startGoal(goal)
        } else {
            goalController.appendGoal(current.timeline.sessionId, goal)
        }
        return SessionDetailState(
            mode = SessionScreenMode.Detail,
            timeline = timeline,
        )
    }
}

data class SessionDetailState(
    val mode: SessionScreenMode,
    val timeline: SessionTimelineState,
)

enum class SessionScreenMode {
    Inbox,
    Detail,
}
