# Hermes Mobile Gateway Mock

Run the mock API used by the KMP MVP while the real Hermes Gateway adapter is being built.

```bash
python3 -m uvicorn backend_plugin.hermes_mobile.server:app --reload --port 8765
```

Smoke checks:

```bash
curl http://127.0.0.1:8765/mobile/v1/status
curl 'http://127.0.0.1:8765/mobile/v1/approvals?status=pending'
curl http://127.0.0.1:8765/mobile/v1/sessions/sess_mock_contribution/timeline
```
