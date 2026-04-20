package com.accessswitch.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accessswitch.R
import com.accessswitch.settings.SettingsViewModel

/**
 * PIN lock settings screen.
 *
 * If no PIN is set: shows option to set one.
 * If PIN is set: shows option to change or remove it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLockScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val hasPinSet = settings.settingsPinHash != null

    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogMode by remember { mutableStateOf(PinDialogMode.SET_NEW) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var pendingNewPin by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_pin_lock)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (hasPinSet) {
                SettingsLabel("PIN is currently set")
                Text(
                    text = "Settings are protected. A PIN is required to access the settings screen from the scanning overlay.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        pinDialogMode = PinDialogMode.SET_NEW
                        pendingNewPin = null
                        pinError = null
                        showPinDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8AB4F8)
                    )
                ) {
                    Text("Change PIN", color = Color.Black)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.clearPin() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Remove PIN", color = Color.White)
                }
            } else {
                SettingsLabel("No PIN set")
                Text(
                    text = "Set a 4-digit PIN to prevent the user from accidentally changing settings.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        pinDialogMode = PinDialogMode.SET_NEW
                        pendingNewPin = null
                        pinError = null
                        showPinDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8AB4F8)
                    )
                ) {
                    Text("Set PIN", color = Color.Black)
                }
            }
        }
    }

    if (showPinDialog) {
        PinDialog(
            mode = pinDialogMode,
            errorMessage = pinError,
            onPinEntered = { pin ->
                when (pinDialogMode) {
                    PinDialogMode.SET_NEW -> {
                        pendingNewPin = pin
                        pinDialogMode = PinDialogMode.CONFIRM_NEW
                        pinError = null
                    }
                    PinDialogMode.CONFIRM_NEW -> {
                        if (pin == pendingNewPin) {
                            viewModel.setPin(pin)
                            showPinDialog = false
                        } else {
                            pinError = "PINs don't match. Try again."
                            pinDialogMode = PinDialogMode.SET_NEW
                            pendingNewPin = null
                        }
                    }
                    PinDialogMode.UNLOCK -> {
                        if (viewModel.verifyPin(pin)) {
                            showPinDialog = false
                        } else {
                            pinError = "Incorrect PIN"
                        }
                    }
                }
            },
            onDismiss = { showPinDialog = false }
        )
    }
}
