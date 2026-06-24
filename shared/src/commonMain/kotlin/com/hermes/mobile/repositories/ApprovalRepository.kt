package com.hermes.mobile.repositories

import com.hermes.mobile.api.HermesApi
import com.hermes.mobile.models.Approval
import com.hermes.mobile.models.ApprovalDecision

class ApprovalRepository(
    private val api: HermesApi,
) {
    suspend fun pending(): List<Approval> = api.pendingApprovals().approvals

    suspend fun detail(id: String): Approval = api.approval(id)

    suspend fun approve(id: String): Approval =
        api.approve(id, ApprovalDecision(comment = "Approved from Hermes Mobile"))

    suspend fun deny(id: String, reason: String): Approval =
        api.deny(id, ApprovalDecision(reason = reason))
}
