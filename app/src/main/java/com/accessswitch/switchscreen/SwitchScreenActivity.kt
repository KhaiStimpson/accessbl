package com.accessswitch.switchscreen

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.accessswitch.AccessSwitchTheme
import com.accessswitch.settings.AppSettings
import com.accessswitch.switchscreen.ui.AsymmetricZones
import com.accessswitch.switchscreen.ui.FullScreenZone
import com.accessswitch.switchscreen.ui.SplitLeftRightZones
import com.accessswitch.switchscreen.ui.SplitTopBottomZones
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen activity for phone-as-switch mode.
 *
 * Displays large touch zones that fire switch events when tapped.
 * Zone layout and switch mappings are driven by settings.
 *
 * Touch handling:
 * - Only ACTION_DOWN is processed (no drag/swipe)
 * - Haptic feedback on every tap (< 16ms target)
 * - Visual flash feedback (white flash, 150ms)
 *
 * Window flags:
 * - FLAG_KEEP_SCREEN_ON — screen stays on during switch session
 * - FLAG_FULLSCREEN — immersive, no status bar
 */
@AndroidEntryPoint
class SwitchScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while in Switch Screen mode
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Full-screen immersive
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContent {
            AccessSwitchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    SwitchScreenContent()
                }
            }
        }
    }
}

@Composable
fun SwitchScreenContent(
    viewModel: SwitchScreenViewModel = viewModel()
) {
    val settings by viewModel.settingsFlow.collectAsState(initial = AppSettings())
    val flashingZone by viewModel.flashingZone.collectAsState()

    val label1 = viewModel.getZoneLabel(0, settings)
    val label2 = viewModel.getZoneLabel(1, settings)

    when (settings.phoneLocalZoneLayout) {
        SwitchZoneLayout.FULL_SCREEN -> {
            FullScreenZone(
                label = label1,
                isFlashing = flashingZone == 0,
                onTap = { viewModel.onZoneTapped(0) },
                onFlashDone = { viewModel.clearFlash() }
            )
        }

        SwitchZoneLayout.LEFT_RIGHT -> {
            SplitLeftRightZones(
                label1 = label1,
                label2 = label2,
                flashingZone = flashingZone,
                onZone1Tap = { viewModel.onZoneTapped(0) },
                onZone2Tap = { viewModel.onZoneTapped(1) },
                onFlashDone = { viewModel.clearFlash() }
            )
        }

        SwitchZoneLayout.TOP_BOTTOM -> {
            SplitTopBottomZones(
                label1 = label1,
                label2 = label2,
                flashingZone = flashingZone,
                onZone1Tap = { viewModel.onZoneTapped(0) },
                onZone2Tap = { viewModel.onZoneTapped(1) },
                onFlashDone = { viewModel.clearFlash() }
            )
        }

        SwitchZoneLayout.ASYMMETRIC_80_20 -> {
            AsymmetricZones(
                label1 = label1,
                label2 = label2,
                flashingZone = flashingZone,
                onZone1Tap = { viewModel.onZoneTapped(0) },
                onZone2Tap = { viewModel.onZoneTapped(1) },
                onFlashDone = { viewModel.clearFlash() }
            )
        }
    }
}
