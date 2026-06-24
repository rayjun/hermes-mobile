package com.hermes.mobile.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.mobile.models.RiskLevel
import com.hermes.mobile.ui.ApprovalCardState
import com.hermes.mobile.ui.components.CommandBarState
import com.hermes.mobile.ui.components.HermesApprovalCard
import com.hermes.mobile.ui.components.HermesCommandBar
import com.hermes.mobile.ui.components.HermesInboxItem
import com.hermes.mobile.ui.components.HermesSectionHeader
import com.hermes.mobile.ui.components.InboxItemKind
import com.hermes.mobile.ui.components.InboxItemState
import com.hermes.mobile.ui.components.SectionHeaderState
import com.hermes.mobile.ui.theme.HermesColors
import com.hermes.mobile.ui.theme.HermesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HermesTheme {
                HermesMobileApp()
            }
        }
    }
}

@Composable
fun HermesMobileApp() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color(HermesColors.Background))
            .padding(top = 20.dp),
    ) {
        BasicText(
            text = "Hermes                         VPS ●",
            style = TextStyle(
                color = color(HermesColors.TextPrimary),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
        HermesSectionHeader(SectionHeaderState("APPROVALS", 1))
        HermesApprovalCard(sampleApprovalCard())
        HermesSectionHeader(SectionHeaderState("RUNNING", 1))
        HermesInboxItem(
            InboxItemState(
                title = "Hermes-Agent contribution #2",
                subtitle = "Thinking · searched files · 21s",
                kind = InboxItemKind.Running,
            ),
        )
        HermesSectionHeader(SectionHeaderState("RECENT", 1))
        HermesInboxItem(
            InboxItemState(
                title = "DeFi morning report delivered",
                subtitle = "telegram · success · 09:01",
                kind = InboxItemKind.Result,
            ),
        )
        Spacer(Modifier.weight(1f))
        HermesCommandBar(CommandBarState())
        Spacer(Modifier.height(6.dp))
    }
}

private fun sampleApprovalCard(): ApprovalCardState = ApprovalCardState(
    id = "appr_mock_git_push",
    title = "Run git push",
    summary = "Hermes wants to push branch fix/mobile-api.",
    risk = RiskLevel.High,
    contextRows = linkedMapOf(
        "Repo" to "rayjun/hermes-agent",
        "Branch" to "fix/mobile-api",
        "Tests" to "128 passed",
    ),
    requiresBiometric = true,
)

private fun color(argb: Long): Color = Color(argb.toULong())
