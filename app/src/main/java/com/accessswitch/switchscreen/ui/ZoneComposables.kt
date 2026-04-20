package com.accessswitch.switchscreen.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Zone colors — high contrast for accessibility.
 */
private val ZONE_1_COLOR = Color(0xFF1565C0) // Blue
private val ZONE_2_COLOR = Color(0xFF2E7D32) // Green
private val ZONE_FLASH_COLOR = Color.White

private const val FLASH_DURATION_MS = 150
private const val ZONE_LABEL_SIZE = 48

/**
 * Full-screen single zone — the entire screen is one switch.
 */
@Composable
fun FullScreenZone(
    label: String,
    isFlashing: Boolean,
    onTap: () -> Unit,
    onFlashDone: () -> Unit
) {
    ZoneBox(
        label = label,
        baseColor = ZONE_1_COLOR,
        isFlashing = isFlashing,
        onTap = onTap,
        onFlashDone = onFlashDone,
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Left-right split — two equal vertical zones side by side.
 */
@Composable
fun SplitLeftRightZones(
    label1: String,
    label2: String,
    flashingZone: Int?,
    onZone1Tap: () -> Unit,
    onZone2Tap: () -> Unit,
    onFlashDone: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        ZoneBox(
            label = label1,
            baseColor = ZONE_1_COLOR,
            isFlashing = flashingZone == 0,
            onTap = onZone1Tap,
            onFlashDone = onFlashDone,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        ZoneBox(
            label = label2,
            baseColor = ZONE_2_COLOR,
            isFlashing = flashingZone == 1,
            onTap = onZone2Tap,
            onFlashDone = onFlashDone,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

/**
 * Top-bottom split — two equal horizontal zones stacked.
 */
@Composable
fun SplitTopBottomZones(
    label1: String,
    label2: String,
    flashingZone: Int?,
    onZone1Tap: () -> Unit,
    onZone2Tap: () -> Unit,
    onFlashDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ZoneBox(
            label = label1,
            baseColor = ZONE_1_COLOR,
            isFlashing = flashingZone == 0,
            onTap = onZone1Tap,
            onFlashDone = onFlashDone,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        ZoneBox(
            label = label2,
            baseColor = ZONE_2_COLOR,
            isFlashing = flashingZone == 1,
            onTap = onZone2Tap,
            onFlashDone = onFlashDone,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}

/**
 * Asymmetric 80/20 split — 80% left zone, 20% right zone.
 */
@Composable
fun AsymmetricZones(
    label1: String,
    label2: String,
    flashingZone: Int?,
    onZone1Tap: () -> Unit,
    onZone2Tap: () -> Unit,
    onFlashDone: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        ZoneBox(
            label = label1,
            baseColor = ZONE_1_COLOR,
            isFlashing = flashingZone == 0,
            onTap = onZone1Tap,
            onFlashDone = onFlashDone,
            modifier = Modifier
                .weight(0.8f)
                .fillMaxHeight()
        )
        ZoneBox(
            label = label2,
            baseColor = ZONE_2_COLOR,
            isFlashing = flashingZone == 1,
            onTap = onZone2Tap,
            onFlashDone = onFlashDone,
            modifier = Modifier
                .weight(0.2f)
                .fillMaxHeight()
        )
    }
}

/**
 * A single zone box with label, background color, tap handling,
 * and flash animation on press.
 *
 * Touch handling uses ACTION_DOWN only — ignores move/up to prevent
 * drag-to-select issues for motor-impaired users.
 */
@Composable
fun ZoneBox(
    label: String,
    baseColor: Color,
    isFlashing: Boolean,
    onTap: () -> Unit,
    onFlashDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isFlashing) ZONE_FLASH_COLOR else baseColor,
        animationSpec = tween(durationMillis = if (isFlashing) 50 else FLASH_DURATION_MS),
        label = "zoneFlash"
    )

    // Reset flash after animation duration
    if (isFlashing) {
        LaunchedEffect(Unit) {
            delay(FLASH_DURATION_MS.toLong())
            onFlashDone()
        }
    }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        // Process only ACTION_DOWN (first pointer contact)
                        val changes = event.changes
                        if (changes.any { it.pressed && !it.previousPressed }) {
                            onTap()
                            // Consume the down event
                            changes.forEach { it.consume() }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isFlashing) Color.Black else Color.White,
            fontSize = ZONE_LABEL_SIZE.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}
