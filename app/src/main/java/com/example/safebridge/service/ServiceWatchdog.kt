package com.example.safebridge.service

import android.app.ActivityManager
import android.content.Context
import com.example.safebridge.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Educational Watchdog for SafeBridgeService.
 *
 * Uses non-intrusive Coroutines and Lifecycle observation.
 * Avoids aggressive alarms, foreground hacks, or system bypasses.
 */
class ServiceWatchdog(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val tag = "ServiceWatchdog"

    private var monitoringJob: Job? = null

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _lastHeartbeat = MutableStateFlow(0L)
    val lastHeartbeat: StateFlow<Long> = _lastHeartbeat.asStateFlow()

    private val _watchdogStatus = MutableStateFlow("Idle")
    val watchdogStatus: StateFlow<String> = _watchdogStatus.asStateFlow()

    var checkIntervalMillis: Long = 5000L

    fun startMonitoring() {
        if (monitoringJob != null && monitoringJob?.isActive == true) return

        _isMonitoring.value = true
        _watchdogStatus.value = "Monitoring Active"
        AppLogger.i(tag, "ServiceWatchdog started (interval: ${checkIntervalMillis}ms)")

        monitoringJob = scope.launch {
            while (isActive) {
                checkServiceHealth()
                delay(checkIntervalMillis)
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _isMonitoring.value = false
        _watchdogStatus.value = "Stopped"
        AppLogger.i(tag, "ServiceWatchdog stopped.")
    }

    private fun checkServiceHealth() {
        val now = System.currentTimeMillis()
        _lastHeartbeat.value = now

        val active = SafeBridgeService.isServiceActive.value
        if (active) {
            _watchdogStatus.value = "Healthy (Running) - Handled ${SafeBridgeService.totalRequests.value} calls"
            AppLogger.d(tag, "Heartbeat check: SafeBridgeService is HEALTHY.")
        } else {
            _watchdogStatus.value = "Warning: SafeBridgeService is Stopped"
            AppLogger.w(tag, "Heartbeat check: SafeBridgeService is currently STOPPED.")
        }
    }
}
