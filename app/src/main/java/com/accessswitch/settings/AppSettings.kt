package com.accessswitch.settings

import android.view.KeyEvent
import com.accessswitch.input.SwitchId
import com.accessswitch.scanning.ScanMode
import com.accessswitch.switchscreen.SwitchZoneLayout

/**
 * All user-configurable settings for AccessSwitch.
 * Persisted via DataStore.
 */
data class AppSettings(
    // Scanning
    val scanMode: ScanMode = ScanMode.AUTO,
    val scanIntervalMs: Long = 1500L,
    val scanLoops: Int = 3,
    val highlightColor: Int = 0xFFFFEB3B.toInt(), // Yellow
    val highlightStyle: HighlightStyle = HighlightStyle.BORDER,
    val audioFeedback: Boolean = true,
    val hapticFeedback: Boolean = true,

    // External HW switch
    val switch1Keycode: Int = KeyEvent.KEYCODE_SPACE,
    val switch2Keycode: Int = KeyEvent.KEYCODE_ENTER,
    val debounceMs: Long = 200L,

    // Phone-as-switch (local touch mode)
    val phoneLocalSwitchEnabled: Boolean = false,
    val phoneLocalZoneLayout: SwitchZoneLayout = SwitchZoneLayout.LEFT_RIGHT,
    val phoneLocalZone1SwitchId: SwitchId = SwitchId.SWITCH_1,
    val phoneLocalZone2SwitchId: SwitchId = SwitchId.SWITCH_2,
    val switchScreenLocked: Boolean = false, // Prevents accidental mode changes

    // Phone-as-switch (BT HID remote mode)
    val phoneBtHidEnabled: Boolean = false,
    val phoneBtHidSwitch1Keycode: Int = KeyEvent.KEYCODE_SPACE,
    val phoneBtHidSwitch2Keycode: Int = KeyEvent.KEYCODE_ENTER,
    val phoneBtHidPairedDeviceAddress: String? = null,

    // Contacts / calling
    val settingsPinHash: String? = null,
    val favouriteContactIds: Set<Long> = emptySet()
)

enum class HighlightStyle {
    BORDER,
    FILL,
    BOTH
}
