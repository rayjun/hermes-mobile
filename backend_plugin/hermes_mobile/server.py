from __future__ import annotations

from fastapi import FastAPI, HTTPException, Query, WebSocket

from .models import ApprovalDecision, ApprovalStatus, GoalRequest, GoalResponse, StatusResponse
from .storage import MockMobileStore


def create_app() -> FastAPI:
    app = FastAPI(title="Hermes Mobile Gateway Mock", version="0.1.0")
    store = MockMobileStore()

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
                "cron": False,
                "artifacts": False,
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
        await websocket.send_json(
            {
                "id": "evt_mock_approval_requested",
                "type": "approval.requested",
                "session_id": "sess_mock_contribution",
                "created_at": "2026-06-24T10:00:00Z",
                "payload": {"approval_id": "appr_mock_git_push"},
            }
        )
        await websocket.close()

    return app


app = create_app()
