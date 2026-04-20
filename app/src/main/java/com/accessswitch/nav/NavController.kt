package com.accessswitch.nav

import com.accessswitch.accessibility.AccessSwitchAccessibilityService
import com.accessswitch.scanning.ScanItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around AccessibilityService.performGlobalAction for OS navigation.
 * Provides ScanItem lists for the scanning overlay.
 */
@Singleton
class NavController @Inject constructor() {

    private val service: AccessSwitchAccessibilityService?
        get() = AccessSwitchAccessibilityService.instance

    fun goHome() {
        service?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
    }

    fun goBack() {
        service?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
    }

    fun openRecents() {
        service?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    fun openNotifications() {
        service?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettings() {
        service?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
    }

    /**
     * Build the scannable items for the navigation panel.
     */
    fun buildScanItems(): List<ScanItem> = listOf(
        ScanItem(id = "nav_home", label = "Home", action = ::goHome),
        ScanItem(id = "nav_back", label = "Back", action = ::goBack),
        ScanItem(id = "nav_recents", label = "Recents", action = ::openRecents),
        ScanItem(id = "nav_notifications", label = "Notifications", action = ::openNotifications),
        ScanItem(id = "nav_quick_settings", label = "Quick Settings", action = ::openQuickSettings),
    )
}
