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
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.safebridge.client.ClientCallbackEvent
import com.example.safebridge.client.ClientConnectionState
import com.example.safebridge.ui.viewmodel.MainViewModel

@Composable
fun ClientDemoScreen(viewModel: MainViewModel) {
    val connectionState by viewModel.clientConnectionState.collectAsState()
    val authStatus by viewModel.clientAuthStatus.collectAsState()
    val lastPing by viewModel.clientLastPing.collectAsState()
    val lastTask by viewModel.clientLastTask.collectAsState()
    val callbackEvents by viewModel.clientCallbackEvents.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    var customReason by remember { mutableStateOf("Need access to compute hashes in sandbox") }
    var oneTimeAccessRequest by remember { mutableStateOf(false) }

    var taskCmd by remember { mutableStateOf("echo") }
    var taskArg by remember { mutableStateOf("Testing Binder round-trip IPC") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("client_demo_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isArabic) "العميل التجريبي (SafeBridgeDemoClient)" else "SafeBridge Demo Client",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) "يحاكي تطبيقًا خارجيًا يتصل بالخدمة عبر Binder ويطلب الصلاحية."
                        else "Simulates a standalone 3rd-party client communicating over Android Binder IPC.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (connectionState) {
                                            ClientConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                                            ClientConnectionState.CONNECTING -> Color(0xFFFFA502)
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "State: $connectionState",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (connectionState == ClientConnectionState.CONNECTED) {
                            Button(
                                onClick = { viewModel.clientDisconnect() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("btn_client_disconnect")
                            ) {
                                Text(if (isArabic) "قطع الاتصال" else "Disconnect")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.clientConnect() },
                                modifier = Modifier.testTag("btn_client_connect")
                            ) {
                                Text(if (isArabic) "اتصال بـ Binder" else "Bind Service")
                            }
                        }
                    }
                }
            }
        }

        // Authorization Status & Request Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) "1. حالة الصلاحية والتفويض (Authorization)" else "1. Authorization Flow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Current Status: $authStatus",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (authStatus.contains("GRANTED", ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Reason for Access") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = oneTimeAccessRequest,
                            onCheckedChange = { oneTimeAccessRequest = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "طلب استخدام لمرة واحدة (One-Time)" else "Request One-Time Access", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.clientRequestAccess(customReason, oneTimeAccessRequest)
                            },
                            enabled = connectionState == ClientConnectionState.CONNECTED,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_request_auth")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "إرسال طلب موافقة" else "Request Access")
                        }

                        OutlinedButton(
                            onClick = { viewModel.clientRefreshStatus() },
                            enabled = connectionState == ClientConnectionState.CONNECTED
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "تحديث" else "Check")
                        }
                    }
                }
            }
        }

        // Binder Call Section (Ping & Task)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) "2. استدعاءات Binder (Ping & Sandbox Tasks)" else "2. Binder IPC Invocations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Ping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.clientPing() },
                            enabled = connectionState == ClientConnectionState.CONNECTED,
                            modifier = Modifier.testTag("btn_client_ping")
                        ) {
                            Text("ping()")
                        }

                        lastPing?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Task Execution
                    OutlinedTextField(
                        value = taskCmd,
                        onValueChange = { taskCmd = it },
                        label = { Text("Task Command (echo, get_status, calc, sha256)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = taskArg,
                        onValueChange = { taskArg = it },
                        label = { Text("Task Argument") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.clientExecuteTask(taskCmd, taskArg) },
                        enabled = connectionState == ClientConnectionState.CONNECTED,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_client_execute_task")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("executeSandboxTask()")
                    }

                    lastTask?.let { res ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Result: $res",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Live Binder Callback Stream
        item {
            Text(
                text = if (isArabic) "أحداث Callback الحية (ISafeBridgeCallback)" else "Live Async Callbacks (ISafeBridgeCallback)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (callbackEvents.isEmpty()) {
            item {
                Text(
                    text = if (isArabic) "لا توجد أحداث بعد." else "No callback events received yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(callbackEvents) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallReceived,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = event.type,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = event.message,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
