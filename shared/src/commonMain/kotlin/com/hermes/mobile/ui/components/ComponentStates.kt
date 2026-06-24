package com.hermes.mobile.ui.components

import com.hermes.mobile.models.RiskLevel

data class SectionHeaderState(
    val title: String,
    val count: Int? = null,
)

data class InboxItemState(
    val title: String,
    val subtitle: String,
    val kind: InboxItemKind,
    val risk: RiskLevel? = null,
    val timestamp: String? = null,
)

enum class InboxItemKind {
    Approval,
    Running,
    Error,
    Result,
}

data class CommandBarState(
    val placeholder: String = "Start with a goal",
    val text: String = "",
    val canSend: Boolean = false,
)
