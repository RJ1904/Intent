package com.example.intent.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.intent.MainActivity
import com.example.intent.R
import com.example.intent.data.AppDatabase
import com.example.intent.data.UsageLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class StatsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, StatsWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // Update widget when size changes
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val (totalTime, totalSessions) = withContext(Dispatchers.IO) {
                getWidgetData(context)
            }

            // Determine widget size with better detection
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

            // Use average dimensions for more stable detection
            val avgWidth = (minWidth + maxWidth) / 2
            val avgHeight = (minHeight + maxHeight) / 2

            // Determine if widget is small (less than 2x2 cells ~140dp x 140dp)
            val isSmallWidget = avgWidth < 180 || avgHeight < 140

            val views = if (isSmallWidget) {
                getSmallWidgetViews(context, totalTime, totalSessions)
            } else {
                getLargeWidgetViews(context, totalTime, totalSessions)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun getSmallWidgetViews(
        context: Context,
        totalTime: String,
        totalSessions: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_small)

        // Set data
        views.setTextViewText(R.id.total_time_small, totalTime)
        views.setTextViewText(R.id.total_sessions_small, totalSessions.toString())

        // Set click intent to open app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root_small, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_logo_small, pendingIntent)

        // Set refresh button
        val refreshIntent = Intent(context, StatsWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refresh_button_small, refreshPendingIntent)

        return views
    }

    private fun getLargeWidgetViews(
        context: Context,
        totalTime: String,
        totalSessions: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_large)

        // Set data
        views.setTextViewText(R.id.total_time, totalTime)
        views.setTextViewText(R.id.total_sessions, totalSessions.toString())

        // Set click intent to open app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Set refresh button
        val refreshIntent = Intent(context, StatsWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)

        return views
    }



    private suspend fun getWidgetData(context: Context): Pair<String, Int> {
        val db = AppDatabase.getDatabase(context)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        val logs = db.appDao().getLogsSinceSync(startOfDay)

        // Calculate total time and sessions
        var totalTimeMs = 0L
        val sessionCount = logs.size

        logs.forEach { log: UsageLog ->
            totalTimeMs += log.duration
        }

        val totalTime = formatDuration(totalTimeMs)

        return Pair(totalTime, sessionCount)
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    private fun android.graphics.drawable.Drawable.toBitmap(
        width: Int,
        height: Int
    ): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        setBounds(0, 0, width, height)
        draw(canvas)
        return bitmap
    }

    data class AppData(
        val packageName: String,
        val appName: String,
        val duration: Long = 0L,
        val sessions: Int = 0,
        val info: String = ""
    )

    companion object {
        private const val ACTION_REFRESH = "com.example.intent.ACTION_REFRESH_WIDGET"
    }
}
