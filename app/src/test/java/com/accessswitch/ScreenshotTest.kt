package com.accessswitch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.takahirom.roborazzi.captureRoboImage
import com.accessswitch.scanning.ScanItem
import com.accessswitch.scanning.ScanState
import com.accessswitch.settings.AppSettings
import com.accessswitch.settings.SettingsViewModel
import com.accessswitch.settings.ui.AppearanceScreen
import com.accessswitch.settings.ui.FeedbackScreen
import com.accessswitch.settings.ui.PinLockScreen
import com.accessswitch.settings.ui.ScanModeScreen
import com.accessswitch.settings.ui.ScanSpeedScreen
import com.accessswitch.settings.ui.SettingsScreen
import com.accessswitch.switchscreen.ui.FullScreenZone
import com.accessswitch.switchscreen.ui.SplitLeftRightZones
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * During `recordRoborazziDebug`, Roborazzi writes to `build/outputs/roborazzi/` using
 * the basename of the path provided. The update-screenshots workflow then copies those
 * files to `docs/screenshots/` so the README can reference them.
 * During a normal test run (`testDebugUnitTest`), captureRoboImage is a no-op.
 */
private fun screenshotPath(name: String) = "build/outputs/roborazzi/$name.png"

private fun mockViewModel(settings: AppSettings = AppSettings()) =
    mockk<SettingsViewModel>(relaxed = true) {
        every { this@mockk.settings } returns MutableStateFlow(settings)
    }

// ─────────────────────────────────────────────────────────────────────────────
// Mobile — portrait (Pixel 5: 393 × 851 dp, xxhdpi)
// ─────────────────────────────────────────────────────────────────────────────

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h851dp-xxhdpi")
class MobileScreenshotTest {

    @Test
    fun mobile_home() {
        captureRoboImage(screenshotPath("mobile_home")) {
            ThemedContent { HomeScreen(onOpenSettings = {}) }
        }
    }

    @Test
    fun mobile_settings() {
        captureRoboImage(screenshotPath("mobile_settings")) {
            ThemedContent {
                SettingsScreen(
                    onNavigateToScanMode = {},
                    onNavigateToScanSpeed = {},
                    onNavigateToSwitchConfig = {},
                    onNavigateToAppearance = {},
                    onNavigateToFeedback = {},
                    onNavigateToPhoneSwitch = {},
                    onNavigateToBtHid = {},
                    onNavigateToPinLock = {},
                    onBack = {}
                )
            }
        }
    }

    @Test
    fun mobile_scan_overlay() {
        val scanState = ScanState(
            isScanning = true,
            highlightedIndex = 0,
            items = sampleScanItems()
        )
        captureRoboImage(screenshotPath("mobile_scan_overlay")) {
            ThemedContent {
                com.accessswitch.overlay.ScanTileGrid(
                    scanState = scanState,
                    settings = AppSettings()
                )
            }
        }
    }

    @Test
    fun mobile_switch_screen_split() {
        captureRoboImage(screenshotPath("mobile_switch_screen")) {
            ThemedContent {
                SplitLeftRightZones(
                    label1 = "NEXT ▶",
                    label2 = "✓ SELECT",
                    flashingZone = null,
                    onZone1Tap = {},
                    onZone2Tap = {},
                    onFlashDone = {}
                )
            }
        }
    }

    @Test
    fun mobile_switch_screen_full() {
        captureRoboImage(screenshotPath("mobile_switch_screen_full")) {
            ThemedContent {
                FullScreenZone(
                    label = "TAP",
                    isFlashing = false,
                    onTap = {},
                    onFlashDone = {}
                )
            }
        }
    }

    @Test
    fun mobile_scan_mode() {
        captureRoboImage(screenshotPath("mobile_scan_mode")) {
            ThemedContent { ScanModeScreen(viewModel = mockViewModel(), onBack = {}) }
        }
    }

    @Test
    fun mobile_scan_speed() {
        captureRoboImage(screenshotPath("mobile_scan_speed")) {
            ThemedContent { ScanSpeedScreen(viewModel = mockViewModel(), onBack = {}) }
        }
    }

    @Test
    fun mobile_appearance() {
        captureRoboImage(screenshotPath("mobile_appearance")) {
            ThemedContent { AppearanceScreen(viewModel = mockViewModel(), onBack = {}) }
        }
    }

    @Test
    fun mobile_feedback() {
        captureRoboImage(screenshotPath("mobile_feedback")) {
            ThemedContent { FeedbackScreen(viewModel = mockViewModel(), onBack = {}) }
        }
    }

    @Test
    fun mobile_pin_lock() {
        captureRoboImage(screenshotPath("mobile_pin_lock")) {
            ThemedContent { PinLockScreen(viewModel = mockViewModel(), onBack = {}) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chromebook — landscape (1280 × 800 dp, mdpi)
// ─────────────────────────────────────────────────────────────────────────────

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-xlarge-land-mdpi")
class ChromebookScreenshotTest {

    @Test
    fun chromebook_home() {
        captureRoboImage(screenshotPath("chromebook_home")) {
            ThemedContent { HomeScreen(onOpenSettings = {}) }
        }
    }

    @Test
    fun chromebook_settings() {
        captureRoboImage(screenshotPath("chromebook_settings")) {
            ThemedContent {
                SettingsScreen(
                    onNavigateToScanMode = {},
                    onNavigateToScanSpeed = {},
                    onNavigateToSwitchConfig = {},
                    onNavigateToAppearance = {},
                    onNavigateToFeedback = {},
                    onNavigateToPhoneSwitch = {},
                    onNavigateToBtHid = {},
                    onNavigateToPinLock = {},
                    onBack = {}
                )
            }
        }
    }

    @Test
    fun chromebook_scan_overlay() {
        val scanState = ScanState(
            isScanning = true,
            highlightedIndex = 0,
            items = sampleScanItems() + listOf(
                ScanItem("vol_up", "Volume Up"),
                ScanItem("vol_down", "Volume Down"),
            )
        )
        captureRoboImage(screenshotPath("chromebook_scan_overlay")) {
            ThemedContent {
                com.accessswitch.overlay.ScanTileGrid(
                    scanState = scanState,
                    settings = AppSettings()
                )
            }
        }
    }

    @Test
    fun chromebook_switch_screen() {
        captureRoboImage(screenshotPath("chromebook_switch_screen")) {
            ThemedContent {
                SplitLeftRightZones(
                    label1 = "NEXT ▶",
                    label2 = "✓ SELECT",
                    flashingZone = null,
                    onZone1Tap = {},
                    onZone2Tap = {},
                    onFlashDone = {}
                )
            }
        }
    }

    @Test
    fun chromebook_scan_mode() {
        captureRoboImage(screenshotPath("chromebook_scan_mode")) {
            ThemedContent { ScanModeScreen(viewModel = mockViewModel(), onBack = {}) }
        }
    }

    @Test
    fun chromebook_appearance() {
        captureRoboImage(screenshotPath("chromebook_appearance")) {
            ThemedContent { AppearanceScreen(viewModel = mockViewModel(), onBack = {}) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun sampleScanItems() = listOf(
    ScanItem("home", "Home"),
    ScanItem("back", "Back"),
    ScanItem("recents", "Recents"),
    ScanItem("notifs", "Alerts"),
    ScanItem("settings", "Settings"),
    ScanItem("netflix", "Netflix"),
    ScanItem("phone", "Phone"),
    ScanItem("stop", "Stop Scanning"),
)

@Composable
private fun ThemedContent(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AccessSwitchTheme { content() }
    }
}
