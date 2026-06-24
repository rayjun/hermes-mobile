# Hermes Mobile MVP Task Breakdown

> Implementation goal: prove the mobile approval loop using KMP + direct Hermes Gateway connection.

## Architecture

```text
KMP App
  ↕ HTTPS/WebSocket
Hermes Gateway Mobile Adapter
  ↕
Hermes Runtime / Approval Engine / Session Store
```

## Milestone 0 — Repository and Documentation

### Task 0.1: Create repository skeleton

**Objective:** Establish the project layout.

**Files:**

- `README.md`
- `docs/mobile-product-spec.md`
- `docs/mobile-api-contract.md`
- `docs/mobile-ui-spec.md`
- `docs/mvp-task-breakdown.md`

**Verification:**

```bash
find ~/projects/hermes-mobile -maxdepth 3 -type f | sort
```

Expected: all docs listed.

## Milestone 1 — Gateway Mobile Adapter Spike

### Task 1.1: Add minimal mobile status endpoint

**Objective:** Prove the app can call Hermes Gateway.

**Backend endpoint:**

```http
GET /mobile/v1/status
```

**Response:**

```json
{
  "status": "online",
  "gateway_ready": true,
  "api_version": "1.0"
}
```

**Verification:**

```bash
curl http://127.0.0.1:8765/mobile/v1/status
```

Expected: JSON status response.

### Task 1.2: Add mock pairing endpoint

**Objective:** Let app register a test device.

**Endpoints:**

```http
POST /mobile/v1/pair/start
POST /mobile/v1/pair/complete
```

**Verification:**

- `pair/start` returns code and QR payload.
- `pair/complete` returns device id and token.

### Task 1.3: Add WebSocket event stream

**Objective:** Prove app can receive live events.

**Endpoint:**

```http
WS /mobile/v1/events
```

**Verification:**

- Connect client.
- Send mock `approval.requested` event.
- Client receives event without polling.

## Milestone 2 — KMP App Skeleton

### Task 2.1: Create KMP project

**Objective:** Set up shared Kotlin code and mobile apps.

**Suggested structure:**

```text
apps/mobile/
shared/
  commonMain/
  androidMain/
  iosMain/
```

**Dependencies:**

- Ktor Client
- Ktor WebSockets
- kotlinx.serialization
- kotlinx-datetime
- SQLDelight
- Compose Multiplatform

**Verification:**

- Android app builds.
- iOS app builds or shared framework compiles.

### Task 2.2: Define shared models

**Objective:** Add serializable models matching API contract.

**Files:**

- `shared/commonMain/kotlin/.../models/Approval.kt`
- `shared/commonMain/kotlin/.../models/HermesEvent.kt`
- `shared/commonMain/kotlin/.../models/Session.kt`
- `shared/commonMain/kotlin/.../models/NodeStatus.kt`

**Verification:**

- JSON encode/decode tests pass.

### Task 2.3: Implement API client

**Objective:** Call status, approvals, approve/deny.

**Files:**

- `shared/commonMain/kotlin/.../api/HermesApi.kt`
- `shared/commonMain/kotlin/.../api/HermesWebSocket.kt`

**Verification:**

- App can fetch `/status` from local Gateway.

## Milestone 3 — Design System

### Task 3.1: Add Hermes theme tokens

**Objective:** Match Desktop visual language.

**Files:**

- `shared/commonMain/kotlin/.../ui/theme/HermesColors.kt`
- `shared/commonMain/kotlin/.../ui/theme/HermesTypography.kt`
- `shared/commonMain/kotlin/.../ui/theme/HermesTheme.kt`

**Verification:**

- Sample screen uses light ice-blue background, compact typography, hairline borders.

### Task 3.2: Add core components

**Objective:** Build reusable UI primitives.

**Components:**

- `HermesScaffold`
- `HermesSectionHeader`
- `HermesInboxItem`
- `HermesApprovalCard`
- `HermesCommandBar`
- `HermesToolCallRow`
- `HermesTimeline`

**Verification:**

- Component preview screen renders the visual language consistently.

## Milestone 4 — Pairing Flow

### Task 4.1: Pairing screen

**Objective:** Let user connect to a Gateway.

**MVP inputs:**

- manual Gateway URL;
- pairing code;
- device name.

QR scan can be P1 if needed.

**Verification:**

- User enters URL/code.
- App calls `pair/complete`.
- Device credentials are stored securely.

### Task 4.2: Secure storage abstraction

**Objective:** Store device token safely.

**KMP expect/actual:**

- iOS: Keychain.
- Android: EncryptedSharedPreferences or DataStore + encrypted key.

**Verification:**

- App restarts and remains paired.

## Milestone 5 — Approval Inbox MVP

### Task 5.1: Inbox screen

**Objective:** Show pending approvals from API.

**Endpoint:**

```http
GET /mobile/v1/approvals?status=pending
```

**Verification:**

- Mock approval appears in Inbox.
- High/medium risk styles render correctly.

### Task 5.2: Live approval events

**Objective:** Update Inbox from WebSocket.

**Event:**

```json
{
  "type": "approval.requested",
  "payload": { "id": "appr_123" }
}
```

**Verification:**

- New approval appears without refresh.

### Task 5.3: Approval detail screen

**Objective:** Show structured approval details.

**Verification:**

- Shows title, risk, summary, context fields, command, reason.
- Raw command is expandable.

### Task 5.4: Approve/Deny actions

**Objective:** Send decisions to Gateway.

**Endpoints:**

```http
POST /mobile/v1/approvals/{id}/approve
POST /mobile/v1/approvals/{id}/deny
```

**Verification:**

- Approve updates approval status.
- Deny requires optional reason.
- Inbox removes resolved approval.

## Milestone 6 — Session Timeline MVP

### Task 6.1: Session list

**Objective:** Display recent sessions.

**Endpoint:**

```http
GET /mobile/v1/sessions
```

**Verification:**

- Recent sessions are listed by updated time.

### Task 6.2: Session timeline screen

**Objective:** Render structured execution transcript.

**Endpoint:**

```http
GET /mobile/v1/sessions/{id}/timeline
```

**Verification:**

- User goals render as plain text blocks.
- Thinking blocks render tool timelines.
- Errors render inline red.
- Assistant results render markdown.

### Task 6.3: Command bar follow-up

**Objective:** Send follow-up goal to a session.

**Endpoint:**

```http
POST /mobile/v1/sessions/{id}/messages
```

**Verification:**

- User submits follow-up.
- Gateway receives text.
- Timeline updates.

## Milestone 7 — Integration Demo

### Task 7.1: End-to-end demo script

**Objective:** Demonstrate the atomic product loop.

**Flow:**

```text
Start Gateway mobile adapter
Pair app
Create mock or real approval
Receive approval in Inbox
Open Approval Detail
Approve from phone
Gateway resolves approval
Session timeline receives update
Send follow-up from command bar
```

**Verification:**

- Record screen capture or write demo notes.
- No manual database edits needed during demo.

## P1 After MVP

- Cron list/detail.
- Artifact list/preview.
- QR scanner.
- Biometric approval.
- Device revoke.
- Audit log.
- Share sheet.

## P2 Later

- Push relay.
- Multi-node switcher.
- Approval policies.
- Full cron edit.
- Voice input.
- Native iOS SwiftUI shell if Compose iOS quality is insufficient.
