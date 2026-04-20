package com.accessswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.accessswitch.settings.SettingsViewModel
import com.accessswitch.settings.ui.AppearanceScreen
import com.accessswitch.settings.ui.BtHidScreen
import com.accessswitch.settings.ui.FeedbackScreen
import com.accessswitch.settings.ui.KeyCaptureDialog
import com.accessswitch.settings.ui.PhoneSwitchScreen
import com.accessswitch.settings.ui.PinLockScreen
import com.accessswitch.settings.ui.ScanModeScreen
import com.accessswitch.settings.ui.ScanSpeedScreen
import com.accessswitch.settings.ui.SettingsScreen
import com.accessswitch.settings.ui.SwitchConfigScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startRoute = if (intent?.getStringExtra("navigate_to") == "settings") {
            "settings"
        } else {
            "home"
        }

        setContent {
            AccessSwitchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    AppNavigation(startDestination = startRoute)
                }
            }
        }
    }
}

@Composable
fun AccessSwitchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}

/**
 * Top-level navigation graph for the app.
 *
 * Routes:
 * - "home" — Main landing screen with status and settings button
 * - "settings" — Top-level settings menu
 * - "settings/{screen}" — Detail settings screens
 */
@Composable
fun AppNavigation(startDestination: String = "home") {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("home") {
            HomeScreen(
                onOpenSettings = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateToScanMode = { navController.navigate("settings/scan_mode") },
                onNavigateToScanSpeed = { navController.navigate("settings/scan_speed") },
                onNavigateToSwitchConfig = { navController.navigate("settings/switch_config") },
                onNavigateToAppearance = { navController.navigate("settings/appearance") },
                onNavigateToFeedback = { navController.navigate("settings/feedback") },
                onNavigateToPhoneSwitch = { navController.navigate("settings/phone_switch") },
                onNavigateToBtHid = { navController.navigate("settings/bt_hid") },
                onNavigateToPinLock = { navController.navigate("settings/pin_lock") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings/scan_mode") {
            ScanModeScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings/scan_speed") {
            ScanSpeedScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings/switch_config") {
            SwitchConfigWithDialogs(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings/appearance") {
            AppearanceScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings/feedback") {
            FeedbackScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings/phone_switch") {
            PhoneSwitchScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings/bt_hid") {
            BtHidWithDialogs(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings/pin_lock") {
            PinLockScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Home screen — landing page showing app status and settings entry point.
 */
@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AccessSwitch",
                color = Color.White,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enable the Accessibility Service in\nSettings > Accessibility to start scanning.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8AB4F8)
                ),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text("Open Settings", color = Color.Black, fontSize = 16.sp)
            }
        }
    }
}

/**
 * Switch config screen with key capture dialog state management.
 */
@Composable
fun SwitchConfigWithDialogs(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var capturingSwitch by remember { mutableStateOf<Int?>(null) } // 1 or 2

    val settings by viewModel.settings.collectAsState()

    SwitchConfigScreen(
        viewModel = viewModel,
        onCaptureSwitch1 = { capturingSwitch = 1 },
        onCaptureSwitch2 = { capturingSwitch = 2 },
        onBack = onBack
    )

    capturingSwitch?.let { switchNum ->
        val currentKeycode = if (switchNum == 1) settings.switch1Keycode else settings.switch2Keycode
        KeyCaptureDialog(
            switchLabel = "Switch $switchNum",
            currentKeycode = currentKeycode,
            onKeyCaptured = { keycode ->
                if (switchNum == 1) viewModel.setSwitch1Keycode(keycode)
                else viewModel.setSwitch2Keycode(keycode)
                capturingSwitch = null
            },
            onDismiss = { capturingSwitch = null }
        )
    }
}

/**
 * BT HID config screen with key capture dialog state management.
 */
@Composable
fun BtHidWithDialogs(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var capturingSwitch by remember { mutableStateOf<Int?>(null) }

    val settings by viewModel.settings.collectAsState()

    BtHidScreen(
        viewModel = viewModel,
        onCaptureSwitch1 = { capturingSwitch = 1 },
        onCaptureSwitch2 = { capturingSwitch = 2 },
        onBack = onBack
    )

    capturingSwitch?.let { switchNum ->
        val currentKeycode = if (switchNum == 1)
            settings.phoneBtHidSwitch1Keycode else settings.phoneBtHidSwitch2Keycode
        KeyCaptureDialog(
            switchLabel = "BT HID Switch $switchNum",
            currentKeycode = currentKeycode,
            onKeyCaptured = { keycode ->
                if (switchNum == 1) viewModel.setPhoneBtHidSwitch1Keycode(keycode)
                else viewModel.setPhoneBtHidSwitch2Keycode(keycode)
                capturingSwitch = null
            },
            onDismiss = { capturingSwitch = null }
        )
    }
}
