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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safebridge.adb.AdbCommand
import com.example.safebridge.adb.AdbDevice
import com.example.safebridge.adb.AdbRiskLevel
import com.example.safebridge.data.model.AdbAuditEntity
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.LowRiskGreen
import com.example.ui.theme.MediumRiskOrange
import com.example.safebridge.ui.viewmodel.MainViewModel

@Composable
fun AdbScreen(viewModel: MainViewModel) {
    val devices by viewModel.adbDevices.collectAsState()
    val activeSession by viewModel.currentAdbSession.collectAsState()
    val isExecuting by viewModel.isAdbExecuting.collectAsState()
    val lastResult by viewModel.lastAdbResult.collectAsState()
    val auditLogs by viewModel.adbAuditFlow.collectAsState()
    val pendingApproval by viewModel.pendingAdbApproval.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var pairingDevice by remember { mutableStateOf<AdbDevice?>(null) }
    var pairingCodeInput by remember { mutableStateOf("123456") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("adb_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) "منفذ ADB الآمن والمقيد (SafeAdbExecutor)" else "SafeAdbExecutor (Restricted Controller)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) "أوامر مسبقة ومفحوصة المعاملات فقط. يمنع shell عشوائي أو root أو الوصول للنظام."
                        else "Strict allowlist only. No arbitrary shell text, no root, zero system bypasses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (activeSession != null) LowRiskGreen else MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeSession?.let { "Active Session: ${it.deviceId} (${it.connectionState})" }
                                ?: (if (isArabic) "لا توجد جلسة نشطة" else "No Active Session"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Tabs: Commands vs Audit Logs
        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (isArabic) "الأوامر والأجهزة" else "Devices & Commands") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (isArabic) "سجل التدقيق (${auditLogs.size})" else "Audit Log (${auditLogs.size})") }
                )
            }
        }

        if (selectedTab == 0) {
            // Devices Section
            item {
                Text(
                    text = if (isArabic) "الأجهزة المقترنة والمكتشفة" else "Approved Devices",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(devices, key = { it.id }) { dev ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = dev.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Host: ${dev.maskedHost} | ID: ${dev.maskedId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (activeSession?.deviceId == dev.id) {
                            Button(
                                onClick = { viewModel.disconnectAdbSession(dev.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(if (isArabic) "قطع" else "Disconnect")
                            }
                        } else {
                            Button(
                                onClick = { pairingDevice = dev },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (isArabic) "اقتران / ربط" else "Pair / Connect")
                            }
                        }
                    }
                }
            }

            // Commands Allowlist Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isArabic) "قائمة الأوامر المصرح بها (Allowlist)" else "Allowlisted Commands",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            val targetDevice = devices.firstOrNull { it.id == activeSession?.deviceId } ?: devices.firstOrNull()

            if (targetDevice != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // LOW RISK
                        CommandItemCard(
                            command = AdbCommand.GetDeviceState,
                            isArabic = isArabic,
                            onExecute = { viewModel.requestCommandExecution(targetDevice, AdbCommand.GetDeviceState) }
                        )

                        CommandItemCard(
                            command = AdbCommand.GetSerialNumber,
                            isArabic = isArabic,
                            onExecute = { viewModel.requestCommandExecution(targetDevice, AdbCommand.GetSerialNumber) }
                        )

                        CommandItemCard(
                            command = AdbCommand.GetBuildSdkVersion,
                            isArabic = isArabic,
                            onExecute = { viewModel.requestCommandExecution(targetDevice, AdbCommand.GetBuildSdkVersion) }
                        )

                        CommandItemCard(
                            command = AdbCommand.GetProductModel,
                            isArabic = isArabic,
                            onExecute = { viewModel.requestCommandExecution(targetDevice, AdbCommand.GetProductModel) }
                        )

                        CommandItemCard(
                            command = AdbCommand.ListUserPackages,
                            isArabic = isArabic,
                            onExecute = { viewModel.requestCommandExecution(targetDevice, AdbCommand.ListUserPackages) }
                        )

                        // MEDIUM RISK (Requires confirmation dialog)
                        CommandItemCard(
                            command = AdbCommand.DumpsysActivities,
                            isArabic = isArabic,
                            onExecute = { viewModel.requestCommandExecution(targetDevice, AdbCommand.DumpsysActivities) }
                        )

                        CommandItemCard(
                            command = AdbCommand.DumpsysPackage("com.example.safebridge"),
                            isArabic = isArabic,
                            onExecute = { viewModel.requestCommandExecution(targetDevice, AdbCommand.DumpsysPackage("com.example.safebridge")) }
                        )

                        CommandItemCard(
                            command = AdbCommand.LogcatTail(30),
                            isArabic = isArabic,
                            onExecute = { viewModel.requestCommandExecution(targetDevice, AdbCommand.LogcatTail(30)) }
                        )

                        CommandItemCard(
                            command = AdbCommand.ListAppDirectory("demo_workspace"),
                            isArabic = isArabic,
                            onExecute = { viewModel.requestCommandExecution(targetDevice, AdbCommand.ListAppDirectory("demo_workspace")) }
                        )

                        // HIGH RISK (Strictly blocked demo)
                        CommandItemCard(
                            command = AdbCommand.ForbiddenAction("Reboot device or invoke su root"),
                            isArabic = isArabic,
                            onExecute = { viewModel.requestCommandExecution(targetDevice, AdbCommand.ForbiddenAction("Reboot / su")) }
                        )
                    }
                }
            }

            // Execution Output Console
            lastResult?.let { res ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (res.success) "SUCCESS (Exit: ${res.exitCode})" else "EXECUTION FAILED",
                                    color = if (res.success) LowRiskGreen else HighRiskRed,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${res.durationMs}ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = res.output,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Audit Log Tab
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "سجل العمليات والتدقيق الأمني" else "Security Audit Trail",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { viewModel.clearAdbAuditLogs() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (isArabic) "مسح السجل" else "Clear Audit")
                    }
                }
            }

            if (auditLogs.isEmpty()) {
                item {
                    Text(
                        text = if (isArabic) "لا توجد سجلات تدقيق حتى الآن." else "No audit records yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(auditLogs, key = { it.id }) { log ->
                    AuditLogRow(log = log)
                }
            }
        }
    }

    // Modal: Pairing dialog
    pairingDevice?.let { dev ->
        AlertDialog(
            onDismissRequest = { pairingDevice = null },
            title = { Text(if (isArabic) "اقتران بجهاز ${dev.name}" else "Pair Device: ${dev.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "أدخل رمز الاقتران المكون من 6 أرقام (Wireless ADB Pairing Code):"
                        else "Enter 6-digit Wireless ADB pairing code:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = pairingCodeInput,
                        onValueChange = { if (it.length <= 6) pairingCodeInput = it },
                        label = { Text("6-digit Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.pairAdbDevice(dev, pairingCodeInput)
                    pairingDevice = null
                }) {
                    Text(if (isArabic) "اقتران واتصال" else "Pair & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { pairingDevice = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Modal: Mandatory AdbCommandApprovalScreen for Medium Risk Commands
    pendingApproval?.let { (device, command) ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelPendingAdbCommand() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MediumRiskOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isArabic) "طلب موافقة تنفيذ أمر ADB" else "ADB Command Approval")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Device: ${device.name} (${device.maskedId})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Operation: ${command.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Risk Level: ${command.riskLevel.name}",
                        fontWeight = FontWeight.Bold,
                        color = MediumRiskOrange,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "Audit Description: ${command.auditDescription}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Arguments: ${command.arguments.joinToString(" ")}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Timeout: ${command.timeoutMs}ms | Output Limit: ${command.outputLimitLines} lines",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmPendingAdbCommand() },
                    colors = ButtonDefaults.buttonColors(containerColor = MediumRiskOrange)
                ) {
                    Text(if (isArabic) "موافقة لمرة واحدة (Allow Once)" else "Allow Once")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelPendingAdbCommand() }) {
                    Text(if (isArabic) "إلغاء (Cancel)" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun CommandItemCard(
    command: AdbCommand,
    isArabic: Boolean,
    onExecute: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (command.riskLevel) {
                                    AdbRiskLevel.LOW -> LowRiskGreen
                                    AdbRiskLevel.MEDIUM -> MediumRiskOrange
                                    AdbRiskLevel.HIGH -> HighRiskRed
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = command.displayName,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = command.auditDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onExecute,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (command.riskLevel) {
                        AdbRiskLevel.LOW -> MaterialTheme.colorScheme.primary
                        AdbRiskLevel.MEDIUM -> MediumRiskOrange
                        AdbRiskLevel.HIGH -> HighRiskRed
                    }
                )
            ) {
                Text(
                    text = when (command.riskLevel) {
                        AdbRiskLevel.LOW -> if (isArabic) "تنفيذ" else "Run"
                        AdbRiskLevel.MEDIUM -> if (isArabic) "طلب موافقة" else "Confirm"
                        AdbRiskLevel.HIGH -> if (isArabic) "محظور" else "Blocked"
                    },
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun AuditLogRow(log: AdbAuditEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.commandDisplay,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = log.result,
                    color = if (log.result == "SUCCESS") LowRiskGreen else HighRiskRed,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = "${log.formattedTime} | Dev: ${log.deviceIdMasked} | ${log.durationMs}ms | UserApproved=${log.userApproval}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (log.outputPreview.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.outputPreview,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )
            }
        }
    }
}
