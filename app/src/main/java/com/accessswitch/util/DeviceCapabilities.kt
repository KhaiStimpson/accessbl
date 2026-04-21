package com.accessswitch.util

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to check device capabilities for graceful degradation.
 *
 * Used by UI components to hide features not available on the current device
 * (e.g., hide phone tile on Chromebook, hide BT HID if unsupported).
 */
@Singleton
class DeviceCapabilities @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var btHidSupported: Boolean? = null

    /**
     * Check if this device has telephony capability (can make phone calls).
     * Returns false on Chromebooks without SIM, tablets without telephony, etc.
     */
    fun hasTelephony(): Boolean {
        // Check if telephony feature is available
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return false
        }

        // Double-check with TelephonyManager
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return telephonyManager?.phoneType != TelephonyManager.PHONE_TYPE_NONE
    }

    /**
     * Check if this device is likely a Chromebook (running Android in ARC++).
     * Heuristic based on device characteristics.
     */
    fun isChromebook(): Boolean {
        // Check for ARC feature
        if (context.packageManager.hasSystemFeature("org.chromium.arc.device_management")) {
            return true
        }

        // Check for typical Chromebook characteristics
        if (context.packageManager.hasSystemFeature("org.chromium.arc")) {
            return true
        }

        // Fallback: check manufacturer/model patterns
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        return manufacturer.contains("chromebook") ||
            model.contains("chromebook") ||
            model.contains("chromeos")
    }

    /**
     * Check if Bluetooth HID Device profile is supported.
     * BluetoothHidDevice was added in API 28 but may not be available on all devices.
     */
    fun isBtHidSupported(): Boolean {
        // Return cached result if available
        btHidSupported?.let { return it }

        // BluetoothHidDevice requires API 28+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            btHidSupported = false
            return false
        }

        // Check if Bluetooth is available
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        if (bluetoothAdapter == null) {
            btHidSupported = false
            return false
        }

        // Check if HID Device profile is supported
        // BluetoothHidDevice is available on API 28+ when a Bluetooth adapter is present.
        // getProfileConnectionState() was previously used here but requires BLUETOOTH_CONNECT
        // permission on API 31+ and always returned true when no exception was thrown,
        // so we simplify: API level + adapter presence is sufficient.
        btHidSupported = true
        return true
    }

    /**
     * Check if the device has a touchscreen (for phone-as-switch local mode).
     */
    fun hasTouchscreen(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    }

    /**
     * Check if Bluetooth is available on this device.
     */
    fun hasBluetooth(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    /**
     * Check if the TelecomManager can handle call controls.
     */
    fun canControlCalls(): Boolean {
        if (!hasTelephony()) return false

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        return telecomManager != null
    }

    /**
     * Get a summary of available features for logging/debugging.
     */
    fun getCapabilitySummary(): Map<String, Boolean> = mapOf(
        "telephony" to hasTelephony(),
        "chromebook" to isChromebook(),
        "bt_hid" to isBtHidSupported(),
        "touchscreen" to hasTouchscreen(),
        "bluetooth" to hasBluetooth(),
        "call_control" to canControlCalls()
    )
}
