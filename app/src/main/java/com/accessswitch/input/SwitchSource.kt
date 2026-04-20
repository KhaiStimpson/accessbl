package com.accessswitch.input

/**
 * Identifies the origin of a switch event.
 */
enum class SwitchSource {
    /** External hardware switch (BT or USB HID) */
    HW_SWITCH,

    /** Phone touchscreen in local Switch Screen mode */
    PHONE_TOUCH_LOCAL,

    /** Phone acting as BT HID remote switch for Chromebook */
    PHONE_BT_HID
}
