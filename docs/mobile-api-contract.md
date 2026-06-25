# Hermes Mobile API Contract

## Overview

Hermes Mobile connects to a structured Mobile API exposed by Hermes Gateway.

The app should not mimic Telegram text messages. It should consume structured resources:

- events
- approvals
- sessions
- timelines
- artifacts
- cron jobs
- node status

Base path:

```http
/mobile/v1
```

## Transport

- REST for resource CRUD and decisions.
- WebSocket for live events.
- HTTPS strongly recommended even on Tailscale/LAN.

## Authentication

MVP may start with bearer device tokens after pairing, but the target design is signed device requests.

### Headers

```http
Authorization: HermesDevice device_id="dev_...", signature="...", timestamp="...", nonce="..."
Content-Type: application/json
```

Signature payload:

```text
METHOD\nPATH\nBODY_SHA256\nTIMESTAMP\nNONCE
```

Gateway verifies:

- device exists and is not revoked;
- timestamp is within allowed skew;
- nonce is not replayed;
- signature matches the registered device public key or shared device credential.

## Pairing

### Start Pairing

```http
POST /mobile/v1/pair/start
```

Used by CLI/Gateway to create a short-lived pairing code.

Response:

```json
{
  "pairing_id": "pair_123",
  "code": "834912",
  "expires_at": "2026-06-24T10:15:00Z",
  "qr_payload": "hermes://pair?url=https://100.64.1.23:8765&code=834912&fingerprint=sha256:..."
}
```

### Complete Pairing

```http
POST /mobile/v1/pair/complete
```

Request:

```json
{
  "code": "834912",
  "device_name": "Ray's iPhone",
  "platform": "ios",
  "public_key": "base64..."
}
```

Response:

```json
{
  "device_id": "dev_123",
  "device_token": "hmob_...",
  "capabilities": {
    "chat": true,
    "approvals": true,
    "sessions": true,
    "cron": false,
    "artifacts": true
  }
}
```

## Status

```http
GET /mobile/v1/status
```

Response:

```json
{
  "node_id": "node_vps",
  "node_name": "VPS Hermes",
  "status": "online",
  "gateway_ready": true,
  "hermes_version": "0.x.x",
  "api_version": "1.0",
  "profile": "default",
  "model": {
    "provider": "openai-codex",
    "model": "gpt-5.5"
  },
  "features": {
    "events": true,
    "approvals": true,
    "session_timeline": true,
    "cron": true,
    "artifacts": true,
    "push_relay": false
  }
}
```

## Events

### WebSocket

```http
WS /mobile/v1/events
```

Client subscription:

```json
{
  "type": "subscribe",
  "topics": ["approvals", "sessions", "cron", "artifacts", "errors"]
}
```

Event envelope:

```json
{
  "id": "evt_123",
  "type": "approval.requested",
  "session_id": "sess_123",
  "created_at": "2026-06-24T10:00:00Z",
  "payload": {}
}
```

### Event Types

| Type | Purpose |
|---|---|
| `session.started` | New session created |
| `session.updated` | Title/status/metadata changed |
| `message.created` | User/assistant message appended |
| `tool.started` | Tool call started |
| `tool.finished` | Tool call completed |
| `tool.failed` | Tool call failed |
| `approval.requested` | Sensitive operation waiting for decision |
| `approval.resolved` | Approval approved/denied/expired |
| `cron.started` | Cron job run began |
| `cron.finished` | Cron job finished |
| `artifact.created` | File/artifact registered |
| `error.raised` | Error event |

## Approvals

### Approval Object

```json
{
  "id": "appr_123",
  "session_id": "sess_123",
  "kind": "terminal_command",
  "risk": "high",
  "status": "pending",
  "title": "Run git push",
  "summary": "Hermes wants to push branch fix/mobile-api.",
  "reason": "The requested change passed tests and needs to be pushed.",
  "details": {
    "command": "git push origin fix/mobile-api",
    "cwd": "/home/ubuntu/projects/hermes-agent",
    "repo": "rayjun/hermes-agent",
    "branch": "fix/mobile-api",
    "files_changed": 3,
    "tests": "128 passed"
  },
  "actions": ["approve", "deny", "ask"],
  "created_at": "2026-06-24T10:00:00Z",
  "expires_at": "2026-06-24T10:15:00Z"
}
```

### List Approvals

```http
GET /mobile/v1/approvals?status=pending
```

### Get Approval

```http
GET /mobile/v1/approvals/{approval_id}
```

### Approve

```http
POST /mobile/v1/approvals/{approval_id}/approve
```

Request:

```json
{
  "comment": "Looks good",
  "biometric_verified": true
}
```

### Deny

```http
POST /mobile/v1/approvals/{approval_id}/deny
```

