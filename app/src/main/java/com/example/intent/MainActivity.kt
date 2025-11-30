package com.example.intent

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.intent.data.AppConfig
import com.example.intent.data.AppDatabase
import com.example.intent.ui.theme.IntentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.intent.ui.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainContent()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    @Composable
    fun MainContent() {
        val viewModel: MainViewModel = viewModel()
        var showPermissionDialog by remember { mutableStateOf(false) }
        // Navigation State
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
        var selectedAppPackage by remember { mutableStateOf<String?>(null) }
        var selectedAppName by remember { mutableStateOf<String?>(null) }
        val settingsManager = remember { com.example.intent.data.SettingsManager(this) }
        val savedThemeMode = settingsManager.getThemeMode()
        val systemInDarkTheme = isSystemInDarkTheme()
        
        // If saved is -1 (default), use system theme. Else use saved.
        var themeMode by remember { 
            mutableStateOf(
                if (savedThemeMode == -1) {
                    if (systemInDarkTheme) 1 else 0
                } else {
                    savedThemeMode
                }
            ) 
        }
        var popupDelay by remember { mutableStateOf(settingsManager.getPopupDelay()) }

        IntentTheme(themeMode = themeMode) {
            LaunchedEffect(Unit) {
                if (!isAccessibilityServiceEnabled() || !Settings.canDrawOverlays(this@MainActivity)) {
                    showPermissionDialog = true
                }
            }

            if (showPermissionDialog) {
                PermissionDialog(
                    onDismiss = { showPermissionDialog = false },
                    onGoToSettings = {
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        } else if (!isAccessibilityServiceEnabled()) {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    }
                )
            }

            val apps by viewModel.allApps.collectAsState()
            val recentLogs by viewModel.recentLogs.collectAsState()
            
            // Filter apps (User apps + Updated System apps - Excluded keywords)
            val filteredApps = remember(apps) {
                apps.filter { app ->
                    val pkg = app.packageName.lowercase()
                    val excludedKeywords = listOf("clock", "camera", "calculator", "dialer", "contacts", "settings", "launcher", "android.vending")
                    !excludedKeywords.any { pkg.contains(it) }
                }
            }
            
            val todayLogs by viewModel.todayLogs.collectAsState()

            Scaffold(
                bottomBar = {
                    if (currentScreen != Screen.AppDetails) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            NavigationBarItem(
                                icon = { Icon(androidx.compose.material.icons.Icons.Default.Home, contentDescription = "Home") }, 
                                label = { Text("Home") },
                                selected = currentScreen == Screen.Dashboard,
                                onClick = { currentScreen = Screen.Dashboard },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                            NavigationBarItem(
                                icon = { Icon(androidx.compose.material.icons.Icons.Default.List, contentDescription = "Apps") }, 
                                label = { Text("Apps") },
                                selected = currentScreen == Screen.Apps,
                                onClick = { currentScreen = Screen.Apps },
                                 colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).background(MaterialTheme.colorScheme.background)) {
                    
                    // Handle Back Press
                    androidx.activity.compose.BackHandler(enabled = currentScreen == Screen.AppDetails) {
                        currentScreen = Screen.Dashboard
                    }

                    androidx.compose.animation.AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == Screen.AppDetails) {
                                androidx.compose.animation.slideInHorizontally { it } + androidx.compose.animation.fadeIn() togetherWith
                                androidx.compose.animation.slideOutHorizontally { -it } + androidx.compose.animation.fadeOut()
                            } else if (initialState == Screen.AppDetails) {
                                androidx.compose.animation.slideInHorizontally { -it } + androidx.compose.animation.fadeIn() togetherWith
                                androidx.compose.animation.slideOutHorizontally { it } + androidx.compose.animation.fadeOut()
                            } else {
                                androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut()
                            }
                        },
                        label = "ScreenTransition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            Screen.Dashboard -> {
                                com.example.intent.ui.home.DashboardScreen(
                                    allApps = filteredApps,
                                    todayLogs = todayLogs,
                                    themeMode = themeMode,
                                    popupDelay = popupDelay,
                                    onPopupDelayChange = { 
                                        popupDelay = it
                                        settingsManager.setPopupDelay(it)
                                    },
                                    onToggleTheme = { 
                                        themeMode = (themeMode + 1) % 3
                                        settingsManager.setThemeMode(themeMode)
                                    },
                                    onAppClick = { app ->
                                        selectedAppPackage = app.packageName
                                        selectedAppName = app.appName
                                        currentScreen = Screen.AppDetails
                                    }
                                )
                            }
                            Screen.Apps -> {
                                com.example.intent.ui.home.AppSelectionScreen(
                                    apps = filteredApps,
                                    onToggleMonitor = { app, isMonitored ->
                                        viewModel.updateAppMonitoring(app.packageName, isMonitored)
                                    }
                                )
                            }
                            Screen.AppDetails -> {
                                if (selectedAppPackage != null) {
                                    val appLogs by viewModel.getLogsForPackage(selectedAppPackage!!).collectAsState(initial = emptyList())
                                    com.example.intent.ui.home.AppDetailsScreen(
                                        packageName = selectedAppPackage!!,
                                        appName = selectedAppName ?: "App",
                                        logs = appLogs,
                                        onBack = { currentScreen = Screen.Dashboard }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    enum class Screen {
        Dashboard, Apps, AppDetails
    }

    @Composable
    fun PermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Permissions Required") },
            text = { Text("Control Buddy needs 'Display over other apps' and 'Accessibility Service' permissions to work.") },
            confirmButton = {
                Button(onClick = onGoToSettings) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }


}
