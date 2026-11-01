package com.example.safebridge.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.safebridge.ui.screens.AboutScreen
import com.example.safebridge.ui.screens.AdbScreen
import com.example.safebridge.ui.screens.ClientDemoScreen
import com.example.safebridge.ui.screens.ClientRequestsScreen
import com.example.safebridge.ui.screens.DashboardScreen
import com.example.safebridge.ui.screens.LogsScreen
import com.example.safebridge.ui.screens.SandboxScreen
import com.example.safebridge.ui.screens.ServiceControlScreen
import com.example.safebridge.ui.screens.SettingsScreen
import com.example.safebridge.ui.viewmodel.MainViewModel

enum class SafeBridgeScreen(
    val titleEn: String,
    val titleAr: String,
    val icon: ImageVector
) {
    DASHBOARD("Dashboard", "الرئيسية", Icons.Default.Dashboard),
    SERVICE("Service", "الخدمة", Icons.Default.Security),
    CLIENTS("Clients", "العملاء", Icons.Default.Cable),
    SANDBOX("Sandbox", "العزل", Icons.Default.Folder),
    DEMO("Demo Client", "التجربة", Icons.Default.Terminal),
    ADB("Safe ADB", "ADB الآمن", Icons.Default.Security),
    LOGS("Logs", "السجلات", Icons.Default.List),
    SETTINGS("Settings", "الإعدادات", Icons.Default.Settings),
    ABOUT("About", "عن التطبيق", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(SafeBridgeScreen.DASHBOARD) }
    val isArabic by viewModel.isArabic.collectAsState()

    androidx.activity.compose.BackHandler(enabled = currentScreen != SafeBridgeScreen.DASHBOARD) {
        currentScreen = SafeBridgeScreen.DASHBOARD
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) currentScreen.titleAr else currentScreen.titleEn,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        bottomBar = {
            ScrollableTabRow(
                selectedTabIndex = currentScreen.ordinal,
                edgePadding = 8.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                SafeBridgeScreen.values().forEach { screen ->
                    Tab(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        text = {
                            Text(
                                text = if (isArabic) screen.titleAr else screen.titleEn,
                                maxLines = 1
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.titleEn,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.testTag("tab_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                SafeBridgeScreen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToService = { currentScreen = SafeBridgeScreen.SERVICE },
                    onNavigateToClients = { currentScreen = SafeBridgeScreen.CLIENTS },
                    onNavigateToSandbox = { currentScreen = SafeBridgeScreen.SANDBOX },
                    onNavigateToAdb = { currentScreen = SafeBridgeScreen.ADB },
                    onNavigateToDemo = { currentScreen = SafeBridgeScreen.DEMO }
                )
                SafeBridgeScreen.SERVICE -> ServiceControlScreen(viewModel = viewModel)
                SafeBridgeScreen.CLIENTS -> ClientRequestsScreen(viewModel = viewModel)
                SafeBridgeScreen.SANDBOX -> SandboxScreen(viewModel = viewModel)
                SafeBridgeScreen.DEMO -> ClientDemoScreen(viewModel = viewModel)
                SafeBridgeScreen.ADB -> AdbScreen(viewModel = viewModel)
                SafeBridgeScreen.LOGS -> LogsScreen(viewModel = viewModel)
                SafeBridgeScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                SafeBridgeScreen.ABOUT -> AboutScreen(viewModel = viewModel)
            }
        }
    }
}
