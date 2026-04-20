package com.accessswitch

import android.view.accessibility.AccessibilityEvent
import com.accessswitch.netflix.NetflixMode
import com.accessswitch.netflix.NetflixModeDetector
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NetflixModeDetectorTest {

    private lateinit var detector: NetflixModeDetector

    @Before
    fun setup() {
        detector = NetflixModeDetector()
    }

    // --- classifyActivity tests ---

    @Test
    fun `classifyActivity detects player activity as PLAYBACK`() {
        assertEquals(
            NetflixMode.PLAYBACK,
            detector.classifyActivity("com.netflix.mediaclient.ui.player.PlayerActivity")
        )
    }

    @Test
    fun `classifyActivity detects VideoPlayerActivity as PLAYBACK`() {
        assertEquals(
            NetflixMode.PLAYBACK,
            detector.classifyActivity("com.netflix.mediaclient.ui.player.VideoPlayerActivity")
        )
    }

    @Test
    fun `classifyActivity detects playback fragment as PLAYBACK`() {
        assertEquals(
            NetflixMode.PLAYBACK,
            detector.classifyActivity("com.netflix.mediaclient.ui.player.PlaybackFragment")
        )
    }

    @Test
    fun `classifyActivity detects home activity as BROWSE`() {
        assertEquals(
            NetflixMode.BROWSE,
            detector.classifyActivity("com.netflix.mediaclient.ui.home.HomeActivity")
        )
    }

    @Test
    fun `classifyActivity detects search as BROWSE`() {
        assertEquals(
            NetflixMode.BROWSE,
            detector.classifyActivity("com.netflix.mediaclient.ui.search.SearchActivity")
        )
    }

    @Test
    fun `classifyActivity detects unknown activity as BROWSE`() {
        assertEquals(
            NetflixMode.BROWSE,
            detector.classifyActivity("com.netflix.mediaclient.ui.SomethingNew")
        )
    }

    // --- Mode state tests ---

    @Test
    fun `initial mode is INACTIVE`() {
        assertEquals(NetflixMode.INACTIVE, detector.mode.value)
    }

    @Test
    fun `onAccessibilityEvent sets BROWSE for Netflix home activity`() {
        val event = createWindowStateEvent(
            NetflixModeDetector.NETFLIX_PACKAGE,
            "com.netflix.mediaclient.ui.home.HomeActivity"
        )
        detector.onAccessibilityEvent(event)
        assertEquals(NetflixMode.BROWSE, detector.mode.value)
    }

    @Test
    fun `onAccessibilityEvent sets PLAYBACK for Netflix player activity`() {
        val event = createWindowStateEvent(
            NetflixModeDetector.NETFLIX_PACKAGE,
            "com.netflix.mediaclient.ui.player.PlayerActivity"
        )
        detector.onAccessibilityEvent(event)
        assertEquals(NetflixMode.PLAYBACK, detector.mode.value)
    }

    @Test
    fun `onAccessibilityEvent sets INACTIVE for non-Netflix package`() {
        // First set to browse
        detector.onAccessibilityEvent(createWindowStateEvent(
            NetflixModeDetector.NETFLIX_PACKAGE,
            "com.netflix.mediaclient.ui.home.HomeActivity"
        ))
        assertEquals(NetflixMode.BROWSE, detector.mode.value)

        // Another app comes to foreground
        detector.onAccessibilityEvent(createWindowStateEvent(
            "com.example.otherapp",
            "com.example.otherapp.MainActivity"
        ))
        assertEquals(NetflixMode.INACTIVE, detector.mode.value)
    }

    @Test
    fun `non-window-state-changed events are ignored`() {
        val event = mockk<AccessibilityEvent>()
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_FOCUSED
        detector.onAccessibilityEvent(event)
        assertEquals(NetflixMode.INACTIVE, detector.mode.value)
    }

    // --- Manual toggle tests ---

    @Test
    fun `manualToggle from INACTIVE goes to BROWSE`() {
        detector.manualToggle()
        assertEquals(NetflixMode.BROWSE, detector.mode.value)
    }

    @Test
    fun `manualToggle from BROWSE goes to PLAYBACK`() {
        detector.setMode(NetflixMode.BROWSE)
        detector.manualToggle()
        assertEquals(NetflixMode.PLAYBACK, detector.mode.value)
    }

    @Test
    fun `manualToggle from PLAYBACK goes to BROWSE`() {
        detector.setMode(NetflixMode.PLAYBACK)
        detector.manualToggle()
        assertEquals(NetflixMode.BROWSE, detector.mode.value)
    }

    @Test
    fun `manual override prevents auto-detection`() {
        detector.manualToggle() // BROWSE, manual override active
        assertEquals(NetflixMode.BROWSE, detector.mode.value)

        // Netflix player activity event — should be ignored due to manual override
        detector.onAccessibilityEvent(createWindowStateEvent(
            NetflixModeDetector.NETFLIX_PACKAGE,
            "com.netflix.mediaclient.ui.player.PlayerActivity"
        ))
        assertEquals(NetflixMode.BROWSE, detector.mode.value) // Still BROWSE
    }

    @Test
    fun `leaving Netflix clears manual override`() {
        detector.manualToggle() // BROWSE, manual override active

        // Leave Netflix
        detector.onAccessibilityEvent(createWindowStateEvent(
            "com.example.otherapp", "com.example.otherapp.MainActivity"
        ))
        assertEquals(NetflixMode.INACTIVE, detector.mode.value)

        // Re-enter Netflix — auto-detection should work again
        detector.onAccessibilityEvent(createWindowStateEvent(
            NetflixModeDetector.NETFLIX_PACKAGE,
            "com.netflix.mediaclient.ui.player.PlayerActivity"
        ))
        assertEquals(NetflixMode.PLAYBACK, detector.mode.value)
    }

    // --- Helper ---

    private fun createWindowStateEvent(pkg: String, cls: String): AccessibilityEvent {
        val event = mockk<AccessibilityEvent>()
        every { event.eventType } returns AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        every { event.packageName } returns pkg
        every { event.className } returns cls
        return event
    }
}
