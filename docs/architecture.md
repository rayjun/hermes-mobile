# Hermes Mobile Architecture

## Summary

Hermes Mobile uses a Kotlin Multiplatform app that connects directly to a structured Mobile API exposed by Hermes Gateway.

```text
KMP Mobile App
  ↕ HTTPS / WebSocket
Hermes Gateway Mobile Adapter
  ↕
Hermes Runtime
  ↕
Terminal / File / Browser / Cron / Skills / Memory / MCP
```

## Key Decisions

### KMP App

Use Kotlin Multiplatform for shared models, API client, repositories, state, and most MVP UI.

MVP UI can use Compose Multiplatform. If iOS polish becomes a priority later, keep the shared core and replace iOS UI with SwiftUI.

### Direct Gateway Connection

The app connects to the user's existing Hermes Gateway over Tailscale, LAN, or a VPS URL.

This keeps the MVP local-first and avoids official cloud relay complexity.

### Structured Mobile Adapter

Do not make the app behave like a Telegram client. Add `/mobile/v1/*` endpoints to Gateway.

The app needs structured data for:

- approvals;
- session timelines;
- tool calls;
- artifacts;
- cron jobs;
- node status.

### Approval Inbox First

The first product loop is approval:

```text
approval.requested event
  → Inbox card
  → Approval detail
  → approve/deny
  → approval.resolved event
  → session continues
```

## Backend Components

```text
hermes_mobile/
  server.py
  auth.py
  storage.py
  events.py
  models.py
  routes/
    status.py
    pair.py
    events.py
    approvals.py
    sessions.py
    artifacts.py
    cron.py
```

## Mobile Components

```text
shared/
  commonMain/
    models/
    api/
    auth/
    repositories/
    store/
    ui/
      theme/
      components/
  androidMain/
    platform/
  iosMain/
    platform/
```

## Security Model

Layers:

1. Network boundary: Tailscale/LAN/VPS HTTPS.
2. Pairing: short-lived code and device registration.
3. Device credentials: stored in Keychain/EncryptedSharedPreferences.
4. Request signing: target design for every authenticated request.
5. Audit log: every approval decision is recorded.
6. Device revoke: remove lost or old devices.

## MVP Limitations

- No official relay.
- No reliable background push on iOS without relay.
- Telegram fallback can still be used for critical notifications.
- Pairing can start manual before QR scanning is implemented.

## Future Relay Architecture

```text
Hermes Gateway
  → encrypted event metadata
  → Hermes Relay
  → APNs / FCM
  → Mobile App
```

Relay should not execute tools or see sensitive command payloads in plaintext.
