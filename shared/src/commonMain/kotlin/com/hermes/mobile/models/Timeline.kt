package com.hermes.mobile.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionTimeline(
    @SerialName("session_id") val sessionId: String,
    val title: String,
    val items: List<TimelineItem>,
)

@Serializable
data class TimelineItem(
    val type: TimelineItemType,
    val id: String,
    @SerialName("created_at") val createdAt: Instant,
    val text: String? = null,
    val title: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    val markdown: String? = null,
)

@Serializable
enum class TimelineItemType {
    @SerialName("user_goal") UserGoal,
    @SerialName("thinking_block") ThinkingBlock,
    @SerialName("assistant_result") AssistantResult,
}

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val summary: String,
    val status: ToolStatus,
    @SerialName("duration_ms") val durationMs: Long? = null,
    val error: String? = null,
)

@Serializable
enum class ToolStatus {
    @SerialName("running") Running,
    @SerialName("completed") Completed,
    @SerialName("failed") Failed,
    @SerialName("waiting_approval") WaitingApproval,
    @SerialName("skipped") Skipped,
}
