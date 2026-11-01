package com.example.safebridge.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.safebridge.data.model.AdbAuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdbAuditDao {
    @Query("SELECT * FROM adb_audit_logs ORDER BY timestamp DESC LIMIT 500")
    fun getRecentAuditLogs(): Flow<List<AdbAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AdbAuditEntity)

    @Query("DELETE FROM adb_audit_logs")
    suspend fun clearLogs()

    @Query("DELETE FROM adb_audit_logs WHERE id NOT IN (SELECT id FROM adb_audit_logs ORDER BY timestamp DESC LIMIT 500)")
    suspend fun pruneLogs()
}
