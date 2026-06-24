package com.hermes.mobile.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Approval(
    val id: String,
    @SerialName("session_id") val sessionId: String? = null,
    val kind: ApprovalKind,
    val risk: RiskLevel,
    val status: ApprovalStatus,
    val title: String,
    val summary: String,
    val reason: String,
    val details: JsonObject,
    val actions: List<String>,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("expires_at") val expiresAt: Instant? = null,
)

@Serializable
enum class ApprovalKind {
    @SerialName("terminal_command") TerminalCommand,
    @SerialName("file_write") FileWrite,
    @SerialName("file_patch") FilePatch,
    @SerialName("git_push") GitPush,
    @SerialName("github_pr_create") GithubPrCreate,
    @SerialName("cron_create") CronCreate,
    @SerialName("memory_write") MemoryWrite,
    @SerialName("skill_write") SkillWrite,
    @SerialName("network_post") NetworkPost,
}

@Serializable
enum class RiskLevel {
    @SerialName("low") Low,
    @SerialName("medium") Medium,
    @SerialName("high") High,
    @SerialName("critical") Critical,
}

@Serializable
enum class ApprovalStatus {
    @SerialName("pending") Pending,
    @SerialName("approved") Approved,
    @SerialName("denied") Denied,
    @SerialName("expired") Expired,
    @SerialName("cancelled") Cancelled,
}

@Serializable
data class ApprovalDecision(
    val comment: String? = null,
    val reason: String? = null,
    @SerialName("biometric_verified") val biometricVerified: Boolean = false,
)

@Serializable
data class ApprovalsResponse(
    val approvals: List<Approval>,
)
