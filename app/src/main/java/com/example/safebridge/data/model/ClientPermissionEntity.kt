package com.example.safebridge.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted client authorization status.
 *
 * @param packageName Android package name of the requesting client
 * @param uid Linux UID assigned to the client application
 * @param granted True if user granted access
 * @param grantedAt Epoch millis timestamp when access was granted
 * @param expiresAt Epoch millis timestamp when access expires (null = permanent until revoked)
 * @param oneTime True if permission can only be used once
 * @param usedCount Number of times permission has been exercised
 * @param reason Purpose stated by client when requesting access
 * @param lastUsedAt Epoch millis of the last IPC invocation
 */
@Entity(tableName = "client_permissions")
data class ClientPermissionEntity(
    @PrimaryKey
    val packageName: String,
    val uid: Int,
    val granted: Boolean,
    val grantedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val oneTime: Boolean = false,
    val usedCount: Int = 0,
    val reason: String = "",
    val lastUsedAt: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() = expiresAt != null && System.currentTimeMillis() > expiresAt

    val isValid: Boolean
        get() = granted && !isExpired && (!oneTime || usedCount < 1)
}
