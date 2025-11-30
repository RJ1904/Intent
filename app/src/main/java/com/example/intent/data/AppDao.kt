package com.example.intent.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_config ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppConfig>>

    @Query("SELECT * FROM app_config WHERE isMonitored = 1")
    fun getMonitoredApps(): List<AppConfig> // Synchronous for Service

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppConfig(appConfig: AppConfig)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllAppConfigs(appConfigs: List<AppConfig>)

    @Query("UPDATE app_config SET isMonitored = :isMonitored WHERE packageName = :packageName")
    suspend fun updateAppMonitoring(packageName: String, isMonitored: Boolean)

    @Query("DELETE FROM app_config WHERE packageName NOT IN (:packageNames)")
    suspend fun deleteAppsNotIn(packageNames: List<String>)

    @Insert
    suspend fun insertUsageLog(log: UsageLog): Long

    @Query("UPDATE usage_log SET duration = :duration WHERE id = :id")
    suspend fun updateUsageDuration(id: Long, duration: Long)

    @Query("SELECT COUNT(*) FROM usage_log WHERE timestamp >= :startTime")
    fun getUsageCountSince(startTime: Long): Flow<Int>
    
    @Query("SELECT * FROM usage_log ORDER BY timestamp DESC LIMIT 20")
    fun getRecentLogs(): Flow<List<UsageLog>>

    @Query("SELECT * FROM usage_log WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getLogsSince(startTime: Long): Flow<List<UsageLog>>

    @Query("SELECT * FROM usage_log WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getLogsSinceSync(startTime: Long): List<UsageLog>

    @Query("SELECT * FROM usage_log WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getLogsForPackage(packageName: String): Flow<List<UsageLog>>
}
