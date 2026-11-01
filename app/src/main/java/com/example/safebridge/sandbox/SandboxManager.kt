package com.example.safebridge.sandbox

import android.content.Context
import com.example.safebridge.logging.AppLogger
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Metadata representation of a file or directory within the Sandbox.
 */
data class SandboxFileInfo(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(lastModified))

    val displaySize: String
        get() {
            if (isDirectory) return "-"
            return when {
                sizeBytes < 1024 -> "$sizeBytes B"
                sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
                else -> String.format(Locale.US, "%.1f MB", sizeBytes.toDouble() / (1024 * 1024))
            }
        }
}

data class SandboxStats(
    val totalFiles: Int,
    val totalDirectories: Int,
    val totalSizeBytes: Long,
    val sandboxPath: String
)

/**
 * Isolated filesystem sandbox manager.
 * Strictly operates inside `context.filesDir/sandbox/`.
 *
 * Implements strict canonical path validation, path traversal denial,
 * absolute path rejection, and a strictly enforced allowlist of sandbox tasks.
 */
class SandboxManager(private val context: Context) {

    private val tag = "SandboxManager"
    val sandboxRoot: File = File(context.filesDir, "sandbox").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    /**
     * Allowlist of safe tasks that clients can request.
     * Real shell execution is strictly disallowed.
     */
    val allowedTasks = setOf("echo", "get_status", "list_sandbox", "sha256", "calc")

    init {
        // Create initial demo files if sandbox is empty
        ensureDefaultFiles()
    }

    private fun ensureDefaultFiles() {
        try {
            val readme = File(sandboxRoot, "README.txt")
            if (!readme.exists()) {
                readme.writeText(
                    "Welcome to SafeBridge Sandbox.\n" +
                            "This isolated environment allows reading, writing, and safe task execution.\n" +
                            "Access outside this directory is strictly blocked by canonical path verification."
                )
            }
            val demoDir = File(sandboxRoot, "demo_workspace")
            if (!demoDir.exists()) {
                demoDir.mkdirs()
                File(demoDir, "sample_task.json").writeText(
                    "{\n  \"task\": \"ipc_demo\",\n  \"status\": \"ready\",\n  \"safe\": true\n}"
                )
            }
        } catch (e: Exception) {
            AppLogger.e(tag, "Failed to initialize default sandbox files: ${e.message}")
        }
    }

    /**
     * Strictly verifies and resolves a relative path into a target File inside sandboxRoot.
     * Throws SecurityException if path traversal, absolute paths, or escaping is attempted.
     */
    fun validateAndResolvePath(relativePath: String): File {
        val trimmed = relativePath.trim()

        if (trimmed.isEmpty()) {
            throw SecurityException("Sandbox path cannot be empty.")
        }

        // 1. Block absolute paths
        if (trimmed.startsWith("/") || trimmed.startsWith("\\") || trimmed.contains(":")) {
            AppLogger.w(tag, "Blocked absolute path attempt: $trimmed")
            throw SecurityException("Absolute paths are forbidden in Sandbox.")
        }

        // 2. Block path traversal tokens
        if (trimmed.contains("..") || trimmed.contains("/.") || trimmed.contains("\\.")) {
            AppLogger.w(tag, "Blocked path traversal attempt: $trimmed")
            throw SecurityException("Path traversal (e.g. '..') is strictly prohibited.")
        }

        // 3. Block sensitive system path substrings
        val lower = trimmed.lowercase(Locale.ROOT)
        val blacklistedTokens = listOf("/system", "/data", "/sdcard", "/vendor", "/proc", "/sys", "/dev", "etc", "mnt")
        for (token in blacklistedTokens) {
            if (lower.contains(token)) {
                AppLogger.w(tag, "Blocked prohibited path token '$token' in $trimmed")
                throw SecurityException("Access to system path '$token' is forbidden.")
            }
        }

        // 4. Resolve against sandboxRoot
        val targetFile = File(sandboxRoot, trimmed)

        // 5. Canonical Path validation (Prevent symlink and canonical escaping)
        val canonicalSandbox = sandboxRoot.canonicalPath
        val canonicalTarget = try {
            targetFile.canonicalPath
        } catch (e: IOException) {
            throw SecurityException("Failed to canonicalize path: ${e.message}")
        }

        if (!canonicalTarget.startsWith(canonicalSandbox)) {
            AppLogger.w(tag, "Canonical path escape detected: target=$canonicalTarget outside sandbox=$canonicalSandbox")
            throw SecurityException("Path escapes Sandbox boundary!")
        }

        return targetFile
    }

