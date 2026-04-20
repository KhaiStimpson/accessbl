package com.accessswitch.input

import android.os.SystemClock
import com.accessswitch.scanning.ScanningEngine
import com.accessswitch.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central coordinator that normalises events from all input sources
 * (HW switch, phone touch, BT HID) into a single stream consumed
 * by [ScanningEngine].
 *
 * Handles cross-source deduplication: if both an external switch AND
 * the phone screen fire the same switch within the debounce window,
 * only the first event is processed.
 */
@Singleton
class SwitchInputHub @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scanningEngine: ScanningEngine
) {
    // Track last event time per switch for cross-source dedup
    private val lastEventTime = mutableMapOf<SwitchId, Long>()

    // Tracks connected input sources
    private val _connectedSources = MutableStateFlow(setOf<SwitchSource>())
    val connectedSources: StateFlow<Set<SwitchSource>> = _connectedSources.asStateFlow()

    // Emits disconnect events for the overlay to show a banner
    private val _disconnectEvent = MutableStateFlow<SwitchSource?>(null)
    val disconnectEvent: StateFlow<SwitchSource?> = _disconnectEvent.asStateFlow()

    /**
     * Called by all input listeners (HwSwitchListener, PhoneSwitchTouchListener, etc.)
     * Routes the event to the scanning engine after dedup.
     */
    fun onRawEvent(switchId: SwitchId, source: SwitchSource) {
        val now = SystemClock.elapsedRealtime()
        if (isDuplicate(switchId, now)) return

        lastEventTime[switchId] = now

        // Track that this source is active
        val currentSources = _connectedSources.value
        if (source !in currentSources) {
            _connectedSources.value = currentSources + source
        }

        // Clear any disconnect banner for this source
        if (_disconnectEvent.value == source) {
            _disconnectEvent.value = null
        }

        scanningEngine.onSwitchPressed(switchId)
    }

    /**
     * Called when a switch source is disconnected (e.g., BT switch lost, USB removed).
     * Emits a disconnect event for the overlay to show a reconnect banner.
     */
    fun onSwitchSourceDisconnected(source: SwitchSource) {
        val currentSources = _connectedSources.value
        _connectedSources.value = currentSources - source
        _disconnectEvent.value = source
    }

    /**
     * Clear the disconnect event (e.g., after the user dismisses the banner).
     */
    fun clearDisconnectEvent() {
        _disconnectEvent.value = null
    }

    /**
     * Returns true if an event for this switch was already processed
     * within the debounce window.
     */
    private fun isDuplicate(switchId: SwitchId, timestampMs: Long): Boolean {
        val lastTime = lastEventTime[switchId] ?: return false
        val debounceMs = settingsRepository.currentSettings.debounceMs
        return (timestampMs - lastTime) < debounceMs
    }
}
