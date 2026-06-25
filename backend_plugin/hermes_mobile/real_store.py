from __future__ import annotations

import json
import sqlite3
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from .models import Approval, ApprovalStatus, Artifact, CronJob, SessionSummary, SessionTimeline, TimelineItem, ToolCall


class StateDbMobileStore:
    def __init__(self, db_path: str | Path) -> None:
        self.db_path = Path(db_path)

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
        raise NotImplementedError("Appending to real Hermes sessions is not wired yet")

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
