package com.accessswitch.accessibility

import android.accessibilityservice.AccessibilityService
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.accessswitch.input.SwitchId
import com.accessswitch.input.SwitchInputHub
import com.accessswitch.input.SwitchSource
import com.accessswitch.netflix.NetflixModeDetector
import com.accessswitch.overlay.OverlayManager
import com.accessswitch.phone.PhoneController
import com.accessswitch.scanning.ScanningEngine
import com.accessswitch.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Core accessibility service that:
 * 1. Captures global key events from external switches (HW switch listener)
 * 2. Provides TYPE_ACCESSIBILITY_OVERLAY capability for the scanning overlay
 * 3. Monitors window state changes for Netflix mode detection
 * 4. Executes performGlobalAction for OS navigation
 * 5. Detects switch device connection/disconnection
 */
@AndroidEntryPoint
class AccessSwitchAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var switchInputHub: SwitchInputHub

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var scanningEngine: ScanningEngine

    @Inject
    lateinit var overlayManager: OverlayManager

    @Inject
    lateinit var phoneController: PhoneController

    @Inject
    lateinit var netflixModeDetector: NetflixModeDetector

    private var inputManager: InputManager? = null

    private val inputDeviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            // A new input device connected — could be a switch
            val device = InputDevice.getDevice(deviceId) ?: return
            if (isExternalKeyboard(device)) {
                // Resume scanning if it was paused due to disconnect
                if (!scanningEngine.state.value.isScanning &&
                    scanningEngine.state.value.items.isNotEmpty()
                ) {
                    scanningEngine.resume()
                }
            }
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            // An input device was disconnected — may be the switch
            // We can't query the device since it's already gone,
            // so check if any external keyboards remain
            if (!hasExternalKeyboard()) {
                switchInputHub.onSwitchSourceDisconnected(SwitchSource.HW_SWITCH)
                scanningEngine.pause()
            }
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            // Device configuration changed — no action needed
        }
    }

    companion object {
        var instance: AccessSwitchAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // Register for input device changes to detect switch connect/disconnect
        inputManager = getSystemService(INPUT_SERVICE) as? InputManager
        inputManager?.registerInputDeviceListener(inputDeviceListener, null)

        // Show the scanning overlay
        overlayManager.show(this)

        // Start phone call state monitoring
        phoneController.startMonitoring()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val settings = settingsRepository.currentSettings
        val switchId = when (event.keyCode) {
            settings.switch1Keycode -> SwitchId.SWITCH_1
            settings.switch2Keycode -> SwitchId.SWITCH_2
            else -> return false
        }

        switchInputHub.onRawEvent(switchId, SwitchSource.HW_SWITCH)
        return true // consume the event
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Forward to Netflix mode detector for browse/playback detection
        netflixModeDetector.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        phoneController.stopMonitoring()
        overlayManager.hide()
        inputManager?.unregisterInputDeviceListener(inputDeviceListener)
        inputManager = null
        instance = null
        super.onDestroy()
    }

    /**
     * Check if an InputDevice is an external keyboard/switch.
     * External switches present as HID keyboards.
     */
    private fun isExternalKeyboard(device: InputDevice): Boolean {
        return device.sources and InputDevice.SOURCE_KEYBOARD != 0 &&
            !device.isVirtual &&
            device.keyboardType == InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC ||
            device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC
    }

    /**
     * Check if any external keyboard devices are currently connected.
     */
    private fun hasExternalKeyboard(): Boolean {
        val deviceIds = InputDevice.getDeviceIds()
        return deviceIds.any { id ->
            val device = InputDevice.getDevice(id)
            device != null && isExternalKeyboard(device)
        }
    }
}
