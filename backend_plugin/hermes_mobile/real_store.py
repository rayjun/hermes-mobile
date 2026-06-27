from __future__ import annotations

import json
import sqlite3
from datetime import UTC, datetime
from pathlib import Path
from secrets import randbelow, token_urlsafe
from typing import Any

from .models import AgentInfo, AgentRequest, Approval, ApprovalStatus, Artifact, CronJob, DeviceInfo, PairingCodeExpired, PairingCompleteRequest, PairingCompleteResponse, PairingStartResponse, SessionSummary, SessionTimeline, TimelineItem, ToolCall, expires_in


class StateDbMobileStore:
    def __init__(self, db_path: str | Path, agents_path: str | Path | None = None) -> None:
        self.db_path = Path(db_path)
        self.agents_path = Path(agents_path) if agents_path else Path.home() / ".hermes" / "mobile_agents.json"
        self.pending_pairings: dict[str, PairingStartResponse] = {}
        self.device_tokens: dict[str, DeviceInfo] = {}
        self.approval_audit_log: list[dict[str, object]] = []
        now = datetime.now(UTC)
        self.agents: dict[str, AgentInfo] = {
            "agent_vps": AgentInfo(
                id="agent_vps",
                name="VPS Hermes",
                base_url="http://127.0.0.1:8765",
                status="online",
                profile="default",
                model="gpt-5.5",
                created_at=now,
                last_seen_at=now,
            )
        }
        self._load_agents()

    def start_pairing(self) -> PairingStartResponse:
        pairing_id = f"pair_{token_urlsafe(8)}"
        code = f"{randbelow(1_000_000):06d}"
        while code in self.pending_pairings:
            code = f"{randbelow(1_000_000):06d}"
        response = PairingStartResponse(
            pairing_id=pairing_id,
            code=code,
            expires_at=expires_in(10),
            qr_payload=f"hermes://pair?url=http://127.0.0.1:8765&code={code}&fingerprint=state-db",
        )
        self.pending_pairings[code] = response
        return response

    def complete_pairing(self, request: PairingCompleteRequest) -> PairingCompleteResponse | None:
        pairing = self.pending_pairings.pop(request.code, None)
        if not pairing:
            return None
        if pairing.expires_at <= datetime.now(UTC):
            raise PairingCodeExpired()
        device_id = f"dev_{token_urlsafe(8)}"
        device_token = f"hmob_{token_urlsafe(32)}"
        self.device_tokens[device_token] = DeviceInfo(
            id=device_id,
            name=request.device_name,
            platform=request.platform,
            created_at=datetime.now(UTC),
        )
        return PairingCompleteResponse(
            device_id=device_id,
            device_token=device_token,
            capabilities={
                "approvals": True,
                "sessions": True,
                "cron": True,
                "artifacts": True,
                "events": True,
            },
        )

    def device_id_for_token(self, token: str) -> str | None:
        device = self.device_tokens.get(token)
        return device.id if device else None

    def device_token_for_id(self, device_id: str) -> str | None:
        for token, device in self.device_tokens.items():
            if device.id == device_id:
                return token
        return None

    def list_devices(self) -> list[DeviceInfo]:
        return sorted(self.device_tokens.values(), key=lambda device: device.created_at, reverse=True)

    def revoke_device(self, device_id: str) -> bool:
        for token, device in list(self.device_tokens.items()):
            if device.id == device_id:
                del self.device_tokens[token]
                return True
        return False

    def list_agents(self) -> list[AgentInfo]:
        return list(self.agents.values())

    def add_agent(self, request: AgentRequest) -> AgentInfo:
        agent_id = f"agent_{token_urlsafe(8)}"
        agent = AgentInfo(
            id=agent_id,
            name=request.name,
            base_url=request.base_url.rstrip("/"),
            status="offline",
            created_at=datetime.now(UTC),
        )
        self.agents[agent_id] = agent
        self._persist_agents()
        return agent

    def remove_agent(self, agent_id: str) -> bool:
        if agent_id == "agent_vps":
            return False
        removed = self.agents.pop(agent_id, None) is not None
        if removed:
            self._persist_agents()
        return removed

    def _load_agents(self) -> None:
        if not self.agents_path.exists():
            return
        try:
            data = json.loads(self.agents_path.read_text())
        except (OSError, json.JSONDecodeError):
            return
        for item in data.get("agents", []):
            try:
                agent = AgentInfo.model_validate(item)
            except Exception:
                continue
            if agent.id != "agent_vps":
                self.agents[agent.id] = agent

    def _persist_agents(self) -> None:
        self.agents_path.parent.mkdir(parents=True, exist_ok=True)
        managed = [agent.model_dump(mode="json") for agent in self.agents.values() if agent.id != "agent_vps"]
        self.agents_path.write_text(json.dumps({"agents": managed}, indent=2, ensure_ascii=False))

    def record_approval_audit(self, approval_id: str, device_id: str, decision: ApprovalStatus, comment: str | None) -> None:
        self.approval_audit_log.append(
            {
                "approval_id": approval_id,
                "device_id": device_id,
                "decision": decision.value,
                "comment": comment,
                "created_at": datetime.now(UTC),
            }
        )

    def list_sessions(self, limit: int = 50) -> list[SessionSummary]:
        with self._connect() as con:
            rows = con.execute(
                """
                SELECT id, title, started_at, ended_at
                FROM sessions
                WHERE archived = 0
                ORDER BY COALESCE(ended_at, started_at) DESC
                LIMIT ?
                """,
                (limit,),
            ).fetchall()
        return [self._session_summary(row) for row in rows]

    def get_timeline(self, session_id: str) -> SessionTimeline | None:
        with self._connect() as con:
            session = con.execute(
                "SELECT id, title, started_at, ended_at FROM sessions WHERE id = ? AND archived = 0",
                (session_id,),
            ).fetchone()
            if not session:
                return None
            messages = con.execute(
                """
                SELECT id, role, content, tool_calls, tool_name, timestamp
                FROM messages
                WHERE session_id = ? AND active = 1
                ORDER BY id ASC
                """,
                (session_id,),
            ).fetchall()
        title = session["title"] or session_id
        return SessionTimeline(
            session_id=session_id,
            title=title,
            items=[item for row in messages if (item := self._timeline_item(row)) is not None],
        )

    def list_approvals(self, status: str | None = None) -> list[Approval]:
        return []

    def list_artifacts(self, limit: int = 50) -> list[Artifact]:
        return []

    def list_cron_jobs(self, limit: int = 50) -> list[CronJob]:
        return []

    def get_cron_job(self, job_id: str) -> CronJob | None:
        return None

    def get_approval(self, approval_id: str) -> Approval | None:
        return None

    def resolve_approval(self, approval_id: str, status: ApprovalStatus) -> Approval | None:
        return None

    def create_session_from_goal(self, goal: str) -> tuple[SessionSummary, SessionTimeline]:
        raise NotImplementedError("Starting real Hermes sessions is not wired yet")

    def append_goal(self, session_id: str, goal: str) -> tuple[SessionSummary, SessionTimeline] | None:
        now = datetime.now(UTC).timestamp()
        with self._connect() as con:
            session = con.execute(
                "SELECT id FROM sessions WHERE id = ? AND archived = 0",
                (session_id,),
            ).fetchone()
            if not session:
                return None
            con.execute(
                """
                INSERT INTO messages(session_id, role, content, timestamp, active, compacted)
                VALUES (?, 'user', ?, ?, 1, 0)
                """,
                (session_id, goal, now),
            )
            con.execute(
                """
                UPDATE sessions
                SET ended_at = NULL,
                    end_reason = NULL,
                    message_count = COALESCE(message_count, 0) + 1
                WHERE id = ?
                """,
                (session_id,),
            )
            con.commit()
        timeline = self.get_timeline(session_id)
        if not timeline:
            return None
        with self._connect() as con:
            row = con.execute(
                "SELECT id, title, started_at, ended_at FROM sessions WHERE id = ? AND archived = 0",
                (session_id,),
            ).fetchone()
        return self._session_summary(row), timeline

    def _connect(self) -> sqlite3.Connection:
        con = sqlite3.connect(self.db_path)
        con.row_factory = sqlite3.Row
        return con

    def _session_summary(self, row: sqlite3.Row) -> SessionSummary:
        updated_at = row["ended_at"] or row["started_at"]
        return SessionSummary(
            id=row["id"],
            title=row["title"] or row["id"],
            status="completed" if row["ended_at"] else "running",
            created_at=self._dt(row["started_at"]),
            updated_at=self._dt(updated_at),
        )

    def _timeline_item(self, row: sqlite3.Row) -> TimelineItem | None:
        role = row["role"]
        content = row["content"] or ""
        created_at = self._dt(row["timestamp"])
        if role == "user":
            return TimelineItem(type="user_goal", id=f"msg_{row['id']}", text=content, created_at=created_at)
        tool_calls = self._tool_calls(row)
        if tool_calls:
            return TimelineItem(
                type="thinking_block",
                id=f"think_{row['id']}",
                title=content.strip() or "Tool calls",
                tool_calls=tool_calls,
                created_at=created_at,
            )
        if role == "assistant" and content.strip():
            return TimelineItem(
                type="assistant_result",
                id=f"msg_{row['id']}",
                markdown=content,
                created_at=created_at,
            )
        return None

    def _tool_calls(self, row: sqlite3.Row) -> list[ToolCall]:
        raw_calls = row["tool_calls"]
        if raw_calls:
            try:
                parsed = json.loads(raw_calls)
            except json.JSONDecodeError:
                parsed = []
            return [self._tool_call(call, index) for index, call in enumerate(parsed, start=1)]
        tool_name = row["tool_name"]
        if tool_name:
            return [
                ToolCall(
                    id=f"tool_{row['id']}",
                    name=tool_name,
                    summary=f"Ran {tool_name}",
                    status="completed",
                )
            ]
        return []

    def _tool_call(self, call: dict[str, Any], index: int) -> ToolCall:
        function = call.get("function") or {}
        name = function.get("name") or call.get("name") or "tool"
        return ToolCall(
            id=str(call.get("id") or f"tool_{index}"),
            name=name,
            summary=f"Requested {name}",
            status="completed",
        )

    def _dt(self, timestamp: float) -> datetime:
        return datetime.fromtimestamp(float(timestamp), UTC)
