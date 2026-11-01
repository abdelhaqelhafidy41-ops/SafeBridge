package com.example.safebridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.safebridge.ui.viewmodel.MainViewModel

@Composable
fun ServiceControlScreen(viewModel: MainViewModel) {
    val isServiceActive by viewModel.isServiceActive.collectAsState()
    val totalRequests by viewModel.totalRequests.collectAsState()
    val activeClients by viewModel.activeClientsCount.collectAsState()
    val watchdogMonitoring by viewModel.watchdogMonitoring.collectAsState()
    val watchdogStatus by viewModel.watchdogStatus.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    var watchdogIntervalSlider by remember { mutableFloatStateOf(5f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("service_control_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isArabic) "إدارة دورة حياة الخدمة" else "Service Lifecycle Controller",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isServiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isServiceActive) {
                                if (isArabic) "الخدمة قيد التشغيل (Running)" else "State: ACTIVE / RUNNING"
                            } else {
                                if (isArabic) "الخدمة متوقفة (Stopped)" else "State: STOPPED"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startService() },
                            enabled = !isServiceActive,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_start_service")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "تشغيل" else "Start")
                        }

                        Button(
                            onClick = { viewModel.stopService() },
                            enabled = isServiceActive,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_stop_service")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "إيقاف" else "Stop")
                        }

                        OutlinedButton(
                            onClick = { viewModel.restartService() },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("btn_restart_service")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "إعادة تشغيل" else "Restart")
                        }
                    }
                }
            }
        }

        // IPC Metrics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isArabic) "إحصائيات Binder الحية" else "Live Binder IPC Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isArabic) "إصدار الخدمة (AIDL Version):" else "AIDL Service Version:")
                        Text("v1.0", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isArabic) "المعاملات المنجزة:" else "Total Requests Handled:")
                        Text(totalRequests.toString(), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isArabic) "المستمعون المسجلون (RemoteCallbackList):" else "Active Callback Listeners:")
                        Text(activeClients.toString(), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isArabic) "إدارة موت Binder (DeathRecipient):" else "Binder Death Handling:")
                        Text(if (isArabic) "مفعل تلقائيًا" else "Active & Linked", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Watchdog Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "مراقب الخدمة (Service Watchdog)" else "Service Watchdog Monitor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "فحص نبضات القلب دوريًا بدون منبهات نظام مزعجة" else "Periodic non-intrusive lifecycle health checks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = watchdogMonitoring,
                            onCheckedChange = {
                                if (it) viewModel.watchdog.startMonitoring()
                                else viewModel.watchdog.stopMonitoring()
                            },
                            modifier = Modifier.testTag("switch_watchdog")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "${if (isArabic) "فاصل فحص نبضات القلب:" else "Heartbeat Interval:"} ${watchdogIntervalSlider.toInt()}s",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Slider(
                        value = watchdogIntervalSlider,
                        onValueChange = {
                            watchdogIntervalSlider = it
                            viewModel.setWatchdogInterval((it * 1000).toLong())
                        },
                        valueRange = 2f..30f,
                        steps = 5,
                        modifier = Modifier.testTag("slider_watchdog_interval")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${if (isArabic) "الحالة الحالية:" else "Current Status:"} $watchdogStatus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Security Notice
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isArabic) "الخدمة تعمل في العملية القياسية للتطبيق، ولا تستخدم Root أو ADB لتشغيل نفسها."
                        else "SafeBridgeService runs solely in the app's unprivileged process and never invokes root or ADB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
