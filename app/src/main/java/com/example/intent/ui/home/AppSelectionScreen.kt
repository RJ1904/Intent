package com.example.intent.ui.home

import android.graphics.drawable.Drawable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.intent.data.AppConfig
import com.example.intent.data.UsageLog
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    apps: List<AppConfig>,
    onToggleMonitor: (AppConfig, Boolean) -> Unit
) {
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
            Text(
                text = "Monitored Apps List",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        val monitoredApps = apps.filter { it.isMonitored }

        val listState = rememberLazyListState()
        
        LazyColumn(
            state = listState,
            flingBehavior = ScrollableDefaults.flingBehavior(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Selected Apps Section
            if (monitoredApps.isNotEmpty()) {
                item {
                    Text(
                        text = "Selected Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(monitoredApps) { app ->
                    AppItem(app = app, onToggle = { onToggleMonitor(app, it) })
                }
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // All Apps Section
            item {
                Text(
                    text = "All Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(apps) { app ->
                AppItem(app = app, onToggle = { onToggleMonitor(app, it) })
            }
        }
    }
}

@Composable
fun AppItem(app: AppConfig, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    // Use produceState to load the icon asynchronously, checking cache first
    val appIcon by produceState<Drawable?>(initialValue = com.example.intent.utils.AppIconCache.getIcon(app.packageName), key1 = app.packageName) {
        // Check cache again in case it was populated just now (though initialValue handles most cases)
        var icon = com.example.intent.utils.AppIconCache.getIcon(app.packageName)
        if (icon == null) {
            withContext(Dispatchers.IO) {
                try {
                    icon = packageManager.getApplicationIcon(app.packageName)
                    if (icon != null) {
                        com.example.intent.utils.AppIconCache.putIcon(app.packageName, icon!!)
                    }
                } catch (e: Exception) {
                    // Handle error or leave as null
                }
            }
        }
        value = icon
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon
        if (appIcon != null) {
            Image(
                painter = rememberDrawablePainter(drawable = appIcon),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = app.isMonitored,
            onCheckedChange = onToggle
        )
    }
}
