# Hermes Mobile UI Spec

## Design Direction

Hermes Mobile should look like Hermes Desktop folded into a phone.

It should not look like Telegram, WhatsApp, a generic AI assistant, or a SaaS dashboard. It should inherit the Desktop UI language:

- light-first;
- low-contrast ice blue/gray background;
- translucent white surfaces;
- compact developer-tool density;
- thin hairline dividers;
- section-based lists;
- execution timelines instead of chat bubbles;
- inline errors;
- `Start with a goal` command bar.

## Design Principles

```text
Compact over spacious
Logs over bubbles
Structure over prose
Approval over notification
Goal over message
Agent state over assistant personality
```

## Navigation

Recommended MVP tabs:

```text
Inbox | Sessions | Cron | Settings
```

Recommended post-MVP tabs:

```text
Inbox | Sessions | Goal | Cron | Files
```

The center `Goal` action opens a command composer sheet.

## App Shell

### Default Screen

```text
┌──────────────────────────────┐
│ Hermes                  VPS ●│
├──────────────────────────────┤
│ Screen content               │
├──────────────────────────────┤
│ Inbox Sessions  +  Cron Files│
└──────────────────────────────┘
```

### Session Detail

```text
┌──────────────────────────────┐
│ ‹ Session title            ⋯ │
├──────────────────────────────┤
│ Timeline                     │
├──────────────────────────────┤
│ + Start with a goal        ↑ │
└──────────────────────────────┘
```

Session detail replaces bottom tabs with the command bar.

## Core Screens

### Pairing

Purpose: connect mobile app to Hermes Gateway.

Elements:

- Hermes logo/title.
- Short explanation: `Connect to your Hermes Gateway`.
- QR scanner.
- Manual URL entry.
- Connection fingerprint confirmation.

States:

- scanning;
- validating gateway;
- pairing code expired;
- paired successfully;
- TLS fingerprint mismatch.

### Inbox

Inbox is the default home screen.

Priority order:

1. Approvals
2. Running
3. Errors
4. Recent

Example:

```text
Hermes                         VPS ●

■ APPROVALS 2

HIGH
Run git push
rayjun/hermes-agent · branch fix/mobile-api · now
[Approve] [Review]

MED
Create cron job
daily report · every day at 09:00 · 3m ago

■ RUNNING 1

Hermes-Agent contribution #2
Thinking · searched files · 21s

■ ERRORS

API call failed after 3 retries
openai-codex · connection error · 4m ago

■ RECENT

DeFi morning report delivered
telegram · success · 09:01
```

### Approval Detail

Purpose: review a sensitive operation safely.

Example:

```text
‹ Approval

HIGH RISK
Run git push

Hermes wants to push branch fix/mobile-api.

Context
Repo        rayjun/hermes-agent
CWD         ~/projects/hermes-agent
Branch      fix/mobile-api
Files       3 changed
Tests       128 passed

Command
git push origin fix/mobile-api

Reason
Hermes completed the requested change and needs to push the branch.

[Approve] [Deny]
Ask Hermes why
```

Design rules:

- Risk badge near top.
- Structured fields before raw command.
- Raw command can be expanded/collapsed.
- High/Critical risk requires biometric confirmation when implemented.
- Approve button is strong but not bright blue by default.

### Session Detail

Purpose: show agent execution transcript.

Do not use chat bubbles.

Example:

```text
‹ Hermes-Agent contribution #2       ⋯

继续找 pr

Thinking
  ◌ Searched files                   458ms
    pattern: "github issue"
  ◼ Read file                        118ms
    hermes_cli/main.py
  ✕ API call failed after 3 retries
    Connection error.

Thinking
  ◌ Searched files                   154ms
  ◼ Ran gh pr list                   1.5s
  ◼ Ran gh issue list                1.3s

找到一个适合的小问题：...

Artifacts
  ▣ issue-analysis.md
  ▣ patch.diff

+ Start with a goal                  ↑
```

### Sessions

Example:

```text
Sessions                      Search

■ PINNED

Hermes-Agent contribution #2
openai-codex · 12m ago

AI-Coding-Context development
gpt-5.5 · yesterday

■ RECENT

Obsidian Vault Sync Rules
telegram · 2h ago

General Greeting and Assistant
telegram · Jun 22
```

### Cron

Example:

```text
Automations

■ ENABLED 4

● mind-github-sync
every hour · next in 12m

● DeFi Morning Report
daily 09:00 · next tomorrow

■ PAUSED 1

○ Blog Watcher
paused · last failed yesterday
```

