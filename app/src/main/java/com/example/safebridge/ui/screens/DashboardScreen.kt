package com.example.safebridge.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safebridge.ui.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToService: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToSandbox: () -> Unit,
    onNavigateToAdb: () -> Unit,
    onNavigateToDemo: () -> Unit
) {
    val isServiceActive by viewModel.isServiceActive.collectAsState()
    val totalRequests by viewModel.totalRequests.collectAsState()
    val activeClients by viewModel.activeClientsCount.collectAsState()
    val watchdogStatus by viewModel.watchdogStatus.collectAsState()
    val adbSession by viewModel.currentAdbSession.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Status Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "حالة خدمة SafeBridge" else "SafeBridge Service Status",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isServiceActive) {
                                    if (isArabic) "قيد التشغيل والجاهزية" else "Active & Ready"
                                } else {
                                    if (isArabic) "متوقفة" else "Stopped"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isServiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        // Pulsing status dot
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isServiceActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isServiceActive) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = "Status Icon",
                                tint = if (isServiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isArabic) "خدمة Binder محلية داخل Sandbox التطبيق، بدون Root وبدون صلاحيات خاصة."
                        else "Unprivileged in-process Binder service isolated in Sandbox. No root, no system bypasses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isServiceActive) {
                            Button(
                                onClick = { viewModel.stopService() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_stop_btn")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "إيقاف" else "Stop")
                            }
                            OutlinedButton(
                                onClick = { viewModel.restartService() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_restart_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "إعادة تشغيل" else "Restart")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startService() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_start_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "تشغيل الخدمة" else "Start Service")
                            }
                        }
                    }
                }
            }
        }

        // Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = if (isArabic) "العملاء النشطون" else "Active Clients",
                    value = activeClients.toString(),
                    subtitle = if (isArabic) "تطبيقات متصلة بـ Binder" else "Connected via Binder",
                    icon = Icons.Default.Cable,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToClients
                )

                StatCard(
                    title = if (isArabic) "إجمالي الطلبات" else "Total IPC Calls",
                    value = totalRequests.toString(),
                    subtitle = if (isArabic) "معاملات آمنة" else "Secure transactions",
                    icon = Icons.Default.Security,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToService
                )
            }
        }

        // Watchdog & ADB Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isArabic) "مراقب الخدمة (Watchdog)" else "Service Watchdog",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = watchdogStatus,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isArabic) "جلسة ADB الآمنة" else "Safe ADB Session",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = adbSession?.let { "Active (${it.connectionState})" } ?: (if (isArabic) "غير متصل" else "Disconnected"),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (adbSession != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quick Navigation Launchers
        item {
            Text(
                text = if (isArabic) "الوحدات الرئيسية" else "Core Subsystems",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionCard(
                    title = if (isArabic) "العميل التجريبي (Demo Client)" else "Interactive Demo Client",
                    description = if (isArabic) "محاكاة اتصال تطبيق خارجي عبر Binder وتنفيذ مهام العزل" else "Simulate client IPC binding, authorization, ping & death recipient",
                    icon = Icons.Default.Terminal,
                    onClick = onNavigateToDemo
                )

                ActionCard(
                    title = if (isArabic) "إدارة بيئة العزل (Sandbox Explorer)" else "Sandbox Filesystem Explorer",
                    description = if (isArabic) "تصفح وإنشاء الملفات داخل filesDir/sandbox/ مع فحص المسارات" else "Inspect and create isolated files with canonical traversal checks",
                    icon = Icons.Default.Folder,
                    onClick = onNavigateToSandbox
                )

                ActionCard(
                    title = if (isArabic) "منفذ ADB الآمن (Safe ADB)" else "Restricted Safe ADB Controller",
                    description = if (isArabic) "أوامر مصرح بها ومعلنة مسبقًا بدون أوامر Shell عشوائية" else "Allowlisted non-root diagnostic commands with approval modal",
                    icon = Icons.Default.Security,
                    onClick = onNavigateToAdb
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
