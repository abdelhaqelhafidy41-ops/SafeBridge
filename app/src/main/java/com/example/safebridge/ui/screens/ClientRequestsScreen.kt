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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.safebridge.data.model.ClientPermissionEntity
import com.example.safebridge.ui.viewmodel.MainViewModel

@Composable
fun ClientRequestsScreen(viewModel: MainViewModel) {
    val permissions by viewModel.permissionsFlow.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    var selectedClientForGrant by remember { mutableStateOf<ClientPermissionEntity?>(null) }
    var selectedDurationMinutes by remember { mutableStateOf<Long?>(60L) }
    var oneTimeAccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("client_requests_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isArabic) "إدارة صلاحيات العملاء" else "Client Authorizations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic) "التحكم في وصول التطبيقات إلى Binder وSandbox" else "Manage apps authorized to bind and run sandbox tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (permissions.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.revokeAllPermissions() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (isArabic) "إلغاء الكل" else "Revoke All")
                    }
                }
            }
        }

        if (permissions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isArabic) "لا توجد طلبات عملاء حتى الآن" else "No Client Requests Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isArabic) "استخدم شاشة العميل التجريبي (Demo Client) لإرسال طلب اتصال واختبار الموافقة."
                            else "Open the Demo Client tab to simulate an incoming IPC authorization request.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(permissions, key = { it.packageName }) { client ->
                ClientPermissionCard(
                    client = client,
                    isArabic = isArabic,
                    onGrantClick = { selectedClientForGrant = client },
                    onRevokeClick = { viewModel.revokePermission(client.packageName) },
                    onDeleteClick = { viewModel.deleteClientRecord(client.packageName) }
                )
            }
        }
    }

    // Grant Access Dialog with Duration Options
    selectedClientForGrant?.let { client ->
        AlertDialog(
            onDismissRequest = { selectedClientForGrant = null },
            title = {
                Text(
                    text = if (isArabic) "منح الصلاحية لـ ${client.packageName}" else "Authorize ${client.packageName}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "حدد مدة الصلاحية أو خيار الاستخدام لمرة واحدة:"
                        else "Choose access duration or single-use permission:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    DurationOptionRow("15 Minutes (15 دقيقة)", selectedDurationMinutes == 15L) {
                        selectedDurationMinutes = 15L
                    }
                    DurationOptionRow("1 Hour (ساعة واحدة)", selectedDurationMinutes == 60L) {
                        selectedDurationMinutes = 60L
                    }
                    DurationOptionRow("24 Hours (يوم كامل)", selectedDurationMinutes == 1440L) {
                        selectedDurationMinutes = 1440L
                    }
                    DurationOptionRow("Permanent (دائم حتى الإلغاء)", selectedDurationMinutes == null) {
                        selectedDurationMinutes = null
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = oneTimeAccess,
                            onCheckedChange = { oneTimeAccess = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "صلاحية لمرة واحدة فقط (One-Time Access)" else "One-Time Access Only",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.grantPermission(client.packageName, selectedDurationMinutes, oneTimeAccess)
                        selectedClientForGrant = null
                    }
                ) {
                    Text(if (isArabic) "تأكيد ومنح" else "Grant Access")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedClientForGrant = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun DurationOptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ClientPermissionCard(
    client: ClientPermissionEntity,
    isArabic: Boolean,
    onGrantClick: () -> Unit,
    onRevokeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (client.isValid) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (client.isValid) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (client.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = client.packageName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "UID: ${client.uid} | Calls: ${client.usedCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (client.reason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${if (isArabic) "السبب:" else "Reason:"} ${client.reason}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        !client.granted -> if (isArabic) "مرفوض / بانتظار الموافقة" else "Pending / Denied"
                        client.isExpired -> if (isArabic) "منتهي الصلاحية (Expired)" else "Expired"
                        client.oneTime && client.usedCount >= 1 -> if (isArabic) "تم استهلاك الصلاحية الواحدة" else "One-Time Used"
                        client.expiresAt == null -> if (isArabic) "صلاحية دائمة (Permanent)" else "Permanent Access"
                        else -> {
                            val remainMin = (client.expiresAt - System.currentTimeMillis()) / 60000
                            if (isArabic) "متبقي $remainMin دقيقة" else "Active (Expires in ${remainMin}m)"
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (client.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (client.isValid) {
                        Button(
                            onClick = onRevokeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "إلغاء" else "Revoke")
                        }
                    } else {
                        Button(
                            onClick = onGrantClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "منح" else "Grant")
                        }
                    }
                }
            }
        }
    }
}
