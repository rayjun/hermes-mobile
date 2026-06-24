package com.hermes.mobile.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionSummary(
    val id: String,
    val title: String,
    val status: SessionStatus,
    val source: String = "mobile",
    val summary: String? = null,
    val provider: String? = null,
    val model: String? = null,
    val workdir: String? = null,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)

@Serializable
enum class SessionStatus {
    @SerialName("running") Running,
    @SerialName("waiting_approval") WaitingApproval,
    @SerialName("completed") Completed,
    @SerialName("failed") Failed,
    @SerialName("cancelled") Cancelled,
}

@Serializable
data class SessionsResponse(
    val sessions: List<SessionSummary>,
)
