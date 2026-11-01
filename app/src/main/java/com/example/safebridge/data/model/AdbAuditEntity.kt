package com.example.safebridge.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persisted audit log entry for every executed ADB command.
 * Stores up to 500 records. Never contains sensitive passwords or keys.
 */
@Entity(tableName = "adb_audit_logs")
data class AdbAuditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceIdMasked: String,
    val commandId: String,
    val commandDisplay: String,
    val riskLevel: String,
    val result: String,
    val durationMs: Long,
    val exitCode: Int,
    val userApproval: Boolean,
    val outputPreview: String
) {
    val formattedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
}
