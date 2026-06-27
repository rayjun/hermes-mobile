# Hermes Mobile Server Installation

This document collects the server-side pieces needed to run Hermes Mobile against a Hermes Agent node.

Hermes Mobile is only the iOS/Android control surface. The phone connects to a Mobile Gateway API served by the machine that already runs Hermes Agent.

## What runs on the server

Install and run the FastAPI Mobile Gateway adapter from this repo:

```text
backend_plugin/hermes_mobile/server.py
```

It exposes the mobile API under:

```text
/mobile/v1/*
```

Default local port:

```text
127.0.0.1:8765
```

Typical endpoints used by the app:

- `GET /mobile/v1/status`
- `POST /mobile/v1/pair/start`
- `POST /mobile/v1/pair/complete`
- `GET /mobile/v1/agents`
- `POST /mobile/v1/agents`
- `DELETE /mobile/v1/agents/{agent_id}`
- `GET /mobile/v1/sessions`
- `GET /mobile/v1/sessions/{session_id}/timeline`
- `POST /mobile/v1/sessions`
- `POST /mobile/v1/sessions/{session_id}/goals`
- `GET /mobile/v1/approvals`
- `POST /mobile/v1/approvals/{approval_id}/decision`
- `GET /mobile/v1/cron/jobs`
- `GET /mobile/v1/artifacts`

## Server prerequisites

On the Hermes server:

- Python 3.11+
- Hermes Agent installed and already configured
- Access to the Hermes profile `state.db`, usually:

```text
~/.hermes/state.db
```

Python packages needed by the gateway:

```text
fastapi
uvicorn
pydantic
pytest          # only for tests
httpx           # only for tests / FastAPI TestClient
```

## Recommended installation

From the server checkout of this repo:

```bash
cd /path/to/hermes-mobile
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install fastapi uvicorn pydantic pytest httpx
```

Run tests before exposing the gateway:

```bash
python -m pytest tests/test_mobile_gateway_mock.py tests/test_state_db_mobile_store.py -q
```

Expected current result:

```text
38 passed, 1 warning
```

## Run modes

### 1. Mock mode

Use this for UI development without reading a real Hermes profile:

```bash
python -m uvicorn backend_plugin.hermes_mobile.server:app \
  --host 127.0.0.1 \
  --port 8765
```

Smoke check:

```bash
curl http://127.0.0.1:8765/mobile/v1/status
```

### 2. Real Hermes `state.db` mode

Use this for a server that should show real Hermes sessions and timelines.

If the default profile DB is at `~/.hermes/state.db`:

```bash
HERMES_MOBILE_USE_STATE_DB=1 \
  python -m uvicorn backend_plugin.hermes_mobile.server:app \
  --host 127.0.0.1 \
  --port 8765
```

If the DB is elsewhere:

```bash
HERMES_MOBILE_STATE_DB=/absolute/path/to/state.db \
  python -m uvicorn backend_plugin.hermes_mobile.server:app \
  --host 127.0.0.1 \
  --port 8765
```

### 3. Live approval bridge mode

Use this when the server process can import the running Hermes approval queue and should expose live approval requests to mobile.

```bash
PYTHONPATH=/path/to/hermes-agent:$PYTHONPATH \
HERMES_MOBILE_USE_STATE_DB=1 \
HERMES_MOBILE_USE_LIVE_APPROVALS=1 \
  python -m uvicorn backend_plugin.hermes_mobile.server:app \
  --host 127.0.0.1 \
  --port 8765
```

Notes:

- Live approvals are in-process/importable only.
- They are not inferred from `state.db`.
- Mobile approve resolves as one-time approval.
- Mobile deny resolves as deny.

## Pair a phone manually

The app normally performs pairing itself. For server smoke testing:

```bash
PAIR=$(curl -s -X POST http://127.0.0.1:8765/mobile/v1/pair/start)
CODE=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["code"])' <<<"$PAIR")

curl -s -X POST http://127.0.0.1:8765/mobile/v1/pair/complete \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"$CODE\",\"device_name\":\"Ray iPhone\",\"platform\":\"ios\"}"
```

The response includes:

```json
{
  "device_id": "...",
  "device_token": "..."
}
```

Use the returned token for protected endpoints:

```bash
curl http://127.0.0.1:8765/mobile/v1/sessions \
  -H 'Authorization: Bearer <device_token_from_pair_complete>'
```

## Managed agent servers persistence

The gateway stores mobile-managed agent server entries outside process memory at:

```text
~/.hermes/mobile_agents.json
```

The built-in primary server is not written to this file:

```text
agent_vps / VPS Hermes / http://127.0.0.1:8765
```

Only extra servers added from the app are persisted. Removing a server updates the JSON file.

## Network exposure

For local simulator development, keep the gateway on loopback:

```text
http://127.0.0.1:8765
```

For a physical phone, expose the gateway through one of:

- Tailscale IP / MagicDNS
- LAN IP
- HTTPS reverse proxy on a VPS

Examples for the mobile app Gateway URL:

```text
http://100.x.y.z:8765
http://192.168.1.10:8765
https://hermes.example.com
```

If binding directly for LAN/Tailscale access:

```bash
HERMES_MOBILE_USE_STATE_DB=1 \
  python -m uvicorn backend_plugin.hermes_mobile.server:app \
  --host 0.0.0.0 \
  --port 8765
```

Security guidance:

- Prefer Tailscale or a private network.
- Do not expose plain HTTP directly to the public internet.
- If using a VPS/public domain, put the service behind HTTPS and restrict access.
- Pairing tokens should be treated as device credentials.

## Optional systemd service

Example service file:

```ini
[Unit]
Description=Hermes Mobile Gateway
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=/opt/hermes-mobile
Environment=HERMES_MOBILE_USE_STATE_DB=1
ExecStart=/opt/hermes-mobile/.venv/bin/python -m uvicorn backend_plugin.hermes_mobile.server:app --host 127.0.0.1 --port 8765
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
```

Install and start:

```bash
sudo cp hermes-mobile-gateway.service /etc/systemd/system/hermes-mobile-gateway.service
sudo systemctl daemon-reload
sudo systemctl enable --now hermes-mobile-gateway
sudo systemctl status hermes-mobile-gateway
```

## Troubleshooting

Check the gateway is listening:

```bash
curl http://127.0.0.1:8765/mobile/v1/status
```

If real sessions do not appear:

1. Confirm the server was started with `HERMES_MOBILE_USE_STATE_DB=1` or `HERMES_MOBILE_STATE_DB=/path/to/state.db`.
2. Confirm the target DB exists and belongs to the intended Hermes profile.
3. Pair the device again after changing gateway host/profile.
4. Check logs from `uvicorn` or `systemctl status hermes-mobile-gateway`.

If the iPhone cannot connect:

1. Confirm the URL is reachable from the phone network.
2. Prefer Tailscale for private access.
3. If using LAN, confirm firewall rules allow TCP `8765`.
4. If using HTTPS reverse proxy, confirm the proxy forwards `/mobile/v1/*` to the uvicorn process.
