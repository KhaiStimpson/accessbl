package com.accessswitch.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accessswitch.R
import com.accessswitch.scanning.ScanMode
import com.accessswitch.settings.AppSettings
import com.accessswitch.settings.HighlightStyle
import com.accessswitch.settings.SettingsViewModel

// --- Shared detail screen scaffold ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// =============================================
// Scan Mode Screen
// =============================================

@Composable
fun ScanModeScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    DetailScreen(
        title = stringResource(R.string.settings_scan_mode),
        onBack = onBack
    ) {
        SettingsLabel("Select how the scanner moves through items:")

        ScanMode.entries.forEach { mode ->
            RadioRow(
                label = mode.name.replace('_', ' ').lowercase()
                    .replaceFirstChar { it.uppercase() },
                description = scanModeDescription(mode),
                selected = settings.scanMode == mode,
                onClick = { viewModel.setScanMode(mode) }
            )
        }
    }
}

private fun scanModeDescription(mode: ScanMode): String = when (mode) {
    ScanMode.AUTO -> "Items are highlighted automatically at a set interval. Press switch to select."
    ScanMode.STEP -> "Switch 1 advances highlight, Switch 2 selects the highlighted item."
    ScanMode.INVERSE -> "Scanning runs automatically. Releasing the switch selects the item."
    ScanMode.ROW_COLUMN -> "Scans rows first, then columns within the selected row."
}

// =============================================
// Scan Speed Screen
// =============================================

