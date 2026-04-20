package com.accessswitch.netflix

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.accessswitch.scanning.ScanItem
import com.accessswitch.scanning.ScanningEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls Netflix via AccessibilityService key event injection.
 * Detects browse vs. playback mode and provides appropriate
 * ScanItem lists for the scanning overlay.
 *
 * Browse mode: D-pad navigation (Up/Down/Left/Right/Select/Back)
 * Playback mode: Media controls (Play/Pause, FF, Rewind, Volume, Stop)
 *
 * Mode detection is handled by [NetflixModeDetector] via accessibility events.
 * Key injection is handled by [KeyInjector].
 */
@Singleton
class NetflixController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modeDetector: NetflixModeDetector,
    private val keyInjector: KeyInjector
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** Whether the Netflix panel is currently active in the scanner */
    private var isActive = false

    /** Reference to the scanning engine when panel is active */
    private var scanningEngine: ScanningEngine? = null

    /** Callback to return to main menu */
    private var backToMenuCallback: (() -> Unit)? = null

    /** Job for observing mode changes */
    private var modeObserverJob: Job? = null

    /**
     * Build scannable items based on current Netflix mode.
     */
    fun buildScanItems(): List<ScanItem> {
        return when (modeDetector.mode.value) {
            NetflixMode.INACTIVE -> buildInactiveItems()
            NetflixMode.BROWSE -> buildBrowseItems()
            NetflixMode.PLAYBACK -> buildPlaybackItems()
        }
    }

    /**
     * Activate the Netflix panel in the scanning engine.
     * Observes mode changes and auto-updates scan items.
     *
     * @param engine The scanning engine to load items into
     * @param backToMenu Callback to return to the main menu
     */
    fun activateNetflixPanel(engine: ScanningEngine, backToMenu: () -> Unit) {
        isActive = true
        scanningEngine = engine
        backToMenuCallback = backToMenu

        // Load initial items
        engine.setItems(buildScanItems())

        // Observe mode changes and auto-update items
        modeObserverJob?.cancel()
        modeObserverJob = scope.launch {
            modeDetector.mode.collectLatest { mode ->
                if (isActive) {
                    engine.setItems(buildScanItems())
                }
            }
        }
    }

    /**
     * Deactivate the Netflix panel.
     */
    fun deactivateNetflixPanel() {
        isActive = false
        modeObserverJob?.cancel()
        modeObserverJob = null
        scanningEngine = null
        backToMenuCallback = null
    }

    /**
     * Launch the Netflix app.
     */
    fun launchNetflix() {
        try {
            // Try launch intent for Netflix
            val launchIntent = context.packageManager.getLaunchIntentForPackage(
                NetflixModeDetector.NETFLIX_PACKAGE
            )
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                // Netflix not installed — open Play Store
                val storeIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${NetflixModeDetector.NETFLIX_PACKAGE}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(storeIntent)
            }
        } catch (_: Exception) {
            // Silent fail
        }
    }

    /**
     * Check if Netflix is installed on the device.
     */
    fun isNetflixInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(
                NetflixModeDetector.NETFLIX_PACKAGE, 0
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    // --- Item builders ---

    /**
     * Items shown when Netflix is not active.
     * Offers to launch Netflix or go back to menu.
     */
    private fun buildInactiveItems(): List<ScanItem> {
        val items = mutableListOf<ScanItem>()

        items.add(ScanItem(
            id = "netflix_launch",
            label = "Open Netflix",
            action = { launchNetflix() }
        ))

        items.add(ScanItem(
            id = "netflix_toggle_mode",
            label = "Toggle Mode",
            action = {
                modeDetector.manualToggle()
            }
        ))

        items.add(ScanItem(
            id = "netflix_back_to_menu",
            label = "Back to Menu",
            action = {
                deactivateNetflixPanel()
                backToMenuCallback?.invoke()
            }
        ))

        return items
    }

    /**
     * D-pad navigation items for Netflix browse mode.
     * 3x3 grid: Up, Down, Left, Right, Select, Back + mode toggle + back to menu.
     */
    private fun buildBrowseItems(): List<ScanItem> = listOf(
        ScanItem(
            id = "netflix_up",
            label = "Up",
            action = { keyInjector.dpadUp() }
        ),
        ScanItem(
            id = "netflix_down",
            label = "Down",
            action = { keyInjector.dpadDown() }
        ),
        ScanItem(
            id = "netflix_left",
            label = "Left",
            action = { keyInjector.dpadLeft() }
        ),
        ScanItem(
            id = "netflix_right",
            label = "Right",
            action = { keyInjector.dpadRight() }
        ),
        ScanItem(
            id = "netflix_select",
            label = "Select",
            action = { keyInjector.dpadCenter() }
        ),
        ScanItem(
            id = "netflix_back",
            label = "Back",
            action = { keyInjector.performBack() }
        ),
        ScanItem(
            id = "netflix_toggle_mode",
            label = "Playback Mode",
            action = { modeDetector.manualToggle() }
        ),
        ScanItem(
            id = "netflix_back_to_menu",
            label = "Back to Menu",
            action = {
                deactivateNetflixPanel()
                backToMenuCallback?.invoke()
            }
        ),
    )

    /**
     * Media control items for Netflix playback mode.
     * Play/Pause, FF, Rewind, Volume, Stop + mode toggle + back to menu.
     */
    private fun buildPlaybackItems(): List<ScanItem> = listOf(
        ScanItem(
            id = "netflix_play_pause",
            label = "Play/Pause",
            action = { keyInjector.mediaPlayPause() }
        ),
        ScanItem(
            id = "netflix_forward",
            label = "+10s",
            action = { keyInjector.mediaFastForward() }
        ),
        ScanItem(
            id = "netflix_rewind",
            label = "-10s",
            action = { keyInjector.mediaRewind() }
        ),
        ScanItem(
            id = "netflix_vol_up",
            label = "Vol +",
            action = { keyInjector.volumeUp() }
        ),
        ScanItem(
            id = "netflix_vol_down",
            label = "Vol -",
            action = { keyInjector.volumeDown() }
        ),
        ScanItem(
            id = "netflix_stop",
            label = "Stop",
            action = { keyInjector.performBack() }
        ),
        ScanItem(
            id = "netflix_toggle_mode",
            label = "Browse Mode",
            action = { modeDetector.manualToggle() }
        ),
        ScanItem(
            id = "netflix_back_to_menu",
            label = "Back to Menu",
            action = {
                deactivateNetflixPanel()
                backToMenuCallback?.invoke()
            }
        ),
    )
}
