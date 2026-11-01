package com.example.safebridge.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Log severity levels for SafeBridge.
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

/**
 * An individual audit log entry.
 * Sensitive data such as passwords, tokens, or raw secrets are strictly redacted.
 */
data class LogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val details: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}

/**
 * Thread-safe in-memory circular logger keeping up to 500 entries.
 * Emits reactive StateFlow updates for UI presentation and provides text export.
 */
object AppLogger {
    private const val MAX_LOGS = 500
    private val logList = CopyOnWriteArrayList<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    @Synchronized
    fun log(level: LogLevel, tag: String, message: String, details: String? = null) {
        val sanitizedMessage = sanitize(message)
        val sanitizedDetails = details?.let { sanitize(it) }

        val entry = LogEntry(
            level = level,
            tag = tag,
            message = sanitizedMessage,
            details = sanitizedDetails
        )

        // Maintain fixed size buffer of max 500 items
        if (logList.size >= MAX_LOGS) {
            logList.removeAt(0)
        }
        logList.add(entry)
        _logsFlow.value = logList.toList()

        // Also log to Android Logcat
        when (level) {
            LogLevel.DEBUG -> android.util.Log.d(tag, sanitizedMessage)
            LogLevel.INFO -> android.util.Log.i(tag, sanitizedMessage)
            LogLevel.WARNING -> android.util.Log.w(tag, sanitizedMessage)
            LogLevel.ERROR -> android.util.Log.e(tag, sanitizedMessage)
        }
    }

    fun d(tag: String, msg: String, details: String? = null) = log(LogLevel.DEBUG, tag, msg, details)
    fun i(tag: String, msg: String, details: String? = null) = log(LogLevel.INFO, tag, msg, details)
    fun w(tag: String, msg: String, details: String? = null) = log(LogLevel.WARNING, tag, msg, details)
    fun e(tag: String, msg: String, details: String? = null) = log(LogLevel.ERROR, tag, msg, details)

    @Synchronized
    fun clear() {
        logList.clear()
        _logsFlow.value = emptyList()
    }

    fun getLogs(): List<LogEntry> = logList.toList()

    /**
     * Export all logs formatted as clean plain text for Android Share.
     */
    fun exportAsText(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val sb = StringBuilder()
        sb.append("=== SafeBridge Audit & IPC Logs ===\n")
        sb.append("Exported at: ${dateFormat.format(Date())}\n")
        sb.append("Total entries: ${logList.size}\n\n")

        for (entry in logList) {
            sb.append("[${dateFormat.format(Date(entry.timestamp))}] ")
            sb.append("[${entry.level.name}] ")
            sb.append("[${entry.tag}] ")
            sb.append(entry.message)
            if (!entry.details.isNullOrEmpty()) {
                sb.append(" | details: ").append(entry.details)
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * Redact sensitive patterns (tokens, passwords, adb keys, private credentials).
     */
    private fun sanitize(input: String): String {
        return input
            .replace(Regex("(?i)(password|secret|token|api_key|privatekey)=[^&\\s]+"), "$1=[REDACTED]")
            .replace(Regex("(?i)bearer\\s+[A-Za-z0-9_.-]+"), "Bearer [REDACTED]")
    }
}
