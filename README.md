# Hermes Mobile

Hermes Mobile is a local-first mobile control surface for Hermes Agent.

It is not a chatbot UI and not a mobile runtime for running agents on-device. The app connects to a user's existing Hermes Gateway and provides a mobile-native way to:

- send goals to Hermes
- review sessions and execution timelines
- approve or deny sensitive actions
- manage automations
- inspect generated artifacts

## Direction

- **Mobile stack:** Kotlin Multiplatform (KMP), Ktor Client, kotlinx.serialization, SQLDelight, secure platform storage, Compose Multiplatform for MVP UI.
- **Connection model:** direct connection to the user's Hermes Gateway over Tailscale/LAN/VPS URL.
- **Backend model:** add a structured `/mobile/v1/*` adapter/API to Hermes Gateway instead of mimicking Telegram-style text messages.
- **Core UX:** Inbox → Approval Detail → Session Timeline → Command Bar.
- **Design language:** match Hermes Desktop — light-first, compact, low-contrast blue-gray surfaces, section-based lists, no chat bubbles.

## Docs

- [`docs/mobile-product-spec.md`](docs/mobile-product-spec.md) — product scope, user flows, feature boundaries.
- [`docs/mobile-api-contract.md`](docs/mobile-api-contract.md) — mobile gateway API and event schemas.
- [`docs/mobile-ui-spec.md`](docs/mobile-ui-spec.md) — desktop-consistent mobile UI design system.
- [`docs/mvp-task-breakdown.md`](docs/mvp-task-breakdown.md) — implementation plan and milestones.

## Current Skeleton

This repo now contains:

- `backend_plugin/hermes_mobile/` — FastAPI mock Mobile Gateway adapter with status, approvals, approval decisions, session timeline, and WebSocket event stream.
- `shared/` — initial KMP shared module skeleton with serializable models, Ktor API client, repositories, and Hermes design token state.
- `tests/` — pytest coverage for the mock gateway API.

Run KMP shared build/tests:

```bash
./gradlew :shared:build --no-daemon
```

Run backend tests:

```bash
python3 -m pytest tests/test_mobile_gateway_mock.py -q
```

Run mock gateway:

```bash
python3 -m uvicorn backend_plugin.hermes_mobile.server:app --host 127.0.0.1 --port 8765
```

## MVP

The first MVP proves one loop:

```text
Pair phone with Hermes Gateway
  → receive pending approval in Inbox
  → inspect structured approval details
  → approve/deny from phone
  → Hermes continues execution
  → session timeline updates live
```

This is the atomic value of Hermes Mobile.
