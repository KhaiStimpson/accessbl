package com.accessswitch.phone

/**
 * Represents a contact from the system contacts database.
 */
data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

/**
 * Call state machine for tracking the current phone call status.
 * Exposed as StateFlow by CallStateManager for the overlay to observe.
 */
sealed class CallState {
    /** No active call */
    object Idle : CallState()

    /** Incoming call ringing */
    data class Ringing(val callerName: String, val callerNumber: String) : CallState()

    /** Outgoing call dialing */
    data class Dialing(val callerName: String, val callerNumber: String) : CallState()

    /** Call is active/connected */
    data class Active(val callerName: String, val durationSeconds: Int = 0) : CallState()

    /** Call has ended */
    object Ended : CallState()

    val isInCall: Boolean
        get() = this is Ringing || this is Dialing || this is Active
}
