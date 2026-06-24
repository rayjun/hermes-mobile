from __future__ import annotations

from copy import deepcopy

from .models import (
    Approval,
    ApprovalStatus,
    RiskLevel,
    SessionSummary,
    SessionTimeline,
    TimelineItem,
    ToolCall,
    expires_in,
    now_utc,
)


class MockMobileStore:
    def __init__(self) -> None:
        created_at = now_utc()
        self.approvals: dict[str, Approval] = {
            "appr_mock_git_push": Approval(
                id="appr_mock_git_push",
                session_id="sess_mock_contribution",
                kind="terminal_command",
                risk=RiskLevel.high,
                status=ApprovalStatus.pending,
                title="Run git push",
                summary="Hermes wants to push branch fix/mobile-api.",
                reason="The requested change passed tests and needs to be pushed for review.",
                details={
                    "command": "git push origin fix/mobile-api",
                    "cwd": "/home/ubuntu/projects/hermes-agent",
                    "repo": "rayjun/hermes-agent",
                    "branch": "fix/mobile-api",
                    "files_changed": 3,
                    "tests": "128 passed",
                },
                created_at=created_at,
                expires_at=expires_in(15),
            )
        }
        self.sessions: dict[str, SessionSummary] = {
            "sess_mock_contribution": SessionSummary(
                id="sess_mock_contribution",
                title="Hermes-Agent contribution #2",
                status="waiting_approval",
                created_at=created_at,
                updated_at=created_at,
            )
        }
        self.timelines: dict[str, SessionTimeline] = {
            "sess_mock_contribution": SessionTimeline(
                session_id="sess_mock_contribution",
                title="Hermes-Agent contribution #2",
                items=[
                    TimelineItem(
                        type="user_goal",
                        id="msg_1",
                        text="继续找 pr",
                        created_at=created_at,
                    ),
                    TimelineItem(
                        type="thinking_block",
                        id="think_1",
                        title="Thinking",
                        created_at=created_at,
                        tool_calls=[
                            ToolCall(
                                id="tool_1",
                                name="search_files",
                                summary="Searched files",
                                status="completed",
                                duration_ms=458,
                            ),
                            ToolCall(
                                id="tool_2",
                                name="read_file",
                                summary="Read hermes_cli/main.py",
                                status="completed",
                                duration_ms=118,
                            ),
                            ToolCall(
                                id="tool_3",
                                name="model_call",
                                summary="API call failed after 3 retries",
                                status="failed",
                                error="Connection error",
                            ),
                        ],
                    ),
                    TimelineItem(
                        type="assistant_result",
                        id="msg_2",
                        markdown="找到一个适合的小问题，可以继续分析并准备最小 diff。",
                        created_at=created_at,
                    ),
                ],
            )
        }

    def list_approvals(self, status: str | None = None) -> list[Approval]:
        approvals = list(self.approvals.values())
        if status:
            approvals = [approval for approval in approvals if approval.status == status]
        return deepcopy(approvals)

    def get_approval(self, approval_id: str) -> Approval | None:
        approval = self.approvals.get(approval_id)
        return deepcopy(approval) if approval else None

    def resolve_approval(self, approval_id: str, status: ApprovalStatus) -> Approval | None:
        approval = self.approvals.get(approval_id)
        if not approval:
            return None
        approval.status = status
        return deepcopy(approval)

    def get_timeline(self, session_id: str) -> SessionTimeline | None:
        timeline = self.timelines.get(session_id)
        return deepcopy(timeline) if timeline else None

    def create_session_from_goal(self, goal: str) -> tuple[SessionSummary, SessionTimeline]:
        created_at = now_utc()
        session_id = f"sess_goal_{len(self.sessions) + 1}"
        session = SessionSummary(
            id=session_id,
            title=goal,
            status="running",
            created_at=created_at,
            updated_at=created_at,
        )
        timeline = SessionTimeline(
            session_id=session_id,
            title=goal,
            items=[
                TimelineItem(
                    type="user_goal",
                    id=f"{session_id}_goal_1",
                    text=goal,
                    created_at=created_at,
                ),
                self._thinking_item(session_id=session_id, index=1, created_at=created_at),
            ],
        )
        self.sessions[session_id] = session
        self.timelines[session_id] = timeline
        return deepcopy(session), deepcopy(timeline)

    def append_goal(self, session_id: str, goal: str) -> tuple[SessionSummary, SessionTimeline] | None:
        session = self.sessions.get(session_id)
        timeline = self.timelines.get(session_id)
        if not session or not timeline:
            return None
        created_at = now_utc()
        next_index = len(timeline.items) + 1
        timeline.items.append(
            TimelineItem(
                type="user_goal",
                id=f"{session_id}_goal_{next_index}",
                text=goal,
                created_at=created_at,
            )
        )
        timeline.items.append(self._thinking_item(session_id=session_id, index=next_index + 1, created_at=created_at))
        session.status = "running"
        session.updated_at = created_at
        return deepcopy(session), deepcopy(timeline)

    def _thinking_item(self, session_id: str, index: int, created_at) -> TimelineItem:
        return TimelineItem(
            type="thinking_block",
            id=f"{session_id}_think_{index}",
            title="Queued",
            created_at=created_at,
            tool_calls=[
                ToolCall(
                    id=f"{session_id}_tool_{index}",
                    name="mobile_goal",
                    summary="Accepted goal from mobile command bar",
                    status="running",
                )
            ],
        )
