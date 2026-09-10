package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.data.model.ConversationEntity
import com.example.ui.theme.AccentError
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.NeonPurpleVariant
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhitePrimary
import java.util.Calendar

@Composable
fun ChatSidebar(
    conversations: List<ConversationEntity>,
    activeConversationId: String?,
    searchQuery: String,
    user: UserProfile?,
    onSearchChange: (String) -> Unit,
    onSelectConversation: (ConversationEntity) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onOpenFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingConversation by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    if (editingConversation != null) {
        AlertDialog(
            onDismissRequest = { editingConversation = null },
            title = { Text("Rename Chat", color = TextWhitePrimary) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Chat Title") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingConversation?.let { onRenameConversation(it.id, renameText) }
                        editingConversation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingConversation = null }) {
                    Text("Cancel", color = TextGraySecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Categorization
    val pinnedChats = conversations.filter { it.isPinned }
    val unpinnedChats = conversations.filter { !it.isPinned }

    val now = Calendar.getInstance()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterdayStart = todayStart - (24 * 60 * 60 * 1000)

    val todayChats = unpinnedChats.filter { it.updatedAt >= todayStart }
    val yesterdayChats = unpinnedChats.filter { it.updatedAt in yesterdayStart until todayStart }
    val previousChats = unpinnedChats.filter { it.updatedAt < yesterdayStart }

    Surface(
        color = DarkSurface,
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .border(1.dp, Color(0xFF221A3B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NeonPurplePrimary, NeonPurpleVariant))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Logo",
                        tint = TextWhitePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Chat",
                        color = TextWhitePrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Next-Gen Assistant",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // New Chat Button
            Button(
                onClick = onNewChatClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF261D45)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    tint = NeonPurpleGlow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Chat",
                    color = TextWhitePrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text("Search chats...", color = TextMuted, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedTextColor = TextWhitePrimary,
                    unfocusedTextColor = TextWhitePrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Conversation List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (pinnedChats.isNotEmpty()) {
                    item { SectionHeader("Pinned") }
                    items(pinnedChats, key = { it.id }) { convo ->
                        ConversationItemRow(
                            conversation = convo,
                            isSelected = convo.id == activeConversationId,
                            onClick = { onSelectConversation(convo) },
                            onRename = {
                                editingConversation = convo
                                renameText = convo.title
                            },
                            onDelete = { onDeleteConversation(convo.id) },
                            onTogglePin = { onTogglePin(convo.id, convo.isPinned) },
                            onToggleFavorite = { onToggleFavorite(convo.id, convo.isFavorite) }
                        )
                    }
                }

                if (todayChats.isNotEmpty()) {
                    item { SectionHeader("Today") }
                    items(todayChats, key = { it.id }) { convo ->
                        ConversationItemRow(
                            conversation = convo,
                            isSelected = convo.id == activeConversationId,
                            onClick = { onSelectConversation(convo) },
                            onRename = {
                                editingConversation = convo
                                renameText = convo.title
                            },
                            onDelete = { onDeleteConversation(convo.id) },
                            onTogglePin = { onTogglePin(convo.id, convo.isPinned) },
                            onToggleFavorite = { onToggleFavorite(convo.id, convo.isFavorite) }
                        )
                    }
                }

                if (yesterdayChats.isNotEmpty()) {
                    item { SectionHeader("Yesterday") }
                    items(yesterdayChats, key = { it.id }) { convo ->
                        ConversationItemRow(
                            conversation = convo,
                            isSelected = convo.id == activeConversationId,
                            onClick = { onSelectConversation(convo) },
                            onRename = {
                                editingConversation = convo
                                renameText = convo.title
                            },
                            onDelete = { onDeleteConversation(convo.id) },
                            onTogglePin = { onTogglePin(convo.id, convo.isPinned) },
                            onToggleFavorite = { onToggleFavorite(convo.id, convo.isFavorite) }
                        )
                    }
                }

                if (previousChats.isNotEmpty()) {
                    item { SectionHeader("Previous 30 Days") }
                    items(previousChats, key = { it.id }) { convo ->
                        ConversationItemRow(
                            conversation = convo,
                            isSelected = convo.id == activeConversationId,
                            onClick = { onSelectConversation(convo) },
                            onRename = {
                                editingConversation = convo
                                renameText = convo.title
                            },
                            onDelete = { onDeleteConversation(convo.id) },
                            onTogglePin = { onTogglePin(convo.id, convo.isPinned) },
                            onToggleFavorite = { onToggleFavorite(convo.id, convo.isFavorite) }
                        )
                    }
                }

                if (conversations.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No conversations yet",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF221A3B), modifier = Modifier.padding(vertical = 8.dp))

            // Navigation items: Files, Settings, Profile
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SidebarNavItem(
                    icon = Icons.Default.Description,
                    label = "Files & Attachments",
                    onClick = onOpenFiles
                )
                SidebarNavItem(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    onClick = onOpenSettings
                )
                SidebarNavItem(
                    icon = Icons.Default.Person,
                    label = user?.name ?: "Account Profile",
                    onClick = onOpenProfile
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun ConversationItemRow(
    conversation: ConversationEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF231B42) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (conversation.isPinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = NeonPurpleGlow,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (conversation.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = conversation.title,
                color = if (isSelected) TextWhitePrimary else TextGraySecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DarkSurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = TextWhitePrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = TextGraySecondary)
                        },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (conversation.isPinned) "Unpin" else "Pin", color = TextWhitePrimary)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = TextGraySecondary)
                        },
                        onClick = {
                            menuExpanded = false
                            onTogglePin()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (conversation.isFavorite) "Remove Star" else "Star", color = TextWhitePrimary)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Star, contentDescription = null, tint = TextGraySecondary)
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = AccentError) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = AccentError)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NeonPurpleGlow,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = TextWhitePrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
