package com.hermes.mobile.ui.components

import com.hermes.mobile.models.RiskLevel
import com.hermes.mobile.ui.theme.HermesColors
import kotlin.test.Test
import kotlin.test.assertEquals

class ComponentStyleTest {
    @Test
    fun mapsRiskLevelsToDesktopConsistentMutedColors() {
        assertEquals(HermesColors.RiskLow, ApprovalRiskStyle.forRisk(RiskLevel.Low).foreground)
        assertEquals(HermesColors.RiskMedium, ApprovalRiskStyle.forRisk(RiskLevel.Medium).foreground)
        assertEquals(HermesColors.RiskHigh, ApprovalRiskStyle.forRisk(RiskLevel.High).foreground)
        assertEquals(HermesColors.RiskCritical, ApprovalRiskStyle.forRisk(RiskLevel.Critical).foreground)
    }

    @Test
    fun formatsSectionHeaderLabelWithCount() {
        assertEquals("APPROVALS 2", sectionHeaderLabel("approvals", 2))
        assertEquals("RECENT", sectionHeaderLabel("recent", null))
    }

    @Test
    fun mapsInboxItemKindToStableStatusMarker() {
        assertEquals("◇", InboxItemStyle.forKind(InboxItemKind.Approval).marker)
        assertEquals("◌", InboxItemStyle.forKind(InboxItemKind.Running).marker)
        assertEquals("✕", InboxItemStyle.forKind(InboxItemKind.Error).marker)
        assertEquals("◼", InboxItemStyle.forKind(InboxItemKind.Result).marker)
    }
}
