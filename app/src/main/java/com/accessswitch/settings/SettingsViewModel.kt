package com.accessswitch.settings

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accessswitch.input.SwitchId
import com.accessswitch.scanning.ScanMode
import com.accessswitch.switchscreen.SwitchZoneLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // --- Scanning ---

    fun setScanMode(mode: ScanMode) = update { it.copy(scanMode = mode) }

    fun setScanInterval(ms: Long) = update {
        it.copy(scanIntervalMs = ms.coerceIn(SCAN_INTERVAL_MIN, SCAN_INTERVAL_MAX))
    }

    fun setScanLoops(loops: Int) = update {
        it.copy(scanLoops = loops.coerceIn(SCAN_LOOPS_MIN, SCAN_LOOPS_MAX))
    }

    fun setHighlightColor(argb: Int) = update { it.copy(highlightColor = argb) }

    fun setHighlightStyle(style: HighlightStyle) = update { it.copy(highlightStyle = style) }

    fun setAudioFeedback(enabled: Boolean) = update { it.copy(audioFeedback = enabled) }

    fun setHapticFeedback(enabled: Boolean) = update { it.copy(hapticFeedback = enabled) }

    // --- HW Switch ---

    fun setSwitch1Keycode(keycode: Int) = update { it.copy(switch1Keycode = keycode) }

    fun setSwitch2Keycode(keycode: Int) = update { it.copy(switch2Keycode = keycode) }

    fun setDebounce(ms: Long) = update {
        it.copy(debounceMs = ms.coerceIn(DEBOUNCE_MIN, DEBOUNCE_MAX))
    }

    // --- Phone-as-Switch (local) ---

    fun setPhoneLocalSwitchEnabled(enabled: Boolean) = update {
        it.copy(phoneLocalSwitchEnabled = enabled)
    }

    fun setPhoneLocalZoneLayout(layout: SwitchZoneLayout) = update {
        it.copy(phoneLocalZoneLayout = layout)
    }

    fun setPhoneLocalZone1Switch(switchId: SwitchId) = update {
        it.copy(phoneLocalZone1SwitchId = switchId)
    }

    fun setPhoneLocalZone2Switch(switchId: SwitchId) = update {
        it.copy(phoneLocalZone2SwitchId = switchId)
    }

    fun setSwitchScreenLocked(locked: Boolean) = update {
        it.copy(switchScreenLocked = locked)
    }

    // --- Phone-as-Switch (BT HID) ---

    fun setPhoneBtHidEnabled(enabled: Boolean) = update {
        it.copy(phoneBtHidEnabled = enabled)
    }

    fun setPhoneBtHidSwitch1Keycode(keycode: Int) = update {
        it.copy(phoneBtHidSwitch1Keycode = keycode)
    }

    fun setPhoneBtHidSwitch2Keycode(keycode: Int) = update {
        it.copy(phoneBtHidSwitch2Keycode = keycode)
    }

    // --- PIN ---

    /**
     * Verify PIN against stored hash. Returns true if PIN matches or no PIN is set.
     */
    fun verifyPin(pin: String): Boolean {
        val stored = settings.value.settingsPinHash ?: return true
        return hashPin(pin) == stored
    }

    fun isPinSet(): Boolean = settings.value.settingsPinHash != null

    fun setPin(pin: String) = update { it.copy(settingsPinHash = hashPin(pin)) }

    fun clearPin() = update { it.copy(settingsPinHash = null) }

    // --- Contacts ---

    fun addFavouriteContact(contactId: Long) = update {
        it.copy(favouriteContactIds = it.favouriteContactIds + contactId)
    }

    fun removeFavouriteContact(contactId: Long) = update {
        it.copy(favouriteContactIds = it.favouriteContactIds - contactId)
    }

    // --- Helpers ---

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
        }
    }

    companion object {
        const val SCAN_INTERVAL_MIN = 500L
        const val SCAN_INTERVAL_MAX = 8000L
        const val SCAN_LOOPS_MIN = 0   // 0 = infinite
        const val SCAN_LOOPS_MAX = 5
        const val DEBOUNCE_MIN = 100L
        const val DEBOUNCE_MAX = 1000L

        fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(pin.toByteArray())
            return hash.joinToString("") { "%02x".format(it) }
        }

        /**
         * Get human-readable name for a keycode.
         */
        fun keycodeName(keycode: Int): String {
            return KeyEvent.keyCodeToString(keycode)
                .removePrefix("KEYCODE_")
                .replace('_', ' ')
                .lowercase()
                .replaceFirstChar { it.uppercase() }
        }
    }
}
