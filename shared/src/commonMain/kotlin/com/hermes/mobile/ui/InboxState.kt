package com.hermes.mobile.ui

import com.hermes.mobile.models.Approval
import com.hermes.mobile.models.RiskLevel
import com.hermes.mobile.ui.components.InboxItemKind
import com.hermes.mobile.ui.components.InboxItemState
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull


data class InboxState(
    val sections: List<InboxSection>,
) {
    companion object {
        fun from(
            approvals: List<Approval>,
            runningSessions: List<RunningSessionItem> = emptyList(),
            recentEvents: List<RecentEventItem> = emptyList(),
        ): InboxState {
            val sections = buildList {
                val approvalItems = approvals.map { approval ->
                    InboxItemState(
                        title = approval.title,
                        subtitle = approval.summary,
                        kind = InboxItemKind.Approval,
                        risk = approval.risk,
                    )
                }
                if (approvalItems.isNotEmpty()) {
                    add(InboxSection("APPROVALS", approvalItems))
                }

                val runningItems = runningSessions.map { session ->
                    InboxItemState(
                        title = session.title,
                        subtitle = session.subtitle,
                        kind = InboxItemKind.Running,
                    )
                }
                if (runningItems.isNotEmpty()) {
                    add(InboxSection("RUNNING", runningItems))
                }

                val recentItems = recentEvents.map { event ->
                    InboxItemState(
                        title = event.title,
                        subtitle = event.subtitle,
                        kind = event.kind,
                    )
                }
                if (recentItems.isNotEmpty()) {
                    add(InboxSection("RECENT", recentItems))
                }
            }
            return InboxState(sections)
        }
    }
}

data class InboxSection(
    val title: String,
    val items: List<InboxItemState>,
)

data class RunningSessionItem(
    val id: String,
    val title: String,
    val subtitle: String,
)

data class RecentEventItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: InboxItemKind,
)

data class ApprovalCardState(
    val id: String,
    val title: String,
    val summary: String,
    val risk: RiskLevel,
    val contextRows: Map<String, String>,
    val requiresBiometric: Boolean,
) {
    companion object {
        fun from(approval: Approval): ApprovalCardState {
            val contextRows = linkedMapOf<String, String>()
            approval.details.stringValue("repo")?.let { contextRows["Repo"] = it }
            approval.details.stringValue("cwd")?.let { contextRows["CWD"] = it }
            approval.details.stringValue("branch")?.let { contextRows["Branch"] = it }
            approval.details.stringValue("tests")?.let { contextRows["Tests"] = it }
            approval.details.stringValue("command")?.let { contextRows["Command"] = it }

            return ApprovalCardState(
                id = approval.id,
                title = approval.title,
                summary = approval.summary,
                risk = approval.risk,
                contextRows = contextRows,
                requiresBiometric = approval.risk == RiskLevel.High || approval.risk == RiskLevel.Critical,
            )
        }
    }
}

private fun Map<String, *>.stringValue(key: String): String? {
    val value = this[key] as? JsonPrimitive ?: return null
    return value.contentOrNull
}
