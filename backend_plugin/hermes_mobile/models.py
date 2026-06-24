from __future__ import annotations

from datetime import UTC, datetime, timedelta
from enum import Enum
from typing import Any, Literal

from pydantic import BaseModel, Field


class RiskLevel(str, Enum):
    low = "low"
    medium = "medium"
    high = "high"
    critical = "critical"


class ApprovalStatus(str, Enum):
    pending = "pending"
    approved = "approved"
    denied = "denied"
    expired = "expired"
    cancelled = "cancelled"


class Approval(BaseModel):
    id: str
    session_id: str | None = None
    kind: str
    risk: RiskLevel
    status: ApprovalStatus
    title: str
    summary: str
    reason: str
    details: dict[str, Any]
    actions: list[str] = Field(default_factory=lambda: ["approve", "deny", "ask"])
    created_at: datetime
    expires_at: datetime | None = None


class ApprovalDecision(BaseModel):
    comment: str | None = None
    reason: str | None = None
    biometric_verified: bool = False


class StatusResponse(BaseModel):
    node_id: str
    node_name: str
    status: Literal["online", "offline"]
    gateway_ready: bool
    hermes_version: str
    api_version: str
    profile: str
    model: dict[str, str]
    features: dict[str, bool]


class ToolCall(BaseModel):
    id: str
    name: str
    summary: str
    status: Literal["running", "completed", "failed", "waiting_approval", "skipped"]
    duration_ms: int | None = None
    error: str | None = None


class TimelineItem(BaseModel):
    type: Literal["user_goal", "thinking_block", "assistant_result"]
    id: str
    created_at: datetime
    text: str | None = None
    title: str | None = None
    tool_calls: list[ToolCall] | None = None
    markdown: str | None = None


class SessionTimeline(BaseModel):
    session_id: str
    title: str
    items: list[TimelineItem]


def now_utc() -> datetime:
    return datetime.now(UTC)


def expires_in(minutes: int) -> datetime:
    return now_utc() + timedelta(minutes=minutes)
