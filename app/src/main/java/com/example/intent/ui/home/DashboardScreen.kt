package com.example.intent.ui.home

import com.example.intent.R
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.intent.data.AppConfig
import com.example.intent.data.UsageLog
import com.example.intent.ui.components.ThemeToggle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings

@Composable
fun DashboardScreen(
    allApps: List<AppConfig>,
    todayLogs: List<UsageLog>,
    themeMode: Int,
    popupDelay: Int,
    onPopupDelayChange: (Int) -> Unit,
    onToggleTheme: () -> Unit,
    onAppClick: (AppConfig) -> Unit
) {
    // Calculate stats
    val appUsageMap = remember(todayLogs) { 
        todayLogs.groupBy { it.packageName }
            .mapValues { entry -> entry.value.sumOf { it.duration } }
    }
    val sessionCountMap = remember(todayLogs) {
        todayLogs.groupBy { it.packageName }
            .mapValues { entry -> entry.value.size }
    }

    val totalDuration = remember(todayLogs) { todayLogs.sumOf { it.duration } }
    val totalSessions = remember(todayLogs) { todayLogs.size }

    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        SettingsDialog(
            currentDelay = popupDelay,
            onDelaySelected = { 
                onPopupDelayChange(it)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Intent Logo Icon
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Intent Logo",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Intent",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThemeToggle(
                    themeMode = themeMode,
                    onToggle = onToggleTheme
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Total Time",
                value = formatDuration(totalDuration),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Total Sessions",
                value = totalSessions.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        val monitoredApps = remember(allApps, appUsageMap) {
            allApps.filter { it.isMonitored }
                .sortedByDescending { appUsageMap[it.packageName] ?: 0L }
        }

        // App Grid
        if (monitoredApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No apps monitored yet. Go to 'Apps' tab to select.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(monitoredApps, key = { it.packageName }) { app ->
                    val sessionCount = sessionCountMap[app.packageName] ?: 0
                    val duration = appUsageMap[app.packageName] ?: 0L
                    
                    AppUsageTile(
                        app = app,
                        sessionCount = sessionCount,
                        duration = duration,
                        onClick = { onAppClick(app) }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun SettingsDialog(
    currentDelay: Int,
    onDelaySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var customDelayText by remember { mutableStateOf(if (currentDelay !in listOf(0, 5, 10)) currentDelay.toString() else "") }
    var isCustomSelected by remember { mutableStateOf(currentDelay !in listOf(0, 5, 10)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Popup Settings") },
        text = {
            Column {
                Text("Delay before 'Continue' button is enabled:")
                Spacer(modifier = Modifier.height(12.dp))
                
                val standardOptions = listOf(0, 5, 10)
                
                // Standard Options
                standardOptions.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                isCustomSelected = false
                                onDelaySelected(seconds) 
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isCustomSelected && currentDelay == seconds,
                            onClick = { 
                                isCustomSelected = false
                                onDelaySelected(seconds) 
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (seconds == 0) "None (Immediate)" else "$seconds seconds")
                    }
                }

                // Custom Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            isCustomSelected = true
                            val delay = customDelayText.toIntOrNull() ?: 0
                            onDelaySelected(delay)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isCustomSelected,
                        onClick = { 
                            isCustomSelected = true
                            val delay = customDelayText.toIntOrNull() ?: 0
                            onDelaySelected(delay)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Custom")
                }

                if (isCustomSelected) {
                    OutlinedTextField(
                        value = customDelayText,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                customDelayText = newValue
                                val delay = newValue.toIntOrNull() ?: 0
                                onDelaySelected(delay)
                            }
                        },
                        label = { Text("Seconds") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp) // Indent to align with text
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AppUsageTile(
    app: AppConfig,
    sessionCount: Int,
    duration: Long,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    val appIcon by produceState<Drawable?>(initialValue = null, key1 = app.packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                packageManager.getApplicationIcon(app.packageName)
            } catch (e: Exception) {
                null
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square tile
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) // Subtle background
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon
            if (appIcon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = appIcon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1), 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$sessionCount sessions • ${formatDuration(duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatDuration(durationMillis: Long): String {
    val hours = durationMillis / 3600000
    val minutes = (durationMillis % 3600000) / 60000
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
