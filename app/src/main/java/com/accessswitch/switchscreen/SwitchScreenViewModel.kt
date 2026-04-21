package com.accessswitch.switchscreen

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import com.accessswitch.input.BluetoothHidDeviceService
import com.accessswitch.input.SwitchId
import com.accessswitch.input.SwitchInputHub
import com.accessswitch.input.SwitchSource
import com.accessswitch.settings.AppSettings
import com.accessswitch.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for SwitchScreenActivity.
 *
 * Observes settings for zone layout and switch mappings.
 * Processes zone taps — determines which switch ID to fire,
 * provides haptic feedback, and routes to SwitchInputHub.
 */
@HiltViewModel
class SwitchScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val switchInputHub: SwitchInputHub
) : ViewModel() {

    val settingsFlow = settingsRepository.settingsFlow

    /**
     * Tracks which zone is currently flashing (for visual feedback).
     * Null = no flash active. Value = zone index (0 or 1).
     */
    private val _flashingZone = MutableStateFlow<Int?>(null)
    val flashingZone: StateFlow<Int?> = _flashingZone.asStateFlow()

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Called when a zone is tapped.
     *
     * @param zoneIndex 0 for zone 1, 1 for zone 2
     */
    fun onZoneTapped(zoneIndex: Int) {
        val settings = settingsRepository.currentSettings
        val switchId = when (zoneIndex) {
            0 -> settings.phoneLocalZone1SwitchId
            1 -> settings.phoneLocalZone2SwitchId
            else -> return
        }

        // Route to local input hub
        switchInputHub.onRawEvent(switchId, SwitchSource.PHONE_TOUCH_LOCAL)

        // Also send via BT HID if enabled and connected (BluetoothHidDevice requires API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && settings.phoneBtHidEnabled) {
            val intent = Intent(context, BluetoothHidDeviceService::class.java).apply {
                action = BluetoothHidDeviceService.ACTION_SEND_KEY
                putExtra(
                    BluetoothHidDeviceService.EXTRA_KEYCODE,
                    when (switchId) {
                        SwitchId.SWITCH_1 -> settings.phoneBtHidSwitch1Keycode
                        SwitchId.SWITCH_2 -> settings.phoneBtHidSwitch2Keycode
                    }
                )
            }
            context.startService(intent)
        }

        // Haptic feedback (< 16ms target)
        vibrateZoneTap()

        // Visual flash feedback
        triggerFlash(zoneIndex)
    }

    /**
     * Determine the switch label for a zone based on current settings.
     */
    fun getZoneLabel(zoneIndex: Int, settings: AppSettings): String {
        val switchId = when (zoneIndex) {
            0 -> settings.phoneLocalZone1SwitchId
            1 -> settings.phoneLocalZone2SwitchId
            else -> SwitchId.SWITCH_1
        }
        return when (switchId) {
            SwitchId.SWITCH_1 -> "NEXT \u25B6"
            SwitchId.SWITCH_2 -> "\u2713 SELECT"
        }
    }

    private fun vibrateZoneTap() {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    ZONE_TAP_VIBRATION_MS,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ZONE_TAP_VIBRATION_MS)
        }
    }

    private fun triggerFlash(zoneIndex: Int) {
        _flashingZone.value = zoneIndex
        // The UI will observe this and reset it after the flash animation
    }

    fun clearFlash() {
        _flashingZone.value = null
    }

    companion object {
        private const val ZONE_TAP_VIBRATION_MS = 30L
    }
}
