package com.accessswitch

import com.accessswitch.input.SwitchId
import com.accessswitch.scanning.ScanMode
import com.accessswitch.settings.AppSettings
import com.accessswitch.settings.HighlightStyle
import com.accessswitch.switchscreen.SwitchZoneLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AppSettings data class and defaults.
 * Note: SettingsRepository requires Android DataStore and is tested via instrumentation tests.
 * These tests verify the data class defaults and field interactions.
 */
class AppSettingsTest {

    @Test
    fun `default scan mode is AUTO`() {
        val settings = AppSettings()
        assertEquals(ScanMode.AUTO, settings.scanMode)
    }

    @Test
    fun `default scan interval is 1500ms`() {
        val settings = AppSettings()
        assertEquals(1500L, settings.scanIntervalMs)
    }

    @Test
    fun `default scan loops is 3`() {
        val settings = AppSettings()
        assertEquals(3, settings.scanLoops)
    }

    @Test
    fun `default highlight style is BORDER`() {
        val settings = AppSettings()
        assertEquals(HighlightStyle.BORDER, settings.highlightStyle)
    }

    @Test
    fun `default highlight color is yellow`() {
        val settings = AppSettings()
        // 0xFFFFEB3B = Material Yellow 500 (WCAG AA compliant)
        assertEquals(0xFFFFEB3B.toInt(), settings.highlightColor)
    }

    @Test
    fun `default audio feedback is true`() {
        val settings = AppSettings()
        assertTrue(settings.audioFeedback)
    }

    @Test
    fun `default haptic feedback is true`() {
        val settings = AppSettings()
        assertTrue(settings.hapticFeedback)
    }

    @Test
    fun `default switch 1 keycode is SPACE`() {
        val settings = AppSettings()
        assertEquals(android.view.KeyEvent.KEYCODE_SPACE, settings.switch1Keycode)
    }

    @Test
    fun `default switch 2 keycode is ENTER`() {
        val settings = AppSettings()
        assertEquals(android.view.KeyEvent.KEYCODE_ENTER, settings.switch2Keycode)
    }

    @Test
    fun `default debounce is 200ms`() {
        val settings = AppSettings()
        assertEquals(200L, settings.debounceMs)
    }

    @Test
    fun `default phone local switch is disabled`() {
        val settings = AppSettings()
        assertFalse(settings.phoneLocalSwitchEnabled)
    }

    @Test
    fun `default phone zone layout is LEFT_RIGHT`() {
        val settings = AppSettings()
        assertEquals(SwitchZoneLayout.LEFT_RIGHT, settings.phoneLocalZoneLayout)
    }

    @Test
    fun `default zone 1 maps to SWITCH_1`() {
        val settings = AppSettings()
        assertEquals(SwitchId.SWITCH_1, settings.phoneLocalZone1SwitchId)
    }

    @Test
    fun `default zone 2 maps to SWITCH_2`() {
        val settings = AppSettings()
        assertEquals(SwitchId.SWITCH_2, settings.phoneLocalZone2SwitchId)
    }

    @Test
    fun `default BT HID is disabled`() {
        val settings = AppSettings()
        assertFalse(settings.phoneBtHidEnabled)
    }

    @Test
    fun `default switch screen locked is false`() {
        val settings = AppSettings()
        assertFalse(settings.switchScreenLocked)
    }

    @Test
    fun `default BT HID switch 1 keycode is SPACE`() {
        val settings = AppSettings()
        assertEquals(android.view.KeyEvent.KEYCODE_SPACE, settings.phoneBtHidSwitch1Keycode)
    }

    @Test
    fun `default BT HID switch 2 keycode is ENTER`() {
        val settings = AppSettings()
        assertEquals(android.view.KeyEvent.KEYCODE_ENTER, settings.phoneBtHidSwitch2Keycode)
    }

    @Test
    fun `default paired device address is null`() {
        val settings = AppSettings()
        assertNull(settings.phoneBtHidPairedDeviceAddress)
    }

    @Test
    fun `default PIN hash is null`() {
        val settings = AppSettings()
        assertNull(settings.settingsPinHash)
    }

    @Test
    fun `default favourite contacts is empty`() {
        val settings = AppSettings()
        assertTrue(settings.favouriteContactIds.isEmpty())
    }

    @Test
    fun `copy with modified scan mode preserves other fields`() {
        val original = AppSettings(
            scanMode = ScanMode.AUTO,
            scanIntervalMs = 2000L,
            highlightColor = 0xFF0000FF.toInt()
        )
        val modified = original.copy(scanMode = ScanMode.STEP)

        assertEquals(ScanMode.STEP, modified.scanMode)
        assertEquals(2000L, modified.scanIntervalMs)
        assertEquals(0xFF0000FF.toInt(), modified.highlightColor)
    }

    @Test
    fun `favourite contacts can be added via copy`() {
        val settings = AppSettings()
        val withFavourites = settings.copy(
            favouriteContactIds = setOf(1L, 2L, 3L)
        )

        assertEquals(3, withFavourites.favouriteContactIds.size)
        assertTrue(withFavourites.favouriteContactIds.contains(2L))
    }

    @Test
    fun `all scan modes are supported`() {
        // Verify all scan modes can be set
        ScanMode.entries.forEach { mode ->
            val settings = AppSettings(scanMode = mode)
            assertEquals(mode, settings.scanMode)
        }
    }

    @Test
    fun `all highlight styles are supported`() {
        HighlightStyle.entries.forEach { style ->
            val settings = AppSettings(highlightStyle = style)
            assertEquals(style, settings.highlightStyle)
        }
    }

    @Test
    fun `all zone layouts are supported`() {
        SwitchZoneLayout.entries.forEach { layout ->
            val settings = AppSettings(phoneLocalZoneLayout = layout)
            assertEquals(layout, settings.phoneLocalZoneLayout)
        }
    }
}
