package com.accessswitch.netflix

import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects whether Netflix is in the foreground and whether it's showing
 * browse (home/search) or playback (video player) UI.
 *
 * Detection strategy:
 * 1. Check `event.packageName` for Netflix's package
 * 2. Inspect `event.className` for known activity patterns:
 *    - Player activities contain "player" or "Player" in the class name
 *    - All other Netflix activities are treated as browse mode
 * 3. Fallback: manual mode toggle if detection fails
 *
 * Risk: Netflix may rename activities in updates. The manual toggle
 * fallback ensures continued functionality.
 */
@Singleton
class NetflixModeDetector @Inject constructor() {

    private val _mode = MutableStateFlow(NetflixMode.INACTIVE)
    val mode: StateFlow<NetflixMode> = _mode.asStateFlow()

    /** Whether the user has manually overridden the mode */
    private var manualOverride = false

    /**
     * Process an accessibility event to detect Netflix mode changes.
     * Called from AccessSwitchAccessibilityService.onAccessibilityEvent().
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: return

        if (packageName == NETFLIX_PACKAGE) {
            if (manualOverride) return // Don't auto-switch when user overrode

            val detectedMode = classifyActivity(className)
            _mode.value = detectedMode
        } else {
            // Another app came to foreground
            if (_mode.value != NetflixMode.INACTIVE) {
                _mode.value = NetflixMode.INACTIVE
                manualOverride = false
            }
        }
    }

    /**
     * Manually toggle between browse and playback modes.
     * Used as a fallback when auto-detection fails.
     */
    fun manualToggle() {
        manualOverride = true
        _mode.value = when (_mode.value) {
            NetflixMode.BROWSE -> NetflixMode.PLAYBACK
            NetflixMode.PLAYBACK -> NetflixMode.BROWSE
            NetflixMode.INACTIVE -> NetflixMode.BROWSE // Assume browse if activating manually
        }
    }

    /**
     * Clear manual override, allowing auto-detection to resume.
     */
    fun clearManualOverride() {
        manualOverride = false
    }

    /**
     * Force a specific mode (used when user explicitly enters Netflix panel).
     */
    fun setMode(mode: NetflixMode) {
        _mode.value = mode
        manualOverride = false
    }

    /**
     * Classify a Netflix activity class name into browse or playback mode.
     *
     * Known patterns (as of Netflix app ~2024):
     * - Player: `com.netflix.mediaclient.ui.player.PlayerActivity`
     *           `...PlayerFragment`, `...VideoPlayerActivity`
     * - Browse: `com.netflix.mediaclient.ui.home.HomeActivity`
     *           `...BrowseActivity`, `...SearchActivity`, `...DetailsActivity`
     *
     * Heuristic: if className contains "player" (case-insensitive), it's playback.
     */
    internal fun classifyActivity(className: String): NetflixMode {
        val lower = className.lowercase()
        return when {
            PLAYER_PATTERNS.any { it in lower } -> NetflixMode.PLAYBACK
            else -> NetflixMode.BROWSE
        }
    }

    companion object {
        const val NETFLIX_PACKAGE = "com.netflix.mediaclient"

        /** Patterns in class names that indicate playback mode */
        private val PLAYER_PATTERNS = listOf(
            "player",
            "playback",
            "videoplayer",
            "watchvideo"
        )
    }
}
