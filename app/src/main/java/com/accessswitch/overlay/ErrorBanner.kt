package com.accessswitch.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enumeration of error/warning states that can be shown in the overlay.
 */
enum class ErrorState {
    /** Bluetooth switch has disconnected */
    BT_SWITCH_DISCONNECTED,

    /** Phone calling is unavailable (no SIM, Chromebook, etc.) */
    CALLING_UNAVAILABLE,

    /** Required permissions not granted */
    PERMISSIONS_MISSING,

    /** BT HID profile not supported on this device */
    BT_HID_UNSUPPORTED,

    /** Switch input source lost */
    SWITCH_SOURCE_LOST
}

/**
 * Data class representing an active error/warning to display.
 */
data class ErrorBannerState(
    val errorState: ErrorState,
    val message: String,
    val isDismissible: Boolean = true
)

/**
 * Error/warning banner displayed at the top of the overlay.
 * Slides in from the top when an error is active.
 *
 * Uses WCAG-compliant high contrast colors.
 */
@Composable
fun ErrorBanner(
    state: ErrorBannerState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state != null,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        state?.let { errorState ->
            val (backgroundColor, icon) = getErrorStyling(errorState.errorState)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = errorState.message,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (errorState.isDismissible) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get styling (background color and icon) for an error state.
 */
private fun getErrorStyling(errorState: ErrorState): Pair<Color, ImageVector> {
    return when (errorState) {
        ErrorState.BT_SWITCH_DISCONNECTED -> Pair(
            Color(0xFFE65100), // Dark orange (warning)
            Icons.Default.BluetoothDisabled
        )
        ErrorState.CALLING_UNAVAILABLE -> Pair(
            Color(0xFF5D4037), // Brown (info)
            Icons.Default.PhoneDisabled
        )
        ErrorState.PERMISSIONS_MISSING -> Pair(
            Color(0xFFB71C1C), // Dark red (error)
            Icons.Default.Security
        )
        ErrorState.BT_HID_UNSUPPORTED -> Pair(
            Color(0xFF5D4037), // Brown (info)
            Icons.Default.BluetoothDisabled
        )
        ErrorState.SWITCH_SOURCE_LOST -> Pair(
            Color(0xFFE65100), // Dark orange (warning)
            Icons.Default.Warning
        )
    }
}

/**
 * Helper to create error banner state from error type.
 */
object ErrorMessages {
    fun btSwitchDisconnected() = ErrorBannerState(
        errorState = ErrorState.BT_SWITCH_DISCONNECTED,
        message = "Bluetooth switch disconnected. Using phone zones.",
        isDismissible = true
    )

    fun callingUnavailable() = ErrorBannerState(
        errorState = ErrorState.CALLING_UNAVAILABLE,
        message = "Phone calling is not available on this device.",
        isDismissible = true
    )

    fun permissionsMissing(permission: String) = ErrorBannerState(
        errorState = ErrorState.PERMISSIONS_MISSING,
        message = "Permission required: $permission. Tap to open settings.",
        isDismissible = false
    )

    fun btHidUnsupported() = ErrorBannerState(
        errorState = ErrorState.BT_HID_UNSUPPORTED,
        message = "Bluetooth HID not supported on this device.",
        isDismissible = true
    )

    fun switchSourceLost() = ErrorBannerState(
        errorState = ErrorState.SWITCH_SOURCE_LOST,
        message = "Switch input lost. Check connection.",
        isDismissible = true
    )
}
