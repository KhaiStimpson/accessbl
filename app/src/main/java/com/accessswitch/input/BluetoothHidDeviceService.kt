package com.accessswitch.input

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.accessswitch.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BT HID connection state.
 */
enum class BtHidState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * Service that registers the phone as a BT HID keyboard device
 * and sends keystroke reports to a paired Chromebook.
 *
 * When the user taps a zone on the Switch Screen, the configured
 * keycode is sent as a HID keyboard report (key-down + immediate key-up)
 * to the paired Chromebook.
 *
 * **API:** BluetoothHidDevice (Android API 28+)
 *
 * **Pairing flow:**
 * 1. Caregiver opens settings → taps "Pair as Switch for Chromebook"
 * 2. Phone enables BT discoverability + registers HID profile
 * 3. Chromebook pairs (phone appears as "AccessSwitch Keyboard")
 * 4. Pairing stored; phone auto-reconnects on subsequent sessions
 *
 * **Latency target:** < 150ms from zone tap to Chromebook onKeyEvent
 */
@RequiresApi(Build.VERSION_CODES.P)
@AndroidEntryPoint
class BluetoothHidDeviceService : Service() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null

    companion object {
        const val CHANNEL_ID = "bt_hid_service"
        const val NOTIFICATION_ID = 2

        const val ACTION_START_PAIRING = "com.accessswitch.ACTION_START_PAIRING"
        const val ACTION_STOP = "com.accessswitch.ACTION_STOP_BT_HID"
        const val ACTION_SEND_KEY = "com.accessswitch.ACTION_SEND_KEY"
        const val EXTRA_KEYCODE = "keycode"

        private const val HID_REPORT_ID: Int = 0
        private const val HID_REPORT_MAX_SIZE = 8

        /**
         * Global state flow — accessible from SettingsViewModel to display
         * connection status without binding to the service.
         */
        private val _state = MutableStateFlow(BtHidState.DISCONNECTED)
        val state: StateFlow<BtHidState> = _state.asStateFlow()

        private val _connectedDeviceName = MutableStateFlow<String?>(null)
        val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

        /**
         * Standard HID keyboard boot descriptor.
         * Defines a keyboard with 8 modifier keys and 6 simultaneous key presses.
         */
        val HID_DESCRIPTOR = byteArrayOf(
            0x05.toByte(), 0x01.toByte(), // Usage Page: Generic Desktop
            0x09.toByte(), 0x06.toByte(), // Usage: Keyboard
            0xa1.toByte(), 0x01.toByte(), // Collection: Application
            // Modifier keys (8 bits)
            0x05.toByte(), 0x07.toByte(), //   Usage Page: Keyboard/Keypad
            0x19.toByte(), 0xe0.toByte(), //   Usage Minimum: Left Control
            0x29.toByte(), 0xe7.toByte(), //   Usage Maximum: Right GUI
            0x15.toByte(), 0x00.toByte(), //   Logical Minimum: 0
            0x25.toByte(), 0x01.toByte(), //   Logical Maximum: 1
            0x75.toByte(), 0x01.toByte(), //   Report Size: 1
            0x95.toByte(), 0x08.toByte(), //   Report Count: 8
            0x81.toByte(), 0x02.toByte(), //   Input: Data, Variable, Absolute
            // Reserved byte
            0x75.toByte(), 0x08.toByte(), //   Report Size: 8
            0x95.toByte(), 0x01.toByte(), //   Report Count: 1
            0x81.toByte(), 0x01.toByte(), //   Input: Constant
            // Key codes (6 bytes)
            0x75.toByte(), 0x08.toByte(), //   Report Size: 8
            0x95.toByte(), 0x06.toByte(), //   Report Count: 6
            0x15.toByte(), 0x00.toByte(), //   Logical Minimum: 0
            0x26.toByte(), 0xff.toByte(), 0x00.toByte(), // Logical Maximum: 255
            0x05.toByte(), 0x07.toByte(), //   Usage Page: Keyboard/Keypad
            0x19.toByte(), 0x00.toByte(), //   Usage Minimum: 0
            0x2a.toByte(), 0xff.toByte(), 0x00.toByte(), // Usage Maximum: 255
            0x81.toByte(), 0x00.toByte(), //   Input: Data, Array
            0xc0.toByte()                 // End Collection
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                disconnect()
                stopSelf()
            }
            ACTION_START_PAIRING -> {
                registerHidProfile()
            }
            ACTION_SEND_KEY -> {
                val keycode = intent.getIntExtra(EXTRA_KEYCODE, 0)
                if (keycode != 0) {
                    sendKeyTap(androidKeycodeToHidKeycode(keycode))
                }
            }
            else -> {
                // Default: try to reconnect to previously paired device
                val pairedAddress = settingsRepository.currentSettings.phoneBtHidPairedDeviceAddress
                if (pairedAddress != null) {
                    registerHidProfile()
                } else {
                    // No paired device — just keep service alive
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        disconnect()
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Register the HID device profile with the Bluetooth stack.
     * This makes the phone discoverable as a HID keyboard.
     */
    @SuppressLint("MissingPermission")
    private fun registerHidProfile() {
        val adapter = bluetoothAdapter ?: return
        _state.value = BtHidState.CONNECTING

        adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = proxy as BluetoothHidDevice

                    val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                        "AccessSwitch Keyboard",
                        "Switch input device",
                        "AccessSwitch",
                        BluetoothHidDevice.SUBCLASS1_KEYBOARD,
                        HID_DESCRIPTOR
                    )

                    val registered = hidDevice?.registerApp(
                        sdpSettings,
                        null, // No QoS for input
                        null, // No QoS for output
                        { it.run() }, // Executor — run on calling thread
                        hidDeviceCallback
                    ) ?: false

                    if (!registered) {
                        _state.value = BtHidState.DISCONNECTED
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = null
                    hostDevice = null
                    _state.value = BtHidState.DISCONNECTED
                    _connectedDeviceName.value = null
                }
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    /**
     * Callback for HID device events — connection, disconnection, etc.
     */
    @SuppressLint("MissingPermission")
    private val hidDeviceCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (registered) {
                // Try to connect to previously paired device
                val pairedAddress = settingsRepository.currentSettings.phoneBtHidPairedDeviceAddress
                if (pairedAddress != null) {
                    val device = bluetoothAdapter?.getRemoteDevice(pairedAddress)
                    if (device != null) {
                        hidDevice?.connect(device)
                    }
                }
                // Otherwise, wait for incoming connection (pairing)
            } else {
                _state.value = BtHidState.DISCONNECTED
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    hostDevice = device
                    _state.value = BtHidState.CONNECTED
                    _connectedDeviceName.value = device?.name ?: device?.address

                    // Store the paired device address for auto-reconnect
                    device?.address?.let { address ->
                        val settings = settingsRepository.currentSettings
                        if (settings.phoneBtHidPairedDeviceAddress != address) {
                            // Update asynchronously
                            serviceScope.launch(Dispatchers.IO) {
                                settingsRepository.updateSettings { current ->
                                    current.copy(phoneBtHidPairedDeviceAddress = address)
                                }
                            }
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    hostDevice = null
                    _state.value = BtHidState.DISCONNECTED
                    _connectedDeviceName.value = null
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _state.value = BtHidState.CONNECTING
                }
            }
        }

        override fun onGetReport(
            device: BluetoothDevice?,
            type: Byte,
            id: Byte,
            bufferSize: Int
        ) {
            // Not needed for keyboard
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            // LED state updates from host — not needed
        }
    }

    /**
     * Send a keycode tap — key-down followed immediately by key-up.
     * This simulates a single key press as perceived by the Chromebook.
     *
     * Executed on IO dispatcher to avoid blocking the main thread (ANR prevention).
     *
     * @param hidKeycode HID usage code (not Android keycode)
     */
    @SuppressLint("MissingPermission")
    fun sendKeyTap(hidKeycode: Byte) {
        val device = hostDevice ?: return
        val hid = hidDevice ?: return

        // Execute BT operations off main thread to prevent ANR
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Key-down report: modifier(1) + reserved(1) + keycode(1) + padding(5) = 8 bytes
                val keyDown = ByteArray(HID_REPORT_MAX_SIZE).also { it[2] = hidKeycode }
                // Key-up report: all zeros
                val keyUp = ByteArray(HID_REPORT_MAX_SIZE)

                hid.sendReport(device, HID_REPORT_ID, keyDown)
                hid.sendReport(device, HID_REPORT_ID, keyUp)
            } catch (e: Exception) {
                // Log but don't crash — BT can be flaky
                com.accessswitch.util.CrashReporter.recordException("BT HID sendKeyTap failed", e)
            }
        }
    }

    /**
     * Send a zone tap as a configured keycode.
     * Called from the Switch Screen's touch handler.
     *
     * @param switchId Which switch was triggered (SWITCH_1 or SWITCH_2)
     */
    @SuppressLint("MissingPermission")
    fun sendZoneTap(switchId: SwitchId) {
        val settings = settingsRepository.currentSettings
        val androidKeycode = when (switchId) {
            SwitchId.SWITCH_1 -> settings.phoneBtHidSwitch1Keycode
            SwitchId.SWITCH_2 -> settings.phoneBtHidSwitch2Keycode
        }
        sendKeyTap(androidKeycodeToHidKeycode(androidKeycode))
    }

    @SuppressLint("MissingPermission")
    private fun disconnect() {
        hostDevice?.let { device ->
            hidDevice?.disconnect(device)
        }
        hidDevice?.unregisterApp()
        hidDevice = null
        hostDevice = null
        _state.value = BtHidState.DISCONNECTED
        _connectedDeviceName.value = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BT HID Switch",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Bluetooth HID switch mode"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AccessSwitch")
            .setContentText("BT HID switch active")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    /**
     * Map Android keycodes to HID usage codes.
     * Only maps the keycodes commonly used as switch inputs.
     */
    private fun androidKeycodeToHidKeycode(androidKeycode: Int): Byte {
        return when (androidKeycode) {
            android.view.KeyEvent.KEYCODE_SPACE -> 0x2C.toByte()
            android.view.KeyEvent.KEYCODE_ENTER -> 0x28.toByte()
            android.view.KeyEvent.KEYCODE_A -> 0x04.toByte()
            android.view.KeyEvent.KEYCODE_B -> 0x05.toByte()
            android.view.KeyEvent.KEYCODE_C -> 0x06.toByte()
            android.view.KeyEvent.KEYCODE_D -> 0x07.toByte()
            android.view.KeyEvent.KEYCODE_E -> 0x08.toByte()
            android.view.KeyEvent.KEYCODE_F -> 0x09.toByte()
            android.view.KeyEvent.KEYCODE_G -> 0x0A.toByte()
            android.view.KeyEvent.KEYCODE_H -> 0x0B.toByte()
            android.view.KeyEvent.KEYCODE_I -> 0x0C.toByte()
            android.view.KeyEvent.KEYCODE_J -> 0x0D.toByte()
            android.view.KeyEvent.KEYCODE_K -> 0x0E.toByte()
            android.view.KeyEvent.KEYCODE_L -> 0x0F.toByte()
            android.view.KeyEvent.KEYCODE_1 -> 0x1E.toByte()
            android.view.KeyEvent.KEYCODE_2 -> 0x1F.toByte()
            android.view.KeyEvent.KEYCODE_3 -> 0x20.toByte()
            android.view.KeyEvent.KEYCODE_4 -> 0x21.toByte()
            android.view.KeyEvent.KEYCODE_5 -> 0x22.toByte()
            android.view.KeyEvent.KEYCODE_F1 -> 0x3A.toByte()
            android.view.KeyEvent.KEYCODE_F2 -> 0x3B.toByte()
            android.view.KeyEvent.KEYCODE_F3 -> 0x3C.toByte()
            android.view.KeyEvent.KEYCODE_F4 -> 0x3D.toByte()
            android.view.KeyEvent.KEYCODE_TAB -> 0x2B.toByte()
            android.view.KeyEvent.KEYCODE_ESCAPE -> 0x29.toByte()
            // Default: send Space
            else -> 0x2C.toByte()
        }
    }
}
