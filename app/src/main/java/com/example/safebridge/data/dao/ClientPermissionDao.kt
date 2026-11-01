package com.example.safebridge.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.safebridge.data.model.ClientPermissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientPermissionDao {
    @Query("SELECT * FROM client_permissions ORDER BY lastUsedAt DESC")
    fun getAllClients(): Flow<List<ClientPermissionEntity>>

    @Query("SELECT * FROM client_permissions WHERE packageName = :packageName LIMIT 1")
    suspend fun getClient(packageName: String): ClientPermissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(permission: ClientPermissionEntity)

    @Update
    suspend fun update(permission: ClientPermissionEntity)

    @Query("DELETE FROM client_permissions WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("UPDATE client_permissions SET granted = 0 WHERE packageName = :packageName")
    suspend fun revoke(packageName: String)

    @Query("UPDATE client_permissions SET granted = 0")
    suspend fun revokeAll()

    @Query("DELETE FROM client_permissions WHERE expiresAt IS NOT NULL AND expiresAt < :currentTime")
    suspend fun purgeExpired(currentTime: Long = System.currentTimeMillis())
}
