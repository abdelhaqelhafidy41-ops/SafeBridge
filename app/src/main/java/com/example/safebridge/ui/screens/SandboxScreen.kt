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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safebridge.sandbox.SandboxFileInfo
import com.example.safebridge.ui.viewmodel.MainViewModel

@Composable
fun SandboxScreen(viewModel: MainViewModel) {
    val files by viewModel.sandboxFiles.collectAsState()
    val stats by viewModel.sandboxStats.collectAsState()
    val lastOutput by viewModel.lastSandboxOutput.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateDirDialog by remember { mutableStateOf(false) }
    var showExecuteTaskDialog by remember { mutableStateOf(false) }
    var viewingFileContent by remember { mutableStateOf<Pair<String, String>?>(null) }

    var newFileName by remember { mutableStateOf("") }
    var newFileContent by remember { mutableStateOf("") }
    var newDirName by remember { mutableStateOf("") }

    var selectedTaskCommand by remember { mutableStateOf("echo") }
    var selectedTaskArg by remember { mutableStateOf("Hello SafeBridge Sandbox!") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sandbox_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Sandbox Root info
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "مستكشف بيئة العزل (Sandbox)" else "Isolated Sandbox Filesystem",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "filesDir/sandbox/ | ${stats.totalFiles} files, ${stats.totalDirectories} dirs (${stats.totalSizeBytes} bytes)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { viewModel.refreshSandbox() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showCreateFileDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "ملف جديد" else "New File")
                        }

                        Button(
                            onClick = { showCreateDirDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "مجلد جديد" else "New Folder")
                        }

                        FilledTonalButton(
                            onClick = { showExecuteTaskDialog = true },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "مهمة آمنة" else "Run Task")
                        }
                    }
                }
            }
        }

        // Security boundary card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isArabic) "الحماية: التحقق من المسار الأساسي (Canonical Path). يتم حظر أي محاولة للهروب مثل '../' أو الوصول لـ /data و /system."
                        else "Guaranteed: Strict canonical path verification blocks traversal attacks ('../') and system paths like /data or /system.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Output Console if any
        lastOutput?.let { out ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isArabic) "مخرجات العملية الأخيرة:" else "Last Operation Console:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = out,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Files List Header
        item {
            Text(
                text = if (isArabic) "الملفات والمجلدات داخل العزل" else "Sandbox Contents",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (files.isEmpty()) {
            item {
                Text(
                    text = if (isArabic) "المجلد فارغ" else "Sandbox is empty.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(files, key = { it.relativePath }) { file ->
                SandboxFileRow(
                    info = file,
                    onView = {
                        val content = viewModel.sandboxManager.readFile(file.relativePath).getOrDefault("[Unable to read]")
                        viewingFileContent = Pair(file.name, content)
                    },
                    onDelete = {
                        viewModel.deleteSandboxFile(file.relativePath)
                    }
                )
            }
        }
    }

    // Dialog: Create File
    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text(if (isArabic) "إنشاء ملف داخل Sandbox" else "Create Sandbox File") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Relative File Name (e.g. notes.txt)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newFileContent,
                        onValueChange = { newFileContent = it },
                        label = { Text("File Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newFileName.isNotBlank()) {
                        viewModel.createSandboxFile(newFileName, newFileContent)
                        newFileName = ""
                        newFileContent = ""
                        showCreateFileDialog = false
                    }
                }) {
                    Text(if (isArabic) "إنشاء" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Dialog: Create Directory
    if (showCreateDirDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDirDialog = false },
            title = { Text(if (isArabic) "إنشاء مجلد داخل Sandbox" else "Create Sandbox Folder") },
            text = {
                OutlinedTextField(
                    value = newDirName,
                    onValueChange = { newDirName = it },
                    label = { Text("Directory Name (e.g. logs)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newDirName.isNotBlank()) {
                        viewModel.createSandboxDir(newDirName)
                        newDirName = ""
                        showCreateDirDialog = false
                    }
                }) {
                    Text(if (isArabic) "إنشاء" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDirDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Dialog: Run Safe Task
    if (showExecuteTaskDialog) {
        AlertDialog(
            onDismissRequest = { showExecuteTaskDialog = false },
            title = { Text(if (isArabic) "تنفيذ مهمة آمنة (Allowlist Tasks)" else "Execute Allowlisted Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "المهام المسموحة فقط: echo, get_status, list_sandbox, sha256, calc"
                        else "Only allowlisted mock tasks: echo, get_status, list_sandbox, sha256, calc",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = selectedTaskCommand,
                        onValueChange = { selectedTaskCommand = it },
                        label = { Text("Command (e.g. echo)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = selectedTaskArg,
                        onValueChange = { selectedTaskArg = it },
                        label = { Text("Argument (e.g. Hello World)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.executeSandboxTask(selectedTaskCommand, selectedTaskArg)
                    showExecuteTaskDialog = false
                }) {
                    Text(if (isArabic) "تنفيذ" else "Execute")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExecuteTaskDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Dialog: View File Content
    viewingFileContent?.let { (name, content) ->
        AlertDialog(
            onDismissRequest = { viewingFileContent = null },
            title = { Text("File: $name", fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        text = content,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewingFileContent = null }) {
                    Text(if (isArabic) "إغلاق" else "Close")
                }
            }
        )
    }
}

@Composable
fun SandboxFileRow(
    info: SandboxFileInfo,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (!info.isDirectory) onView() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (info.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (info.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = info.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${info.displaySize} | ${info.formattedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
