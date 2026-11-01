package com.example.safebridge.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.RemoteCallbackList
import com.example.safebridge.ipc.ISafeBridgeCallback
import com.example.safebridge.ipc.ISafeBridgeService
import com.example.safebridge.logging.AppLogger
import com.example.safebridge.permission.AccessCheckResult
import com.example.safebridge.permission.PermissionManager
import com.example.safebridge.sandbox.SandboxManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicLong

/**
 * SafeBridge Secure Android Binder Service.
 *
 * Runs exclusively in the standard unprivileged app process.
 * Does not require or use root, ADB, or elevated system permissions.
 *
 * Implements AIDL Binder interface with strict UID verification,
 * authorization checks, DeathRecipient handlers, and isolated sandbox execution.
 */
class SafeBridgeService : Service() {

    private val tag = "SafeBridgeService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var permissionManager: PermissionManager
    private lateinit var sandboxManager: SandboxManager

    private val requestCounter = AtomicLong(0)
    private val startTime = System.currentTimeMillis()

    // RemoteCallbackList cleanly handles multi-client callbacks and process termination
    private val callbacks = object : RemoteCallbackList<ISafeBridgeCallback>() {
        override fun onCallbackDied(callback: ISafeBridgeCallback?, cookie: Any?) {
            super.onCallbackDied(callback, cookie)
            val clientPkg = cookie as? String ?: "unknown"
            AppLogger.w(tag, "Binder death detected: Client process died ($clientPkg)")
        }
    }

    companion object {
        const val SERVICE_VERSION = 1
        const val ACTION_BIND = "com.example.safebridge.action.BIND_SERVICE"

        // State observable by Manager UI
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive = _isServiceActive.asStateFlow()

        private val _totalRequests = MutableStateFlow(0L)
        val totalRequests = _totalRequests.asStateFlow()

        private val _activeClientsCount = MutableStateFlow(0)
        val activeClientsCount = _activeClientsCount.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.i(tag, "SafeBridgeService onCreate: Initializing Binder service...")
        permissionManager = PermissionManager(applicationContext, serviceScope)
        sandboxManager = SandboxManager(applicationContext)
        _isServiceActive.value = true
        AppLogger.i(tag, "SafeBridgeService is now active and ready to accept Binder connections.")
    }

