package com.accessswitch.scanning

/**
 * Represents a single scannable item in the scanning overlay.
 *
 * @param id Unique identifier for this item
 * @param label Display text shown on the tile
 * @param iconRes Optional drawable resource ID for an icon
 * @param action Lambda executed when this item is selected
 * @param children Sub-items for row-column scanning (e.g., items within a row)
 */
data class ScanItem(
    val id: String,
    val label: String,
    val iconRes: Int? = null,
    val action: () -> Unit = {},
    val children: List<ScanItem> = emptyList()
)
