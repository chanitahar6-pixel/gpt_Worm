package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferences
import com.example.data.UserProfile
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhitePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    user: UserProfile?,
    apiHealth: Boolean?,
    onUpdatePreferences: ((AppPreferences) -> AppPreferences) -> Unit,
    onResetMemory: (() -> Unit) -> Unit,
    onClearAllChats: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showResetMemoryConfirm by remember { mutableStateOf(false) }
    var showClearChatsConfirm by remember { mutableStateOf(false) }

    if (showResetMemoryConfirm) {
        AlertDialog(
            onDismissRequest = { showResetMemoryConfirm = false },
            title = { Text("Reset AI Memory", color = TextWhitePrimary) },
            text = {
                Text(
                    text = "This will invoke POST /reset on the server and clear current context memory for user ID ${user?.id ?: "123456"}.",
                    color = TextGraySecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetMemoryConfirm = false
                        onResetMemory {
                            Toast.makeText(context, "AI Memory reset completed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetMemoryConfirm = false }) {
                    Text("Cancel", color = TextGraySecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showClearChatsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearChatsConfirm = false },
            title = { Text("Clear All History", color = TextWhitePrimary) },
            text = {
                Text(
                    text = "Are you sure you want to delete all local chats and messages? This cannot be undone.",
                    color = TextGraySecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearChatsConfirm = false
                        onClearAllChats()
                        Toast.makeText(context, "All chats deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentError)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatsConfirm = false }) {
                    Text("Cancel", color = TextGraySecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextWhitePrimary, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhitePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            SettingsSection(title = "Appearance", icon = Icons.Default.DarkMode) {
                SettingsSwitchTile(
                    title = "Dark Theme",
                    subtitle = "Sleek dark interface with neon accents",
                    checked = preferences.darkTheme,
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(darkTheme = checked) }
                    }
                )
            }

            // Chat Configuration Section
            SettingsSection(title = "Chat Preferences", icon = Icons.Default.Chat) {
                SettingsSwitchTile(
                    title = "Show Thinking Details",
                    subtitle = "Display animated phases while AI processes",
                    checked = preferences.showThinkingDetails,
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(showThinkingDetails = checked) }
                    }
                )

                HorizontalDivider(color = Color(0xFF261D42))

                SettingsSwitchTile(
                    title = "Enter to Send",
                    subtitle = "Pressing Enter sends the message immediately",
                    checked = preferences.enterToSend,
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(enterToSend = checked) }
                    }
                )

                HorizontalDivider(color = Color(0xFF261D42))

                SettingsSwitchTile(
                    title = "Show Timestamps",
                    subtitle = "Display time of each message",
                    checked = preferences.showTimestamps,
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(showTimestamps = checked) }
                    }
                )

                HorizontalDivider(color = Color(0xFF261D42))

                SettingsSwitchTile(
                    title = "Markdown & Code Rendering",
                    subtitle = "Format code blocks, headers, and tables",
                    checked = preferences.markdownRendering,
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(markdownRendering = checked) }
                    }
                )
            }

            // Data & Memory Controls Section
            SettingsSection(title = "Data & Memory Management", icon = Icons.Default.Storage) {
                SettingsActionTile(
                    title = "Reset AI Server Memory",
                    subtitle = "Clear session context on AI server (/reset)",
                    actionLabel = "Reset",
                    actionColor = NeonPurplePrimary,
                    onClick = { showResetMemoryConfirm = true }
                )

                HorizontalDivider(color = Color(0xFF261D42))

                SettingsActionTile(
                    title = "Clear Chat History",
                    subtitle = "Delete all stored conversations and messages",
                    actionLabel = "Clear",
                    actionColor = AccentError,
                    onClick = { showClearChatsConfirm = true }
                )
            }

            // About & System Info Section
            SettingsSection(title = "System & API Info", icon = Icons.Default.Info) {
                InfoRow(label = "Application Version", value = "1.0.0 (Production)")
                InfoRow(label = "AI Server URL", value = "http://51.75.118.171:20085")
                InfoRow(
                    label = "Service Health",
                    value = if (apiHealth == true) "Online & Connected" else if (apiHealth == false) "Temporarily Unavailable" else "Checking...",
                    valueColor = if (apiHealth == true) AccentSuccess else AccentError
                )
            }

            // Account & Logout Section
            if (user != null) {
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1828)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = AccentError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Sign Out", color = AccentError, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonPurpleGlow,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = TextWhitePrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF261D42), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextWhitePrimary,
                checkedTrackColor = NeonPurplePrimary,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color(0xFF261D42)
            )
        )
    }
}

@Composable
private fun SettingsActionTile(
    title: String,
    subtitle: String,
    actionLabel: String,
    actionColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = actionColor.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = actionLabel, color = actionColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = TextWhitePrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp)
        Text(text = value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
