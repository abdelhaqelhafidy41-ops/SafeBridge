package com.example.safebridge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.safebridge.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val isArabic by viewModel.isArabic.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (isArabic) "الإعدادات والتفضيلات" else "Settings & Preferences",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Appearance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) "المظهر والواجهة" else "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "الوضع الداكن (Dark Mode)" else "Dark Theme",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isArabic) "التبديل بين السمة الداكنة والفاتحة" else "Toggle between Dark and Light mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("switch_dark_mode")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "اللغة (Language)" else "Language (العربية / English)",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isArabic) "الحالية: العربية" else "Current: English",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isArabic,
                            onCheckedChange = { viewModel.toggleLanguage() },
                            modifier = Modifier.testTag("switch_language")
                        )
                    }
                }
            }
        }

        // Sandbox & Cache Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) "إدارة بيئة العزل والبيانات" else "Sandbox & Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) "مسح سجلات التدقيق أو مسح ملفات السجلات النصية."
                        else "Purge expired client authorizations and clear audit buffers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.clearAdbAuditLogs() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (isArabic) "مسح سجل تدقيق ADB" else "Clear ADB Audit Log")
                    }
                }
            }
        }

        // Security Checklist Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) "مراجعة قواعد الأمان القياسية" else "Security Compliance Audit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CheckItem("Explicit Intents used for all bindings")
                    CheckItem("Canonical path validation on every sandbox path")
                    CheckItem("UID verification via PackageManager against Binder caller")
                    CheckItem("Strict ADB command allowlist (No arbitrary user shell)")
                    CheckItem("All HIGH risk commands unconditionally rejected")
                    CheckItem("Zero sensitive credentials or ADB keys in logs")
                    CheckItem("IBinder.DeathRecipient on both Client & Service")
                }
            }
        }
    }
}

@Composable
fun CheckItem(label: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("✓ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