### Artifacts

Example:

```text
Artifacts

■ TODAY

▣ hermes-mobile-architecture.md
Markdown · 18 KB · from Session

▣ approval-flow.png
Image · 240 KB · generated
```

### Settings

Example:

```text
Settings

■ NODE

● VPS Hermes
https://100.64.1.23:8765
gateway ready

■ MODEL

openai-codex
gpt-5.5

■ TOOLS

terminal · file · web · cron · browser

■ DEVICES

Ray's iPhone
paired today

■ SECURITY

Approval policy
Biometric approval
Audit log
```

## Design Tokens

### Colors

```kotlin
object HermesColors {
    val Background = Color(0xFFF7FAFC)
    val BackgroundSubtle = Color(0xFFF1F5F9)

    val Surface = Color(0xEFFFFFFF)
    val SurfaceMuted = Color(0xBFFFFFFF)
    val SurfaceHover = Color(0xFFEFF6FF)
    val SurfacePressed = Color(0xFFE2E8F0)

    val Border = Color(0xFFD8E0EA)
    val BorderSubtle = Color(0xFFE6ECF2)
    val BorderStrong = Color(0xFFCBD5E1)

    val TextPrimary = Color(0xFF172033)
    val TextSecondary = Color(0xFF64748B)
    val TextTertiary = Color(0xFF94A3B8)
    val TextDisabled = Color(0xFFCBD5E1)

    val Blue = Color(0xFF3B82F6)
    val BlueSoft = Color(0xFFEFF6FF)
    val BlueMuted = Color(0xFF93C5FD)

    val Error = Color(0xFFB91C1C)
    val ErrorSoft = Color(0xFFFEE2E2)
    val Success = Color(0xFF059669)
    val Warning = Color(0xFFD97706)

    val RiskLow = Color(0xFF64748B)
    val RiskMedium = Color(0xFFD97706)
    val RiskHigh = Color(0xFFDC2626)
    val RiskCritical = Color(0xFF991B1B)
    val RiskMediumBg = Color(0xFFFFF7ED)
    val RiskHighBg = Color(0xFFFEF2F2)
}
```

### Typography

| Token | Size | Weight | Usage |
|---|---:|---:|---|
| screenTitle | 16 | 600 | Top bar title |
| sectionLabel | 10 | 600 | Section headers |
| rowTitle | 13 | 500 | List item primary text |
| rowSubtitle | 11 | 400 | Metadata |
| body | 14 | 400 | Normal content |
| bodySmall | 12 | 400 | Secondary content |
| mono | 12 | 400 | Code/tool output |
| monoSmall | 11 | 400 | Tool timeline |
| badge | 9 | 700 | Risk/status badge |

### Spacing

| Token | Value |
|---|---:|
| screenHorizontal | 12 |
| sectionTop | 18 |
| rowVertical | 8 |
| toolIndent | 16 |
| rowMinHeight | 44 |
| commandBarHeight | 48 |

### Radius

| Token | Value |
|---|---:|
| panel | 10 |
| input | 8 |
| card | 10 |
| button | 8 |

## Component Set

### HermesScaffold

Top bar + content + optional bottom bar.

### HermesSectionHeader

Displays `■ APPROVALS 2` style labels.

### HermesInboxItem

Compact event row for approvals, running sessions, errors, and recent results.

### HermesApprovalCard

Compact approval display for Inbox.

### HermesApprovalDetail

Full detail screen for approval review.

### HermesTimeline

Renders session timeline blocks.

### HermesToolCallRow

Renders rows like:

```text
◼ Read file                          118ms
✕ API call failed after 3 retries
```

### HermesCommandBar

Bottom input with `Start with a goal` placeholder.

### HermesCodeBlock

Small monospace block for commands, logs, diffs.

### HermesStatusDot

Node/session/status indicator.

## Acceptance Checklist

- [ ] Light theme is default.
- [ ] Background is ice blue/gray, not pure white.
- [ ] Lists use section headers.
- [ ] Section headers include small blue marker.
- [ ] Session detail does not use chat bubbles.
- [ ] Tool calls render as timeline/log rows.
- [ ] Errors are inline red, not giant alerts.
- [ ] Command bar says `Start with a goal`.
- [ ] Approval appears above all other Inbox content.
- [ ] Typography is compact.
- [ ] Borders are 1px hairlines.
- [ ] Primary blue is used sparingly.
- [ ] UI remains close to Hermes Desktop visual language.
