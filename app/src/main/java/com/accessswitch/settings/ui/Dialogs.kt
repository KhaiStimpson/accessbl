package com.accessswitch.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accessswitch.settings.SettingsViewModel

/**
 * Dialog that captures the next physical key press and returns its keycode.
 * Used for configuring switch key assignments.
 *
 * The dialog listens for key events on a focused Box. When a key is pressed,
 * it captures the keycode and dismisses.
 */
@Composable
fun KeyCaptureDialog(
    switchLabel: String,
    currentKeycode: Int,
    onKeyCaptured: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var capturedKeycode by remember { mutableStateOf<Int?>(null) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                text = "Configure $switchLabel",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (capturedKeycode == null) {
                    Text(
                        text = "Press the key you want to assign to $switchLabel",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pulsing indicator
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8AB4F8).copy(alpha = 0.2f))
                            .border(2.dp, Color(0xFF8AB4F8), CircleShape)
                            .focusRequester(focusRequester)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    capturedKeycode = event.key.nativeKeyCode
                                    true
                                } else {
                                    false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "...",
                            color = Color(0xFF8AB4F8),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Current: ${SettingsViewModel.keycodeName(currentKeycode)}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = "Key captured!",
                        color = Color(0xFF4CAF50),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = SettingsViewModel.keycodeName(capturedKeycode!!),
                        color = Color(0xFF8AB4F8),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            if (capturedKeycode != null) {
                Button(
                    onClick = { onKeyCaptured(capturedKeycode!!) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8AB4F8)
                    )
                ) {
                    Text("Apply", color = Color.Black)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * PIN entry dialog for settings protection.
 *
 * Shows a numeric keypad with 4-digit PIN entry.
 * Supports both "enter PIN to unlock" and "set new PIN" modes.
 */
@Composable
fun PinDialog(
    mode: PinDialogMode,
    onPinEntered: (String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                text = when (mode) {
                    PinDialogMode.UNLOCK -> "Enter PIN"
                    PinDialogMode.SET_NEW -> "Set New PIN"
                    PinDialogMode.CONFIRM_NEW -> "Confirm PIN"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PIN dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < pin.length) Color(0xFF8AB4F8)
                                    else Color(0xFF3D3D3D)
                                )
                        )
                    }
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = Color(0xFFF44336),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Number pad
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )

                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.size(56.dp))
                            } else {
                                NumPadKey(
                                    label = key,
                                    onClick = {
                                        when (key) {
                                            "DEL" -> {
                                                if (pin.isNotEmpty()) {
                                                    pin = pin.dropLast(1)
                                                }
                                            }
                                            else -> {
                                                if (pin.length < 4) {
                                                    pin += key
                                                    if (pin.length == 4) {
                                                        onPinEntered(pin)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun NumPadKey(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Color(0xFF2D2D2D))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = if (label == "DEL") 12.sp else 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

enum class PinDialogMode {
    UNLOCK,
    SET_NEW,
    CONFIRM_NEW
}
