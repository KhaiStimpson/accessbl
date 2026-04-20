package com.accessswitch

import com.accessswitch.phone.CallControlManager
import com.accessswitch.phone.CallState
import com.accessswitch.phone.CallStateManager
import com.accessswitch.phone.Contact
import com.accessswitch.phone.ContactsManager
import com.accessswitch.phone.PhoneController
import com.accessswitch.scanning.ScanningEngine
import com.accessswitch.settings.AppSettings
import com.accessswitch.settings.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var contactsManager: ContactsManager
    private lateinit var callStateManager: CallStateManager
    private lateinit var callControlManager: CallControlManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var scanningEngine: ScanningEngine
    private lateinit var phoneController: PhoneController

    private val callStateFlow = MutableStateFlow<CallState>(CallState.Idle)

    private val testContacts = listOf(
        Contact(id = 1L, name = "Alice", phoneNumber = "+1234567890"),
        Contact(id = 2L, name = "Bob", phoneNumber = "+0987654321"),
        Contact(id = 3L, name = "Charlie", phoneNumber = "+1112223333"),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        contactsManager = mockk(relaxed = true)
        callStateManager = mockk(relaxed = true)
        callControlManager = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        every { callStateManager.callState } returns callStateFlow
        every { contactsManager.getFavouriteContacts() } returns testContacts
        every { callControlManager.isTelephonyAvailable() } returns true
        every { callControlManager.isMuted() } returns false
        every { callControlManager.isSpeakerOn() } returns false
        every { settingsRepository.currentSettings } returns AppSettings()

        val context = mockk<android.content.Context>(relaxed = true)

        phoneController = PhoneController(
            context = context,
            contactsManager = contactsManager,
            callStateManager = callStateManager,
            callControlManager = callControlManager,
            settingsRepository = settingsRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `buildScanItems returns contact list when idle`() {
        val items = phoneController.buildScanItems()
        // 3 contacts + dialpad + back = 5
        assertEquals(5, items.size)
        assertEquals("contact_1", items[0].id)
        assertEquals("Alice", items[0].label)
        assertEquals("contact_2", items[1].id)
        assertEquals("contact_3", items[2].id)
        assertEquals("phone_dialpad", items[3].id)
        assertEquals("phone_back", items[4].id)
    }

    @Test
    fun `buildScanItems returns ringing items on incoming call`() {
        callStateFlow.value = CallState.Ringing("Alice")
        val items = phoneController.buildScanItems()
        assertEquals(2, items.size)
        assertEquals("call_answer", items[0].id)
        assertEquals("call_decline", items[1].id)
        assertTrue(items[0].label.contains("Alice"))
    }

    @Test
    fun `buildScanItems returns in-call items when active`() {
        callStateFlow.value = CallState.Active("Bob", 30)
        val items = phoneController.buildScanItems()
        assertEquals(5, items.size)
        assertEquals("call_end", items[0].id)
        assertEquals("call_mute", items[1].id)
        assertEquals("call_speaker", items[2].id)
        assertEquals("call_vol_up", items[3].id)
        assertEquals("call_vol_down", items[4].id)
    }

    @Test
    fun `buildScanItems shows mute label based on state`() {
        callStateFlow.value = CallState.Active("Bob", 0)
        every { callControlManager.isMuted() } returns true
        val items = phoneController.buildScanItems()
        assertEquals("Unmute", items[1].label)
    }

    @Test
    fun `buildScanItems returns dialing items`() {
        callStateFlow.value = CallState.Dialing("Charlie")
        val items = phoneController.buildScanItems()
        assertEquals(2, items.size)
        assertEquals("call_status", items[0].id)
        assertEquals("call_end", items[1].id)
    }

    @Test
    fun `buildScanItems returns ended items`() {
        callStateFlow.value = CallState.Ended
        val items = phoneController.buildScanItems()
        assertEquals(1, items.size)
        assertEquals("call_ended", items[0].id)
    }

    @Test
    fun `buildScanItems shows unavailable when no telephony`() {
        every { callControlManager.isTelephonyAvailable() } returns false
        val items = phoneController.buildScanItems()
        assertEquals(2, items.size)
        assertEquals("phone_unavailable", items[0].id)
        assertEquals("phone_back", items[1].id)
    }

    @Test
    fun `contact list limited to MAX_CONTACTS_DISPLAYED`() {
        val manyContacts = (1..15).map {
            Contact(id = it.toLong(), name = "Contact $it", phoneNumber = "+$it")
        }
        every { contactsManager.getFavouriteContacts() } returns manyContacts

        val items = phoneController.buildScanItems()
        // 9 contacts + dialpad + back = 11
        val contactItems = items.filter { it.id.startsWith("contact_") }
        assertEquals(9, contactItems.size)
    }

    @Test
    fun `startMonitoring calls callStateManager startListening`() {
        phoneController.startMonitoring()
        verify { callStateManager.startListening() }
    }

    @Test
    fun `stopMonitoring calls callStateManager stopListening`() {
        phoneController.stopMonitoring()
        verify { callStateManager.stopListening() }
    }
}

class CallStateTest {

    @Test
    fun `Idle is not in call`() {
        assertTrue(!CallState.Idle.isInCall)
    }

    @Test
    fun `Ringing is not in call`() {
        assertTrue(!CallState.Ringing("Alice").isInCall)
    }

    @Test
    fun `Active is in call`() {
        assertTrue(CallState.Active("Bob", 10).isInCall)
    }

    @Test
    fun `Dialing is in call`() {
        assertTrue(CallState.Dialing("Charlie").isInCall)
    }

    @Test
    fun `Ended is not in call`() {
        assertTrue(!CallState.Ended.isInCall)
    }
}
