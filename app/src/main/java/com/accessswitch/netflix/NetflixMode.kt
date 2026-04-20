package com.accessswitch.netflix

/**
 * Netflix operating modes.
 */
enum class NetflixMode {
    /** Not in Netflix (another app is in foreground) */
    INACTIVE,
    /** Netflix browse/home screen — uses D-pad navigation */
    BROWSE,
    /** Netflix playback — uses media controls */
    PLAYBACK
}
