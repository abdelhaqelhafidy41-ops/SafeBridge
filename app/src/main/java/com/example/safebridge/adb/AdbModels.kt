package com.example.safebridge.adb

/**
 * Risk classification for ADB operations.
 * HIGH operations are strictly blocked by SafeAdbExecutor policy.
 */
enum class AdbRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class AdbTransportType {
    WIRELESS_LOCAL,
    COMPANION_SERVICE,
    LOCAL_BRIDGE
}

/**
 * Permitted ADB connection lifecycle states.
 */
enum class AdbConnectionState {
    DISCONNECTED,
    DISCOVERING,
    PAIRING_REQUIRED,
    PAIRING,
    CONNECTED,
    AUTHENTICATED,
    EXPIRED,
    FAILED
}

data class AdbDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val paired: Boolean = false,
    val lastConnected: Long = 0L
) {
    val maskedId: String
        get() {
            if (id.length <= 4) return "****"
            return id.take(2) + "***" + id.takeLast(2)
        }

    val maskedHost: String
        get() {
            val parts = host.split(".")
            if (parts.size == 4) {
                return "${parts[0]}.${parts[1]}.***.***:$port"
            }
            return "127.0.0.1:$port"
        }
}

data class AdbSession(
    val deviceId: String,
    val transportType: AdbTransportType,
    var connectionState: AdbConnectionState,
    var authenticated: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    var lastActivityAt: Long = System.currentTimeMillis(),
    var userApproved: Boolean = false,
    var expiresAt: Long = System.currentTimeMillis() + (30 * 60 * 1000L) // 30 min session
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
}

/**
 * Strongly-typed ADB Command definitions.
 * Raw arbitrary shell strings from user input are strictly rejected.
 */
sealed class AdbCommand(
    val commandId: String,
    val displayName: String,
    val arguments: List<String>,
    val riskLevel: AdbRiskLevel,
    val requiresConfirmation: Boolean,
    val timeoutMs: Long = 5000L,
    val outputLimitLines: Int = 100,
    val auditDescription: String
) {
    object GetDeviceState : AdbCommand(
        commandId = "cmd_get_state",
        displayName = "Get Device State",
        arguments = listOf("get-state"),
        riskLevel = AdbRiskLevel.LOW,
        requiresConfirmation = false,
        auditDescription = "Query current device connection status"
    )

    object GetSerialNumber : AdbCommand(
        commandId = "cmd_get_serial",
        displayName = "Get Serial Number",
        arguments = listOf("get-serialno"),
        riskLevel = AdbRiskLevel.LOW,
        requiresConfirmation = false,
        auditDescription = "Query hardware serial number"
    )

    object GetBuildSdkVersion : AdbCommand(
        commandId = "cmd_get_sdk",
        displayName = "Get Android SDK Version",
        arguments = listOf("shell", "getprop", "ro.build.version.sdk"),
        riskLevel = AdbRiskLevel.LOW,
        requiresConfirmation = false,
        auditDescription = "Query ro.build.version.sdk property"
    )

    object GetProductModel : AdbCommand(
        commandId = "cmd_get_model",
        displayName = "Get Product Model",
        arguments = listOf("shell", "getprop", "ro.product.model"),
        riskLevel = AdbRiskLevel.LOW,
        requiresConfirmation = false,
        auditDescription = "Query ro.product.model property"
    )

    object ListUserPackages : AdbCommand(
        commandId = "cmd_list_packages",
        displayName = "List Third-Party Packages",
        arguments = listOf("shell", "pm", "list", "packages", "-3"),
        riskLevel = AdbRiskLevel.LOW,
        requiresConfirmation = false,
        auditDescription = "List non-system packages installed on device"
    )

    data class DumpsysPackage(val packageName: String) : AdbCommand(
        commandId = "cmd_dumpsys_pkg",
        displayName = "Dumpsys Package ($packageName)",
        arguments = listOf("shell", "dumpsys", "package", packageName),
        riskLevel = AdbRiskLevel.MEDIUM,
        requiresConfirmation = true,
        auditDescription = "Retrieve diagnostic package information for $packageName"
    )

    object DumpsysActivities : AdbCommand(
        commandId = "cmd_dumpsys_activities",
        displayName = "Dumpsys Current Activities",
        arguments = listOf("shell", "dumpsys", "activity", "activities"),
        riskLevel = AdbRiskLevel.MEDIUM,
        requiresConfirmation = true,
        outputLimitLines = 150,
        auditDescription = "Retrieve active activities stack diagnostic"
    )

    data class LogcatTail(val lineCount: Int) : AdbCommand(
        commandId = "cmd_logcat_tail",
        displayName = "Logcat Tail ($lineCount lines)",
        arguments = listOf("shell", "logcat", "-d", "-t", lineCount.toString()),
        riskLevel = AdbRiskLevel.MEDIUM,
        requiresConfirmation = true,
        outputLimitLines = lineCount,
        auditDescription = "Inspect recent $lineCount lines of logcat"
    )

    data class ListAppDirectory(val relativeDirectory: String) : AdbCommand(
        commandId = "cmd_ls_app_dir",
        displayName = "List App Sandbox Directory ($relativeDirectory)",
        arguments = listOf("shell", "ls", "-la", relativeDirectory),
        riskLevel = AdbRiskLevel.MEDIUM,
        requiresConfirmation = true,
        auditDescription = "List approved application directory: $relativeDirectory"
    )

    data class MkdirAppDirectory(val relativeDirectory: String) : AdbCommand(
        commandId = "cmd_mkdir_app_dir",
        displayName = "Create Directory ($relativeDirectory)",
        arguments = listOf("shell", "mkdir", "-p", relativeDirectory),
        riskLevel = AdbRiskLevel.MEDIUM,
        requiresConfirmation = true,
        auditDescription = "Create directory within approved app sandbox: $relativeDirectory"
    )

    data class PushFile(val localPath: String, val remoteDir: String) : AdbCommand(
        commandId = "cmd_push_file",
        displayName = "Push File ($localPath -> $remoteDir)",
        arguments = listOf("push", localPath, remoteDir),
        riskLevel = AdbRiskLevel.MEDIUM,
        requiresConfirmation = true,
        auditDescription = "Push file to approved app directory"
    )

    data class PullFile(val remotePath: String, val localDir: String) : AdbCommand(
        commandId = "cmd_pull_file",
        displayName = "Pull File ($remotePath -> $localDir)",
        arguments = listOf("pull", remotePath, localDir),
        riskLevel = AdbRiskLevel.MEDIUM,
        requiresConfirmation = true,
        auditDescription = "Pull file from approved app directory"
    )

    /**
     * Forbidden high-risk actions.
     * Demonstrates strict rejection by policy.
     */
    data class ForbiddenAction(val actionName: String) : AdbCommand(
        commandId = "cmd_forbidden",
        displayName = "FORBIDDEN: $actionName",
        arguments = listOf("forbidden"),
        riskLevel = AdbRiskLevel.HIGH,
        requiresConfirmation = true,
        auditDescription = "High-risk command strictly rejected by security policy."
    )
}

data class AdbExecutionResult(
    val success: Boolean,
    val exitCode: Int,
    val output: String,
    val durationMs: Long,
    val truncated: Boolean = false
)