@Composable
fun ScanSpeedScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    DetailScreen(
        title = stringResource(R.string.settings_scan_speed),
        onBack = onBack
    ) {
        SettingsLabel("Scan Interval")
        Text(
            text = "${settings.scanIntervalMs} ms",
            color = Color(0xFF8AB4F8),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Slider(
            value = settings.scanIntervalMs.toFloat(),
            onValueChange = { viewModel.setScanInterval(it.toLong()) },
            valueRange = SettingsViewModel.SCAN_INTERVAL_MIN.toFloat()..SettingsViewModel.SCAN_INTERVAL_MAX.toFloat(),
            steps = 14, // 500ms steps
            colors = sliderColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Fast (500ms)", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text("Slow (8000ms)", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsLabel("Scan Loops")
        Text(
            text = if (settings.scanLoops == 0) "Infinite" else "${settings.scanLoops}",
            color = Color(0xFF8AB4F8),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Slider(
            value = settings.scanLoops.toFloat(),
            onValueChange = { viewModel.setScanLoops(it.toInt()) },
            valueRange = SettingsViewModel.SCAN_LOOPS_MIN.toFloat()..SettingsViewModel.SCAN_LOOPS_MAX.toFloat(),
            steps = 4,
            colors = sliderColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Infinite", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text("5 loops", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

// =============================================
// Switch Config Screen
// =============================================

@Composable
fun SwitchConfigScreen(
    viewModel: SettingsViewModel,
    onCaptureSwitch1: () -> Unit,
    onCaptureSwitch2: () -> Unit,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    DetailScreen(
        title = stringResource(R.string.settings_switch_config),
        onBack = onBack
    ) {
        SettingsLabel("Switch 1 Key")
        KeycodeButton(
            keycode = settings.switch1Keycode,
            onClick = onCaptureSwitch1
        )

        SettingsLabel("Switch 2 Key")
        KeycodeButton(
            keycode = settings.switch2Keycode,
            onClick = onCaptureSwitch2
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsLabel("Debounce Duration")
        Text(
            text = "${settings.debounceMs} ms",
            color = Color(0xFF8AB4F8),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Slider(
            value = settings.debounceMs.toFloat(),
            onValueChange = { viewModel.setDebounce(it.toLong()) },
            valueRange = SettingsViewModel.DEBOUNCE_MIN.toFloat()..SettingsViewModel.DEBOUNCE_MAX.toFloat(),
            steps = 8,
            colors = sliderColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("100ms", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text("1000ms", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun KeycodeButton(
    keycode: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .clickable(onClick = onClick)
            .border(1.dp, Color(0xFF3D3D3D), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SettingsViewModel.keycodeName(keycode),
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = "Tap to change",
                color = Color(0xFF8AB4F8),
                fontSize = 13.sp
            )
        }
    }
}

// =============================================
// Appearance Screen
// =============================================

@Composable
fun AppearanceScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    DetailScreen(
        title = stringResource(R.string.settings_appearance),
        onBack = onBack
    ) {
        SettingsLabel("Highlight Style")

        HighlightStyle.entries.forEach { style ->
            RadioRow(
                label = style.name.lowercase().replaceFirstChar { it.uppercase() },
                description = highlightStyleDescription(style),
                selected = settings.highlightStyle == style,
                onClick = { viewModel.setHighlightStyle(style) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsLabel("Highlight Color")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            highlightColorOptions.forEach { (name, argb) ->
                ColorChip(
                    color = Color(argb),
                    label = name,
                    selected = settings.highlightColor == argb,
                    onClick = { viewModel.setHighlightColor(argb) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preview tile
        SettingsLabel("Preview")
        HighlightPreview(
            highlightColor = Color(settings.highlightColor),
            highlightStyle = settings.highlightStyle
        )
    }
}

private fun highlightStyleDescription(style: HighlightStyle): String = when (style) {
    HighlightStyle.BORDER -> "Colored border ring around the highlighted item."
    HighlightStyle.FILL -> "Colored fill background on the highlighted item."
    HighlightStyle.BOTH -> "Both border ring and background fill."
}

private val highlightColorOptions = listOf(
    "Yellow" to 0xFFFFEB3B.toInt(),
    "Blue" to 0xFF2196F3.toInt(),
    "Green" to 0xFF4CAF50.toInt(),
    "Red" to 0xFFF44336.toInt(),
    "White" to 0xFFFFFFFF.toInt()
)

@Composable
private fun ColorChip(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (selected) Modifier.border(3.dp, Color.White, CircleShape)
                    else Modifier
                )
        )
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun HighlightPreview(
    highlightColor: Color,
    highlightStyle: HighlightStyle
) {
    val borderMod = when (highlightStyle) {
        HighlightStyle.FILL -> Modifier
        else -> Modifier.border(4.dp, highlightColor, RoundedCornerShape(12.dp))
    }
    val bgColor = when (highlightStyle) {
        HighlightStyle.BORDER -> Color(0xFF2D2D2D)
        else -> highlightColor.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(borderMod)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Sample",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

// =============================================
// Feedback Screen
// =============================================

@Composable
fun FeedbackScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    DetailScreen(
        title = "Feedback",
        onBack = onBack
    ) {
        ToggleRow(
            label = "Haptic Feedback",
            description = "Vibrate on item selection",
            checked = settings.hapticFeedback,
            onToggle = { viewModel.setHapticFeedback(it) }
        )

        ToggleRow(
            label = "Audio Feedback",
            description = "Play tone on highlight advance",
            checked = settings.audioFeedback,
            onToggle = { viewModel.setAudioFeedback(it) }
        )
    }
}

// =============================================
// Phone-as-Switch Screen
// =============================================

@Composable
fun PhoneSwitchScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onLaunchSwitchScreen: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()

    DetailScreen(
        title = stringResource(R.string.settings_phone_switch),
        onBack = onBack
    ) {
        ToggleRow(
            label = "Enable Phone Touch Zones",
            description = "Use the phone screen as a two-switch input",
            checked = settings.phoneLocalSwitchEnabled,
            onToggle = { viewModel.setPhoneLocalSwitchEnabled(it) }
        )

        if (settings.phoneLocalSwitchEnabled) {
            SettingsLabel("Zone Layout")

            com.accessswitch.switchscreen.SwitchZoneLayout.entries.forEach { layout ->
                RadioRow(
                    label = layout.name.replace('_', ' ').lowercase()
                        .replaceFirstChar { it.uppercase() },
                    description = zoneLayoutDescription(layout),
                    selected = settings.phoneLocalZoneLayout == layout,
                    onClick = { viewModel.setPhoneLocalZoneLayout(layout) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsLabel("Zone 1 maps to")
            SwitchIdSelector(
                selected = settings.phoneLocalZone1SwitchId,
                onSelect = { viewModel.setPhoneLocalZone1Switch(it) }
            )

            SettingsLabel("Zone 2 maps to")
            SwitchIdSelector(
                selected = settings.phoneLocalZone2SwitchId,
                onSelect = { viewModel.setPhoneLocalZone2Switch(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLaunchSwitchScreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8AB4F8),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Launch Switch Screen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun zoneLayoutDescription(
    layout: com.accessswitch.switchscreen.SwitchZoneLayout
): String = when (layout) {
    com.accessswitch.switchscreen.SwitchZoneLayout.FULL_SCREEN -> "Entire screen is one zone (single switch)"
    com.accessswitch.switchscreen.SwitchZoneLayout.LEFT_RIGHT -> "Left half = Zone 1, Right half = Zone 2"
    com.accessswitch.switchscreen.SwitchZoneLayout.TOP_BOTTOM -> "Top half = Zone 1, Bottom half = Zone 2"
    com.accessswitch.switchscreen.SwitchZoneLayout.ASYMMETRIC_80_20 -> "80% left = Zone 1, 20% right = Zone 2"
}

@Composable
private fun SwitchIdSelector(
    selected: com.accessswitch.input.SwitchId,
    onSelect: (com.accessswitch.input.SwitchId) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        com.accessswitch.input.SwitchId.entries.forEach { switchId ->
            RadioRow(
                label = switchId.name.replace('_', ' '),
                description = "",
                selected = selected == switchId,
                onClick = { onSelect(switchId) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =============================================
// BT HID Screen
// =============================================

@Composable
fun BtHidScreen(
    viewModel: SettingsViewModel,
    onCaptureSwitch1: () -> Unit,
    onCaptureSwitch2: () -> Unit,
    onBack: () -> Unit,
    onStartPairing: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    btHidState: com.accessswitch.input.BtHidState = com.accessswitch.input.BtHidState.DISCONNECTED,
    connectedDeviceName: String? = null
) {
    val settings by viewModel.settings.collectAsState()

    DetailScreen(
        title = stringResource(R.string.settings_bt_hid),
        onBack = onBack
    ) {
        ToggleRow(
            label = "Enable BT HID Mode",
            description = "Use phone as a Bluetooth keyboard switch for another device",
            checked = settings.phoneBtHidEnabled,
            onToggle = { viewModel.setPhoneBtHidEnabled(it) }
        )

        if (settings.phoneBtHidEnabled) {
            SettingsLabel("BT HID Switch 1 Keycode")
            KeycodeButton(
                keycode = settings.phoneBtHidSwitch1Keycode,
                onClick = onCaptureSwitch1
            )

            SettingsLabel("BT HID Switch 2 Keycode")
            KeycodeButton(
                keycode = settings.phoneBtHidSwitch2Keycode,
                onClick = onCaptureSwitch2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Connection status
            SettingsLabel("Connection Status")
            val statusColor = when (btHidState) {
                com.accessswitch.input.BtHidState.CONNECTED -> Color(0xFF4CAF50)
                com.accessswitch.input.BtHidState.CONNECTING -> Color(0xFFFFC107)
                com.accessswitch.input.BtHidState.DISCONNECTED -> Color.White.copy(alpha = 0.5f)
            }
            val statusText = when (btHidState) {
                com.accessswitch.input.BtHidState.CONNECTED ->
                    "Connected to ${connectedDeviceName ?: settings.phoneBtHidPairedDeviceAddress ?: "device"}"
                com.accessswitch.input.BtHidState.CONNECTING -> "Connecting..."
                com.accessswitch.input.BtHidState.DISCONNECTED -> {
                    if (settings.phoneBtHidPairedDeviceAddress != null) {
                        "Disconnected (previously paired: ${settings.phoneBtHidPairedDeviceAddress})"
                    } else {
                        "No device paired"
                    }
                }
            }
            Text(text = statusText, color = statusColor, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Pair / Disconnect button
            when (btHidState) {
                com.accessswitch.input.BtHidState.CONNECTED -> {
                    Button(
                        onClick = onDisconnect,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Disconnect")
                    }
                }
                com.accessswitch.input.BtHidState.CONNECTING -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Connecting...")
                    }
                }
                com.accessswitch.input.BtHidState.DISCONNECTED -> {
                    Button(
                        onClick = onStartPairing,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8AB4F8),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (settings.phoneBtHidPairedDeviceAddress != null) "Reconnect" else "Pair as Switch",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// =============================================
// Shared components
// =============================================

@Composable
fun SettingsLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.8f),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun RadioRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF1E1E1E) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF8AB4F8),
                unselectedColor = Color.White.copy(alpha = 0.5f)
            )
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onToggle(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Color.White, fontSize = 15.sp)
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color(0xFF8AB4F8),
                uncheckedTrackColor = Color(0xFF3D3D3D)
            )
        )
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Color(0xFF8AB4F8),
    activeTrackColor = Color(0xFF8AB4F8),
    inactiveTrackColor = Color(0xFF3D3D3D)
)
