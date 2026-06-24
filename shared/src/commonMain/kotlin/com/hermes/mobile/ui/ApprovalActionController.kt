package com.hermes.mobile.ui

import com.hermes.mobile.models.Approval
import com.hermes.mobile.models.ApprovalDecision
import com.hermes.mobile.models.RiskLevel

interface ApprovalActionGateway {
    suspend fun approve(id: String, decision: ApprovalDecision): Approval
    suspend fun deny(id: String, decision: ApprovalDecision): Approval
}

sealed class ApprovalActionError(message: String) : RuntimeException(message) {
    data class BiometricRequired(val approvalId: String) :
        ApprovalActionError("biometric_required:$approvalId")
}

class ApprovalActionController(
    private val gateway: ApprovalActionGateway,
) {
    suspend fun approve(
        approval: Approval,
        biometricVerified: Boolean,
    ): Approval {
        if (approval.risk.requiresBiometric() && !biometricVerified) {
            throw ApprovalActionError.BiometricRequired(approval.id)
        }
        return gateway.approve(
            approval.id,
            ApprovalDecision(biometricVerified = biometricVerified),
        )
    }

    suspend fun deny(
        approval: Approval,
        reason: String? = null,
    ): Approval = gateway.deny(
        approval.id,
        ApprovalDecision(reason = reason, biometricVerified = false),
    )
}

private fun RiskLevel.requiresBiometric(): Boolean = this == RiskLevel.High || this == RiskLevel.Critical
