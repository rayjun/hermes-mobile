# Hermes Mobile Security Notes

## Security Goals

Hermes Mobile can approve actions that affect files, terminals, repositories, APIs, cron jobs, and external services. The security model must assume that a paired mobile device is powerful and must be revocable.

## Baseline Requirements

- Gateway must not expose mobile endpoints without explicit enablement.
- Devices must pair with short-lived codes.
- Devices must be individually identifiable and revocable.
- Approval actions must be audited.
- High-risk approvals should require biometric confirmation when supported.
- The app must not expose arbitrary filesystem browsing by default.
- Artifacts should be registered and scoped.

## Device Pairing

Pairing should include:

- Gateway URL;
- short-lived pairing code;
- TLS/server fingerprint;
- device name;
- platform;
- public key or generated credential.

Pairing code should expire quickly and be single-use.

## Device Storage

Mobile credentials must be stored in:

- iOS Keychain;
- Android EncryptedSharedPreferences or equivalent secure storage.

Do not store tokens in plain SQLDelight tables.

## Request Authentication

Target design uses signed requests:

```text
METHOD
PATH
BODY_SHA256
TIMESTAMP
NONCE
```

Gateway rejects:

- expired timestamps;
- replayed nonces;
- revoked devices;
- invalid signatures.

## Risk Levels

| Risk | Examples | UX |
|---|---|---|
| Low | read-only status, tests, session list | normal approval or no approval |
| Medium | file writes, package installs, cron edits | explicit approve |
| High | git push, PR creation, destructive command | biometric + explicit approve |
| Critical | deleting directories, disabling safety, exposing secrets | default deny or double confirmation |

## Audit Log

Record:

- device id;
- session id;
- approval id;
- action;
- decision;
- risk;
- timestamp;
- relevant metadata.

## Local-First Network Model

Recommended MVP deployment:

```text
Phone ↔ Tailscale/LAN ↔ Hermes Gateway
```

Tailscale reduces exposure but does not replace pairing. Pairing is still needed for revoke, audit, and future relay compatibility.
