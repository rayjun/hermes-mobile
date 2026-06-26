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

- `backend_plugin/hermes_mobile/` — FastAPI Mobile Gateway adapter with mock mode plus read-only Hermes `state.db` mode for real session listing/timeline inspection; includes status, pairing start/complete, bearer-token protection and optional HMAC `HermesDevice` signed auth for mobile resource/action endpoints, device list/revoke, approval audit entries, approvals, approval decisions, goal/session creation, session timeline, artifacts, read-only cron jobs, and WebSocket event stream including session timeline updates. Optional `HERMES_MOBILE_USE_LIVE_APPROVALS=1` bridges the oldest blocking in-process Hermes `tools.approval` request per session into the mobile approvals API.
- `shared/` — initial KMP shared module skeleton with serializable models, Ktor API client, WebSocket event stream, repositories, Compose runtime theme tokens, Inbox reducer state, Approval card state, approval action controller, pairing controller, goal/session controller, session detail controller, sessions loader, artifacts loader, cron jobs loader, live session event reducer, and shared Compose components for section headers, inbox rows, approval cards, approve/deny actions, and editable command bar.
- `apps/androidApp/` — Android Compose shell rendering the Desktop-consistent Inbox using `/mobile/v1` gateway data with an offline sample fallback, approve/deny actions, Sessions, Artifacts, and Cron tabs backed by `/mobile/v1/sessions`, `/mobile/v1/artifacts`, and `/mobile/v1/cron/jobs`, read-only Cron job details, a Settings tab for saving the Gateway URL and pairing a device, and a `Start with a goal` command bar that opens a session detail timeline, continues the same session on follow-up goals, and applies live WebSocket timeline updates.
- `apps/iosApp/` — installable SwiftUI iOS shell with Gateway and Pairing settings placeholders. The Xcode target embeds the KMP `shared` framework via `:shared:embedAndSignAppleFrameworkForXcode`; full iOS networking/pairing wiring is the next slice.
- `tests/` — pytest coverage for the mock gateway API.

Run KMP shared build/tests:

```bash
./gradlew :shared:build --no-daemon
```

Run Android debug build:

```bash
# Requires local.properties with sdk.dir=/path/to/android-sdk or ANDROID_HOME set
./gradlew :apps:androidApp:assembleDebug --no-daemon
```

Run and install the iOS shell on macOS:

```bash
open apps/iosApp/iosApp.xcodeproj
# In Xcode: select the iosApp scheme, choose a simulator or connected iPhone,
# set your Team under Signing & Capabilities for a physical device, then Run.
```

The iOS target runs this build phase before compiling the app:

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

This embeds the KMP `shared` framework for the selected simulator/device SDK. Real iOS device installation requires Xcode/macOS and Apple signing; Linux verification can only check the Gradle shared metadata/JVM build and project files.

The Android debug shell connects to the mock gateway at `http://10.0.2.2:8765` when run in an emulator. Start the local mock gateway first:

```bash
python3 -m uvicorn backend_plugin.hermes_mobile.server:app --host 127.0.0.1 --port 8765
```

If the gateway is unavailable, the app renders a sample approval fallback instead of a blank screen.

Configure the mobile Gateway URL from the Android Settings tab. The URL is saved alongside the paired device id/token in Android encrypted shared preferences and can point at emulator, LAN, Tailscale, or VPS endpoints. Use `Test connection` before saving to probe `GET /mobile/v1/status` and show Online/Offline/Invalid feedback. Changing the Gateway URL clears the paired device token so a token is not reused against another host:

```text
http://10.0.2.2:8765
http://192.168.1.10:8765
http://100.x.y.z:8765
https://your-vps.example
```

Run backend tests:

```bash
python3 -m pytest tests/test_mobile_gateway_mock.py tests/test_state_db_mobile_store.py -q
```

Run mock gateway:

```bash
python3 -m uvicorn backend_plugin.hermes_mobile.server:app --host 127.0.0.1 --port 8765
```

Run the read-only Hermes state DB adapter against a local profile:

```bash
HERMES_MOBILE_STATE_DB=$HOME/.hermes/state.db \
  python3 -m uvicorn backend_plugin.hermes_mobile.server:app --host 127.0.0.1 --port 8765

PAIR=$(curl -s -X POST http://127.0.0.1:8765/mobile/v1/pair/start)
CODE=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["code"])' <<<"$PAIR")
curl -s -X POST http://127.0.0.1:8765/mobile/v1/pair/complete \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"$CODE\",\"device_name\":\"Ray Android\",\"platform\":\"android\"}"
# Then call /mobile/v1/sessions with the token returned by pair/complete.
```

This mode currently exposes real session summaries and timelines from `state.db` after pairing/auth. Starting/appending real Hermes sessions and approval control remain mock-mode or future runtime integration work.

Run with the experimental live approval bridge enabled:

```bash
PYTHONPATH=$HOME/projects/hermes-agent:$PYTHONPATH \
HERMES_MOBILE_USE_LIVE_APPROVALS=1 \
  python3 -m uvicorn backend_plugin.hermes_mobile.server:app --host 127.0.0.1 --port 8765
```

The bridge only sees in-process/importable Hermes Gateway approval queues. It does not persist approvals to disk and does not infer approvals from `state.db`. Mobile approve resolves a live request as one-time approval; mobile deny resolves it as deny.

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
