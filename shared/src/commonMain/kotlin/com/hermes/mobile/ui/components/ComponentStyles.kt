package com.hermes.mobile.ui.components

import com.hermes.mobile.models.RiskLevel
import com.hermes.mobile.ui.theme.HermesColors


data class ApprovalRiskStyle(
    val foreground: Long,
    val background: Long,
    val label: String,
) {
    companion object {
        fun forRisk(risk: RiskLevel): ApprovalRiskStyle = when (risk) {
            RiskLevel.Low -> ApprovalRiskStyle(HermesColors.RiskLow, HermesColors.BlueSoft, "LOW")
            RiskLevel.Medium -> ApprovalRiskStyle(HermesColors.RiskMedium, HermesColors.RiskMediumBg, "MED")
            RiskLevel.High -> ApprovalRiskStyle(HermesColors.RiskHigh, HermesColors.RiskHighBg, "HIGH")
            RiskLevel.Critical -> ApprovalRiskStyle(HermesColors.RiskCritical, HermesColors.RiskHighBg, "CRITICAL")
        }
    }
}

data class InboxItemStyle(
    val marker: String,
    val foreground: Long,
) {
    companion object {
        fun forKind(kind: InboxItemKind): InboxItemStyle = when (kind) {
            InboxItemKind.Approval -> InboxItemStyle("◇", HermesColors.Warning)
            InboxItemKind.Running -> InboxItemStyle("◌", HermesColors.Blue)
            InboxItemKind.Error -> InboxItemStyle("✕", HermesColors.Error)
            InboxItemKind.Result -> InboxItemStyle("◼", HermesColors.TextTertiary)
        }
    }
}

fun sectionHeaderLabel(title: String, count: Int?): String = buildString {
    append(title.uppercase())
    if (count != null) {
        append(' ')
        append(count)
    }
}
