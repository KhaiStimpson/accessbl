package com.accessswitch.switchscreen

/**
 * Layout options for the phone Switch Screen touch zones.
 */
enum class SwitchZoneLayout {
    /** Entire screen is a single zone = Switch 1 */
    FULL_SCREEN,

    /** Left half = Switch 1, Right half = Switch 2 */
    LEFT_RIGHT,

    /** Top half = Switch 1, Bottom half = Switch 2 */
    TOP_BOTTOM,

    /** 80% left = Switch 1, 20% right strip = Switch 2 */
    ASYMMETRIC_80_20
}
