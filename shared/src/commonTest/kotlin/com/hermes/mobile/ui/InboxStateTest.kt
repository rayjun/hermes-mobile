package com.hermes.mobile.ui

import com.hermes.mobile.models.Approval
import com.hermes.mobile.models.ApprovalKind
import com.hermes.mobile.models.ApprovalStatus
import com.hermes.mobile.models.RiskLevel
import com.hermes.mobile.ui.components.InboxItemKind
import kotlinx.datetime.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InboxStateTest {
    @Test
    fun mapsPendingApprovalsToApprovalItemsFirst() {
        val state = InboxState.from(
            approvals = listOf(highRiskApproval(), mediumRiskApproval()),
            runningSessions = listOf(RunningSessionItem("sess_1", "Running task", "Thinking · 21s")),
            recentEvents = listOf(RecentEventItem("evt_1", "Done", "success · now", InboxItemKind.Result)),
        )

        assertEquals(3, state.sections.size)
        assertEquals("APPROVALS", state.sections[0].title)
        assertEquals(2, state.sections[0].items.size)
        assertEquals(InboxItemKind.Approval, state.sections[0].items[0].kind)
        assertEquals(RiskLevel.High, state.sections[0].items[0].risk)
        assertEquals("RUNNING", state.sections[1].title)
        assertEquals("RECENT", state.sections[2].title)
    }

    @Test
    fun excludesEmptySections() {
        val state = InboxState.from(
            approvals = emptyList(),
            runningSessions = emptyList(),
            recentEvents = listOf(RecentEventItem("evt_1", "Done", "success · now", InboxItemKind.Result)),
        )

        assertEquals(1, state.sections.size)
        assertEquals("RECENT", state.sections.single().title)
    }

    @Test
    fun createsApprovalCardStateWithStructuredContext() {
        val card = ApprovalCardState.from(highRiskApproval())

        assertEquals("appr_high", card.id)
        assertEquals("Run git push", card.title)
        assertEquals(RiskLevel.High, card.risk)
        assertEquals("rayjun/hermes-agent", card.contextRows["Repo"])
        assertEquals("fix/mobile-api", card.contextRows["Branch"])
        assertTrue(card.requiresBiometric)
    }
}

private fun highRiskApproval(): Approval = Approval(
    id = "appr_high",
    sessionId = "sess_1",
    kind = ApprovalKind.TerminalCommand,
    risk = RiskLevel.High,
    status = ApprovalStatus.Pending,
    title = "Run git push",
    summary = "Hermes wants to push branch fix/mobile-api.",
    reason = "The change passed tests and should be pushed.",
    details = buildJsonObject {
        put("repo", "rayjun/hermes-agent")
        put("branch", "fix/mobile-api")
        put("cwd", "/home/ubuntu/projects/hermes-agent")
        put("command", "git push origin fix/mobile-api")
        put("tests", "128 passed")
    },
    actions = listOf("approve", "deny", "ask"),
    createdAt = Instant.parse("2026-06-24T10:00:00Z"),
)

private fun mediumRiskApproval(): Approval = highRiskApproval().copy(
    id = "appr_medium",
    risk = RiskLevel.Medium,
    title = "Create cron job",
)
