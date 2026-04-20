package com.accessswitch.phone

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the current phone call state by listening to TelephonyManager callbacks.
 *
 * Uses TelephonyCallback on API 31+ and PhoneStateListener on API 26-30.
 * Exposes a StateFlow<CallState> that the overlay and PhoneController observe
 * to switch between contact list and in-call controls.
 */
@Singleton
class CallStateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsManager: ContactsManager
) {
    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    // Track the current call's number for caller ID
    private var currentCallNumber: String? = null

    // Duration timer
    private var durationJob: Job? = null
    private var callStartTimeMs: Long = 0L

    // Listener references for cleanup
    private var telephonyCallback: Any? = null // TelephonyCallback (API 31+)
    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null

    private var isRegistered = false

    /**
     * Start listening for call state changes.
     * Should be called when the accessibility service starts.
     */
    fun startListening() {
        if (isRegistered) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerTelephonyCallback()
        } else {
            registerPhoneStateListener()
        }
        isRegistered = true
    }

    /**
     * Stop listening for call state changes.
     * Should be called when the accessibility service stops.
     */
    fun stopListening() {
        if (!isRegistered) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            unregisterTelephonyCallback()
        } else {
            unregisterPhoneStateListener()
        }
        durationJob?.cancel()
        isRegistered = false
    }

    /**
     * Set the number being dialed for outgoing calls.
     * Called by PhoneController before initiating a call.
     */
    fun setOutgoingCallNumber(number: String) {
        currentCallNumber = number
    }

    /**
     * Handle telephony state changes from either callback mechanism.
     */
    private fun onCallStateChanged(state: Int, phoneNumber: String?) {
        val number = phoneNumber ?: currentCallNumber ?: ""
        val callerName = if (number.isNotEmpty()) {
            contactsManager.getContactNameByNumber(number)
        } else {
            "Unknown"
        }

        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> {
                if (_callState.value.isInCall) {
                    // Call just ended
                    _callState.value = CallState.Ended
                    durationJob?.cancel()

                    // Auto-transition back to Idle after a brief delay
                    scope.launch {
                        delay(ENDED_DISPLAY_MS)
                        if (_callState.value is CallState.Ended) {
                            _callState.value = CallState.Idle
                        }
                    }
                } else {
                    _callState.value = CallState.Idle
                }
                currentCallNumber = null
            }

            TelephonyManager.CALL_STATE_RINGING -> {
                currentCallNumber = number
                _callState.value = CallState.Ringing(
                    callerName = callerName,
                    callerNumber = number
                )
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Call is active (answered or dialing)
                durationJob?.cancel()
                callStartTimeMs = System.currentTimeMillis()

                _callState.value = CallState.Active(
                    callerName = callerName,
                    durationSeconds = 0
                )

                // Start duration counter
                durationJob = scope.launch {
                    while (true) {
                        delay(1000)
                        val elapsed = ((System.currentTimeMillis() - callStartTimeMs) / 1000).toInt()
                        val current = _callState.value
                        if (current is CallState.Active) {
                            _callState.value = current.copy(durationSeconds = elapsed)
                        } else {
                            break
                        }
                    }
                }
            }
        }
    }

    // --- API 31+ TelephonyCallback ---

    @Suppress("NewApi")
    private fun registerTelephonyCallback() {
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                this@CallStateManager.onCallStateChanged(state, null)
            }
        }
        telephonyCallback = callback
        telephonyManager.registerTelephonyCallback(
            context.mainExecutor,
            callback
        )
    }

    @Suppress("NewApi")
    private fun unregisterTelephonyCallback() {
        (telephonyCallback as? TelephonyCallback)?.let {
            telephonyManager.unregisterTelephonyCallback(it)
        }
        telephonyCallback = null
    }

    // --- API 26-30 PhoneStateListener ---

    @Suppress("DEPRECATION")
    private fun registerPhoneStateListener() {
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                this@CallStateManager.onCallStateChanged(state, phoneNumber)
            }
        }
        phoneStateListener = listener
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    @Suppress("DEPRECATION")
    private fun unregisterPhoneStateListener() {
        phoneStateListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        phoneStateListener = null
    }

    companion object {
        /** How long to show "Call Ended" before returning to Idle */
        private const val ENDED_DISPLAY_MS = 2000L
    }
}
