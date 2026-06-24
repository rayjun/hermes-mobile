package com.hermes.mobile.ui

import com.hermes.mobile.models.Approval
import com.hermes.mobile.models.ApprovalKind
import com.hermes.mobile.models.ApprovalStatus
import com.hermes.mobile.models.NodeStatus
import com.hermes.mobile.models.RiskLevel
import kotlinx.datetime.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface InboxGateway {
    suspend fun status(): NodeStatus
    suspend fun pendingApprovals(): List<Approval>
}

sealed interface InboxLoadState {
    data object Loading : InboxLoadState

    data class Ready(
        val nodeName: String,
        val nodeStatus: String,
        val inbox: InboxState,
        val approvalCards: List<ApprovalCardState>,
        val notice: String? = null,
        val usingFallback: Boolean = false,
    ) : InboxLoadState
}

class InboxLoader(
    private val gateway: InboxGateway,
) {
    suspend fun load(): InboxLoadState.Ready = try {
        val status = gateway.status()
        val approvals = gateway.pendingApprovals()
        InboxLoadState.Ready(
            nodeName = status.nodeName,
            nodeStatus = status.status,
            inbox = InboxState.from(approvals = approvals),
            approvalCards = approvals.map(ApprovalCardState::from),
        )
    } catch (_: Throwable) {
        val fallback = fallbackApproval()
        InboxLoadState.Ready(
            nodeName = "Hermes",
            nodeStatus = "offline",
            inbox = InboxState.from(approvals = listOf(fallback)),
            approvalCards = listOf(ApprovalCardState.from(fallback)),
            notice = "Gateway unavailable · showing sample data",
            usingFallback = true,
        )
    }
}

private fun fallbackApproval(): Approval = Approval(
    id = "appr_sample_git_push",
    sessionId = "sess_sample",
    kind = ApprovalKind.GitPush,
    risk = RiskLevel.High,
    status = ApprovalStatus.Pending,
    title = "Run git push",
    summary = "Hermes wants to push branch fix/mobile-api.",
    reason = "Sample approval shown while the gateway is offline.",
    details = buildJsonObject {
        put("repo", "rayjun/hermes-agent")
        put("branch", "fix/mobile-api")
        put("tests", "128 passed")
    },
    actions = listOf("approve", "deny"),
    createdAt = Instant.parse("2026-06-24T12:00:00Z"),
)
