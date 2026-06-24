package com.hermes.mobile.ui

import com.hermes.mobile.models.Approval
import com.hermes.mobile.models.ApprovalDecision
import com.hermes.mobile.models.ApprovalKind
import com.hermes.mobile.models.ApprovalStatus
import com.hermes.mobile.models.RiskLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.buildJsonObject

class ApprovalActionControllerTest {
    @Test
    fun approveHighRiskRequiresBiometricVerification() = runTest {
        val gateway = RecordingApprovalGateway(highRiskPending())
        val controller = ApprovalActionController(gateway)

        val error = assertFailsWith<ApprovalActionError.BiometricRequired> {
            controller.approve(highRiskPending(), biometricVerified = false)
        }

        assertEquals("appr_high", error.approvalId)
        assertEquals(0, gateway.approveCalls)
    }

    @Test
    fun approveHighRiskSendsBiometricDecision() = runTest {
        val gateway = RecordingApprovalGateway(highRiskPending())
        val controller = ApprovalActionController(gateway)

        val approval = controller.approve(highRiskPending(), biometricVerified = true)

        assertEquals(ApprovalStatus.Approved, approval.status)
        assertEquals(1, gateway.approveCalls)
        assertEquals(ApprovalDecision(biometricVerified = true), gateway.lastDecision)
    }

    @Test
    fun denyDoesNotRequireBiometricVerification() = runTest {
        val gateway = RecordingApprovalGateway(highRiskPending())
        val controller = ApprovalActionController(gateway)

        val approval = controller.deny(highRiskPending(), reason = "Not safe")

        assertEquals(ApprovalStatus.Denied, approval.status)
        assertEquals(1, gateway.denyCalls)
        assertEquals(ApprovalDecision(reason = "Not safe", biometricVerified = false), gateway.lastDecision)
    }
}

private class RecordingApprovalGateway(
    private val pending: Approval,
) : ApprovalActionGateway {
    var approveCalls = 0
    var denyCalls = 0
    var lastDecision: ApprovalDecision? = null

    override suspend fun approve(id: String, decision: ApprovalDecision): Approval {
        approveCalls += 1
        lastDecision = decision
        return pending.copy(status = ApprovalStatus.Approved)
    }

    override suspend fun deny(id: String, decision: ApprovalDecision): Approval {
        denyCalls += 1
        lastDecision = decision
        return pending.copy(status = ApprovalStatus.Denied)
    }
}

private fun highRiskPending(): Approval = Approval(
    id = "appr_high",
    sessionId = "sess_1",
    kind = ApprovalKind.TerminalCommand,
    risk = RiskLevel.High,
    status = ApprovalStatus.Pending,
    title = "Run git push",
    summary = "Hermes wants to push branch fix/mobile-api.",
    reason = "Tests passed and this needs review.",
    details = buildJsonObject {},
    actions = listOf("approve", "deny"),
    createdAt = Instant.parse("2026-06-24T12:00:00Z"),
)
