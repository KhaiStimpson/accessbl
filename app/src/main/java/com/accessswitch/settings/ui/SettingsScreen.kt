package com.accessswitch.settings.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accessswitch.R

/**
 * Top-level settings screen showing categorized navigation cards.
 * Each card navigates to a detail settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToScanMode: () -> Unit,
    onNavigateToScanSpeed: () -> Unit,
    onNavigateToSwitchConfig: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onNavigateToPhoneSwitch: () -> Unit,
    onNavigateToBtHid: () -> Unit,
    onNavigateToPinLock: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Scanning")

            SettingsCard(
                icon = Icons.Filled.ViewComfy,
                title = stringResource(R.string.settings_scan_mode),
                subtitle = "Auto, Step, Inverse, Row-Column",
                onClick = onNavigateToScanMode
            )
            SettingsCard(
                icon = Icons.Filled.Speed,
                title = stringResource(R.string.settings_scan_speed),
                subtitle = "Scan interval and loop count",
                onClick = onNavigateToScanSpeed
            )

            SectionHeader("Input")

            SettingsCard(
                icon = Icons.Filled.Keyboard,
                title = stringResource(R.string.settings_switch_config),
                subtitle = "Switch keycodes and debounce",
                onClick = onNavigateToSwitchConfig
            )
            SettingsCard(
                icon = Icons.Filled.TouchApp,
                title = stringResource(R.string.settings_phone_switch),
                subtitle = "Phone touch zone configuration",
                onClick = onNavigateToPhoneSwitch
            )
            SettingsCard(
                icon = Icons.Filled.Bluetooth,
                title = stringResource(R.string.settings_bt_hid),
                subtitle = "Bluetooth HID remote switch",
                onClick = onNavigateToBtHid
            )

            SectionHeader("Display")

            SettingsCard(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.settings_appearance),
                subtitle = "Highlight color and style",
                onClick = onNavigateToAppearance
            )
            SettingsCard(
                icon = Icons.Filled.Hearing,
                title = "Feedback",
                subtitle = "Audio and haptic feedback",
                onClick = onNavigateToFeedback
            )

            SectionHeader("Security")

            SettingsCard(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.settings_pin_lock),
                subtitle = "Protect settings with a PIN",
                onClick = onNavigateToPinLock
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFF8AB4F8),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D2D2D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF8AB4F8),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }
    }
}
