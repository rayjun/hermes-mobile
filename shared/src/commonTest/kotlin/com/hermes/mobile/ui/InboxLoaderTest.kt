package com.hermes.mobile.ui

import com.hermes.mobile.models.Approval
import com.hermes.mobile.models.ApprovalKind
import com.hermes.mobile.models.ApprovalStatus
import com.hermes.mobile.models.RiskLevel
import com.hermes.mobile.models.NodeStatus
import com.hermes.mobile.models.ModelInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.buildJsonObject

class InboxLoaderTest {
    @Test
    fun loadsPendingApprovalsIntoReadyInboxState() = runTest {
        val loader = InboxLoader(
            gateway = FakeInboxGateway(
                status = NodeStatus(
                    nodeId = "node_local",
                    nodeName = "Hermes VPS",
                    status = "online",
                    gatewayReady = true,
                    hermesVersion = "0.1.0",
                    apiVersion = "mobile-v1",
                    profile = "default",
                    model = ModelInfo(provider = "mock", model = "mock"),
                    features = mapOf("approvals" to true),
                ),
                approvals = listOf(sampleApproval()),
            ),
        )

        val state = loader.load()

        assertEquals("Hermes VPS", state.nodeName)
        assertEquals("online", state.nodeStatus)
        assertFalse(state.usingFallback)
        assertEquals(listOf("APPROVALS"), state.inbox.sections.map { it.title })
        assertEquals("Approve git push", state.inbox.sections.single().items.single().title)
        assertEquals("Approve git push", state.approvalCards.single().title)
    }

    @Test
    fun fallsBackToSampleInboxWhenGatewayIsUnavailable() = runTest {
        val loader = InboxLoader(
            gateway = FakeInboxGateway(failure = IllegalStateException("offline")),
        )

        val state = loader.load()

        assertEquals("offline", state.nodeStatus)
        assertTrue(state.usingFallback)
        assertEquals("Gateway unavailable · showing sample data", state.notice)
        assertTrue(state.inbox.sections.isNotEmpty())
    }
}

private class FakeInboxGateway(
    private val status: NodeStatus? = null,
    private val approvals: List<Approval> = emptyList(),
    private val failure: Throwable? = null,
) : InboxGateway {
    override suspend fun status(): NodeStatus {
        failure?.let { throw it }
        return checkNotNull(status)
    }

    override suspend fun pendingApprovals(): List<Approval> {
        failure?.let { throw it }
        return approvals
    }
}

private fun sampleApproval(): Approval = Approval(
    id = "appr_1",
    sessionId = "sess_1",
    kind = ApprovalKind.GitPush,
    risk = RiskLevel.High,
    status = ApprovalStatus.Pending,
    title = "Approve git push",
    summary = "Push branch fix/mobile-api.",
    reason = "User requested mobile gateway work.",
    details = buildJsonObject {},
    actions = listOf("approve", "deny"),
    createdAt = Instant.parse("2026-06-24T12:00:00Z"),
)
