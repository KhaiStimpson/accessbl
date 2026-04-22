package com.accessswitch.settings

import android.content.Context
import android.view.KeyEvent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.accessswitch.input.SwitchId
import com.accessswitch.scanning.ScanMode
import com.accessswitch.switchscreen.SwitchZoneLayout
import com.accessswitch.util.StartupLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "access_switch_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SCAN_MODE = stringPreferencesKey("scan_mode")
        val SCAN_INTERVAL_MS = longPreferencesKey("scan_interval_ms")
        val SCAN_LOOPS = intPreferencesKey("scan_loops")
        val HIGHLIGHT_COLOR = intPreferencesKey("highlight_color")
        val HIGHLIGHT_STYLE = stringPreferencesKey("highlight_style")
        val AUDIO_FEEDBACK = booleanPreferencesKey("audio_feedback")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")

        val SWITCH1_KEYCODE = intPreferencesKey("switch1_keycode")
        val SWITCH2_KEYCODE = intPreferencesKey("switch2_keycode")
        val DEBOUNCE_MS = longPreferencesKey("debounce_ms")

        val PHONE_LOCAL_SWITCH_ENABLED = booleanPreferencesKey("phone_local_switch_enabled")
        val PHONE_LOCAL_ZONE_LAYOUT = stringPreferencesKey("phone_local_zone_layout")
        val PHONE_LOCAL_ZONE1_SWITCH = stringPreferencesKey("phone_local_zone1_switch")
        val PHONE_LOCAL_ZONE2_SWITCH = stringPreferencesKey("phone_local_zone2_switch")
        val SWITCH_SCREEN_LOCKED = booleanPreferencesKey("switch_screen_locked")

        val PHONE_BT_HID_ENABLED = booleanPreferencesKey("phone_bt_hid_enabled")
        val PHONE_BT_HID_SWITCH1_KEYCODE = intPreferencesKey("phone_bt_hid_switch1_keycode")
        val PHONE_BT_HID_SWITCH2_KEYCODE = intPreferencesKey("phone_bt_hid_switch2_keycode")
        val PHONE_BT_HID_PAIRED_DEVICE = stringPreferencesKey("phone_bt_hid_paired_device")

        val SETTINGS_PIN_HASH = stringPreferencesKey("settings_pin_hash")
        val FAVOURITE_CONTACT_IDS = stringSetPreferencesKey("favourite_contact_ids")
    }

    /**
     * Observable flow of settings. UI should collect this.
     */
    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        mapPrefsToSettings(prefs)
    }

    // Cached settings for fast synchronous access from onKeyEvent
    @Volatile
    private var _cachedSettings: AppSettings = AppSettings()
    private var _settingsLoadedFromDisk = false

    /**
     * Synchronous read for use in non-coroutine contexts (e.g., onKeyEvent).
     * Returns the cached value — updated whenever DataStore emits.
     * Defaults to AppSettings() until the first DataStore emission arrives.
     */
    val currentSettings: AppSettings
        get() = _cachedSettings

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Populate cache asynchronously so the main thread is never blocked.
        // Uses AppSettings() defaults until the first DataStore emission arrives
        // (typically within a few milliseconds).
        StartupLogger.log("SettingsRepository: init — starting async DataStore load")
        repositoryScope.launch {
            try {
                settingsFlow.collect { settings ->
                    _cachedSettings = settings
                    if (!_settingsLoadedFromDisk) {
                        _settingsLoadedFromDisk = true
                        StartupLogger.log("SettingsRepository: initial settings loaded from DataStore")
                    }
                }
            } catch (e: Exception) {
                StartupLogger.error("SettingsRepository: DataStore read failed", e)
            }
        }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val current = _cachedSettings
        val updated = transform(current)
        context.dataStore.edit { prefs ->
            prefs[Keys.SCAN_MODE] = updated.scanMode.name
            prefs[Keys.SCAN_INTERVAL_MS] = updated.scanIntervalMs
            prefs[Keys.SCAN_LOOPS] = updated.scanLoops
            prefs[Keys.HIGHLIGHT_COLOR] = updated.highlightColor
            prefs[Keys.HIGHLIGHT_STYLE] = updated.highlightStyle.name
            prefs[Keys.AUDIO_FEEDBACK] = updated.audioFeedback
            prefs[Keys.HAPTIC_FEEDBACK] = updated.hapticFeedback

            prefs[Keys.SWITCH1_KEYCODE] = updated.switch1Keycode
            prefs[Keys.SWITCH2_KEYCODE] = updated.switch2Keycode
            prefs[Keys.DEBOUNCE_MS] = updated.debounceMs

            prefs[Keys.PHONE_LOCAL_SWITCH_ENABLED] = updated.phoneLocalSwitchEnabled
            prefs[Keys.PHONE_LOCAL_ZONE_LAYOUT] = updated.phoneLocalZoneLayout.name
            prefs[Keys.PHONE_LOCAL_ZONE1_SWITCH] = updated.phoneLocalZone1SwitchId.name
            prefs[Keys.PHONE_LOCAL_ZONE2_SWITCH] = updated.phoneLocalZone2SwitchId.name
            prefs[Keys.SWITCH_SCREEN_LOCKED] = updated.switchScreenLocked

            prefs[Keys.PHONE_BT_HID_ENABLED] = updated.phoneBtHidEnabled
            prefs[Keys.PHONE_BT_HID_SWITCH1_KEYCODE] = updated.phoneBtHidSwitch1Keycode
            prefs[Keys.PHONE_BT_HID_SWITCH2_KEYCODE] = updated.phoneBtHidSwitch2Keycode
            updated.phoneBtHidPairedDeviceAddress?.let {
                prefs[Keys.PHONE_BT_HID_PAIRED_DEVICE] = it
            }

            updated.settingsPinHash?.let { prefs[Keys.SETTINGS_PIN_HASH] = it }
            prefs[Keys.FAVOURITE_CONTACT_IDS] = updated.favouriteContactIds.map { it.toString() }.toSet()
        }
        _cachedSettings = updated
    }

    private fun mapPrefsToSettings(prefs: Preferences): AppSettings {
        return AppSettings(
            scanMode = prefs[Keys.SCAN_MODE]?.let {
                try { ScanMode.valueOf(it) } catch (_: Exception) { ScanMode.AUTO }
            } ?: ScanMode.AUTO,
            scanIntervalMs = prefs[Keys.SCAN_INTERVAL_MS] ?: 1500L,
            scanLoops = prefs[Keys.SCAN_LOOPS] ?: 3,
            highlightColor = prefs[Keys.HIGHLIGHT_COLOR] ?: 0xFFFFEB3B.toInt(),
            highlightStyle = prefs[Keys.HIGHLIGHT_STYLE]?.let {
                try { HighlightStyle.valueOf(it) } catch (_: Exception) { HighlightStyle.BORDER }
            } ?: HighlightStyle.BORDER,
            audioFeedback = prefs[Keys.AUDIO_FEEDBACK] ?: false,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,

            switch1Keycode = prefs[Keys.SWITCH1_KEYCODE] ?: KeyEvent.KEYCODE_SPACE,
            switch2Keycode = prefs[Keys.SWITCH2_KEYCODE] ?: KeyEvent.KEYCODE_ENTER,
            debounceMs = prefs[Keys.DEBOUNCE_MS] ?: 200L,

            phoneLocalSwitchEnabled = prefs[Keys.PHONE_LOCAL_SWITCH_ENABLED] ?: false,
            phoneLocalZoneLayout = prefs[Keys.PHONE_LOCAL_ZONE_LAYOUT]?.let {
                try { SwitchZoneLayout.valueOf(it) } catch (_: Exception) { SwitchZoneLayout.LEFT_RIGHT }
            } ?: SwitchZoneLayout.LEFT_RIGHT,
            phoneLocalZone1SwitchId = prefs[Keys.PHONE_LOCAL_ZONE1_SWITCH]?.let {
                try { SwitchId.valueOf(it) } catch (_: Exception) { SwitchId.SWITCH_1 }
            } ?: SwitchId.SWITCH_1,
            phoneLocalZone2SwitchId = prefs[Keys.PHONE_LOCAL_ZONE2_SWITCH]?.let {
                try { SwitchId.valueOf(it) } catch (_: Exception) { SwitchId.SWITCH_2 }
            } ?: SwitchId.SWITCH_2,
            switchScreenLocked = prefs[Keys.SWITCH_SCREEN_LOCKED] ?: false,

            phoneBtHidEnabled = prefs[Keys.PHONE_BT_HID_ENABLED] ?: false,
            phoneBtHidSwitch1Keycode = prefs[Keys.PHONE_BT_HID_SWITCH1_KEYCODE] ?: KeyEvent.KEYCODE_SPACE,
            phoneBtHidSwitch2Keycode = prefs[Keys.PHONE_BT_HID_SWITCH2_KEYCODE] ?: KeyEvent.KEYCODE_ENTER,
            phoneBtHidPairedDeviceAddress = prefs[Keys.PHONE_BT_HID_PAIRED_DEVICE],

            settingsPinHash = prefs[Keys.SETTINGS_PIN_HASH],
            favouriteContactIds = prefs[Keys.FAVOURITE_CONTACT_IDS]
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet() ?: emptySet()
        )
    }
}
