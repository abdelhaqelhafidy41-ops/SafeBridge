package com.example.safebridge.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.safebridge.ui.viewmodel.MainViewModel

@Composable
fun AboutScreen(viewModel: MainViewModel) {
    val isArabic by viewModel.isArabic.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("about_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Identity Header
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.safebridge_icon_1793570239506),
                        contentDescription = "SafeBridge Icon",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "SafeBridge",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Educational Secure Android IPC & Sandbox Framework",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Version 1.0.0 (API Level 26 - 36)",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Philosophy & Independence Notice
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "الاستقلالية والحدود الأمنية" else "Architectural Scope & Boundaries",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isArabic) """
                            SafeBridge هو تطبيق تعليمي مستقل تمامًا لشرح تقنيات Android Binder IPC وبيئة العزل (Sandbox) والتنفيذ الآمن والمقيد لـ ADB.
                            
                            • ليس نسخة من Shizuku أو Rish ولا يستخدم كوده أو حزم Rikka.
                            • لا يتطلب ولا يستعمل صلاحيات Root.
                            • لا يستخدم ثغرات أو صلاحيات نظام غير موثقة.
                            • جميع العمليات تتم داخل filesDir/sandbox/ الخاص بالتطبيق فقط.
                            • تواصل ADB مقيد بـ Allowlist ثابتة لأوامر التشخيص وقراءة الحالة فقط دون أوامر Shell عامة.
                        """.trimIndent()
                        else """
                            SafeBridge is a standalone educational architecture exploring Android Binder IPC, isolated Sandbox containment, and highly restricted Safe ADB invocation.
                            
                            • Independent implementation: Not a clone of Shizuku or Rish, uses zero third-party private code or Rikka packages.
                            • Pure unprivileged execution: No root required, no ADB system escalation.
                            • Zero undocumented system hooks: Operates within standard Android SDK guarantees.
                            • Filesystem isolation: Confined strictly within context.filesDir/sandbox/.
                            • Safe ADB Controller: Strictly parameter-checked allowlist for non-destructive diagnostics only.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // IPC Flow diagram description
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) "بنية الاتصال (IPC Architecture)" else "IPC Topology & Death Handling",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) """
                            Client App (UID B)
                              └── binds to SafeBridgeService (UID A)
                                    ├── AIDL Stub verifies Binder.getCallingUid()
                                    ├── PermissionManager checks grant, expiry & one-time rule
                                    ├── SandboxManager checks canonical path validation
                                    └── RemoteCallbackList tracks death with IBinder.DeathRecipient
                        """.trimIndent()
                        else """
                            Client App (UID B)
                              └── binds to SafeBridgeService (UID A)
                                    ├── AIDL Stub verifies Binder.getCallingUid()
                                    ├── PermissionManager checks grant, expiry & one-time rule
                                    ├── SandboxManager checks canonical path validation
                                    └── RemoteCallbackList tracks death with IBinder.DeathRecipient
                        """.trimIndent(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