    /**
     * Create a new file in Sandbox.
     */
    fun createFile(relativePath: String, content: String = ""): Result<String> {
        return runCatching {
            val file = validateAndResolvePath(relativePath)
            if (file.exists()) {
                throw IOException("File already exists: $relativePath")
            }
            file.parentFile?.mkdirs()
            file.writeText(content)
            AppLogger.i(tag, "Created file: $relativePath (${content.length} chars)")
            "File created successfully: $relativePath"
        }
    }

    /**
     * Read content of a file in Sandbox.
     */
    fun readFile(relativePath: String): Result<String> {
        return runCatching {
            val file = validateAndResolvePath(relativePath)
            if (!file.exists() || file.isDirectory) {
                throw IOException("File not found: $relativePath")
            }
            if (file.length() > 512 * 1024) {
                throw IOException("File exceeds maximum read limit of 512 KB.")
            }
            val content = file.readText()
            AppLogger.d(tag, "Read file: $relativePath (${content.length} chars)")
            content
        }
    }

    /**
     * Write content to a file in Sandbox (overwriting or appending).
     */
    fun writeFile(relativePath: String, content: String, append: Boolean = false): Result<String> {
        return runCatching {
            val file = validateAndResolvePath(relativePath)
            file.parentFile?.mkdirs()
            if (append) {
                file.appendText(content)
            } else {
                file.writeText(content)
            }
            AppLogger.i(tag, "Wrote to file: $relativePath (append=$append, size=${file.length()} bytes)")
            "File written successfully (${file.length()} bytes)"
        }
    }

    /**
     * Delete a file or empty directory in Sandbox.
     */
    fun deleteFile(relativePath: String): Result<Boolean> {
        return runCatching {
            val file = validateAndResolvePath(relativePath)
            if (!file.exists()) {
                throw IOException("Target does not exist: $relativePath")
            }
            if (file == sandboxRoot) {
                throw SecurityException("Cannot delete sandbox root directory!")
            }
            val deleted = file.deleteRecursively()
            AppLogger.i(tag, "Deleted path: $relativePath (success=$deleted)")
            deleted
        }
    }

    /**
     * Create a directory in Sandbox.
     */
    fun createDirectory(relativePath: String): Result<String> {
        return runCatching {
            val dir = validateAndResolvePath(relativePath)
            if (dir.exists()) {
                throw IOException("Directory already exists: $relativePath")
            }
            val created = dir.mkdirs()
            if (!created) {
                throw IOException("Failed to create directory: $relativePath")
            }
            AppLogger.i(tag, "Created directory: $relativePath")
            "Directory created: $relativePath"
        }
    }

