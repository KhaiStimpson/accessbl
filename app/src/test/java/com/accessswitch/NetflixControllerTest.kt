package com.accessswitch

import com.accessswitch.netflix.KeyInjector
import com.accessswitch.netflix.NetflixController
import com.accessswitch.netflix.NetflixMode
import com.accessswitch.netflix.NetflixModeDetector
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetflixControllerTest {

    private lateinit var modeDetector: NetflixModeDetector
    private lateinit var keyInjector: KeyInjector
    private lateinit var controller: NetflixController
    private val modeFlow = MutableStateFlow(NetflixMode.INACTIVE)

    @Before
    fun setup() {
        modeDetector = mockk(relaxed = true)
        keyInjector = mockk(relaxed = true)
        every { modeDetector.mode } returns modeFlow

        controller = NetflixController(
            context = mockk(relaxed = true),
            modeDetector = modeDetector,
            keyInjector = keyInjector
        )
    }

    @Test
    fun `inactive mode shows launch + toggle + back items`() {
        modeFlow.value = NetflixMode.INACTIVE
        val items = controller.buildScanItems()
        assertEquals(3, items.size)
        assertTrue(items.any { it.id == "netflix_launch" })
        assertTrue(items.any { it.id == "netflix_toggle_mode" })
        assertTrue(items.any { it.id == "netflix_back_to_menu" })
    }

    @Test
    fun `browse mode shows 8 items with D-pad controls`() {
        modeFlow.value = NetflixMode.BROWSE
        val items = controller.buildScanItems()
        assertEquals(8, items.size)
        assertTrue(items.any { it.id == "netflix_up" })
        assertTrue(items.any { it.id == "netflix_down" })
        assertTrue(items.any { it.id == "netflix_left" })
        assertTrue(items.any { it.id == "netflix_right" })
        assertTrue(items.any { it.id == "netflix_select" })
        assertTrue(items.any { it.id == "netflix_back" })
        assertTrue(items.any { it.id == "netflix_toggle_mode" })
        assertTrue(items.any { it.id == "netflix_back_to_menu" })
    }

    @Test
    fun `playback mode shows 8 items with media controls`() {
        modeFlow.value = NetflixMode.PLAYBACK
        val items = controller.buildScanItems()
        assertEquals(8, items.size)
        assertTrue(items.any { it.id == "netflix_play_pause" })
        assertTrue(items.any { it.id == "netflix_forward" })
        assertTrue(items.any { it.id == "netflix_rewind" })
        assertTrue(items.any { it.id == "netflix_vol_up" })
        assertTrue(items.any { it.id == "netflix_vol_down" })
        assertTrue(items.any { it.id == "netflix_stop" })
        assertTrue(items.any { it.id == "netflix_toggle_mode" })
        assertTrue(items.any { it.id == "netflix_back_to_menu" })
    }

    @Test
    fun `browse mode toggle label says Playback Mode`() {
        modeFlow.value = NetflixMode.BROWSE
        val items = controller.buildScanItems()
        val toggle = items.first { it.id == "netflix_toggle_mode" }
        assertEquals("Playback Mode", toggle.label)
    }

    @Test
    fun `playback mode toggle label says Browse Mode`() {
        modeFlow.value = NetflixMode.PLAYBACK
        val items = controller.buildScanItems()
        val toggle = items.first { it.id == "netflix_toggle_mode" }
        assertEquals("Browse Mode", toggle.label)
    }

    @Test
    fun `back to menu item always present in all modes`() {
        for (mode in NetflixMode.entries) {
            modeFlow.value = mode
            val items = controller.buildScanItems()
            assertTrue("Back to menu missing in $mode",
                items.any { it.id == "netflix_back_to_menu" })
        }
    }
}
