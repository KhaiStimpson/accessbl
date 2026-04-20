package com.accessswitch.input

/**
 * A normalised switch event from any input source.
 */
data class SwitchEvent(
    val switchId: SwitchId,
    val source: SwitchSource,
    val timestampMs: Long
)