    override fun onDestroy() {
        AppLogger.w(tag, "SafeBridgeService onDestroy: Shutting down Binder service...")
        _isServiceActive.value = false
        // Notify all registered clients of service shutdown
        try {
            val count = callbacks.beginBroadcast()
            for (i in 0 until count) {
                try {
                    callbacks.getBroadcastItem(i).onServiceStatusChanged("Service Stopped")
                } catch (e: Exception) {
                    // Client already disconnected
                }
            }
            callbacks.finishBroadcast()
            callbacks.kill()
        } catch (e: Exception) {
            AppLogger.e(tag, "Error closing callbacks: ${e.message}")
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        val callingUid = Binder.getCallingUid()
        val callingPid = Binder.getCallingPid()
        AppLogger.i(tag, "onBind requested: UID=$callingUid, PID=$callingPid, action=${intent?.action}")
        return binder
    }

    /**
     * AIDL Binder Stub implementation with comprehensive security checks.
     */
    private val binder = object : ISafeBridgeService.Stub() {

        override fun getServiceVersion(): Int {
            return SERVICE_VERSION
        }

        override fun ping(): String {
            val callingUid = Binder.getCallingUid()
            requestCounter.incrementAndGet()
            _totalRequests.value = requestCounter.get()
            AppLogger.d(tag, "IPC: ping() received from UID=$callingUid")
            return "PONG: SafeBridge v$SERVICE_VERSION [UID=$callingUid, Uptime=${(System.currentTimeMillis() - startTime) / 1000}s]"
        }

        override fun requestClientAccess(packageName: String, reason: String, oneTime: Boolean): Boolean {
            val callingUid = Binder.getCallingUid()
            requestCounter.incrementAndGet()
            _totalRequests.value = requestCounter.get()
            AppLogger.i(tag, "IPC: requestClientAccess() from UID=$callingUid, pkg=$packageName, reason='$reason', oneTime=$oneTime")

            // UID ownership check
            if (!permissionManager.verifyUidOwnership(callingUid, packageName)) {
                AppLogger.e(tag, "Security violation: UID $callingUid tried to claim package $packageName")
                return false
            }

            return runBlocking {
                permissionManager.recordClientRequest(callingUid, packageName, reason, oneTime)
            }
        }

        override fun revokeClientAccess(packageName: String): Boolean {
            val callingUid = Binder.getCallingUid()
            requestCounter.incrementAndGet()
            _totalRequests.value = requestCounter.get()

            // Only the hosting Manager process or verified self can revoke
            val isHost = callingUid == android.os.Process.myUid()
            val isSelf = permissionManager.verifyUidOwnership(callingUid, packageName)

            if (!isHost && !isSelf) {
                AppLogger.w(tag, "Unauthorized revoke attempt from UID=$callingUid on $packageName")
                return false
            }

            runBlocking {
                permissionManager.revokeAccess(packageName)
            }

            // Notify registered client callbacks
            notifyClientAccessRevoked(packageName, "Revoked by user or administrator.")
            return true
        }

        override fun getClientStatus(packageName: String): String {
            val callingUid = Binder.getCallingUid()
            if (!permissionManager.verifyUidOwnership(callingUid, packageName)) {
                return "STATUS: ERROR_UID_MISMATCH"
            }

            val checkResult = runBlocking {
                permissionManager.checkAccess(callingUid, packageName)
            }

            return when (checkResult) {
                is AccessCheckResult.Granted -> "STATUS: GRANTED (Remaining: ${if (checkResult.entity.expiresAt == null) "Permanent" else "${(checkResult.entity.expiresAt - System.currentTimeMillis()) / 60000}m"})"
                is AccessCheckResult.Denied -> "STATUS: DENIED (${checkResult.reason})"
                is AccessCheckResult.Expired -> "STATUS: EXPIRED"
                is AccessCheckResult.OneTimeExhausted -> "STATUS: ONE_TIME_EXHAUSTED"
                is AccessCheckResult.NotFound -> "STATUS: NOT_REQUESTED"
                is AccessCheckResult.SpoofedUid -> "STATUS: ERROR_SPOOFED_UID"
            }
        }

        override fun executeSandboxTask(packageName: String, command: String, argument: String): String {
            val callingUid = Binder.getCallingUid()
            requestCounter.incrementAndGet()
            _totalRequests.value = requestCounter.get()

            AppLogger.i(tag, "IPC: executeSandboxTask() from UID=$callingUid, pkg=$packageName, cmd='$command'")

            // 1. Verify UID
            if (!permissionManager.verifyUidOwnership(callingUid, packageName)) {
                val err = "SECURITY ERROR: UID verification failed."
                AppLogger.e(tag, err)
                return err
            }

            // 2. Verify authorization
            val check = runBlocking {
                permissionManager.checkAccess(callingUid, packageName)
            }

            if (check !is AccessCheckResult.Granted) {
                val denialMsg = "PERMISSION DENIED: Client '$packageName' has no active access. Status: $check"
                AppLogger.w(tag, denialMsg)
                return denialMsg
            }

            // 3. Execute isolated sandbox task
            val taskResult = sandboxManager.executeTask(command, argument)
            val output = taskResult.fold(
                onSuccess = { it },
                onFailure = { "TASK ERROR: ${it.message}" }
            )

            // 4. Trigger callback asynchronously
            serviceScope.launch {
                val count = callbacks.beginBroadcast()
                for (i in 0 until count) {
                    val cookie = callbacks.getBroadcastCookie(i) as? String
                    if (cookie == packageName) {
                        try {
                            callbacks.getBroadcastItem(i).onTaskCompleted(
                                "task_${System.currentTimeMillis()}",
                                taskResult.isSuccess,
                                if (taskResult.isSuccess) "Completed" else "Failed",
                                output
                            )
                        } catch (e: Exception) {
                            AppLogger.e(tag, "Failed to send task completion callback: ${e.message}")
                        }
                    }
                }
                callbacks.finishBroadcast()
            }

            return output
        }

        override fun registerCallback(packageName: String, callback: ISafeBridgeCallback?): Boolean {
            if (callback == null) return false
            val callingUid = Binder.getCallingUid()

            if (!permissionManager.verifyUidOwnership(callingUid, packageName)) {
                AppLogger.e(tag, "registerCallback failed: UID mismatch for $packageName")
                return false
            }

            val binderDeathRecipient = IBinder.DeathRecipient {
                AppLogger.w(tag, "DeathRecipient: Client process died for package: $packageName")
                _activeClientsCount.value = callbacks.registeredCallbackCount
            }

            try {
                callback.asBinder().linkToDeath(binderDeathRecipient, 0)
            } catch (e: Exception) {
                AppLogger.e(tag, "Failed to link DeathRecipient: ${e.message}")
                return false
            }

            val registered = callbacks.register(callback, packageName)
            _activeClientsCount.value = callbacks.registeredCallbackCount
            AppLogger.i(tag, "Client callback registered for $packageName (active: ${_activeClientsCount.value})")
            return registered
        }

        override fun unregisterCallback(packageName: String, callback: ISafeBridgeCallback?): Boolean {
            if (callback == null) return false
            val unregistered = callbacks.unregister(callback)
            _activeClientsCount.value = callbacks.registeredCallbackCount
            AppLogger.i(tag, "Client callback unregistered for $packageName (active: ${_activeClientsCount.value})")
            return unregistered
        }
    }

    private fun notifyClientAccessRevoked(packageName: String, reason: String) {
        serviceScope.launch {
            val count = callbacks.beginBroadcast()
            for (i in 0 until count) {
                val cookie = callbacks.getBroadcastCookie(i) as? String
                if (cookie == packageName) {
                    try {
                        callbacks.getBroadcastItem(i).onAccessRevoked(reason)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            callbacks.finishBroadcast()
        }
    }
}
