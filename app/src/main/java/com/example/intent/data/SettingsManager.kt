package com.example.intent.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_POPUP_DELAY = "popup_delay"
        const val KEY_THEME_MODE = "theme_mode" // 0=Light, 1=Dark, 2=PitchBlack
    }

    fun getPopupDelay(): Int {
        return prefs.getInt(KEY_POPUP_DELAY, 0)
    }

    fun setPopupDelay(seconds: Int) {
        prefs.edit().putInt(KEY_POPUP_DELAY, seconds).apply()
    }

    fun getThemeMode(): Int {
        return prefs.getInt(KEY_THEME_MODE, -1) // Default -1 (Not set, follow system)
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }
}
