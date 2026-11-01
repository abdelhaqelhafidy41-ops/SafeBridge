package com.example.safebridge.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.safebridge.ipc.ISafeBridgeCallback
import com.example.safebridge.ipc.ISafeBridgeService
import com.example.safebridge.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ClientConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
    DISCONNECTED_BY_DEATH
}

data class ClientCallbackEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val type: String,
    val message: String
)

/**
 * SafeBridgeDemoClient.
 *
 * Demonstrates how client applications interact with SafeBridge over Binder IPC:
 * - Binds to SafeBridgeService explicitly.
 * - Handles IBinder.DeathRecipient on server process termination.
 * - Registers ISafeBridgeCallback for asynchronous updates.
 * - Requests client authorization with customizable reason.
 * - Performs safe sandbox tasks and pings.
 * - Reconnects cleanly on unexpected binder disconnection.
 */
class SafeBridgeDemoClient(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val tag = "DemoClient"
    val clientPackageName: String = context.packageName

    private var remoteService: ISafeBridgeService? = null
    private var deathRecipient: IBinder.DeathRecipient? = null

    private val _connectionState = MutableStateFlow(ClientConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ClientConnectionState> = _connectionState.asStateFlow()

    private val _lastPingResult = MutableStateFlow<String?>(null)
    val lastPingResult: StateFlow<String?> = _lastPingResult.asStateFlow()

    private val _lastTaskResult = MutableStateFlow<String?>(null)
    val lastTaskResult: StateFlow<String?> = _lastTaskResult.asStateFlow()

    private val _authStatus = MutableStateFlow("Not Requested")
    val authStatus: StateFlow<String> = _authStatus.asStateFlow()

    private val _callbackEvents = MutableStateFlow<List<ClientCallbackEvent>>(emptyList())
    val callbackEvents: StateFlow<List<ClientCallbackEvent>> = _callbackEvents.asStateFlow()

    var autoReconnectEnabled = true

    private val callbackStub = object : ISafeBridgeCallback.Stub() {
        override fun onTaskCompleted(taskId: String, success: Boolean, message: String, output: String) {
            AppLogger.i(tag, "Client received onTaskCompleted: $taskId (success=$success): $message")
            addEvent("TaskCompleted", "[$taskId] $message -> $output")
        }

        override fun onAccessRevoked(reason: String) {
            AppLogger.w(tag, "Client received onAccessRevoked: $reason")
            _authStatus.value = "Revoked: $reason"
            addEvent("AccessRevoked", reason)
        }

        override fun onServiceStatusChanged(statusMessage: String) {
            AppLogger.i(tag, "Client received onServiceStatusChanged: $statusMessage")
            addEvent("ServiceStatus", statusMessage)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            AppLogger.i(tag, "ServiceConnection onServiceConnected: Binder received from $name")
            if (service == null) {
                _connectionState.value = ClientConnectionState.ERROR
                return
            }

            try {
                remoteService = ISafeBridgeService.Stub.asInterface(service)

                // Setup death recipient to detect if the service process crashes
                deathRecipient = IBinder.DeathRecipient {
                    AppLogger.w(tag, "Binder death detected on Client side!")
                    _connectionState.value = ClientConnectionState.DISCONNECTED_BY_DEATH
                    remoteService = null
                    addEvent("BinderDeath", "SafeBridgeService died unexpectedly.")

                    if (autoReconnectEnabled) {
                        scheduleReconnect()
                    }
                }
                service.linkToDeath(deathRecipient!!, 0)

                // Register client callback
                remoteService?.registerCallback(clientPackageName, callbackStub)
                _connectionState.value = ClientConnectionState.CONNECTED
                addEvent("Connected", "Binder interface bound successfully.")

                // Check initial authorization status
                refreshStatus()
            } catch (e: Exception) {
                AppLogger.e(tag, "Failed to initialize remote binder: ${e.message}")
                _connectionState.value = ClientConnectionState.ERROR
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            AppLogger.w(tag, "ServiceConnection onServiceDisconnected: $name")
            remoteService = null
            _connectionState.value = ClientConnectionState.DISCONNECTED
            addEvent("Disconnected", "Service disconnected by system.")
        }
    }

    fun connect() {
        if (_connectionState.value == ClientConnectionState.CONNECTED ||
            _connectionState.value == ClientConnectionState.CONNECTING
        ) return

        _connectionState.value = ClientConnectionState.CONNECTING
        val intent = Intent("com.example.safebridge.action.BIND_SERVICE").apply {
            setPackage(context.packageName) // Explicit intent prevents Intent spoofing
        }

        val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (!bound) {
            AppLogger.e(tag, "bindService returned false!")
            _connectionState.value = ClientConnectionState.ERROR
            addEvent("Error", "bindService returned false.")
        } else {
            AppLogger.i(tag, "bindService requested with action BIND_SERVICE.")
        }
    }

    fun disconnect() {
        if (_connectionState.value == ClientConnectionState.DISCONNECTED) return
        try {
            remoteService?.unregisterCallback(clientPackageName, callbackStub)
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            AppLogger.w(tag, "Error unbinding service: ${e.message}")
        } finally {
            remoteService = null
            _connectionState.value = ClientConnectionState.DISCONNECTED
            addEvent("Unbound", "Client manually disconnected.")
        }
    }

    fun sendPing(): String {
        val service = remoteService ?: return "Not connected to service"
        return try {
            val response = service.ping()
            _lastPingResult.value = response
            AppLogger.i(tag, "Ping result: $response")
            response
        } catch (e: Exception) {
            val err = "Ping failed: ${e.message}"
            AppLogger.e(tag, err)
            _lastPingResult.value = err
            err
        }
    }

    fun requestAccess(reason: String = "Demo Client Operations", oneTime: Boolean = false): Boolean {
        val service = remoteService ?: return false
        return try {
            val accepted = service.requestClientAccess(clientPackageName, reason, oneTime)
            _authStatus.value = if (accepted) "Pending Approval" else "Rejected Immediately"
            AppLogger.i(tag, "Access requested: accepted=$accepted, reason='$reason'")
            refreshStatus()
            accepted
        } catch (e: Exception) {
            AppLogger.e(tag, "Request access failed: ${e.message}")
            false
        }
    }

    fun refreshStatus(): String {
        val service = remoteService ?: return "Not connected"
        return try {
            val status = service.getClientStatus(clientPackageName)
            _authStatus.value = status
            status
        } catch (e: Exception) {
            val err = "Status check error: ${e.message}"
            _authStatus.value = err
            err
        }
    }

    fun executeTask(command: String, argument: String): String {
        val service = remoteService ?: return "Error: Service not connected"
        return try {
            val res = service.executeSandboxTask(clientPackageName, command, argument)
            _lastTaskResult.value = res
            AppLogger.i(tag, "executeTask result: $res")
            res
        } catch (e: Exception) {
            val err = "Execution failed: ${e.message}"
            AppLogger.e(tag, err)
            _lastTaskResult.value = err
            err
        }
    }

    private fun scheduleReconnect() {
        scope.launch {
            AppLogger.i(tag, "Auto-reconnect waiting 2 seconds before retry...")
            delay(2000)
            if (_connectionState.value != ClientConnectionState.CONNECTED) {
                AppLogger.i(tag, "Attempting auto-reconnection...")
                connect()
            }
        }
    }

    private fun addEvent(type: String, message: String) {
        val event = ClientCallbackEvent(type = type, message = message)
        val current = _callbackEvents.value.toMutableList()
        current.add(0, event)
        if (current.size > 50) current.removeAt(current.size - 1)
        _callbackEvents.value = current
    }
}
