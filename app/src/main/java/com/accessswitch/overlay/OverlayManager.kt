package com.accessswitch.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.accessswitch.scanning.ScanningEngine
import com.accessswitch.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the TYPE_ACCESSIBILITY_OVERLAY window that renders the scanning grid.
 *
 * This window can only be created by an AccessibilityService, so this class
 * receives the service's Context to obtain a WindowManager.
 *
 * Hosts a ComposeView inside the overlay that observes ScanningEngine.state
 * and renders the grid of ScanItem tiles with highlight.
 */
@Singleton
class OverlayManager @Inject constructor(
    private val scanningEngine: ScanningEngine,
    private val settingsRepository: SettingsRepository
) {
    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var isShowing = false

    /**
     * Create and show the scanning overlay window.
     * Must be called from AccessSwitchAccessibilityService with its context.
     */
    fun show(serviceContext: Context) {
        if (isShowing) return

        val wm = serviceContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        // Create lifecycle owner for ComposeView
        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner

        val composeView = ComposeView(serviceContext).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                ScanningOverlayContent(
                    scanningEngine = scanningEngine,
                    settingsRepository = settingsRepository
                )
            }
        }
        overlayView = composeView

        owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        wm.addView(composeView, params)
        isShowing = true
    }

    /**
     * Remove the overlay window and clean up.
     */
    fun hide() {
        if (!isShowing) return

        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
                // View may already be removed
            }
        }

        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        overlayView = null
        lifecycleOwner = null
        windowManager = null
        isShowing = false
    }

    /**
     * Returns whether the overlay is currently displayed.
     */
    fun isVisible(): Boolean = isShowing
}

/**
 * Minimal LifecycleOwner + SavedStateRegistryOwner for ComposeView
 * used outside an Activity/Fragment (in an overlay window).
 */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
