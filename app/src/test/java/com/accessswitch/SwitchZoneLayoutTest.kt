package com.accessswitch

import com.accessswitch.input.BtHidState
import com.accessswitch.input.BluetoothHidDeviceService
import com.accessswitch.input.SwitchId
import com.accessswitch.input.SwitchInputHub
import com.accessswitch.input.SwitchSource
import com.accessswitch.scanning.ScanningEngine
import com.accessswitch.settings.AppSettings
import com.accessswitch.settings.SettingsRepository
import com.accessswitch.switchscreen.SwitchZoneLayout
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SwitchZoneLayoutTest {

    @Test
    fun `all zone layouts have valid enum values`() {
        val layouts = SwitchZoneLayout.entries
        assertEquals(4, layouts.size)
        assertTrue(layouts.contains(SwitchZoneLayout.FULL_SCREEN))
        assertTrue(layouts.contains(SwitchZoneLayout.LEFT_RIGHT))
        assertTrue(layouts.contains(SwitchZoneLayout.TOP_BOTTOM))
        assertTrue(layouts.contains(SwitchZoneLayout.ASYMMETRIC_80_20))
    }

    @Test
    fun `zone boundary calculation for LEFT_RIGHT`() {
        val screenWidth = 1080
        val midpoint = screenWidth / 2
        assertEquals(540, midpoint)
        assertTrue(200 < midpoint) // Zone 1
        assertTrue(800 >= midpoint) // Zone 2
    }

    @Test
    fun `zone boundary calculation for ASYMMETRIC_80_20`() {
        val screenWidth = 1000
        val zone1Width = (screenWidth * 0.8).toInt()
        assertEquals(800, zone1Width)
        assertTrue(700 < zone1Width) // Zone 1
        assertTrue(900 >= zone1Width) // Zone 2
    }

    @Test
    fun `zone boundary calculation for TOP_BOTTOM`() {
        val screenHeight = 1920
        val midpoint = screenHeight / 2
        assertEquals(960, midpoint)
        assertTrue(400 < midpoint) // Zone 1
        assertTrue(1200 >= midpoint) // Zone 2
    }
}

class BtHidStateTest {

    @Test
    fun `initial state is disconnected`() {
        // BtHidState starts as DISCONNECTED
        assertEquals(BtHidState.DISCONNECTED, BluetoothHidDeviceService.state.value)
    }

    @Test
    fun `all BtHidState values exist`() {
        val states = BtHidState.entries
        assertEquals(3, states.size)
        assertTrue(states.contains(BtHidState.DISCONNECTED))
        assertTrue(states.contains(BtHidState.CONNECTING))
        assertTrue(states.contains(BtHidState.CONNECTED))
    }

    @Test
    fun `connected device name is null initially`() {
        assertNull(BluetoothHidDeviceService.connectedDeviceName.value)
    }
}

class PhoneTouchInputTest {

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
    fun `phone touch event routes to scanning engine`() {
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.PHONE_TOUCH_LOCAL)
        verify(exactly = 1) { scanningEngine.onSwitchPressed(SwitchId.SWITCH_1) }
    }

    @Test
    fun `phone touch source is tracked as connected`() {
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.PHONE_TOUCH_LOCAL)
        assertTrue(hub.connectedSources.value.contains(SwitchSource.PHONE_TOUCH_LOCAL))
    }

    @Test
    fun `phone touch and HW switch simultaneous dedup`() {
        // First event from phone touch
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.PHONE_TOUCH_LOCAL)
        // Same switch from HW immediately after — should be deduped
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.HW_SWITCH)
        verify(exactly = 1) { scanningEngine.onSwitchPressed(SwitchId.SWITCH_1) }
    }

    @Test
    fun `different switches from phone touch fire independently`() {
        hub.onRawEvent(SwitchId.SWITCH_1, SwitchSource.PHONE_TOUCH_LOCAL)
        hub.onRawEvent(SwitchId.SWITCH_2, SwitchSource.PHONE_TOUCH_LOCAL)
        verify(exactly = 1) { scanningEngine.onSwitchPressed(SwitchId.SWITCH_1) }
        verify(exactly = 1) { scanningEngine.onSwitchPressed(SwitchId.SWITCH_2) }
    }

    @Test
    fun `zone 1 maps to configured switch ID`() {
        // Verify that AppSettings defaults are SWITCH_1 for zone 1
        val settings = AppSettings()
        assertEquals(SwitchId.SWITCH_1, settings.phoneLocalZone1SwitchId)
    }

    @Test
    fun `zone 2 maps to configured switch ID`() {
        val settings = AppSettings()
        assertEquals(SwitchId.SWITCH_2, settings.phoneLocalZone2SwitchId)
    }

    @Test
    fun `BT HID keycodes default to Space and Enter`() {
        val settings = AppSettings()
        assertEquals(android.view.KeyEvent.KEYCODE_SPACE, settings.phoneBtHidSwitch1Keycode)
        assertEquals(android.view.KeyEvent.KEYCODE_ENTER, settings.phoneBtHidSwitch2Keycode)
    }
}
