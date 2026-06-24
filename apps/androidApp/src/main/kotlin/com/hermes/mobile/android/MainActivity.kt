package com.hermes.mobile.android

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.hermes.mobile.api.HermesEventStream
import com.hermes.mobile.api.defaultHttpClient
import com.hermes.mobile.models.Approval
import com.hermes.mobile.models.SessionSummary
import com.hermes.mobile.ui.ApprovalActionController
import com.hermes.mobile.ui.GatewayConnectionResult
import com.hermes.mobile.ui.GatewayConnectionState
import com.hermes.mobile.ui.GatewaySettingsController
import com.hermes.mobile.ui.GatewaySettingsError
import com.hermes.mobile.ui.GatewaySettingsState
import com.hermes.mobile.ui.GoalController
import com.hermes.mobile.ui.InboxLoadState
import com.hermes.mobile.ui.SessionDetailController
import com.hermes.mobile.ui.SessionDetailState
import com.hermes.mobile.ui.SessionLiveEventController
import com.hermes.mobile.ui.SessionsLoadState
import com.hermes.mobile.ui.SessionsLoader
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val GatewayPrefsName = "hermes_mobile_gateway"
private const val GatewayBaseUrlKey = "gateway_base_url"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HermesTheme {
                val gatewaySettingsController = remember { GatewaySettingsController() }
                val gatewayPrefs = remember { getSharedPreferences(GatewayPrefsName, Context.MODE_PRIVATE) }
                var gatewaySettings by remember {
                    mutableStateOf(gatewaySettingsController.initialState(gatewayPrefs.getString(GatewayBaseUrlKey, null)))
                }
                var gatewayInput by remember { mutableStateOf(gatewaySettings.baseUrl) }
                var state by remember { mutableStateOf<InboxLoadState>(InboxLoadState.Loading) }
                var sessionsState by remember { mutableStateOf<SessionsLoadState>(SessionsLoadState.Loading) }
                var selectedTab by remember { mutableStateOf(if (gatewaySettings.configured) MobileTab.Inbox else MobileTab.Settings) }
                var commandText by remember { mutableStateOf("") }
                var sessionDetail by remember { mutableStateOf<SessionDetailState?>(null) }
                val api = remember(gatewaySettings.baseUrl) { HermesApi(gatewaySettings.baseUrl) }
                val loader = remember(api) { InboxLoader(api) }
                val sessionsLoader = remember(api) { SessionsLoader(api) }
                val actionController = remember(api) { ApprovalActionController(api) }
                val goalController = remember(api) { GoalController(api) }
                val sessionController = remember(goalController) { SessionDetailController(goalController) }
                val eventStream = remember(gatewaySettings.baseUrl) { HermesEventStream(gatewaySettings.baseUrl, defaultHttpClient()) }
                val liveEventController = remember { SessionLiveEventController() }
                val scope = rememberCoroutineScope()
                LaunchedEffect(gatewaySettings.baseUrl) {
                    state = loader.load()
                    sessionsState = sessionsLoader.load()
                }
                LaunchedEffect(sessionDetail?.timeline?.sessionId) {
                    val activeDetail = sessionDetail ?: return@LaunchedEffect
                    runCatching {
                        eventStream.events(activeDetail.timeline.sessionId).collect { event ->
                            sessionDetail = sessionDetail?.let { liveEventController.apply(it, event) }
                        }
                    }
                }
                HermesMobileApp(
                    state = state,
                    sessionsState = sessionsState,
                    selectedTab = selectedTab,
                    gatewaySettings = gatewaySettings,
                    gatewayInput = gatewayInput,
                    sessionDetail = sessionDetail,
                    commandText = commandText,
                    onCommandTextChange = { commandText = it },
                    onSendGoal = {
                        val goal = commandText
                        scope.launch {
                            sessionDetail = sessionController.submitGoal(sessionDetail, goal)
                            selectedTab = MobileTab.Sessions
                            commandText = ""
                            state = loader.load()
                            sessionsState = sessionsLoader.load()
                        }
                    },
                    onBackToInbox = {
                        sessionDetail = null
                        selectedTab = MobileTab.Inbox
                    },
                    onSelectTab = { tab ->
                        selectedTab = tab
                        sessionDetail = null
                        if (tab == MobileTab.Sessions) {
                            scope.launch { sessionsState = sessionsLoader.load() }
                        }
                    },
                    onGatewayInputChange = { gatewayInput = it },
                    onTestGateway = {
                        gatewaySettings = gatewaySettings.copy(
                            connection = GatewayConnectionResult(GatewayConnectionState.Testing, "Testing gateway…"),
                            error = null,
                        )
                        scope.launch {
                            gatewaySettings = gatewaySettings.copy(
                                connection = gatewaySettingsController.testConnection(gatewayInput, api),
                                error = null,
                            )
                        }
                    },
                    onSaveGateway = {
                        try {
                            val next = gatewaySettingsController.save(gatewayInput)
                            gatewayPrefs.edit().putString(GatewayBaseUrlKey, next.baseUrl).apply()
                            gatewaySettings = next
                            gatewayInput = next.baseUrl
                            sessionDetail = null
                            state = InboxLoadState.Loading
                            sessionsState = SessionsLoadState.Loading
                            selectedTab = MobileTab.Inbox
                        } catch (_: GatewaySettingsError.InvalidUrl) {
                            gatewaySettings = gatewaySettings.copy(error = "Enter an http:// or https:// gateway URL")
                        }
                    },
                    onOpenSession = { session ->
                        scope.launch {
                            sessionDetail = sessionsLoader.openSession(session.id)
                            selectedTab = MobileTab.Sessions
                        }
                    },
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
    sessionsState: SessionsLoadState,
    selectedTab: MobileTab,
    gatewaySettings: GatewaySettingsState,
    gatewayInput: String,
    sessionDetail: SessionDetailState?,
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    onSendGoal: () -> Unit,
    onBackToInbox: () -> Unit,
    onSelectTab: (MobileTab) -> Unit,
    onGatewayInputChange: (String) -> Unit,
    onTestGateway: () -> Unit,
    onSaveGateway: () -> Unit,
    onOpenSession: (SessionSummary) -> Unit,
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
        } else when (selectedTab) {
            MobileTab.Inbox -> when (state) {
                InboxLoadState.Loading -> LoadingInbox(commandText, onCommandTextChange, onSendGoal)
                is InboxLoadState.Ready -> ReadyInbox(
                    state = state,
                    commandText = commandText,
                    onCommandTextChange = onCommandTextChange,
                    onSendGoal = onSendGoal,
                    onBackToInbox = onBackToInbox,
                    onSelectTab = onSelectTab,
                    onOpenSession = onOpenSession,
                    onApprove = onApprove,
                    onDeny = onDeny,
                )
            }
            MobileTab.Sessions -> SessionsScreen(
                state = sessionsState,
                selectedTab = selectedTab,
                onSelectTab = onSelectTab,
                onOpenSession = onOpenSession,
            )
            MobileTab.Settings -> SettingsScreen(
                gatewaySettings = gatewaySettings,
                gatewayInput = gatewayInput,
                selectedTab = selectedTab,
                onGatewayInputChange = onGatewayInputChange,
                onTestGateway = onTestGateway,
                onSaveGateway = onSaveGateway,
                onSelectTab = onSelectTab,
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
    onSelectTab: (MobileTab) -> Unit,
    onOpenSession: (SessionSummary) -> Unit,
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
    BottomNav(selected = MobileTab.Inbox, onSelectTab = onSelectTab)
    HermesCommandBar(
        state = CommandBarState(text = commandText, canSend = commandText.isNotBlank()),
        onTextChange = onCommandTextChange,
        onSend = onSendGoal,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ColumnScope.SessionsScreen(
    state: SessionsLoadState,
    selectedTab: MobileTab,
    onSelectTab: (MobileTab) -> Unit,
    onOpenSession: (SessionSummary) -> Unit,
) {
    TopBar(nodeName = "Hermes", nodeStatus = "online")
    HermesSectionHeader(SectionHeaderState("SESSIONS"))
    when (state) {
        SessionsLoadState.Loading -> BasicText(
            text = "Loading sessions…",
            style = TextStyle(color = color(HermesColors.TextSecondary), fontSize = 13.sp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
        is SessionsLoadState.Ready -> {
            state.notice?.let { notice ->
                BasicText(
                    text = notice,
                    style = TextStyle(color = color(HermesColors.Warning), fontSize = 11.sp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            state.sessions.forEach { session ->
                HermesInboxItem(
                    InboxItemState(
                        title = session.title,
                        subtitle = "${session.status.name.lowercase()} · ${session.updatedAt}",
                        kind = if (session.status.name == "Completed") InboxItemKind.Result else InboxItemKind.Running,
                    ),
                    modifier = Modifier.clickable { onOpenSession(session) },
                )
            }
        }
    }
    Spacer(Modifier.weight(1f))
    BottomNav(selected = selectedTab, onSelectTab = onSelectTab)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun BottomNav(
    selected: MobileTab,
    onSelectTab: (MobileTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        BasicText(
            text = if (selected == MobileTab.Inbox) "● Inbox" else "○ Inbox",
            style = TextStyle(color = color(HermesColors.TextSecondary), fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .weight(1f)
                .clickable { onSelectTab(MobileTab.Inbox) },
        )
        BasicText(
            text = if (selected == MobileTab.Sessions) "● Sessions" else "○ Sessions",
            style = TextStyle(color = color(HermesColors.TextSecondary), fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .weight(1f)
                .clickable { onSelectTab(MobileTab.Sessions) },
        )
        BasicText(
            text = if (selected == MobileTab.Settings) "● Settings" else "○ Settings",
            style = TextStyle(color = color(HermesColors.TextSecondary), fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .weight(1f)
                .clickable { onSelectTab(MobileTab.Settings) },
        )
    }
}

@Composable
private fun ColumnScope.SettingsScreen(
    gatewaySettings: GatewaySettingsState,
    gatewayInput: String,
    selectedTab: MobileTab,
    onGatewayInputChange: (String) -> Unit,
    onTestGateway: () -> Unit,
    onSaveGateway: () -> Unit,
    onSelectTab: (MobileTab) -> Unit,
) {
    TopBar(nodeName = "Gateway", nodeStatus = if (gatewaySettings.configured) "online" else "offline")
    HermesSectionHeader(SectionHeaderState("GATEWAY"))
    BasicText(
        text = "Connect Hermes Mobile to your local Gateway over Tailscale, LAN, emulator, or VPS.",
        style = TextStyle(color = color(HermesColors.TextSecondary), fontSize = 13.sp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
    BasicText(
        text = "Current: ${gatewaySettings.baseUrl}",
        style = TextStyle(color = color(HermesColors.TextPrimary), fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
    gatewaySettings.error?.let { error ->
        BasicText(
            text = error,
            style = TextStyle(color = color(HermesColors.Error), fontSize = 11.sp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
    gatewaySettings.connection?.let { connection ->
        BasicText(
            text = "${connection.state.name}: ${connection.message}",
            style = TextStyle(
                color = color(
                    when (connection.state) {
                        GatewayConnectionState.Online -> HermesColors.Success
                        GatewayConnectionState.Offline, GatewayConnectionState.Invalid -> HermesColors.Error
                        GatewayConnectionState.Testing, GatewayConnectionState.Unknown -> HermesColors.TextSecondary
                    }
                ),
                fontSize = 11.sp,
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
    HermesCommandBar(
        state = CommandBarState(
            placeholder = GatewaySettingsController.DefaultGatewayBaseUrl,
            text = gatewayInput,
            canSend = gatewayInput.isNotBlank(),
        ),
        onTextChange = onGatewayInputChange,
        onSend = onSaveGateway,
    )
    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        BasicText(
            text = "Test connection",
            style = TextStyle(color = color(HermesColors.Blue), fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .weight(1f)
                .clickable { onTestGateway() },
        )
        BasicText(
            text = "Save gateway",
            style = TextStyle(color = color(HermesColors.Blue), fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .weight(1f)
                .clickable { onSaveGateway() },
        )
    }
    BasicText(
        text = "Examples: http://10.0.2.2:8765 · http://100.x.y.z:8765 · https://your-vps.example",
        style = TextStyle(color = color(HermesColors.TextSecondary), fontSize = 11.sp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
    Spacer(Modifier.weight(1f))
    BottomNav(selected = selectedTab, onSelectTab = onSelectTab)
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

enum class MobileTab {
    Inbox,
    Sessions,
    Settings,
}

private fun color(argb: Long): Color = Color(argb.toULong())
