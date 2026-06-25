from fastapi.testclient import TestClient

from backend_plugin.hermes_mobile.server import create_app


def test_status_endpoint_reports_mobile_api_capabilities():
    client = TestClient(create_app())

    response = client.get("/mobile/v1/status")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "online"
    assert body["gateway_ready"] is True
    assert body["api_version"] == "1.0"
    assert body["features"]["approvals"] is True
    assert body["features"]["session_timeline"] is True


def test_pending_approvals_endpoint_returns_structured_approval():
    client = TestClient(create_app())

    response = client.get("/mobile/v1/approvals?status=pending")

    assert response.status_code == 200
    approvals = response.json()["approvals"]
    assert len(approvals) == 1
    approval = approvals[0]
    assert approval["id"] == "appr_mock_git_push"
    assert approval["kind"] == "terminal_command"
    assert approval["risk"] == "high"
    assert approval["status"] == "pending"
    assert approval["details"]["repo"] == "rayjun/hermes-agent"


def test_approve_endpoint_resolves_pending_approval():
    client = TestClient(create_app())

    response = client.post(
        "/mobile/v1/approvals/appr_mock_git_push/approve",
        json={"comment": "Looks good", "biometric_verified": False},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["id"] == "appr_mock_git_push"
    assert body["status"] == "approved"


def test_create_session_from_goal_returns_structured_timeline():
    client = TestClient(create_app())

    response = client.post(
        "/mobile/v1/sessions",
        json={"goal": "Summarize pending approvals and suggest next action"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["session"]["id"].startswith("sess_goal_")
    assert body["session"]["title"] == "Summarize pending approvals and suggest next action"
    assert body["timeline"]["session_id"] == body["session"]["id"]
    assert body["timeline"]["items"][0]["type"] == "user_goal"
    assert body["timeline"]["items"][0]["text"] == "Summarize pending approvals and suggest next action"


def test_append_goal_to_existing_session_adds_execution_log_item():
    client = TestClient(create_app())

    response = client.post(
        "/mobile/v1/sessions/sess_mock_contribution/goals",
        json={"goal": "Continue with the safest next step"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["timeline"]["session_id"] == "sess_mock_contribution"
    assert body["timeline"]["items"][-1]["type"] == "thinking_block"
    assert body["timeline"]["items"][-2]["text"] == "Continue with the safest next step"


def test_session_timeline_returns_execution_log_not_chat_bubbles():
    client = TestClient(create_app())

    response = client.get("/mobile/v1/sessions/sess_mock_contribution/timeline")

    assert response.status_code == 200
    body = response.json()
    assert body["session_id"] == "sess_mock_contribution"
    item_types = [item["type"] for item in body["items"]]
    assert item_types == ["user_goal", "thinking_block", "assistant_result"]
    thinking = body["items"][1]
    assert thinking["title"] == "Thinking"
    assert thinking["tool_calls"][0]["status"] == "completed"


def test_artifacts_endpoint_returns_session_outputs():
    client = TestClient(create_app())

    response = client.get("/mobile/v1/artifacts")

    assert response.status_code == 200
    artifacts = response.json()["artifacts"]
    assert len(artifacts) == 2
    assert artifacts[0]["id"] == "art_mock_patch"
    assert artifacts[0]["session_id"] == "sess_mock_contribution"
    assert artifacts[0]["kind"] == "file"
    assert artifacts[0]["title"] == "mobile-api.patch"
    assert artifacts[0]["metadata"]["language"] == "diff"


def test_cron_jobs_endpoint_returns_read_only_automations():
    client = TestClient(create_app())

    response = client.get("/mobile/v1/cron/jobs")

    assert response.status_code == 200
    jobs = response.json()["jobs"]
    assert len(jobs) == 2
    assert jobs[0]["id"] == "cron_mock_morning_report"
    assert jobs[0]["name"] == "DeFi morning report"
    assert jobs[0]["enabled"] is True
    assert jobs[0]["last_run"]["status"] == "success"


def test_events_websocket_emits_structured_approval_event():
    client = TestClient(create_app())
    with client.websocket_connect("/mobile/v1/events") as websocket:
        event = websocket.receive_json()

    assert event["type"] == "approval.requested"
    assert event["session_id"] == "sess_mock_contribution"
    assert event["payload"]["approval_id"] == "appr_mock_git_push"


def test_events_websocket_emits_session_timeline_updates():
    client = TestClient(create_app())
    with client.websocket_connect("/mobile/v1/events?session_id=sess_mock_contribution") as websocket:
        events = [websocket.receive_json(), websocket.receive_json(), websocket.receive_json()]

    assert [event["type"] for event in events] == [
        "approval.requested",
        "session.started",
        "session.updated",
    ]
    updated = events[-1]
    assert updated["session_id"] == "sess_mock_contribution"
    assert updated["payload"]["timeline"]["items"][-1]["type"] == "assistant_result"
