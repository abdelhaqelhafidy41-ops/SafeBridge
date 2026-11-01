# SafeBridge Security Model & Threat Boundaries 🛡️

## 1. Security Architecture Principles

SafeBridge enforces a strict defense-in-depth model across all layers of execution:

```
+-------------------------------------------------------------+
|                      Client Process                         |
|  - Uses explicit Intent targeting SafeBridgeService         |
|  - Attaches IBinder.DeathRecipient to monitor server state  |
+-------------------------------------------------------------+
                               |
                               | (Binder IPC via AIDL)
                               v
+-------------------------------------------------------------+
|                   SafeBridgeService (Manager)               |
|  1. Calling UID Authenticity Verification                   |
|     - Binder.getCallingUid() cross-checked via PackageManager|
|  2. PermissionManager Validation                            |
|     - Grant active? Expiration timestamp valid?            |
|     - One-time quota consumed?                              |
|  3. RemoteCallbackList Tracking                             |
|     - Monitors client process death                         |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                Isolated Filesystem Sandbox                  |
|  - Root: context.filesDir/sandbox/                          |
|  - Canonical Path Sanitization                              |
|  - Absolute Path Denial (/system, /data, /sdcard rejected)  |
|  - Allowlisted task evaluation (No arbitrary Runtime.exec)  |
+-------------------------------------------------------------+
```

## 2. Threat Vector Mitigations

| Threat Vector | Potential Impact | SafeBridge Defense |
|---|---|---|
| **UID / Package Spoofing** | Attacker claims to be an authorized package | `PermissionManager.verifyUidOwnership()` calls `PackageManager.getPackagesForUid(callingUid)` to verify authenticity before processing IPC requests. |
| **Path Traversal Attacks** | Escaping sandbox using `../` or encoded slashes | `SandboxManager.validateAndResolvePath()` inspects tokens for `..`, prohibits leading slashes, and checks that `target.canonicalPath.startsWith(sandboxRoot.canonicalPath)`. |
| **Command Injection in ADB** | Executing arbitrary commands using `;`, `&`, `|`, `` ` `` | `AdbArgumentValidator` strips and rejects all shell metacharacters. Raw command strings are impossible because commands are defined as strongly-typed Kotlin objects. |
| **Privilege Escalation** | Escalating to root or system user | SafeBridge has zero root code. High-risk commands (`su`, `reboot`, package management) are rejected unconditionally with `exitCode = -1`. |
| **Credential & Key Leakage** | Storing ADB pairing codes or private keys | `AppLogger` and `AdbAuditDao` strictly sanitize sensitive keywords (`password`, `token`, `secret`, `privatekey`) and mask device IDs. |
| **Service Denial / Death** | Server or client crash leaves dangling references | Both sides install `IBinder.DeathRecipient`. Server uses `RemoteCallbackList` which automatically cleans up dead client processes. |

## 3. Safe ADB Risk Classification

- **LOW (Auto-Permitted if Paired):** Non-sensitive read-only device properties (`get-state`, `get-serialno`, `ro.build.version.sdk`, `ro.product.model`, `pm list packages -3`).
- **MEDIUM (Mandatory Confirmation):** Package diagnostic dumps (`dumpsys package <pkg>`), activity stack query (`dumpsys activity`), log tailing (`logcat -d -t N`), and approved sandbox directory listings.
- **HIGH (STRICTLY PROHIBITED):** Root invocation (`su`), device reboot, app install/uninstall, and file modification outside the sandbox.
