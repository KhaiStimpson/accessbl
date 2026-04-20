package com.accessswitch

import com.accessswitch.input.SwitchId
import com.accessswitch.input.SwitchInputHub
import com.accessswitch.input.SwitchSource
import com.accessswitch.scanning.ScanningEngine
import com.accessswitch.settings.AppSettings
import com.accessswitch.settings.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SwitchInputHubTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var scanningEngine: ScanningEngine
    private lateinit var hub: SwitchInputHub

    @Before
    fun setup() {
        settingsRepository = mockk()
        scanningEngine = mockk(relaxed = true)
        every { settingsRepository.currentSettings } returns AppSettings(debounceMs = 200L)
        hub = SwitchInputHub(settingsRepository, scanningEngine)
    }

    @Test
    fun `raw event forwards to scanning engine`() {
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.HW_SWITCH)
        verify(exactly = 1) { scanningEngine.onSwitchPressed(SwitchId.SWITCH_1) }
    }

    @Test
    fun `duplicate event within debounce window is ignored`() {
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.HW_SWITCH)
        // Second event immediately after — should be deduped
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.PHONE_TOUCH_LOCAL)
        verify(exactly = 1) { scanningEngine.onSwitchPressed(SwitchId.SWITCH_1) }
    }

    @Test
    fun `different switches are not deduped`() {
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.HW_SWITCH)
        hub.onRawEvent(SwitchId.SWITCH_2, SwitchSource.PHONE_TOUCH_LOCAL)
        verify(exactly = 1) { scanningEngine.onSwitchPressed(SwitchId.SWITCH_1) }
        verify(exactly = 1) { scanningEngine.onSwitchPressed(SwitchId.SWITCH_2) }
    }

    // --- Phase 1 additional tests ---

    @Test
    fun `raw event tracks source as connected`() {
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.HW_SWITCH)
        assertTrue(hub.connectedSources.value.contains(SwitchSource.HW_SWITCH))
    }

    @Test
    fun `disconnect event emits source`() {
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.HW_SWITCH)
        hub.onSwitchSourceDisconnected(SwitchSource.HW_SWITCH)

        assertEquals(SwitchSource.HW_SWITCH, hub.disconnectEvent.value)
        assertTrue(!hub.connectedSources.value.contains(SwitchSource.HW_SWITCH))
    }

    @Test
    fun `reconnect clears disconnect event`() {
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.HW_SWITCH)
        hub.onSwitchSourceDisconnected(SwitchSource.HW_SWITCH)
        assertEquals(SwitchSource.HW_SWITCH, hub.disconnectEvent.value)

        // Reconnect (new event from the source)
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.HW_SWITCH)
        assertNull(hub.disconnectEvent.value)
    }

    @Test
    fun `clear disconnect event manually`() {
        hub.onSwitchSourceDisconnected(SwitchSource.HW_SWITCH)
        assertEquals(SwitchSource.HW_SWITCH, hub.disconnectEvent.value)

        hub.clearDisconnectEvent()
        assertNull(hub.disconnectEvent.value)
    }

    @Test
    fun `multiple sources tracked independently`() {
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.HW_SWITCH)
        hub.onRawEvent(SwitchId.SWITCH_2, SwitchSource.PHONE_TOUCH_LOCAL)

        val sources = hub.connectedSources.value
        assertTrue(sources.contains(SwitchSource.HW_SWITCH))
        assertTrue(sources.contains(SwitchSource.PHONE_TOUCH_LOCAL))

        hub.onSwitchSourceDisconnected(SwitchSource.HW_SWITCH)
        val afterDisconnect = hub.connectedSources.value
        assertTrue(!afterDisconnect.contains(SwitchSource.HW_SWITCH))
        assertTrue(afterDisconnect.contains(SwitchSource.PHONE_TOUCH_LOCAL))
    }
}
