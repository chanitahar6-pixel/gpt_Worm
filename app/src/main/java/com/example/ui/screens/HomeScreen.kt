package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatViewModel
import com.example.ui.components.AttachedFilePreview
import com.example.ui.components.ChatComposer
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.ChatSidebar
import com.example.ui.components.FileAttachmentHelper
import com.example.ui.components.ThinkingIndicator
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AccentWarning
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.NeonPurpleVariant
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhitePrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onNavigateToFiles: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val activeConvo by viewModel.activeConversation.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val conversations by viewModel.filteredConversations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val user by viewModel.userState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val attachedFile by viewModel.currentAttachedFile.collectAsState()
    val apiHealth by viewModel.apiHealth.collectAsState()

    var composerText by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTitleInput by remember { mutableStateOf("") }
    var topMenuExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // File picker contract
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val preview = FileAttachmentHelper.readFileFromUri(context, uri)
            if (preview != null) {
                viewModel.setAttachedFile(preview)
                Toast.makeText(context, "File attached: ${preview.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Unable to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-scroll on new message
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showRenameDialog && activeConvo != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Chat", color = TextWhitePrimary) },
            text = {
                OutlinedTextField(
                    value = renameTitleInput,
                    onValueChange = { renameTitleInput = it },
                    singleLine = true,
                    label = { Text("Chat Title") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeConvo?.let { viewModel.updateTitle(it.id, renameTitleInput) }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = TextGraySecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                modifier = Modifier.width(300.dp)
            ) {
                ChatSidebar(
                    conversations = conversations,
                    activeConversationId = activeConvo?.id,
                    searchQuery = searchQuery,
                    user = user,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onSelectConversation = { convo ->
                        viewModel.selectConversation(convo)
                        scope.launch { drawerState.close() }
                    },
                    onNewChatClick = {
                        viewModel.createNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteConversation = { viewModel.deleteConversation(it) },
                    onRenameConversation = { id, title -> viewModel.updateTitle(id, title) },
                    onTogglePin = { id, pinned -> viewModel.togglePin(id, pinned) },
                    onToggleFavorite = { id, fav -> viewModel.toggleFavorite(id, fav) },
                    onOpenFiles = {
                        scope.launch { drawerState.close() }
                        onNavigateToFiles()
                    },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    onOpenProfile = {
                        scope.launch { drawerState.close() }
                        onNavigateToProfile()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.clickable {
                                activeConvo?.let {
                                    renameTitleInput = it.title
                                    showRenameDialog = true
                                }
                            }
                        ) {
                            Text(
                                text = activeConvo?.title ?: "AI Chat",
                                color = TextWhitePrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (apiHealth) {
                                                true -> AccentSuccess
                                                false -> AccentError
                                                else -> AccentWarning
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (apiHealth == true) "AI Online" else "Offline Server",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Drawer",
                                tint = TextWhitePrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.createNewConversation() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = NeonPurpleGlow
                            )
                        }

                        Box {
                            IconButton(onClick = { topMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = TextWhitePrimary
                                )
                            }

                            DropdownMenu(
                                expanded = topMenuExpanded,
                                onDismissRequest = { topMenuExpanded = false },
                                modifier = Modifier.background(DarkSurfaceElevated)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Reset AI Memory", color = TextWhitePrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonPurpleGlow)
                                    },
                                    onClick = {
                                        topMenuExpanded = false
                                        viewModel.resetMemory { success ->
                                            val msg = if (success) "Memory reset on AI server" else "Failed to reset memory"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename Chat", color = TextWhitePrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = TextGraySecondary)
                                    },
                                    onClick = {
                                        topMenuExpanded = false
                                        activeConvo?.let {
                                            renameTitleInput = it.title
                                            showRenameDialog = true
                                        }
                                    }
                                )
                                activeConvo?.let { convo ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (convo.isPinned) "Unpin" else "Pin", color = TextWhitePrimary)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.PushPin, contentDescription = null, tint = TextGraySecondary)
                                        },
                                        onClick = {
                                            topMenuExpanded = false
                                            viewModel.togglePin(convo.id, convo.isPinned)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Chat", color = AccentError) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = AccentError)
                                        },
                                        onClick = {
                                            topMenuExpanded = false
                                            viewModel.deleteConversation(convo.id)
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )
            },
            bottomBar = {
                ChatComposer(
                    text = composerText,
                    onTextChange = { composerText = it },
                    onSend = {
                        val txt = composerText
                        val att = attachedFile
                        composerText = ""
                        viewModel.sendMessage(txt, att)
                    },
                    onStop = { viewModel.stopGenerating() },
                    isGenerating = isGenerating,
                    attachedFile = attachedFile,
                    onAttachFileClick = { filePickerLauncher.launch("*/*") },
                    onRemoveAttachedFile = { viewModel.setAttachedFile(null) },
                    enterToSend = preferences.enterToSend
                )
            },
            containerColor = DarkBackground,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Offline banner if server is not responding
                AnimatedVisibility(
                    visible = apiHealth == false,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = Color(0xFF32161A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AI service is temporarily unavailable",
                                color = AccentError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { viewModel.checkHealth() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry connection",
                                    tint = AccentError,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (messages.isEmpty() && !isThinking) {
                    // Empty State Screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(NeonPurplePrimary, NeonPurpleVariant))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = TextWhitePrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Welcome to AI Chat",
                                color = TextWhitePrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Ask anything, write code, analyze files, or brainstorm ideas.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                            )

                            // Suggestion Cards Grid
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SuggestionPromptCard(
                                    title = "Write Kotlin Code",
                                    subtitle = "Explain Coroutines and Flows with examples",
                                    icon = Icons.Default.Code,
                                    onClick = {
                                        viewModel.sendMessage("Explain Kotlin coroutines and flows with a clean example")
                                    }
                                )

                                SuggestionPromptCard(
                                    title = "System Architecture",
                                    subtitle = "Design a clean MVVM pattern in Android",
                                    icon = Icons.Default.Psychology,
                                    onClick = {
                                        viewModel.sendMessage("Explain MVVM architecture in Android with Jetpack Compose")
                                    }
                                )

                                SuggestionPromptCard(
                                    title = "Upload & Analyze File",
                                    subtitle = "Attach any document or code to summarize",
                                    icon = Icons.Default.UploadFile,
                                    onClick = { filePickerLauncher.launch("*/*") }
                                )
                            }
                        }
                    }
                } else {
                    // Chat Messages List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            ChatMessageItem(
                                message = message,
                                showTimestamps = preferences.showTimestamps,
                                markdownEnabled = preferences.markdownRendering,
                                onRegenerate = { viewModel.regenerateLastResponse() },
                                onDelete = { viewModel.deleteMessage(message.id) },
                                onRetry = { viewModel.retryMessage(message.id) }
                            )
                        }

                        if (isThinking) {
                            item {
                                ThinkingIndicator(
                                    showDetails = preferences.showThinkingDetails,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionPromptCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF261D42), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonPurpleGlow,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextWhitePrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
