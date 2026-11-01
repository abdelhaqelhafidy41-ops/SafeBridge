package com.example.safebridge.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safebridge.logging.LogEntry
import com.example.safebridge.logging.LogLevel
import com.example.ui.theme.BridgeAmber
import com.example.ui.theme.BridgeCyan
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.LowRiskGreen
import com.example.safebridge.ui.viewmodel.MainViewModel

@Composable
fun LogsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val allLogs by viewModel.logsFlow.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf<LogLevel?>(null) }

    val filteredLogs = allLogs.filter { entry ->
        val matchesLevel = selectedLevel == null || entry.level == selectedLevel
        val matchesQuery = searchQuery.isBlank() ||
                entry.message.contains(searchQuery, ignoreCase = true) ||
                entry.tag.contains(searchQuery, ignoreCase = true) ||
                (entry.details?.contains(searchQuery, ignoreCase = true) == true)
        matchesLevel && matchesQuery
    }.reversed()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("logs_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Controls Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isArabic) "سجل النظام والـ IPC" else "System & IPC Logs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Buffer: ${allLogs.size}/500 entries (Sensitive data redacted)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, viewModel.getExportText())
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export SafeBridge Logs"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }

                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            }
        }

        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (isArabic) "بحث في السجلات..." else "Search logs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
        }

        // Level Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedLevel == null,
                    onClick = { selectedLevel = null },
                    label = { Text("ALL") }
                )
                FilterChip(
                    selected = selectedLevel == LogLevel.DEBUG,
                    onClick = { selectedLevel = if (selectedLevel == LogLevel.DEBUG) null else LogLevel.DEBUG },
                    label = { Text("DEBUG") }
                )
                FilterChip(
                    selected = selectedLevel == LogLevel.INFO,
                    onClick = { selectedLevel = if (selectedLevel == LogLevel.INFO) null else LogLevel.INFO },
                    label = { Text("INFO") }
                )
                FilterChip(
                    selected = selectedLevel == LogLevel.WARNING,
                    onClick = { selectedLevel = if (selectedLevel == LogLevel.WARNING) null else LogLevel.WARNING },
                    label = { Text("WARN") }
                )
                FilterChip(
                    selected = selectedLevel == LogLevel.ERROR,
                    onClick = { selectedLevel = if (selectedLevel == LogLevel.ERROR) null else LogLevel.ERROR },
                    label = { Text("ERROR") }
                )
            }
        }

        // Log Entries
        if (filteredLogs.isEmpty()) {
            item {
                Text(
                    text = if (isArabic) "لا توجد سجلات مطابقة." else "No log records found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                LogItemCard(entry = log)
            }
        }
    }
}

@Composable
fun LogItemCard(entry: LogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (entry.level) {
                                    LogLevel.DEBUG -> MaterialTheme.colorScheme.outline
                                    LogLevel.INFO -> LowRiskGreen
                                    LogLevel.WARNING -> BridgeAmber
                                    LogLevel.ERROR -> HighRiskRed
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.level.name,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = entry.tag,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Text(
                    text = entry.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )

            entry.details?.let { det ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = det,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}