    /**
     * List files in a relative directory in Sandbox.
     */
    fun listFiles(relativeDir: String = ""): Result<List<SandboxFileInfo>> {
        return runCatching {
            val dir = if (relativeDir.isEmpty()) sandboxRoot else validateAndResolvePath(relativeDir)
            if (!dir.exists() || !dir.isDirectory) {
                throw IOException("Directory does not exist: $relativeDir")
            }

            val files = dir.listFiles() ?: emptyArray()
            val baseCanonical = sandboxRoot.canonicalPath

            files.map { f ->
                val fCanonical = f.canonicalPath
                val rel = if (fCanonical.startsWith(baseCanonical)) {
                    fCanonical.substring(baseCanonical.length).removePrefix(File.separator)
                } else {
                    f.name
                }
                SandboxFileInfo(
                    name = f.name,
                    relativePath = rel,
                    isDirectory = f.isDirectory,
                    sizeBytes = if (f.isFile) f.length() else 0L,
                    lastModified = f.lastModified()
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
        }
    }

    /**
     * Get aggregate statistics of Sandbox.
     */
    fun getSandboxStats(): SandboxStats {
        var countFiles = 0
        var countDirs = 0
        var totalBytes = 0L

        fun walk(dir: File) {
            val list = dir.listFiles() ?: return
            for (f in list) {
                if (f.isDirectory) {
                    countDirs++
                    walk(f)
                } else {
                    countFiles++
                    totalBytes += f.length()
                }
            }
        }
        walk(sandboxRoot)

        return SandboxStats(
            totalFiles = countFiles,
            totalDirectories = countDirs,
            totalSizeBytes = totalBytes,
            sandboxPath = sandboxRoot.absolutePath
        )
    }

    /**
     * Execute a simulated safe task from allowlist.
     * No shell command execution is permitted.
     */
    fun executeTask(command: String, argument: String): Result<String> {
        val trimmedCmd = command.trim().lowercase(Locale.ROOT)
        AppLogger.i(tag, "Received sandbox task request: cmd='$trimmedCmd', arg='$argument'")

        if (!allowedTasks.contains(trimmedCmd)) {
            val errMsg = "Security violation: Task '$trimmedCmd' is not in allowlist: $allowedTasks"
            AppLogger.w(tag, errMsg)
            return Result.failure(SecurityException(errMsg))
        }

        return runCatching {
            when (trimmedCmd) {
                "echo" -> {
                    // Safe echo simulation
                    "ECHO: $argument"
                }

                "get_status" -> {
                    val stats = getSandboxStats()
                    "Sandbox Status: Active | Files: ${stats.totalFiles} | Dirs: ${stats.totalDirectories} | Size: ${stats.totalSizeBytes} bytes"
                }

                "list_sandbox" -> {
                    val list = listFiles("").getOrThrow()
                    val entries = list.joinToString("\n") { (if (it.isDirectory) "[DIR] " else "[FILE] ") + it.name }
                    "Sandbox Root Contents:\n$entries"
                }

                "sha256" -> {
                    if (argument.isBlank()) throw IllegalArgumentException("Missing target file argument for sha256")
                    val file = validateAndResolvePath(argument)
                    if (!file.exists() || file.isDirectory) throw IOException("File not found: $argument")
                    val digest = MessageDigest.getInstance("SHA-256")
                    val bytes = file.readBytes()
                    val hash = digest.digest(bytes).joinToString("") { "%02x".format(it) }
                    "SHA-256 ($argument): $hash"
                }

                "calc" -> {
                    // Safe basic expression evaluation (e.g. "4 + 5" or argument length)
                    val trimmed = argument.trim()
                    if (trimmed.matches(Regex("^[0-9+\\-*/\\s]+$"))) {
                        val parts = trimmed.split(Regex("\\s+"))
                        if (parts.size == 3) {
                            val a = parts[0].toDoubleOrNull() ?: 0.0
                            val op = parts[1]
                            val b = parts[2].toDoubleOrNull() ?: 0.0
                            val res = when (op) {
                                "+" -> a + b
                                "-" -> a - b
                                "*" -> a * b
                                "/" -> if (b != 0.0) a / b else Double.NaN
                                else -> 0.0
                            }
                            "Calculation Result: $res"
                        } else {
                            "Length: ${trimmed.length} characters"
                        }
                    } else {
                        "Argument Length: ${trimmed.length}"
                    }
                }

                else -> throw SecurityException("Unknown task: $trimmedCmd")
            }
        }
    }
}
