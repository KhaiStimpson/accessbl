package com.accessswitch.phone

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.accessswitch.scanning.ScanItem
import com.accessswitch.scanning.ScanningEngine
import com.accessswitch.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates phone calling functionality.
 *
 * Coordinates between:
 * - [ContactsManager] for querying contacts
 * - [CallStateManager] for tracking call state
 * - [CallControlManager] for in-call actions
 * - [ScanningEngine] for updating the overlay with appropriate scan items
 *
 * Builds different ScanItem lists depending on call state:
 * - Idle: contact list (favourites) + dialpad option
 * - Ringing: Answer / Decline
 * - Active: End Call, Mute, Speaker, Volume Up, Volume Down
 * - Ended: brief display then return to contact list
 */
@Singleton
class PhoneController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsManager: ContactsManager,
    private val callStateManager: CallStateManager,
    private val callControlManager: CallControlManager,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** Expose call state for external observers (overlay, main menu) */
    val callState: StateFlow<CallState> = callStateManager.callState

    // Reference to scanning engine, set when phone panel is active
    private var scanningEngine: ScanningEngine? = null
    private var isPhonePanelActive = false

    // Callback to return to main menu
    private var onBackToMenu: (() -> Unit)? = null

    /**
     * Initialize call state monitoring.
     * Called from AccessSwitchAccessibilityService.onServiceConnected.
     */
    fun startMonitoring() {
        callStateManager.startListening()

        // Auto-switch to in-call overlay when call state changes
        callState.onEach { state ->
            if (isPhonePanelActive) {
                updateScanItemsForState(state)
            } else if (state is CallState.Ringing) {
                // Auto-surface incoming call overlay even if phone panel isn't active
                showIncomingCallOverlay()
            }
        }.launchIn(scope)
    }

    /**
     * Stop call state monitoring.
     */
    fun stopMonitoring() {
        callStateManager.stopListening()
    }

    /**
     * Activate the phone panel in the scanning overlay.
     * Called when user selects "Phone" from main menu.
     */
    fun activatePhonePanel(engine: ScanningEngine, backToMenu: () -> Unit) {
        scanningEngine = engine
        onBackToMenu = backToMenu
        isPhonePanelActive = true

        if (!callControlManager.isTelephonyAvailable()) {
            // No telephony on this device (e.g., Chromebook)
            engine.setItems(buildNoTelephonyItems())
            return
        }

        updateScanItemsForState(callState.value)
    }

    /**
     * Deactivate the phone panel (returning to main menu).
     */
    fun deactivatePhonePanel() {
        isPhonePanelActive = false
        scanningEngine = null
        onBackToMenu = null
    }

    /**
     * Build the scannable items for the phone panel.
     * Returns items appropriate for the current call state.
     */
    fun buildScanItems(): List<ScanItem> {
        if (!callControlManager.isTelephonyAvailable()) {
            return buildNoTelephonyItems()
        }

        return when (callState.value) {
            is CallState.Idle -> buildContactListItems()
            is CallState.Ringing -> buildRingingItems()
            is CallState.Dialing -> buildDialingItems()
            is CallState.Active -> buildInCallItems()
            is CallState.Ended -> buildCallEndedItems()
        }
    }

    // --- Private item builders ---

    /**
     * Build contact list for idle state (no active call).
     * Shows favourite contacts + dialpad + back to menu.
     */
    private fun buildContactListItems(): List<ScanItem> {
        val items = mutableListOf<ScanItem>()

        val contacts = contactsManager.getFavouriteContacts()
        for (contact in contacts.take(MAX_CONTACTS_DISPLAYED)) {
            items.add(
                ScanItem(
                    id = "contact_${contact.id}",
                    label = contact.name,
                    action = { initiateCall(contact) }
                )
            )
        }

        // Dialpad option (Should Have feature)
        items.add(
            ScanItem(
                id = "phone_dialpad",
                label = "Dialpad",
                action = { /* Phase 3B: show dialpad sub-panel */ }
            )
        )

        // Back to main menu
        items.add(
            ScanItem(
                id = "phone_back",
                label = "Back to Menu",
                action = { returnToMainMenu() }
            )
        )

        return items
    }

    /**
     * Build items for incoming call (ringing state).
     * Answer / Decline
     */
    private fun buildRingingItems(): List<ScanItem> {
        val state = callState.value as? CallState.Ringing
        val callerName = state?.callerName ?: "Unknown"

        return listOf(
            ScanItem(
                id = "call_answer",
                label = "Answer\n$callerName",
                action = { callControlManager.answerCall() }
            ),
            ScanItem(
                id = "call_decline",
                label = "Decline",
                action = { callControlManager.endCall() }
            )
        )
    }

    /**
     * Build items for outgoing call (dialing state).
     * Only End Call available while dialing.
     */
    private fun buildDialingItems(): List<ScanItem> {
        val state = callState.value as? CallState.Dialing
        val callerName = state?.callerName ?: "Unknown"

        return listOf(
            ScanItem(
                id = "call_status",
                label = "Calling\n$callerName",
                action = { } // Info tile, no action
            ),
            ScanItem(
                id = "call_end",
                label = "End Call",
                action = {
                    callControlManager.endCall()
                    callControlManager.resetAudioState()
                }
            )
        )
    }

    /**
     * Build in-call control items for active call.
     * End Call, Mute, Speaker, Volume Up, Volume Down
     */
    private fun buildInCallItems(): List<ScanItem> {
        val state = callState.value as? CallState.Active
        val callerName = state?.callerName ?: ""
        val duration = state?.durationSeconds ?: 0
        val durationStr = formatDuration(duration)

        return listOf(
            ScanItem(
                id = "call_end",
                label = "End Call",
                action = {
                    callControlManager.endCall()
                    callControlManager.resetAudioState()
                }
            ),
            ScanItem(
                id = "call_mute",
                label = if (callControlManager.isMuted()) "Unmute" else "Mute",
                action = { callControlManager.toggleMute() }
            ),
            ScanItem(
                id = "call_speaker",
                label = if (callControlManager.isSpeakerOn()) "Speaker Off" else "Speaker On",
                action = { callControlManager.toggleSpeaker() }
            ),
            ScanItem(
                id = "call_vol_up",
                label = "Volume Up",
                action = { callControlManager.volumeUp() }
            ),
            ScanItem(
                id = "call_vol_down",
                label = "Volume Down",
                action = { callControlManager.volumeDown() }
            ),
        )
    }

    /**
     * Build items for call ended state (briefly shown).
     */
    private fun buildCallEndedItems(): List<ScanItem> = listOf(
        ScanItem(
            id = "call_ended",
            label = "Call Ended",
            action = { } // Auto-transitions back to Idle
        )
    )

    /**
     * Build items when telephony is not available (e.g., Chromebook).
     */
    private fun buildNoTelephonyItems(): List<ScanItem> = listOf(
        ScanItem(
            id = "phone_unavailable",
            label = "Phone Not Available\non This Device",
            action = { }
        ),
        ScanItem(
            id = "phone_back",
            label = "Back to Menu",
            action = { returnToMainMenu() }
        )
    )

    // --- Actions ---

    /**
     * Initiate an outgoing call to a contact.
     */
    private fun initiateCall(contact: Contact) {
        callStateManager.setOutgoingCallNumber(contact.phoneNumber)

        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${Uri.encode(contact.phoneNumber)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(callIntent)
        } catch (_: SecurityException) {
            // CALL_PHONE permission not granted
        }
    }

    /**
     * Auto-surface the incoming call overlay, even if phone panel isn't the active panel.
     */
    private fun showIncomingCallOverlay() {
        val engine = scanningEngine ?: return
        engine.setItems(buildRingingItems())
        isPhonePanelActive = true
    }

    /**
     * Return to the main menu.
     */
    private fun returnToMainMenu() {
        deactivatePhonePanel()
        onBackToMenu?.invoke()
    }

    /**
     * Update the scanning engine items based on the current call state.
     */
    private fun updateScanItemsForState(state: CallState) {
        val engine = scanningEngine ?: return
        val items = when (state) {
            is CallState.Idle -> buildContactListItems()
            is CallState.Ringing -> buildRingingItems()
            is CallState.Dialing -> buildDialingItems()
            is CallState.Active -> buildInCallItems()
            is CallState.Ended -> buildCallEndedItems()
        }
        engine.setItems(items)
    }

    /**
     * Format seconds into MM:SS display.
     */
    private fun formatDuration(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return "%d:%02d".format(min, sec)
    }

    companion object {
        /** Max contacts to show in the phone panel grid */
        private const val MAX_CONTACTS_DISPLAYED = 9
    }
}