Request:

```json
{
  "reason": "Do not push yet; show me the diff first."
}
```

### Ask

```http
POST /mobile/v1/approvals/{approval_id}/ask
```

Request:

```json
{
  "question": "Why is this high risk?"
}
```

## Sessions

### List Sessions

```http
GET /mobile/v1/sessions?status=running&limit=50
```

Response:

```json
{
  "sessions": [
    {
      "id": "sess_123",
      "title": "Hermes-Agent contribution #2",
      "status": "running",
      "source": "mobile",
      "summary": "Searching for a small upstream PR candidate",
      "provider": "openai-codex",
      "model": "gpt-5.5",
      "workdir": "/home/ubuntu/projects/hermes-agent",
      "created_at": "2026-06-24T09:00:00Z",
      "updated_at": "2026-06-24T09:30:00Z"
    }
  ]
}
```

### Create Session

```http
POST /mobile/v1/sessions
```

Request:

```json
{
  "goal": "Help me inspect pending Hermes approvals",
  "node_id": "node_vps",
  "options": {
    "toolsets": ["terminal", "file", "web"],
    "workdir": "/home/ubuntu/projects/hermes-agent"
  }
}
```

### Send Goal to Session

```http
POST /mobile/v1/sessions/{session_id}/messages
```

Request:

```json
{
  "text": "Continue and show me the diff before pushing.",
  "attachments": []
}
```

### Session Timeline

```http
GET /mobile/v1/sessions/{session_id}/timeline
```

Response:

```json
{
  "session_id": "sess_123",
  "title": "Hermes-Agent contribution #2",
  "items": [
    {
      "type": "user_goal",
      "id": "msg_1",
      "text": "继续找 pr",
      "created_at": "2026-06-24T09:00:00Z"
    },
    {
      "type": "thinking_block",
      "id": "think_1",
      "title": "Thinking",
      "created_at": "2026-06-24T09:00:05Z",
      "tool_calls": [
        {
          "id": "tool_1",
          "name": "search_files",
          "summary": "Searched files",
          "status": "completed",
          "duration_ms": 458
        },
        {
          "id": "tool_2",
          "name": "model_call",
          "summary": "API call failed after 3 retries",
          "status": "failed",
          "error": "Connection error"
        }
      ]
    },
    {
      "type": "assistant_result",
      "id": "msg_2",
      "markdown": "找到一个适合的小问题...",
      "created_at": "2026-06-24T09:01:00Z"
    }
  ]
}
```

## Artifacts

```http
GET /mobile/v1/artifacts?session_id=sess_123
GET /mobile/v1/artifacts/{artifact_id}
GET /mobile/v1/artifacts/{artifact_id}/download
```

Artifact:

```json
{
  "id": "art_123",
  "session_id": "sess_123",
  "kind": "file",
  "title": "mobile-api.patch",
  "summary": "Patch generated for Hermes Mobile API adapter changes.",
  "mime_type": "text/x-diff",
  "uri": "file:///home/ubuntu/projects/hermes-mobile/mobile-api.patch",
  "size_bytes": 18420,
  "metadata": {"language": "diff", "repo": "rayjun/hermes-mobile"},
  "created_at": "2026-06-24T09:10:00Z"
}
```

## Cron

```http
GET /mobile/v1/cron/jobs
GET /mobile/v1/cron/jobs/{job_id}
POST /mobile/v1/cron/jobs/{job_id}/run
POST /mobile/v1/cron/jobs/{job_id}/pause
POST /mobile/v1/cron/jobs/{job_id}/resume
PATCH /mobile/v1/cron/jobs/{job_id}
```

MVP mobile clients currently use the cron list/detail endpoints as read-only views. Mutating endpoints (`run`, `pause`, `resume`, `PATCH`) require approval/audit policy before mobile wiring.

Cron job:

```json
{
  "id": "cron_mock_morning_report",
  "name": "DeFi morning report",
  "schedule": "every hour",
  "enabled": true,
  "next_run_at": "2026-06-24T11:00:00Z",
  "last_run": {
    "status": "success",
    "summary": "Pushed memory and skills to private repo.",
    "finished_at": "2026-06-24T10:00:10Z"
  }
}
```

## Errors

Error response:

```json
{
  "error": {
    "code": "approval_expired",
    "message": "The approval request has expired.",
    "details": {}
  }
}
```

Common codes:

| Code | Meaning |
|---|---|
| `unauthorized` | Missing/invalid device auth |
| `device_revoked` | Device revoked |
| `pairing_expired` | Pairing code expired |
| `approval_not_found` | Approval does not exist |
| `approval_expired` | Approval expired |
| `session_not_found` | Session does not exist |
| `feature_unavailable` | Gateway does not support feature |
