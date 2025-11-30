package com.example.intent.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.intent.data.AppConfig
import com.example.intent.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val appDao = database.appDao()

    val allApps = appDao.getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs = appDao.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculate start of day for todayLogs
    private val startOfDay: Long
        get() {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }

    val todayLogs = appDao.getLogsSince(startOfDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncInstalledApps()
    }

    private fun syncInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if we need to populate the database (using getMonitoredApps as proxy for "is DB populated/used" per original logic, 
            // but checking getAllApps would be more correct. Sticking to safe insertion.)
            
            val pm = getApplication<Application>().packageManager
            val installedApps = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                .filter {
                    // Only filter out apps that cannot be launched by the user
                    pm.getLaunchIntentForPackage(it.packageName) != null
                }
                .map {
                    AppConfig(
                        packageName = it.packageName,
                        appName = it.applicationInfo.loadLabel(pm).toString(),
                        isMonitored = false
                    )
                }

            appDao.insertAllAppConfigs(installedApps)
            
            // Remove apps that are no longer installed
            val installedPackageNames = installedApps.map { it.packageName }
            appDao.deleteAppsNotIn(installedPackageNames)
        }
    }

    fun updateAppMonitoring(packageName: String, isMonitored: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            appDao.updateAppMonitoring(packageName, isMonitored)
        }
    }
    
    fun getLogsForPackage(packageName: String) = appDao.getLogsForPackage(packageName)
}
