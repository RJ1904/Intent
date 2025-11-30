package com.example.intent.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.intent.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OverlayManager(private val context: Context) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val database = AppDatabase.getDatabase(context)

    // Lifecycle management for Compose
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun showOverlay(packageName: String, onContinue: (String, String) -> Unit, onDismiss: () -> Unit) {
        if (overlayView != null) {
            android.util.Log.d("OverlayManager", "Overlay already showing, ignoring request for $packageName")
            return // Already showing
        }

        android.util.Log.d("OverlayManager", "Creating overlay for $packageName")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // Full screen
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        val composeView = ComposeView(context).apply {
            setContent {
                val settingsManager = com.example.intent.data.SettingsManager(context)
                val delaySeconds = settingsManager.getPopupDelay()
                val savedThemeMode = settingsManager.getThemeMode()
                
                val configuration = context.resources.configuration
                val isSystemDark = (configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                
                val finalThemeMode = if (savedThemeMode == -1) {
                    if (isSystemDark) 1 else 0
                } else {
                    savedThemeMode
                }
                
                com.example.intent.ui.theme.IntentTheme(themeMode = finalThemeMode) {
                    OverlayScreen(
                        delaySeconds = delaySeconds,
                        onContinue = { intent, note ->
                            removeOverlay()
                            onContinue(intent, note)
                        },
                        onDismiss = {
                            removeOverlay()
                            onDismiss()
                        }
                    )
                }
            }
        }

        // Required for Compose to work in a WindowManager view
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        overlayView = composeView
        try {
            windowManager.addView(overlayView, params)
            android.util.Log.d("OverlayManager", "Overlay added to WindowManager")
        } catch (e: Exception) {
            android.util.Log.e("OverlayManager", "Error adding overlay", e)
            e.printStackTrace()
        }
    }

    fun removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
}
