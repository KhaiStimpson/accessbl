package com.accessswitch.util

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accessswitch.accessibility.AccessSwitchAccessibilityService

/**
 * In-app diagnostics panel.
 *
 * Shows startup log entries from [StartupLogger] and live service status
 * so issues can be diagnosed without a USB / logcat connection.
 *
 * Tap the "DIAG" badge to expand / collapse the panel.
 */
@Composable
fun DiagnosticsPanel(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(true) }
    val entries by StartupLogger.entries.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new entries arrive
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Header row ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(Color(0xFF1E1E1E))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF8AB4F8))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "DIAG",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Startup Diagnostics",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = if (expanded) "▲ hide" else "▼ show",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }

        // ── Expandable body ───────────────────────────────────────────────────
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(Color(0xFF121212))
                    .border(
                        1.dp,
                        Color(0xFF333333),
                        RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
                    .padding(8.dp)
            ) {
                // Service status
                ServiceStatusRow()

                Spacer(modifier = Modifier.height(8.dp))

                // Log entries
                if (entries.isEmpty()) {
                    Text(
                        text = "No log entries yet…",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(4.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(entries) { entry ->
                            Text(
                                text = "[+${entry.elapsedMs}ms] ${entry.message}",
                                color = if (entry.isError) Color(0xFFFF6B6B) else Color(0xFFB0BEC5),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceStatusRow() {
    val accessibilityActive = AccessSwitchAccessibilityService.instance != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusChip(
            label = "Accessibility",
            active = accessibilityActive,
            activeColor = Color(0xFF81C784),
            inactiveColor = Color(0xFFEF9A9A)
        )
    }
}

@Composable
private fun StatusChip(
    label: String,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) activeColor else inactiveColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: ${if (active) "ON" else "OFF"}",
            color = if (active) activeColor else inactiveColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
