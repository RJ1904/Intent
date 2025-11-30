package com.example.intent.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isMonitored: Boolean = false
)

@Entity(tableName = "usage_log")
data class UsageLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long,
    val intentCategory: String, // e.g., "Bored", "Learning"
    val userNote: String,
    val duration: Long = 0
)
