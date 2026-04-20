package com.accessswitch.scanning

import com.accessswitch.input.SwitchId
import com.accessswitch.overlay.FeedbackManager
import com.accessswitch.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Current state of the scanning engine, observed by the overlay UI.
 */
data class ScanState(
    /** Index of the currently highlighted item (-1 if idle) */
    val highlightedIndex: Int = -1,
    /** Whether scanning is actively running */
    val isScanning: Boolean = false,
    /** The current list of scannable items */
    val items: List<ScanItem> = emptyList(),
    /** Current scan loop count */
    val currentLoop: Int = 0,
    /** Whether we're in a row-column sub-scan (scanning within a row) */
    val inSubScan: Boolean = false,
    /** Index of the selected row (for row-column mode) */
    val selectedRowIndex: Int = -1,
    /** The original top-level items (preserved during sub-scan) */
    val topLevelItems: List<ScanItem> = emptyList()
)

/**
 * Core scanning engine that manages the scan timer, state machine,
 * and selection logic.
 *
 * State machine: IDLE -> SCANNING (RUNNING) -> SELECTED (ACTION) -> IDLE
 *
 * Receives switch events from [SwitchInputHub] (via [onSwitchPressed]).
 * Emits [ScanState] for the overlay to observe and render highlights.
 */
@Singleton
class ScanningEngine @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val feedbackManager: FeedbackManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var scanJob: Job? = null

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    /**
     * Set the list of scannable items and optionally start scanning.
     */
    fun setItems(items: List<ScanItem>, autoStart: Boolean = true) {
        _state.value = ScanState(items = items, topLevelItems = items)
        if (autoStart && items.isNotEmpty()) {
            start()
        }
    }

    /**
     * Start or restart auto-scan from the beginning.
     */
    fun start() {
        val items = _state.value.items
        if (items.isEmpty()) return

        val settings = settingsRepository.currentSettings
        when (settings.scanMode) {
            ScanMode.AUTO -> startAutoScan()
            ScanMode.STEP -> {
                // Step scan: just highlight first item, wait for switch presses
                _state.value = _state.value.copy(
                    isScanning = true,
                    highlightedIndex = 0,
                    currentLoop = 0
                )
            }
            ScanMode.INVERSE -> startAutoScan() // Similar to auto but select on release
            ScanMode.ROW_COLUMN -> startAutoScan() // Scan rows first
        }
    }

    /**
     * Stop scanning and return to idle.
     */
    fun stop() {
        scanJob?.cancel()
        scanJob = null
        _state.value = ScanState(items = _state.value.topLevelItems.ifEmpty { _state.value.items })
    }

    /**
     * Pause scanning (e.g., screen off, switch disconnected).
     */
    fun pause() {
        scanJob?.cancel()
        scanJob = null
        _state.value = _state.value.copy(isScanning = false)
    }

    /**
     * Resume scanning from where it was paused.
     */
    fun resume() {
        if (_state.value.items.isNotEmpty() && !_state.value.isScanning) {
            startAutoScan()
        }
    }

    /**
     * Called by SwitchInputHub when a switch is pressed.
     */
    fun onSwitchPressed(switchId: SwitchId) {
        val currentState = _state.value
        if (!currentState.isScanning || currentState.items.isEmpty()) return

        val settings = settingsRepository.currentSettings

        when (settings.scanMode) {
            ScanMode.AUTO -> {
                // In auto-scan, any switch press selects the current item
                selectCurrentItem()
            }
            ScanMode.STEP -> {
                when (switchId) {
                    SwitchId.SWITCH_1 -> advanceHighlight()
                    SwitchId.SWITCH_2 -> selectCurrentItem()
                }
            }
            ScanMode.INVERSE -> {
                // Inverse: press stops scan and selects
                selectCurrentItem()
            }
            ScanMode.ROW_COLUMN -> {
                handleRowColumnPress(switchId)
            }
        }
    }

    private fun startAutoScan() {
        scanJob?.cancel()
        val settings = settingsRepository.currentSettings

        _state.value = _state.value.copy(
            isScanning = true,
            highlightedIndex = 0,
            currentLoop = 0
        )

        scanJob = scope.launch {
            var index = 0
            var loops = 0
            val items = _state.value.items

            while (true) {
                _state.value = _state.value.copy(
                    highlightedIndex = index,
                    currentLoop = loops
                )

                // Fire audio feedback on advance (not on the very first highlight)
                if (index > 0 || loops > 0) {
                    feedbackManager.onHighlightAdvance()
                }

                delay(settings.scanIntervalMs)

                index++
                if (index >= items.size) {
                    index = 0
                    loops++
                    if (settings.scanLoops > 0 && loops >= settings.scanLoops) {
                        stop()
                        return@launch
                    }
                }
            }
        }
    }

    private fun advanceHighlight() {
        val current = _state.value
        val nextIndex = (current.highlightedIndex + 1) % current.items.size
        _state.value = current.copy(highlightedIndex = nextIndex)
        feedbackManager.onHighlightAdvance()
    }

    private fun selectCurrentItem() {
        val current = _state.value
        val index = current.highlightedIndex
        if (index < 0 || index >= current.items.size) return

        val item = current.items[index]

        // Provide selection feedback
        feedbackManager.onItemSelected()

        // Execute the action
        item.action()

        // Restart scanning from beginning
        scanJob?.cancel()
        start()
    }

    private fun handleRowColumnPress(switchId: SwitchId) {
        val current = _state.value

        if (!current.inSubScan) {
            // Currently scanning rows — a press enters the highlighted row
            val rowItem = current.items.getOrNull(current.highlightedIndex) ?: return
            if (rowItem.children.isNotEmpty()) {
                // Enter sub-scan for this row's children
                _state.value = current.copy(
                    inSubScan = true,
                    selectedRowIndex = current.highlightedIndex,
                    items = rowItem.children,
                    highlightedIndex = 0
                )
                scanJob?.cancel()
                startAutoScan()
            } else {
                selectCurrentItem()
            }
        } else {
            // Currently scanning within a row — select the item
            selectCurrentItem()
        }
    }
}
