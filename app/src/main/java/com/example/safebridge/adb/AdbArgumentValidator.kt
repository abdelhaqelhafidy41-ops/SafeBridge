package com.example.safebridge.adb

import com.example.safebridge.logging.AppLogger
import java.io.File
import java.util.Locale

/**
 * Strict Argument & Injection Validator for Safe ADB Execution.
 */
object AdbArgumentValidator {

    private const val TAG = "AdbValidator"

    // Prohibited shell injection characters
    private val INJECTION_REGEX = Regex("[;&|`$(){}<>\\n\\r*?\"'\\\\]")

    // Sensitive system paths
    private val PROHIBITED_PATHS = listOf(
        "/system",
        "/data",
        "/vendor",
        "/proc",
        "/sys",
        "/dev",
        "/sdcard",
        "/etc",
        "/root"
    )

    // Valid Android package pattern
    private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    /**
     * Validates a generic argument token.
     * Enforces length bounds, prohibits shell metacharacters, and rejects injection attempts.
     */
    fun validateArgument(argument: String, maxLen: Int = 128): Result<String> {
        val trimmed = argument.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Argument cannot be empty."))
        }

        if (trimmed.length > maxLen) {
            return Result.failure(IllegalArgumentException("Argument exceeds maximum length of $maxLen characters."))
        }

        // Check for shell metacharacters
        if (INJECTION_REGEX.containsMatchIn(trimmed)) {
            AppLogger.w(TAG, "Shell injection character detected in argument: '$trimmed'")
            return Result.failure(SecurityException("Prohibited shell metacharacter detected in argument."))
        }

        return Result.success(trimmed)
    }

    /**
     * Strictly validates an Android package name argument.
     */
    fun validatePackageName(packageName: String): Result<String> {
        val validated = validateArgument(packageName, maxLen = 120).getOrElse {
            return Result.failure(it)
        }

        if (!PACKAGE_NAME_REGEX.matches(validated)) {
            AppLogger.w(TAG, "Invalid package name format: '$validated'")
            return Result.failure(IllegalArgumentException("Invalid package name format: '$validated'"))
        }

        return Result.success(validated)
    }

    /**
     * Strictly validates a path argument.
     * Prevents path traversal (../), absolute root paths outside sandbox, and system directories.
     */
    fun validatePath(path: String, sandboxBaseDir: File): Result<String> {
        val validated = validateArgument(path, maxLen = 200).getOrElse {
            return Result.failure(it)
        }

        // Prohibit path traversal tokens
        if (validated.contains("..") || validated.contains("/.") || validated.contains("\\")) {
            AppLogger.w(TAG, "Path traversal attempt blocked: '$validated'")
            return Result.failure(SecurityException("Path traversal pattern ('..') is strictly prohibited."))
        }

        val lower = validated.lowercase(Locale.ROOT)
        for (prohibited in PROHIBITED_PATHS) {
            if (lower.startsWith(prohibited) || lower.contains("$prohibited/")) {
                AppLogger.w(TAG, "Prohibited system path access attempt: '$validated'")
                return Result.failure(SecurityException("Access to system path '$prohibited' is blocked."))
            }
        }

        // Verify canonical path within sandbox if resolved
        val targetFile = if (validated.startsWith("/")) File(validated) else File(sandboxBaseDir, validated)
        val canonicalSandbox = sandboxBaseDir.canonicalPath
        val canonicalTarget = targetFile.canonicalPath

        if (!canonicalTarget.startsWith(canonicalSandbox)) {
            AppLogger.w(TAG, "Target '$canonicalTarget' escapes approved sandbox '$canonicalSandbox'")
            return Result.failure(SecurityException("Path escapes approved application directory!"))
        }

        return Result.success(validated)
    }

    /**
     * Validates logcat line limit.
     */
    fun validateLineCount(count: Int): Result<Int> {
        if (count !in 1..200) {
            return Result.failure(IllegalArgumentException("Line count must be between 1 and 200."))
        }
        return Result.success(count)
    }
}
