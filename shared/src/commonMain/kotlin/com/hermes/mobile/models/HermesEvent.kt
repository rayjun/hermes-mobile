package com.hermes.mobile.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class HermesEvent(
    val id: String,
    val type: HermesEventType,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("created_at") val createdAt: Instant,
    val payload: JsonObject,
)

@Serializable
enum class HermesEventType {
    @SerialName("session.started") SessionStarted,
    @SerialName("session.updated") SessionUpdated,
    @SerialName("message.created") MessageCreated,
    @SerialName("tool.started") ToolStarted,
    @SerialName("tool.finished") ToolFinished,
    @SerialName("tool.failed") ToolFailed,
    @SerialName("approval.requested") ApprovalRequested,
    @SerialName("approval.resolved") ApprovalResolved,
    @SerialName("cron.started") CronStarted,
    @SerialName("cron.finished") CronFinished,
    @SerialName("artifact.created") ArtifactCreated,
    @SerialName("error.raised") ErrorRaised,
}
