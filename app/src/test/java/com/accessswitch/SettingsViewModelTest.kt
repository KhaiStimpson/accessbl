package com.accessswitch

import com.accessswitch.settings.AppSettings
import com.accessswitch.settings.SettingsRepository
import com.accessswitch.settings.SettingsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: SettingsViewModel
    private lateinit var settingsFlow: MutableStateFlow<AppSettings>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsFlow = MutableStateFlow(AppSettings())
        settingsRepository = mockk(relaxed = true)
        every { settingsRepository.settingsFlow } returns settingsFlow
        every { settingsRepository.currentSettings } returns settingsFlow.value

        viewModel = SettingsViewModel(settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- PIN tests ---

    @Test
    fun `hashPin produces consistent SHA-256 output`() {
        val hash1 = SettingsViewModel.hashPin("1234")
        val hash2 = SettingsViewModel.hashPin("1234")
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // SHA-256 = 32 bytes = 64 hex chars
    }

    @Test
    fun `hashPin produces different output for different PINs`() {
        val hash1 = SettingsViewModel.hashPin("1234")
        val hash2 = SettingsViewModel.hashPin("5678")
        assertTrue(hash1 != hash2)
    }

    @Test
    fun `verifyPin returns true when no PIN is set`() {
        assertTrue(viewModel.verifyPin("anything"))
    }

    @Test
    fun `verifyPin returns true for correct PIN`() {
        val hash = SettingsViewModel.hashPin("9999")
        settingsFlow.value = AppSettings(settingsPinHash = hash)
        // Need to update the mock to return current flow value
        every { settingsRepository.currentSettings } answers { settingsFlow.value }

        viewModel = SettingsViewModel(settingsRepository)
        assertTrue(viewModel.verifyPin("9999"))
    }

    @Test
    fun `verifyPin returns false for wrong PIN`() {
        val hash = SettingsViewModel.hashPin("1234")
        settingsFlow.value = AppSettings(settingsPinHash = hash)
        every { settingsRepository.currentSettings } answers { settingsFlow.value }

        viewModel = SettingsViewModel(settingsRepository)
        assertFalse(viewModel.verifyPin("0000"))
    }

    @Test
    fun `isPinSet returns false initially`() {
        assertFalse(viewModel.isPinSet())
    }

    // --- keycodeName test ---

    @Test
    fun `keycodeName formats keycode to readable string`() {
        // Space key = KeyEvent.KEYCODE_SPACE = 62
        val name = SettingsViewModel.keycodeName(62)
        assertEquals("Space", name)
    }

    // --- Range coercion tests ---

    @Test
    fun `setScanInterval coerces to valid range`() = runTest {
        val transformSlot = slot<(AppSettings) -> AppSettings>()
        coEvery { settingsRepository.updateSettings(capture(transformSlot)) } answers {
            val transformed = transformSlot.captured(AppSettings())
            settingsFlow.value = transformed
        }

        viewModel.setScanInterval(100L) // Below min (500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(500L, settingsFlow.value.scanIntervalMs)
    }

    @Test
    fun `setScanInterval coerces max`() = runTest {
        val transformSlot = slot<(AppSettings) -> AppSettings>()
        coEvery { settingsRepository.updateSettings(capture(transformSlot)) } answers {
            val transformed = transformSlot.captured(AppSettings())
            settingsFlow.value = transformed
        }

        viewModel.setScanInterval(10000L) // Above max (8000)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(8000L, settingsFlow.value.scanIntervalMs)
    }

    @Test
    fun `setDebounce coerces to valid range`() = runTest {
        val transformSlot = slot<(AppSettings) -> AppSettings>()
        coEvery { settingsRepository.updateSettings(capture(transformSlot)) } answers {
            val transformed = transformSlot.captured(AppSettings())
            settingsFlow.value = transformed
        }

        viewModel.setDebounce(50L) // Below min (100)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(100L, settingsFlow.value.debounceMs)
    }
}
