# Hermes Mobile Product Spec

## Positioning

Hermes Mobile is the mobile control surface for Hermes Agent.

It is not a general AI chat app. It is not a mobile environment for running the full Hermes runtime. Hermes continues to run on the user's Mac, Linux host, VPS, or homelab. The mobile app controls and observes that runtime.

## Product Promise

> Run Hermes anywhere. Approve from your phone. Never lose sight of what your agent is doing.

## Core User Jobs

1. **Start work remotely**
   - Send a goal to Hermes from mobile.
   - Attach URLs, files, images, or shared content.

2. **Stay in control**
   - See active sessions, running tasks, pending approvals, failures, and outputs.
   - Understand what Hermes wants to do before it does it.

3. **Approve safely**
   - Review command, risk, repository, changed files, reason, and test status.
   - Approve, deny, edit, or ask for explanation.

4. **Manage automations**
   - View cron jobs.
   - Run now, pause, resume, inspect last output.

5. **Access artifacts**
   - Preview generated markdown, PDFs, images, HTML, logs, and code diffs.

## Product Principles

- **Control surface, not chatbot** — the primary object is agent execution, not conversation.
- **Goal over message** — users give Hermes goals; Hermes executes.
- **Logs over bubbles** — sessions are execution transcripts, not IM threads.
- **Approval over notification** — the most important mobile-native action is safe approval.
- **Structure over prose** — mobile should receive structured events and timelines, not parse free-form text.
- **Local-first by default** — connect directly to the user's own Gateway through Tailscale/LAN/VPS.
- **Relay later** — APNs/FCM relay and hosted encrypted routing are v2 features, not MVP blockers.

## Target Users

### Primary

Developers and power users who already run Hermes through CLI, Telegram, or a gateway and want mobile control.

### Secondary

Users running Hermes automations such as cron jobs, research monitors, DeFi reports, server watchdogs, or repo maintenance agents.

## Non-Goals for MVP

- Running Hermes runtime on-device.
- Official cloud relay.
- Team approval workflows.
- Full model/provider management.
- Workflow builder.
- Complex skill editor.
- Complete filesystem browser.
- Chat-bubble style assistant UI.

## MVP Scope

### P0

- Node pairing by QR code.
- Device auth and secure token storage.
- Gateway status.
- Inbox with pending approvals and recent events.
- Approval detail page.
- Approve/deny flow.
- Session list.
- Session timeline with structured tool calls.
- Command bar for sending a follow-up goal.

### P1

- Cron list/detail.
- Run now / pause / resume.
- Artifact list and preview.
- Basic node settings.
- Audit log view.

### P2

- Share sheet.
- Voice input.
- Biometric approval for high-risk actions.
- Approval policies.
- Push relay.
- Multi-node switching.

## Core MVP Flow

```text
1. User runs `hermes mobile pair` or opens Mobile Pairing in Gateway.
2. Hermes shows QR code containing Gateway URL, pairing code, and TLS fingerprint.
3. User scans with Hermes Mobile.
4. App registers the device and stores credentials in secure storage.
5. App opens Inbox and connects to `/mobile/v1/events` WebSocket.
6. Hermes requests approval for a sensitive operation.
7. App displays the approval card.
8. User opens detail, reviews risk/context/command, and approves or denies.
9. Hermes receives decision and continues or aborts.
10. Session timeline updates live.
```

## Success Criteria

The MVP is successful if a user can:

- pair a mobile device with a local Hermes Gateway;
- receive a structured approval request;
- approve or deny it from the phone;
- see the session continue in a live timeline;
- send a follow-up goal from the command bar.

## Key Risks

| Risk | Mitigation |
|---|---|
| Mobile becomes just another chat UI | Build Inbox, Approval, Timeline first; defer generic chat polish |
| Gateway only emits text | Add structured mobile event/timeline APIs |
| Security model is weak | Pairing, device revoke, signed requests, audit log |
| Push notifications are hard | MVP uses foreground WebSocket + Telegram fallback |
| UI diverges from Desktop | Establish shared design tokens and checklist |
