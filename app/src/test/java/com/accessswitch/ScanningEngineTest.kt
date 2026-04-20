package com.accessswitch

import com.accessswitch.input.SwitchId
import com.accessswitch.overlay.FeedbackManager
import com.accessswitch.scanning.ScanItem
import com.accessswitch.scanning.ScanMode
import com.accessswitch.scanning.ScanningEngine
import com.accessswitch.settings.AppSettings
import com.accessswitch.settings.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanningEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var feedbackManager: FeedbackManager
    private lateinit var engine: ScanningEngine

    private val testItems = listOf(
        ScanItem(id = "1", label = "Item 1"),
        ScanItem(id = "2", label = "Item 2"),
        ScanItem(id = "3", label = "Item 3"),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk()
        feedbackManager = mockk(relaxed = true)
        every { settingsRepository.currentSettings } returns AppSettings(
            scanMode = ScanMode.AUTO,
            scanIntervalMs = 1000L,
            scanLoops = 3
        )
        engine = ScanningEngine(settingsRepository, feedbackManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is idle`() {
        val state = engine.state.value
        assertFalse(state.isScanning)
        assertEquals(-1, state.highlightedIndex)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `setItems starts scanning in auto mode`() = runTest {
        engine.setItems(testItems)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = engine.state.value
        assertTrue(state.isScanning)
        assertEquals(0, state.highlightedIndex)
    }

    @Test
    fun `stop returns to idle`() = runTest {
        engine.setItems(testItems)
        testDispatcher.scheduler.advanceUntilIdle()

        engine.stop()
        val state = engine.state.value
        assertFalse(state.isScanning)
        assertEquals(-1, state.highlightedIndex)
    }

    @Test
    fun `auto-scan switch press selects current item`() = runTest {
        var selected = false
        val items = listOf(
            ScanItem(id = "1", label = "Item 1", action = { selected = true }),
            ScanItem(id = "2", label = "Item 2"),
        )

        engine.setItems(items)
        testDispatcher.scheduler.advanceUntilIdle()

        engine.onSwitchPressed(SwitchId.SWITCH_1)
        assertTrue(selected)
    }

    @Test
    fun `step-scan switch 1 advances, switch 2 selects`() = runTest {
        every { settingsRepository.currentSettings } returns AppSettings(
            scanMode = ScanMode.STEP,
            scanIntervalMs = 1000L,
            scanLoops = 3
        )

        var selected = false
        val items = listOf(
            ScanItem(id = "1", label = "Item 1"),
            ScanItem(id = "2", label = "Item 2", action = { selected = true }),
        )

        engine.setItems(items)
        testDispatcher.scheduler.advanceUntilIdle()

        // Advance to item 2
        engine.onSwitchPressed(SwitchId.SWITCH_1)
        assertEquals(1, engine.state.value.highlightedIndex)

        // Select item 2
        engine.onSwitchPressed(SwitchId.SWITCH_2)
        assertTrue(selected)
    }

    @Test
    fun `pause stops scanning, resume restarts`() = runTest {
        engine.setItems(testItems)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(engine.state.value.isScanning)

        engine.pause()
        assertFalse(engine.state.value.isScanning)

        engine.resume()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(engine.state.value.isScanning)
    }

    // --- Phase 1 additional tests ---

    @Test
    fun `auto-scan advances through items at interval`() = runTest {
        engine.setItems(testItems)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, engine.state.value.highlightedIndex)

        // Advance past one interval
        testDispatcher.scheduler.advanceTimeBy(1001)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, engine.state.value.highlightedIndex)

        // Advance past another interval
        testDispatcher.scheduler.advanceTimeBy(1001)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, engine.state.value.highlightedIndex)
    }

    @Test
    fun `auto-scan wraps around and increments loop count`() = runTest {
        engine.setItems(testItems)
        testDispatcher.scheduler.advanceUntilIdle()

        // Advance through all 3 items (3 intervals = 3000ms)
        testDispatcher.scheduler.advanceTimeBy(3001)
        testDispatcher.scheduler.runCurrent()
        // After wrapping, we should be back at index 0 with loop = 1
        assertEquals(0, engine.state.value.highlightedIndex)
        assertEquals(1, engine.state.value.currentLoop)
    }

    @Test
    fun `auto-scan stops after configured number of loops`() = runTest {
        every { settingsRepository.currentSettings } returns AppSettings(
            scanMode = ScanMode.AUTO,
            scanIntervalMs = 1000L,
            scanLoops = 2 // Stop after 2 loops
        )

        engine.setItems(testItems)
        testDispatcher.scheduler.advanceUntilIdle()

        // 2 full loops = 2 * 3 items * 1000ms = 6000ms
        testDispatcher.scheduler.advanceTimeBy(6001)
        testDispatcher.scheduler.runCurrent()

        // Should have stopped
        assertFalse(engine.state.value.isScanning)
    }

    @Test
    fun `step-scan advance wraps around`() = runTest {
        every { settingsRepository.currentSettings } returns AppSettings(
            scanMode = ScanMode.STEP,
            scanIntervalMs = 1000L,
            scanLoops = 3
        )

        engine.setItems(testItems) // 3 items
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, engine.state.value.highlightedIndex)

        engine.onSwitchPressed(SwitchId.SWITCH_1) // -> 1
        engine.onSwitchPressed(SwitchId.SWITCH_1) // -> 2
        engine.onSwitchPressed(SwitchId.SWITCH_1) // -> 0 (wrap)
        assertEquals(0, engine.state.value.highlightedIndex)
    }

    @Test
    fun `row-column mode enters sub-scan on press`() = runTest {
        every { settingsRepository.currentSettings } returns AppSettings(
            scanMode = ScanMode.ROW_COLUMN,
            scanIntervalMs = 1000L,
            scanLoops = 3
        )

        val childItems = listOf(
            ScanItem(id = "child_1", label = "Child 1"),
            ScanItem(id = "child_2", label = "Child 2"),
        )

        val items = listOf(
            ScanItem(id = "row_1", label = "Row 1", children = childItems),
            ScanItem(id = "row_2", label = "Row 2"),
        )

        engine.setItems(items)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(engine.state.value.inSubScan)

        // Press to enter first row's children
        engine.onSwitchPressed(SwitchId.SWITCH_1)

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(engine.state.value.inSubScan)
        assertEquals(0, engine.state.value.selectedRowIndex)
        assertEquals(2, engine.state.value.items.size) // child items
    }

    @Test
    fun `selection triggers haptic feedback`() = runTest {
        val items = listOf(
            ScanItem(id = "1", label = "Item 1"),
        )

        engine.setItems(items)
        testDispatcher.scheduler.advanceUntilIdle()

        engine.onSwitchPressed(SwitchId.SWITCH_1) // select
        verify { feedbackManager.onItemSelected() }
    }

    @Test
    fun `auto-scan advance triggers audio feedback`() = runTest {
        engine.setItems(testItems)
        testDispatcher.scheduler.advanceUntilIdle()

        // Advance past one interval to trigger advance feedback
        testDispatcher.scheduler.advanceTimeBy(1001)
        testDispatcher.scheduler.runCurrent()

        verify(atLeast = 1) { feedbackManager.onHighlightAdvance() }
    }

    @Test
    fun `inverse mode selects on press`() = runTest {
        every { settingsRepository.currentSettings } returns AppSettings(
            scanMode = ScanMode.INVERSE,
            scanIntervalMs = 1000L,
            scanLoops = 3
        )

        var selected = false
        val items = listOf(
            ScanItem(id = "1", label = "Item 1", action = { selected = true }),
            ScanItem(id = "2", label = "Item 2"),
        )

        engine.setItems(items)
        testDispatcher.scheduler.advanceUntilIdle()

        engine.onSwitchPressed(SwitchId.SWITCH_1)
        assertTrue(selected)
    }

    @Test
    fun `switch press does nothing when not scanning`() = runTest {
        var selected = false
        val items = listOf(
            ScanItem(id = "1", label = "Item 1", action = { selected = true }),
        )

        engine.setItems(items, autoStart = false)
        engine.onSwitchPressed(SwitchId.SWITCH_1)
        assertFalse(selected)
    }

    @Test
    fun `setItems with autoStart false does not start scanning`() = runTest {
        engine.setItems(testItems, autoStart = false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(engine.state.value.isScanning)
    }
}
