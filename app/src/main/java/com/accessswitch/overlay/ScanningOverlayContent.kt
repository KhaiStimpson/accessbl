package com.accessswitch.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accessswitch.scanning.ScanItem
import com.accessswitch.scanning.ScanState
import com.accessswitch.scanning.ScanningEngine
import com.accessswitch.settings.AppSettings
import com.accessswitch.settings.HighlightStyle
import com.accessswitch.settings.SettingsRepository

/**
 * Root composable for the scanning overlay.
 * Observes ScanningEngine state and renders the tile grid with highlight.
 */
@Composable
fun ScanningOverlayContent(
    scanningEngine: ScanningEngine,
    settingsRepository: SettingsRepository
) {
    val scanState by scanningEngine.state.collectAsState()
    val settings = settingsRepository.currentSettings

    if (!scanState.isScanning || scanState.items.isEmpty()) {
        // Don't render anything when not scanning
        return
    }

    ScanTileGrid(
        scanState = scanState,
        settings = settings
    )
}

/**
 * Grid of scannable item tiles with highlight indicator.
 *
 * Uses adaptive columns with a minimum size of 120dp per tile.
 * The highlighted tile shows a colored border/fill based on settings.
 */
@Composable
fun ScanTileGrid(
    scanState: ScanState,
    settings: AppSettings
) {
    val highlightColor = Color(settings.highlightColor)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = MIN_TILE_SIZE_DP.dp),
            contentPadding = PaddingValues(GRID_PADDING.dp),
            horizontalArrangement = Arrangement.spacedBy(TILE_SPACING.dp),
            verticalArrangement = Arrangement.spacedBy(TILE_SPACING.dp),
            modifier = Modifier.padding(GRID_PADDING.dp)
        ) {
            itemsIndexed(scanState.items) { index, item ->
                ScanTile(
                    item = item,
                    isHighlighted = index == scanState.highlightedIndex,
                    highlightColor = highlightColor,
                    highlightStyle = settings.highlightStyle
                )
            }
        }
    }
}

/**
 * Individual scan tile representing one ScanItem.
 *
 * Minimum size 120x120dp as per TDD spec.
 * Shows label and optional icon. When highlighted, shows
 * colored border/fill/both depending on HighlightStyle setting.
 */
@Composable
fun ScanTile(
    item: ScanItem,
    isHighlighted: Boolean,
    highlightColor: Color,
    highlightStyle: HighlightStyle
) {
    val shape = RoundedCornerShape(TILE_CORNER_RADIUS.dp)

    // Animate highlight transitions
    val borderColor by animateColorAsState(
        targetValue = when {
            isHighlighted && highlightStyle != HighlightStyle.FILL -> highlightColor
            else -> Color.White.copy(alpha = 0.3f)
        },
        animationSpec = tween(durationMillis = HIGHLIGHT_ANIM_MS),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHighlighted && highlightStyle != HighlightStyle.BORDER ->
                highlightColor.copy(alpha = 0.3f)
            else -> TILE_BG_COLOR
        },
        animationSpec = tween(durationMillis = HIGHLIGHT_ANIM_MS),
        label = "bgColor"
    )

    val borderWidth = if (isHighlighted && highlightStyle != HighlightStyle.FILL) {
        HIGHLIGHT_BORDER_WIDTH.dp
    } else {
        DEFAULT_BORDER_WIDTH.dp
    }

    Box(
        modifier = Modifier
            .sizeIn(minWidth = MIN_TILE_SIZE_DP.dp, minHeight = MIN_TILE_SIZE_DP.dp)
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(TILE_INNER_PADDING.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item.iconRes?.let { iconRes ->
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = item.label,
                    tint = if (isHighlighted) highlightColor else Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Text(
                text = item.label,
                color = if (isHighlighted) Color.White else Color.White.copy(alpha = 0.9f),
                fontSize = TILE_TEXT_SIZE.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- Constants ---

/** Minimum tile size in dp (per TDD spec) */
private const val MIN_TILE_SIZE_DP = 120

/** Spacing between tiles in dp */
private const val TILE_SPACING = 8

/** Padding around the entire grid */
private const val GRID_PADDING = 16

/** Corner radius for tile shape */
private const val TILE_CORNER_RADIUS = 12

/** Inner padding of each tile */
private const val TILE_INNER_PADDING = 12

/** Border width when highlighted */
private const val HIGHLIGHT_BORDER_WIDTH = 4

/** Border width when not highlighted */
private const val DEFAULT_BORDER_WIDTH = 1

/** Animation duration for highlight transitions in ms */
private const val HIGHLIGHT_ANIM_MS = 150

/** Default tile background color (dark) */
private val TILE_BG_COLOR = Color(0xFF2D2D2D)

/** Font size for tile labels */
private const val TILE_TEXT_SIZE = 16
