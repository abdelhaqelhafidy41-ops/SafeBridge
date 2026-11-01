package com.example.safebridge.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safebridge.adb.AdbCommand
import com.example.safebridge.adb.AdbDevice
import com.example.safebridge.adb.AdbExecutionResult
import com.example.safebridge.adb.AdbSession
import com.example.safebridge.adb.SafeAdbExecutor
import com.example.safebridge.client.ClientCallbackEvent
import com.example.safebridge.client.ClientConnectionState
import com.example.safebridge.client.SafeBridgeDemoClient
import com.example.safebridge.data.SafeBridgeDatabase
import com.example.safebridge.data.model.AdbAuditEntity
import com.example.safebridge.data.model.ClientPermissionEntity
import com.example.safebridge.logging.AppLogger
import com.example.safebridge.logging.LogEntry
import com.example.safebridge.logging.LogLevel
import com.example.safebridge.permission.PermissionManager
import com.example.safebridge.sandbox.SandboxFileInfo
import com.example.safebridge.sandbox.SandboxManager
import com.example.safebridge.sandbox.SandboxStats
import com.example.safebridge.service.SafeBridgeService
import com.example.safebridge.service.ServiceWatchdog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main ViewModel orchestrating Manager, Binder Service, PermissionManager,
 * SandboxManager, Demo Client, and SafeAdbExecutor.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    val permissionManager = PermissionManager(context, viewModelScope)
    val sandboxManager = SandboxManager(context)
    val watchdog = ServiceWatchdog(context, viewModelScope)
    val adbExecutor = SafeAdbExecutor(context, sandboxManager)
    val demoClient = SafeBridgeDemoClient(context, viewModelScope)

    // Database flows
    private val database = SafeBridgeDatabase.getInstance(context)
    val permissionsFlow: StateFlow<List<ClientPermissionEntity>> =
        permissionManager.getAllPermissions().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val adbAuditFlow: StateFlow<List<AdbAuditEntity>> =
        database.adbAuditDao().getRecentAuditLogs().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Service status
    val isServiceActive: StateFlow<Boolean> = SafeBridgeService.isServiceActive
    val totalRequests: StateFlow<Long> = SafeBridgeService.totalRequests
    val activeClientsCount: StateFlow<Int> = SafeBridgeService.activeClientsCount

    // Watchdog
    val watchdogMonitoring: StateFlow<Boolean> = watchdog.isMonitoring
    val watchdogStatus: StateFlow<String> = watchdog.watchdogStatus

    // Demo Client
    val clientConnectionState: StateFlow<ClientConnectionState> = demoClient.connectionState
    val clientAuthStatus: StateFlow<String> = demoClient.authStatus
    val clientLastPing: StateFlow<String?> = demoClient.lastPingResult
    val clientLastTask: StateFlow<String?> = demoClient.lastTaskResult
    val clientCallbackEvents: StateFlow<List<ClientCallbackEvent>> = demoClient.callbackEvents

    // Logs
    val logsFlow: StateFlow<List<LogEntry>> = AppLogger.logsFlow
    private val _logLevelFilter = MutableStateFlow<LogLevel?>(null)
    val logLevelFilter: StateFlow<LogLevel?> = _logLevelFilter.asStateFlow()

    // Sandbox state
    private val _sandboxFiles = MutableStateFlow<List<SandboxFileInfo>>(emptyList())
    val sandboxFiles: StateFlow<List<SandboxFileInfo>> = _sandboxFiles.asStateFlow()

    private val _sandboxStats = MutableStateFlow(sandboxManager.getSandboxStats())
    val sandboxStats: StateFlow<SandboxStats> = _sandboxStats.asStateFlow()

    private val _lastSandboxOutput = MutableStateFlow<String?>(null)
    val lastSandboxOutput: StateFlow<String?> = _lastSandboxOutput.asStateFlow()

    // ADB state
    val adbDevices: StateFlow<List<AdbDevice>> = adbExecutor.devices
    val currentAdbSession: StateFlow<AdbSession?> = adbExecutor.currentSession
    val isAdbExecuting: StateFlow<Boolean> = adbExecutor.isExecuting

    private val _lastAdbResult = MutableStateFlow<AdbExecutionResult?>(null)
    val lastAdbResult: StateFlow<AdbExecutionResult?> = _lastAdbResult.asStateFlow()

    // Pending command approval modal state
    private val _pendingAdbApproval = MutableStateFlow<Pair<AdbDevice, AdbCommand>?>(null)
    val pendingAdbApproval: StateFlow<Pair<AdbDevice, AdbCommand>?> = _pendingAdbApproval.asStateFlow()

    // Settings
    private val _isArabic = MutableStateFlow(false)
    val isArabic: StateFlow<Boolean> = _isArabic.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        AppLogger.i("MainViewModel", "Initializing SafeBridge MainViewModel")
        refreshSandbox()
        watchdog.startMonitoring()
        // Auto-start service initially for seamless demo
        startService()
    }

    // --- Service Control Actions ---

    fun startService() {
        val intent = Intent(context, SafeBridgeService::class.java)
        context.startService(intent)
        AppLogger.i("MainViewModel", "User invoked START Service.")
    }

    fun stopService() {
        val intent = Intent(context, SafeBridgeService::class.java)
        context.stopService(intent)
        AppLogger.i("MainViewModel", "User invoked STOP Service.")
    }

    fun restartService() {
        viewModelScope.launch {
            stopService()
            kotlinx.coroutines.delay(600)
            startService()
            AppLogger.i("MainViewModel", "User invoked RESTART Service.")
        }
    }

    // --- Permission Actions ---

    fun grantPermission(packageName: String, durationMinutes: Long?, oneTime: Boolean = false) {
        viewModelScope.launch {
            permissionManager.grantAccess(packageName, durationMinutes, oneTime)
            demoClient.refreshStatus()
        }
    }

    fun revokePermission(packageName: String) {
        viewModelScope.launch {
            permissionManager.revokeAccess(packageName)
            demoClient.refreshStatus()
        }
    }

    fun revokeAllPermissions() {
        viewModelScope.launch {
            permissionManager.revokeAll()
            demoClient.refreshStatus()
        }
    }

    fun deleteClientRecord(packageName: String) {
        viewModelScope.launch {
            permissionManager.deleteClient(packageName)
            demoClient.refreshStatus()
        }
    }

    // --- Sandbox Actions ---

    fun refreshSandbox() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = sandboxManager.listFiles("").getOrDefault(emptyList())
            val stats = sandboxManager.getSandboxStats()
            _sandboxFiles.value = list
            _sandboxStats.value = stats
        }
    }

    fun createSandboxFile(fileName: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = sandboxManager.createFile(fileName, content)
            _lastSandboxOutput.value = res.fold({ it }, { "Error: ${it.message}" })
            refreshSandbox()
        }
    }

    fun writeSandboxFile(fileName: String, content: String, append: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = sandboxManager.writeFile(fileName, content, append)
            _lastSandboxOutput.value = res.fold({ it }, { "Error: ${it.message}" })
            refreshSandbox()
        }
    }

    fun deleteSandboxFile(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = sandboxManager.deleteFile(fileName)
            _lastSandboxOutput.value = if (res.isSuccess) "Deleted $fileName" else "Delete failed"
            refreshSandbox()
        }
    }

    fun createSandboxDir(dirName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = sandboxManager.createDirectory(dirName)
            _lastSandboxOutput.value = res.fold({ it }, { "Error: ${it.message}" })
            refreshSandbox()
        }
    }

    fun executeSandboxTask(command: String, argument: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = sandboxManager.executeTask(command, argument)
            _lastSandboxOutput.value = res.fold({ it }, { "Error: ${it.message}" })
            refreshSandbox()
        }
    }

    // --- Demo Client Actions ---

    fun clientConnect() = demoClient.connect()
    fun clientDisconnect() = demoClient.disconnect()
    fun clientPing() = demoClient.sendPing()
    fun clientRequestAccess(reason: String, oneTime: Boolean) = demoClient.requestAccess(reason, oneTime)
    fun clientRefreshStatus() = demoClient.refreshStatus()
    fun clientExecuteTask(command: String, argument: String) = demoClient.executeTask(command, argument)

    // --- ADB Actions ---

    fun pairAdbDevice(device: AdbDevice, code: String): Result<AdbSession> {
        return adbExecutor.pairAndConnectDevice(device, code)
    }

    fun disconnectAdbSession(deviceId: String) {
        adbExecutor.disconnectSession(deviceId)
    }

    fun requestCommandExecution(device: AdbDevice, command: AdbCommand) {
        if (command.requiresConfirmation) {
            _pendingAdbApproval.value = Pair(device, command)
        } else {
            executeAdbCommandDirect(device, command, false)
        }
    }

    fun confirmPendingAdbCommand() {
        val pending = _pendingAdbApproval.value ?: return
        _pendingAdbApproval.value = null
        executeAdbCommandDirect(pending.first, pending.second, true)
    }

    fun cancelPendingAdbCommand() {
        _pendingAdbApproval.value = null
    }

    private fun executeAdbCommandDirect(device: AdbDevice, command: AdbCommand, approved: Boolean) {
        viewModelScope.launch {
            val res = adbExecutor.executeCommand(device, command, approved)
            _lastAdbResult.value = res
        }
    }

    fun clearAdbAuditLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            database.adbAuditDao().clearLogs()
        }
    }

    // --- Logging Actions ---

    fun setLogLevelFilter(level: LogLevel?) {
        _logLevelFilter.value = level
    }

    fun clearLogs() {
        AppLogger.clear()
    }

    fun getExportText(): String = AppLogger.exportAsText()

    // --- Settings Actions ---

    fun toggleLanguage() {
        _isArabic.value = !_isArabic.value
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setWatchdogInterval(millis: Long) {
        watchdog.checkIntervalMillis = millis
    }
}
