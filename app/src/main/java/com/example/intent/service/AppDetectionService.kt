package com.example.intent.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.intent.data.AppDatabase
import com.example.intent.ui.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class AppDetectionService : AccessibilityService() {

    private lateinit var overlayManager: OverlayManager
    private lateinit var database: AppDatabase
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    // Cache monitored apps to avoid DB hits on every event
    private var monitoredPackages = setOf<String>()
    
    // Prevent loop: Keep track of currently unlocked app
    private var currentUnlockedPackage: String? = null
    
    private var monitoringJob: Job? = null
    // 10 minutes in milliseconds
    private val USAGE_LIMIT_MS = 10 * 60 * 1000L

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.d("AppDetectionService", "Service connected")
        overlayManager = OverlayManager(this)
        database = AppDatabase.getDatabase(this)
        
        // Observe monitored apps
        serviceScope.launch(Dispatchers.IO) {
            database.appDao().getAllApps().collect { apps ->
                monitoredPackages = apps.filter { it.isMonitored }.map { it.packageName }.toSet()
                android.util.Log.d("AppDetectionService", "Monitored packages updated: $monitoredPackages")
            }
        }
    }

    // Session tracking
    private var currentSessionStart: Long = 0
    private var currentLogId: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            android.util.Log.d("AppDetectionService", "Window changed: $packageName")
            

            
            // Ignore system packages (Notification shade, Keyboard, etc.) to prevent session reset
            val ignoredPackages = setOf(
                "com.android.systemui",
                "com.google.android.inputmethod.latin", // Gboard
                "com.google.android.gms", // Play Services (Ads often run here)
                "android" // Android System
            )

            if (ignoredPackages.contains(packageName)) {
                android.util.Log.d("AppDetectionService", "Ignoring system package: $packageName")
                return
            }

            if (monitoredPackages.contains(packageName)) {
                android.util.Log.d("AppDetectionService", "Match found for: $packageName")
                if (currentUnlockedPackage == packageName) {
                    android.util.Log.d("AppDetectionService", "Already unlocked: $packageName")
                    // Already unlocked this session, ignore
                    return
                }
                
                // Check overlay permission to avoid crash
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    android.util.Log.e("AppDetectionService", "Overlay permission missing")
                    return
                }

                android.util.Log.d("AppDetectionService", "Showing overlay for: $packageName")
                // Show overlay
                overlayManager.showOverlay(
                    packageName = packageName,
                    onContinue = { intent, note ->
                        android.util.Log.d("AppDetectionService", "Unlocked: $packageName")
                        
                        // If we are continuing an existing session (e.g. after 10 min reminder),
                        // close the previous log entry first.
                        if (currentUnlockedPackage == packageName && currentLogId > 0L) {
                             val duration = System.currentTimeMillis() - currentSessionStart
                             val logIdToUpdate = currentLogId
                             serviceScope.launch(Dispatchers.IO) {
                                 database.appDao().updateUsageDuration(logIdToUpdate, duration)
                                 android.util.Log.d("AppDetectionService", "Session rotated. Updated duration: ${duration}ms for log $logIdToUpdate")
                             }
                        }

                        // Start (or restart) session
                        currentUnlockedPackage = packageName
                        currentSessionStart = System.currentTimeMillis()
                        
                        // Log start
                        serviceScope.launch(Dispatchers.IO) {
                            currentLogId = database.appDao().insertUsageLog(
                                com.example.intent.data.UsageLog(
                                    packageName = packageName,
                                    timestamp = currentSessionStart,
                                    intentCategory = intent,
                                    userNote = note
                                )
                            )
                        }
                        
                        // Start the 10-minute timer
                        startMonitoringJob(packageName)
                    },
                    onDismiss = {
                        android.util.Log.d("AppDetectionService", "Dismissed: $packageName")
                        // Go home
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        currentUnlockedPackage = null
                    }
                )
            } else {
                // Switched to a non-monitored app, reset lock and save duration
                monitoringJob?.cancel()
                if (currentUnlockedPackage != null) {
                     android.util.Log.d("AppDetectionService", "Ending session for $currentUnlockedPackage (switched to $packageName)")
                     val duration = System.currentTimeMillis() - currentSessionStart
                     val logIdToUpdate = currentLogId
                     
                     if (logIdToUpdate > 0) {
                         serviceScope.launch(Dispatchers.IO) {
                             database.appDao().updateUsageDuration(logIdToUpdate, duration)
                             android.util.Log.d("AppDetectionService", "Updated duration: ${duration}ms for log $logIdToUpdate")
                         }
                     }
                }
                currentUnlockedPackage = null
                currentLogId = 0
            }
        }
    }

    override fun onInterrupt() {
        overlayManager.removeOverlay()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        overlayManager.removeOverlay()
    }

    private fun startMonitoringJob(packageName: String) {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch(Dispatchers.Main) {
            delay(USAGE_LIMIT_MS)
            if (currentUnlockedPackage == packageName) {
                android.util.Log.d("AppDetectionService", "10 minutes passed for $packageName. Showing overlay.")
                // Force show overlay by bypassing the "already unlocked" check in onAccessibilityEvent 
                // (since we are calling overlayManager directly).
                // Note: OverlayManager.showOverlay checks if view is already attached.
                // If the user is using the app, the overlay should be gone, so this will show it.
                
                overlayManager.showOverlay(
                    packageName = packageName,
                    onContinue = { intent, note ->
                        // Recursively call the same logic as the main onContinue
                        // We can actually just trigger the same callback logic if we extract it, 
                        // but for now let's duplicate the essential "continue" logic or expose a method.
                        // Actually, we can just pass the SAME callback as above? 
                        // But we don't have access to the `onContinue` lambda defined in `onAccessibilityEvent`.
                        
                        // Let's copy the logic:
                        android.util.Log.d("AppDetectionService", "Unlocked (after timer): $packageName")
                        
                        // Close previous log
                        if (currentLogId > 0L) {
                             val duration = System.currentTimeMillis() - currentSessionStart
                             val logIdToUpdate = currentLogId
                             serviceScope.launch(Dispatchers.IO) {
                                 database.appDao().updateUsageDuration(logIdToUpdate, duration)
                             }
                        }
                        
                        // Start new session
                        currentUnlockedPackage = packageName
                        currentSessionStart = System.currentTimeMillis()
                        
                        serviceScope.launch(Dispatchers.IO) {
                            currentLogId = database.appDao().insertUsageLog(
                                com.example.intent.data.UsageLog(
                                    packageName = packageName,
                                    timestamp = currentSessionStart,
                                    intentCategory = intent,
                                    userNote = note
                                )
                            )
                        }
                        
                        // Restart timer
                        startMonitoringJob(packageName)
                    },
                    onDismiss = {
                        android.util.Log.d("AppDetectionService", "Dismissed (after timer): $packageName")
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        currentUnlockedPackage = null
                        monitoringJob?.cancel()
                    }
                )
            }
        }
    }
}
