package com.example.safebridge.permission

import android.content.Context
import android.content.pm.PackageManager
import com.example.safebridge.data.SafeBridgeDatabase
import com.example.safebridge.data.model.ClientPermissionEntity
import com.example.safebridge.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Result of client access verification.
 */
sealed class AccessCheckResult {
    data class Granted(val entity: ClientPermissionEntity) : AccessCheckResult()
    data class Denied(val reason: String) : AccessCheckResult()
    data class Expired(val entity: ClientPermissionEntity) : AccessCheckResult()
    object OneTimeExhausted : AccessCheckResult()
    object SpoofedUid : AccessCheckResult()
    object NotFound : AccessCheckResult()
}

/**
 * Notification event for incoming client authorization requests.
 */
data class ClientAccessRequestEvent(
    val packageName: String,
    val uid: Int,
    val reason: String,
    val oneTime: Boolean
)

/**
 * Core Permission Manager.
 * Validates calling UIDs, prevents spoofing, enforces expiration & one-time policies,
 * and manages persistence via Room.
 */
class PermissionManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val tag = "PermissionManager"
    private val database = SafeBridgeDatabase.getInstance(context)
    private val dao = database.clientPermissionDao()
    private val packageManager: PackageManager = context.packageManager

    private val _accessRequestEvents = MutableSharedFlow<ClientAccessRequestEvent>(replay = 10)
    val accessRequestEvents = _accessRequestEvents.asSharedFlow()

    fun getAllPermissions(): Flow<List<ClientPermissionEntity>> = dao.getAllClients()

    /**
     * Strictly verifies whether the calling UID matches the claimed packageName.
     * Prevents Intent/Binder spoofing.
     */
    fun verifyUidOwnership(callingUid: Int, claimedPackage: String): Boolean {
        // Special case: Manager application itself calling local client
        if (callingUid == android.os.Process.myUid() && claimedPackage == context.packageName) {
            return true
        }

        val packages = packageManager.getPackagesForUid(callingUid)
        if (packages == null || !packages.contains(claimedPackage)) {
            AppLogger.e(tag, "SECURITY: Spoofed UID detected! callingUid=$callingUid claimedPackage=$claimedPackage actualPackages=${packages?.joinToString()}")
            return false
        }
        return true
    }

    /**
     * Check if a client application currently has valid permission to invoke Binder IPC.
     */
    suspend fun checkAccess(callingUid: Int, packageName: String): AccessCheckResult = withContext(Dispatchers.IO) {
        // 1. Verify UID authenticity
        if (!verifyUidOwnership(callingUid, packageName)) {
            return@withContext AccessCheckResult.SpoofedUid
        }

        // 2. Query persisted permission
        val entity = dao.getClient(packageName) ?: return@withContext AccessCheckResult.NotFound

        // 3. Check expiration
        if (entity.isExpired) {
            AppLogger.w(tag, "Client access expired for package: $packageName")
            return@withContext AccessCheckResult.Expired(entity)
        }

        // 4. Check one-time exhaustion
        if (entity.oneTime && entity.usedCount >= 1) {
            AppLogger.w(tag, "One-time client access already consumed for package: $packageName")
            return@withContext AccessCheckResult.OneTimeExhausted
        }

        // 5. Check if granted
        if (!entity.granted) {
            return@withContext AccessCheckResult.Denied("Access was denied by administrator.")
        }

        // 6. Record usage count and timestamp
        val updated = entity.copy(
            usedCount = entity.usedCount + 1,
            lastUsedAt = System.currentTimeMillis()
        )
        dao.update(updated)

        AppLogger.i(tag, "Access granted to $packageName (used: ${updated.usedCount})")
        AccessCheckResult.Granted(updated)
    }

    /**
     * Record an incoming client request from Binder IPC.
     */
    suspend fun recordClientRequest(
        callingUid: Int,
        packageName: String,
        reason: String,
        oneTime: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (!verifyUidOwnership(callingUid, packageName)) {
            return@withContext false
        }

        val existing = dao.getClient(packageName)
        val request = ClientPermissionEntity(
            packageName = packageName,
            uid = callingUid,
            granted = existing?.granted ?: false, // Don't auto-grant without manager UI action
            grantedAt = existing?.grantedAt ?: System.currentTimeMillis(),
            expiresAt = existing?.expiresAt,
            oneTime = oneTime,
            usedCount = existing?.usedCount ?: 0,
            reason = reason,
            lastUsedAt = System.currentTimeMillis()
        )
        dao.insertOrUpdate(request)

        _accessRequestEvents.emit(
            ClientAccessRequestEvent(
                packageName = packageName,
                uid = callingUid,
                reason = reason,
                oneTime = oneTime
            )
        )
        AppLogger.i(tag, "Client request recorded for $packageName: '$reason'")
        true
    }

    /**
     * Grant access to client from Manager UI.
     *
     * @param durationMinutes null for permanent, or duration in minutes (e.g. 15, 60, 1440)
     */
    suspend fun grantAccess(
        packageName: String,
        durationMinutes: Long?,
        oneTime: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val expiresAt = durationMinutes?.let { now + it * 60 * 1000L }

        val existing = dao.getClient(packageName)
        val uid = existing?.uid ?: try {
            packageManager.getPackageUid(packageName, 0)
        } catch (e: Exception) {
            0
        }

        val updated = ClientPermissionEntity(
            packageName = packageName,
            uid = uid,
            granted = true,
            grantedAt = now,
            expiresAt = expiresAt,
            oneTime = oneTime,
            usedCount = 0,
            reason = existing?.reason ?: "Admin authorized",
            lastUsedAt = now
        )
        dao.insertOrUpdate(updated)
        AppLogger.i(tag, "Admin GRANTED access to $packageName (expires in ${durationMinutes ?: "Permanent"} min, oneTime=$oneTime)")
    }

    /**
     * Revoke access for a specific client.
     */
    suspend fun revokeAccess(packageName: String) = withContext(Dispatchers.IO) {
        dao.revoke(packageName)
        AppLogger.i(tag, "Admin REVOKED access for $packageName")
    }

    /**
     * Revoke all permissions.
     */
    suspend fun revokeAll() = withContext(Dispatchers.IO) {
        dao.revokeAll()
        AppLogger.i(tag, "Admin REVOKED ALL client permissions")
    }

    /**
     * Delete client permission record completely.
     */
    suspend fun deleteClient(packageName: String) = withContext(Dispatchers.IO) {
        dao.delete(packageName)
        AppLogger.i(tag, "Deleted record for $packageName")
    }

    /**
     * Purge expired authorizations.
     */
    suspend fun purgeExpired() = withContext(Dispatchers.IO) {
        dao.purgeExpired()
    }
}
