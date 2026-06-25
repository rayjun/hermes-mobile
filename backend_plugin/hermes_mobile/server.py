from __future__ import annotations

import os
from pathlib import Path
from typing import Protocol

from fastapi import FastAPI, HTTPException, Query, WebSocket

from .live_approvals import LiveApprovalMobileStore
from .models import Approval, ApprovalDecision, ApprovalStatus, Artifact, CronJob, GoalRequest, GoalResponse, SessionSummary, SessionTimeline, StatusResponse
from .real_store import StateDbMobileStore
from .storage import MockMobileStore


class MobileStore(Protocol):
    def list_sessions(self, limit: int = 50) -> list[SessionSummary]: ...
    def list_artifacts(self, limit: int = 50) -> list[Artifact]: ...
    def list_cron_jobs(self, limit: int = 50) -> list[CronJob]: ...
    def get_cron_job(self, job_id: str) -> CronJob | None: ...
    def list_approvals(self, status: str | None = None) -> list[Approval]: ...
    def get_approval(self, approval_id: str) -> Approval | None: ...
    def resolve_approval(self, approval_id: str, status: ApprovalStatus) -> Approval | None: ...
    def create_session_from_goal(self, goal: str) -> tuple[SessionSummary, SessionTimeline]: ...
    def append_goal(self, session_id: str, goal: str) -> tuple[SessionSummary, SessionTimeline] | None: ...
    def get_timeline(self, session_id: str) -> SessionTimeline | None: ...


def create_default_store() -> MobileStore:
    state_db = os.getenv("HERMES_MOBILE_STATE_DB")
    base_store: MobileStore
    if state_db:
        base_store = StateDbMobileStore(state_db)
    else:
        default_state = Path.home() / ".hermes" / "state.db"
        if os.getenv("HERMES_MOBILE_USE_STATE_DB") == "1" and default_state.exists():
            base_store = StateDbMobileStore(default_state)
        else:
            base_store = MockMobileStore()
    if os.getenv("HERMES_MOBILE_USE_LIVE_APPROVALS") == "1":
        return LiveApprovalMobileStore(base_store)
    return base_store


def create_app(store: MobileStore | None = None) -> FastAPI:
    app = FastAPI(title="Hermes Mobile Gateway Mock", version="0.1.0")
    store = store or create_default_store()

    @app.get("/mobile/v1/status", response_model=StatusResponse)
    def status() -> StatusResponse:
        return StatusResponse(
            node_id="node_mock_vps",
            node_name="VPS Hermes",
            status="online",
            gateway_ready=True,
            hermes_version="0.x.x",
            api_version="1.0",
            profile="default",
            model={"provider": "openai-codex", "model": "gpt-5.5"},
            features={
                "events": True,
                "approvals": True,
                "session_timeline": True,
                "cron": True,
                "artifacts": True,
                "push_relay": False,
            },
        )

    @app.get("/mobile/v1/approvals")
    def list_approvals(status: str | None = Query(default=None)) -> dict[str, object]:
        return {"approvals": store.list_approvals(status=status)}

    @app.get("/mobile/v1/approvals/{approval_id}")
    def get_approval(approval_id: str) -> object:
        approval = store.get_approval(approval_id)
        if not approval:
            raise HTTPException(status_code=404, detail="approval_not_found")
        return approval

    @app.post("/mobile/v1/approvals/{approval_id}/approve")
    def approve(approval_id: str, decision: ApprovalDecision) -> object:
        approval = store.resolve_approval(approval_id, ApprovalStatus.approved)
        if not approval:
            raise HTTPException(status_code=404, detail="approval_not_found")
        return approval

    @app.post("/mobile/v1/approvals/{approval_id}/deny")
    def deny(approval_id: str, decision: ApprovalDecision) -> object:
        approval = store.resolve_approval(approval_id, ApprovalStatus.denied)
        if not approval:
            raise HTTPException(status_code=404, detail="approval_not_found")
        return approval

    @app.get("/mobile/v1/sessions")
    def list_sessions() -> dict[str, object]:
        return {"sessions": store.list_sessions()}

    @app.get("/mobile/v1/artifacts")
    def list_artifacts() -> dict[str, object]:
        return {"artifacts": store.list_artifacts()}

    @app.get("/mobile/v1/cron/jobs")
    def list_cron_jobs() -> dict[str, object]:
        return {"jobs": store.list_cron_jobs()}

    @app.get("/mobile/v1/cron/jobs/{job_id}")
    def get_cron_job(job_id: str) -> object:
        job = store.get_cron_job(job_id)
        if not job:
            raise HTTPException(status_code=404, detail="cron_job_not_found")
        return job

    @app.post("/mobile/v1/sessions", response_model=GoalResponse)
    def create_session(request: GoalRequest) -> GoalResponse:
        session, timeline = store.create_session_from_goal(request.goal)
        return GoalResponse(session=session, timeline=timeline)

    @app.post("/mobile/v1/sessions/{session_id}/goals", response_model=GoalResponse)
    def append_goal(session_id: str, request: GoalRequest) -> GoalResponse:
        result = store.append_goal(session_id, request.goal)
        if not result:
            raise HTTPException(status_code=404, detail="session_not_found")
        session, timeline = result
        return GoalResponse(session=session, timeline=timeline)

    @app.get("/mobile/v1/sessions/{session_id}/timeline")
    def session_timeline(session_id: str) -> object:
        timeline = store.get_timeline(session_id)
        if not timeline:
            raise HTTPException(status_code=404, detail="session_not_found")
        return timeline

    @app.websocket("/mobile/v1/events")
    async def events(websocket: WebSocket) -> None:
        await websocket.accept()
        session_id = websocket.query_params.get("session_id")
        await websocket.send_json(
            {
                "id": "evt_mock_approval_requested",
                "type": "approval.requested",
                "session_id": "sess_mock_contribution",
                "created_at": "2026-06-24T10:00:00Z",
                "payload": {"approval_id": "appr_mock_git_push"},
            }
        )
        if session_id:
            timeline = store.get_timeline(session_id)
            if timeline:
                await websocket.send_json(
                    {
                        "id": "evt_mock_session_started",
                        "type": "session.started",
                        "session_id": session_id,
                        "created_at": "2026-06-24T10:00:01Z",
                        "payload": {"timeline": timeline.model_dump(mode="json")},
                    }
                )
                await websocket.send_json(
                    {
                        "id": "evt_mock_session_updated",
                        "type": "session.updated",
                        "session_id": session_id,
                        "created_at": "2026-06-24T10:00:02Z",
                        "payload": {"timeline": timeline.model_dump(mode="json")},
                    }
                )
        await websocket.close()

    return app


app = create_app()
