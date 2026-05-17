package com.example.offlinelifeline.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.offlinelifeline.R
import com.example.offlinelifeline.core.model.ChatConversation
import com.example.offlinelifeline.core.model.ToolType
import com.example.offlinelifeline.ui.components.LifelineTopBar
import com.example.offlinelifeline.ui.i18n.AppStrings
import com.example.offlinelifeline.ui.i18n.LocalAppStrings
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onToolSelected: (ToolType) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showCamera by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<ChatConversation?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let(viewModel::addImageFromUri)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCamera = true
    }

    if (showCamera) {
        CameraCaptureDialog(
            createOutputFile = viewModel::createCameraRawFile,
            onImageCaptured = { file ->
                showCamera = false
                viewModel.addImageFromCameraFile(file)
            },
            onDismiss = { showCamera = false }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(strings.clearCurrentDialogTitle) },
            text = { Text(strings.clearCurrentDialogBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearConversation()
                    }
                ) {
                    Text(strings.confirmClear)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    conversationToDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text(strings.deleteConversationTitle) },
            text = { Text(strings.deleteConversationBody(conversation.localizedTitle(strings))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        conversationToDelete = null
                        viewModel.deleteConversation(conversation.id)
                    }
                ) {
                    Text(strings.confirmDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    val chatContent: @Composable (Modifier, Boolean) -> Unit = { contentModifier, showMenuIcon ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            LifelineTopBar(
                title = "OffLifeline",
                navigationIcon = if (showMenuIcon) Icons.Default.Menu else null,
                navigationContentDescription = strings.history,
                onNavigationClick = if (showMenuIcon) {
                    { scope.launch { drawerState.open() } }
                } else {
                    null
                },
                actionIcon = Icons.Default.Delete,
                actionContentDescription = strings.clearCurrent,
                actionEnabled = uiState.messages.isNotEmpty() || uiState.pendingImages.isNotEmpty(),
                onActionClick = { showClearConfirm = true }
            )
            if (uiState.errorMessage != null) {
                RuntimeStatusText(
                    runtimeState = uiState.runtimeState,
                    modelAssetState = uiState.modelAssetState,
                    errorMessage = uiState.errorMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { message ->
                    ChatMessageBubble(
                        message = message,
                        onToolSelected = onToolSelected,
                        chatTextSizeSp = uiState.chatTextSizeSp
                    )
                }
            }

            ChatInputBar(
                text = uiState.inputText,
                pendingImages = uiState.pendingImages,
                isProcessingImage = uiState.isProcessingImage,
                canSend = uiState.canSend,
                isGenerating = uiState.isGenerating,
                isFreeChatMode = uiState.isFreeChatMode,
                chatTextSizeSp = uiState.chatTextSizeSp,
                onTextChanged = viewModel::onInputChanged,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopGeneration,
                onPickImage = { galleryLauncher.launch("image/*") },
                onOpenCamera = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        showCamera = true
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onRemoveImage = viewModel::removePendingImage
            )
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val usePermanentHistory = maxWidth >= 720.dp
        if (usePermanentHistory) {
            Row(modifier = Modifier.fillMaxSize()) {
                ConversationHistoryPanel(
                    conversations = uiState.conversations,
                    selectedConversationId = uiState.selectedConversationId,
                    isFreeChatMode = uiState.isFreeChatMode,
                    onToggleFreeChatMode = viewModel::toggleFreeChatMode,
                    onNewConversation = viewModel::createConversation,
                    onSelectConversation = viewModel::selectConversation,
                    onDeleteConversation = { conversationToDelete = it },
                    modifier = Modifier.width(300.dp)
                )
                chatContent(Modifier.weight(1f), false)
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        ConversationHistoryPanel(
                            conversations = uiState.conversations,
                            selectedConversationId = uiState.selectedConversationId,
                            isFreeChatMode = uiState.isFreeChatMode,
                            onToggleFreeChatMode = viewModel::toggleFreeChatMode,
                            onNewConversation = {
                                viewModel.createConversation()
                                scope.launch { drawerState.close() }
                            },
                            onSelectConversation = {
                                viewModel.selectConversation(it)
                                scope.launch { drawerState.close() }
                            },
                            onDeleteConversation = { conversationToDelete = it },
                            modifier = Modifier.width(300.dp)
                        )
                    }
                }
            ) {
                chatContent(Modifier.fillMaxSize(), true)
            }
        }
    }
}

@Composable
private fun ConversationHistoryPanel(
    conversations: List<ChatConversation>,
    selectedConversationId: String?,
    isFreeChatMode: Boolean,
    onToggleFreeChatMode: () -> Unit,
    onNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (ChatConversation) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var showFreeChatInfo by remember { mutableStateOf(false) }
    if (showFreeChatInfo) {
        AlertDialog(
            onDismissRequest = { showFreeChatInfo = false },
            title = { Text(if (strings.languageTag.startsWith("en")) "Free chat" else "自由对话") },
            text = {
                Text(
                    if (strings.languageTag.startsWith("en")) {
                        "Free chat lets you ask more open-ended questions. Safety checks and the offline knowledge base still stay active, but the assistant will be less strict about forcing a survival-response structure."
                    } else {
                        "自由对话可以更开放地提问。安全校验和离线知识库仍然开启，但助手不会强制按照应急处置结构来回答。"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showFreeChatInfo = false }) {
                    Text(if (strings.languageTag.startsWith("en")) "Got it" else "知道了")
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = strings.history,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = if (isFreeChatMode) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    contentColor = if (isFreeChatMode) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            color = androidx.compose.ui.graphics.Color.Transparent,
                            onClick = onToggleFreeChatMode
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFreeChatMode) strings.freeChatModeOn else strings.freeChatModeOff,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showFreeChatInfo = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_info_24),
                                contentDescription = if (strings.languageTag.startsWith("en")) {
                                    "About free chat"
                                } else {
                                    "自由对话说明"
                                },
                                tint = if (isFreeChatMode) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                Button(
                    onClick = onNewConversation,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = strings.newConversation,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(
                    items = conversations,
                    key = { it.id }
                ) { conversation ->
                    ConversationHistoryRow(
                        conversation = conversation,
                        selected = conversation.id == selectedConversationId,
                        onSelect = { onSelectConversation(conversation.id) },
                        onDelete = { onDeleteConversation(conversation) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationHistoryRow(
    conversation: ChatConversation,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = conversation.localizedTitle(strings),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = strings.delete,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun ChatConversation.localizedTitle(strings: AppStrings): String {
    return if (hasDefaultTitle) strings.defaultConversationTitle else title
}
