package com.hermes.mobile.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.mobile.api.HermesApi
import com.hermes.mobile.models.Approval
import com.hermes.mobile.ui.ApprovalActionController
import com.hermes.mobile.ui.GoalController
import com.hermes.mobile.ui.InboxLoadState
import com.hermes.mobile.ui.SessionDetailController
import com.hermes.mobile.ui.SessionDetailState
import com.hermes.mobile.ui.TimelineRowKind
import com.hermes.mobile.ui.InboxLoader
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
import kotlinx.coroutines.launch

private const val DefaultGatewayBaseUrl = "http://10.0.2.2:8765"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HermesTheme {
                var state by remember { mutableStateOf<InboxLoadState>(InboxLoadState.Loading) }
                var commandText by remember { mutableStateOf("") }
                var sessionDetail by remember { mutableStateOf<SessionDetailState?>(null) }
                val api = remember { HermesApi(DefaultGatewayBaseUrl) }
                val loader = remember { InboxLoader(api) }
                val actionController = remember { ApprovalActionController(api) }
                val goalController = remember { GoalController(api) }
                val sessionController = remember { SessionDetailController(goalController) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) {
                    state = loader.load()
                }
                HermesMobileApp(
                    state = state,
                    sessionDetail = sessionDetail,
                    commandText = commandText,
                    onCommandTextChange = { commandText = it },
                    onSendGoal = {
                        val goal = commandText
                        scope.launch {
                            sessionDetail = sessionController.submitGoal(sessionDetail, goal)
                            commandText = ""
                            state = loader.load()
                        }
                    },
                    onBackToInbox = { sessionDetail = null },
                    onApprove = { approval ->
                        scope.launch {
                            actionController.approve(approval, biometricVerified = true)
                            state = loader.load()
                        }
                    },
                    onDeny = { approval ->
                        scope.launch {
                            actionController.deny(approval, reason = "Denied from mobile")
                            state = loader.load()
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun HermesMobileApp(
    state: InboxLoadState,
    sessionDetail: SessionDetailState?,
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    onSendGoal: () -> Unit,
    onBackToInbox: () -> Unit,
    onApprove: (Approval) -> Unit,
    onDeny: (Approval) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color(HermesColors.Background))
            .padding(top = 20.dp),
    ) {
        val detail = sessionDetail
        if (detail != null) {
            SessionDetailScreen(
                detail = detail,
                commandText = commandText,
                onCommandTextChange = onCommandTextChange,
                onSendGoal = onSendGoal,
                onBackToInbox = onBackToInbox,
            )
        } else when (state) {
            InboxLoadState.Loading -> LoadingInbox(commandText, onCommandTextChange, onSendGoal)
            is InboxLoadState.Ready -> ReadyInbox(
                state = state,
                commandText = commandText,
                onCommandTextChange = onCommandTextChange,
                onSendGoal = onSendGoal,
                onBackToInbox = onBackToInbox,
                onApprove = onApprove,
                onDeny = onDeny,
            )
        }
    }
}

@Composable
private fun ColumnScope.LoadingInbox(
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    onSendGoal: () -> Unit,
) {
    TopBar(nodeName = "Hermes", nodeStatus = "loading")
    HermesSectionHeader(SectionHeaderState("INBOX"))
    BasicText(
        text = "Connecting to Hermes Gateway…",
        style = TextStyle(color = color(HermesColors.TextSecondary), fontSize = 13.sp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    )
    Spacer(Modifier.weight(1f))
    HermesCommandBar(
        state = CommandBarState(text = commandText, canSend = commandText.isNotBlank()),
        onTextChange = onCommandTextChange,
        onSend = onSendGoal,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ColumnScope.ReadyInbox(
    state: InboxLoadState.Ready,
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    onSendGoal: () -> Unit,
    onBackToInbox: () -> Unit,
    onApprove: (Approval) -> Unit,
    onDeny: (Approval) -> Unit,
) {
    TopBar(nodeName = state.nodeName, nodeStatus = state.nodeStatus)
    state.notice?.let { notice ->
        BasicText(
            text = notice,
            style = TextStyle(color = color(HermesColors.Warning), fontSize = 11.sp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }

    state.inbox.sections.forEach { section ->
        HermesSectionHeader(SectionHeaderState(section.title, section.items.size))
        if (section.title == "APPROVALS") {
            state.approvalCards.forEach { card ->
                val approval = state.approvals.firstOrNull { it.id == card.id }
                HermesApprovalCard(
                    state = card,
                    onApprove = approval?.let { { onApprove(it) } },
                    onDeny = approval?.let { { onDeny(it) } },
                )
            }
        } else {
            section.items.forEach { item -> HermesInboxItem(item) }
        }
    }

    Spacer(Modifier.weight(1f))
    HermesCommandBar(
        state = CommandBarState(text = commandText, canSend = commandText.isNotBlank()),
        onTextChange = onCommandTextChange,
        onSend = onSendGoal,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ColumnScope.SessionDetailScreen(
    detail: SessionDetailState,
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    onSendGoal: () -> Unit,
    onBackToInbox: () -> Unit,
) {
    BasicText(
        text = "‹ Inbox                         ${detail.timeline.title}",
        style = TextStyle(
            color = color(HermesColors.TextPrimary),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(onClick = onBackToInbox),
    )
    HermesSectionHeader(SectionHeaderState("SESSION", detail.timeline.rows.size))
    detail.timeline.rows.forEach { row ->
        HermesInboxItem(
            InboxItemState(
                title = row.title,
                subtitle = row.subtitle,
                kind = when (row.kind) {
                    TimelineRowKind.UserGoal -> InboxItemKind.Running
                    TimelineRowKind.Thinking -> InboxItemKind.Running
                    TimelineRowKind.Result -> InboxItemKind.Result
                },
            )
        )
    }
    Spacer(Modifier.weight(1f))
    HermesCommandBar(
        state = CommandBarState(
            placeholder = "Continue this session",
            text = commandText,
            canSend = commandText.isNotBlank(),
        ),
        onTextChange = onCommandTextChange,
        onSend = onSendGoal,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun TopBar(nodeName: String, nodeStatus: String) {
    val statusMarker = if (nodeStatus == "online") "●" else "○"
    BasicText(
        text = "Hermes                         $nodeName $statusMarker",
        style = TextStyle(
            color = color(HermesColors.TextPrimary),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

private fun color(argb: Long): Color = Color(argb.toULong())
