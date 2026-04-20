package com.accessswitch.overlay

import android.content.Context
import android.content.Intent
import com.accessswitch.MainActivity
import com.accessswitch.nav.NavController
import com.accessswitch.netflix.NetflixController
import com.accessswitch.phone.PhoneController
import com.accessswitch.scanning.ScanItem
import com.accessswitch.scanning.ScanningEngine
import com.accessswitch.util.DeviceCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the main menu scan items — the top-level grid shown when
 * scanning starts. Each item navigates to a sub-panel (Phone, Netflix,
 * Nav, Settings) or performs a direct action.
 *
 * Sub-panel items are provided by their respective controllers
 * (NavController, PhoneController, NetflixController).
 *
 * Uses [DeviceCapabilities] to hide unavailable features (graceful degradation).
 */
@Singleton
class MainMenuProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val navController: NavController,
    private val phoneController: PhoneController,
    private val netflixController: NetflixController,
    private val scanningEngine: ScanningEngine,
    private val deviceCapabilities: DeviceCapabilities
) {
    /**
     * Build the main menu items.
     * Dynamically includes/excludes items based on device capabilities.
     *
     * - Navigate: Always available
     * - Phone: Hidden on Chromebook or devices without telephony
     * - Netflix: Always available (works on both platforms)
     * - Settings: Always available
     */
    fun buildMainMenu(): List<ScanItem> {
        val items = mutableListOf<ScanItem>()

        // Navigate - always available
        items.add(
            ScanItem(
                id = "menu_nav",
                label = "Navigate",
                action = { loadNavPanel() }
            )
        )

        // Phone - only on devices with telephony
        if (deviceCapabilities.hasTelephony() && !deviceCapabilities.isChromebook()) {
            items.add(
                ScanItem(
                    id = "menu_phone",
                    label = "Phone",
                    action = { loadPhonePanel() }
                )
            )
        }

        // Netflix - always available
        items.add(
            ScanItem(
                id = "menu_netflix",
                label = "Netflix",
                action = { loadNetflixPanel() }
            )
        )

        // Settings - always available
        items.add(
            ScanItem(
                id = "menu_settings",
                label = "Settings",
                action = { openSettingsActivity() }
            )
        )

        return items
    }

    /**
     * Check if phone functionality is available on this device.
     */
    fun isPhoneAvailable(): Boolean {
        return deviceCapabilities.hasTelephony() && !deviceCapabilities.isChromebook()
    }

    /**
     * Check if BT HID remote mode is available on this device.
     */
    fun isBtHidAvailable(): Boolean {
        return deviceCapabilities.isBtHidSupported()
    }

    /**
     * Load phone panel items into the scanner.
     * Replaces the current scan items with contacts + call controls.
     */
    private fun loadPhonePanel() {
        if (!isPhoneAvailable()) {
            // Should not happen since menu item is hidden, but handle gracefully
            loadMainMenu()
            return
        }
        phoneController.activatePhonePanel(scanningEngine) { loadMainMenu() }
    }

    /**
     * Load Netflix panel items into the scanner.
     * Replaces the current scan items with Netflix controls.
     */
    private fun loadNetflixPanel() {
        netflixController.activateNetflixPanel(scanningEngine) { loadMainMenu() }
    }

    /**
     * Load navigation sub-panel items into the scanner.
     * Replaces the current scan items with nav actions + back button.
     */
    private fun loadNavPanel() {
        val navItems = buildNavPanel()
        scanningEngine.setItems(navItems)
    }

    /**
     * Build navigation sub-panel items.
     * Used when the user selects "Navigate" from the main menu.
     */
    fun buildNavPanel(): List<ScanItem> {
        val navItems = navController.buildScanItems().toMutableList()
        // Add a "Back to Menu" item
        navItems.add(
            ScanItem(
                id = "nav_back_to_menu",
                label = "Back to Menu",
                action = { loadMainMenu() }
            )
        )
        return navItems
    }

    /**
     * Reload the main menu items into the scanner.
     */
    fun loadMainMenu() {
        val menuItems = buildMainMenu()
        scanningEngine.setItems(menuItems)
    }

    /**
     * Launch the settings activity from the scanning overlay.
     */
    private fun openSettingsActivity() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "settings")
        }
        context.startActivity(intent)
    }
}
