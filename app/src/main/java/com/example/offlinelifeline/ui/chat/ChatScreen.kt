package com.example.offlinelifeline.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.offlinelifeline.core.model.ChatConversation
import com.example.offlinelifeline.core.model.ToolType
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
            text = { Text(strings.deleteConversationBody(conversation.title)) },
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

    // ── 聊天内容主体 ──────────────────────────────────────────────────────────
    val chatContent: @Composable (Modifier, Boolean) -> Unit = { contentModifier, showMenuIcon ->
        Column(modifier = contentModifier.fillMaxSize()) {
            RuntimeStatusBanner(
                runtimeState = uiState.runtimeState,
                modelAssetState = uiState.modelAssetState,
                isGenerating = uiState.isGenerating,
                errorMessage = uiState.errorMessage
            )

            // 顶部操作栏：图标化
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：菜单图标（仅手机模式显示，宽屏侧边栏常驻不需要）
                if (showMenuIcon) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = strings.history
                        )
                    }
                }

                // 标题
                Text(
                    text = strings.routeChat,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (showMenuIcon) 0.dp else 12.dp)
                )

                // 右侧：清空当前对话图标
                IconButton(
                    enabled = uiState.messages.isNotEmpty() || uiState.pendingImages.isNotEmpty(),
                    onClick = { showClearConfirm = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = strings.clearCurrent,
                        tint = if (uiState.messages.isNotEmpty() || uiState.pendingImages.isNotEmpty())
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { message ->
                    ChatMessageBubble(
                        message = message,
                        onToolSelected = onToolSelected,
                        chatTextSizeSp = uiState.chatTextSizeSp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider()

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

    // ── 布局：宽屏用永久侧边栏，手机用抽屉 ──────────────────────────────────
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
                    modifier = Modifier.width(260.dp)
                )
                chatContent(Modifier.weight(1f), false)
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
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

// ── 侧边栏面板 ────────────────────────────────────────────────────────────────
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
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题
            Text(
                text = strings.history,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.titleMedium
            )

            // 自由对话模式切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isFreeChatMode,
                    onClick = onToggleFreeChatMode,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (isFreeChatMode) strings.freeChatModeOn
                            else strings.freeChatModeOff
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 新建对话按钮（图标+文字）
            Button(
                onClick = onNewConversation,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(strings.newConversation)
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            Spacer(modifier = Modifier.height(4.dp))

            // 对话历史列表
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp),
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

// ── 历史记录行 ────────────────────────────────────────────────────────────────
@Composable
private fun ConversationHistoryRow(
    conversation: ChatConversation,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onSelect)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = conversation.title,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        // 删除按钮改为图标
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = strings.delete,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
