# KMP Implementation Notes

## Recommended Stack

| Area | Choice |
|---|---|
| Shared language | Kotlin Multiplatform |
| HTTP | Ktor Client |
| WebSocket | Ktor WebSockets |
| JSON | kotlinx.serialization |
| Date/time | kotlinx-datetime |
| Local cache | SQLDelight |
| Secure storage | expect/actual platform abstraction |
| MVP UI | Compose Multiplatform |
| Android platform UI | Compose |
| iOS future option | SwiftUI shell over shared core |

## Shared Module Layout

```text
shared/commonMain/kotlin/com/hermes/mobile/
  models/
  api/
  auth/
  repositories/
  store/
  ui/
    theme/
    components/
```

## Platform Bridges

```text
SecureStorage
Biometrics
QrScanner
Notifications
ShareReceiver
OpenExternalFile
```

Use `expect` declarations in `commonMain` and `actual` implementations in `androidMain` / `iosMain`.

## Data Flow

```text
HermesApi / HermesWebSocket
  → Repositories
  → AppState / screen state
  → Compose screens
```

## Offline Behavior

MVP:

- cache node config and device credentials;
- show last known Inbox/Session summaries;
- disable approval actions while offline;
- reconnect WebSocket automatically.

Later:

- outbox for queued goals;
- multi-node state;
- relay-backed push.

## UI Modules

Build design system first:

```text
HermesTheme
HermesScaffold
HermesSectionHeader
HermesInboxItem
HermesApprovalCard
HermesCommandBar
HermesTimeline
HermesToolCallRow
HermesCodeBlock
HermesStatusDot
```
