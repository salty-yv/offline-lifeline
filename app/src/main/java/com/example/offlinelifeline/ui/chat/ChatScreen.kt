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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.offlinelifeline.core.model.ChatConversation
import com.example.offlinelifeline.core.model.ToolType
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
            title = { Text("清空当前对话？") },
            text = { Text("这会删除当前对话里的本地消息，并重置模型上下文。这个操作不能撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearConversation()
                    }
                ) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    conversationToDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("删除这条对话？") },
            text = { Text("这会删除「${conversation.title}」里的本地消息记录。删除后不能撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        conversationToDelete = null
                        viewModel.deleteConversation(conversation.id)
                    }
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    val chatContent: @Composable (Modifier, Boolean) -> Unit = { contentModifier, showHistoryButton ->
        Column(modifier = contentModifier.fillMaxSize()) {
            RuntimeStatusBanner(
                runtimeState = uiState.runtimeState,
                modelAssetState = uiState.modelAssetState,
                isGenerating = uiState.isGenerating,
                errorMessage = uiState.errorMessage
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (showHistoryButton) {
                    TextButton(onClick = { scope.launch { drawerState.open() } }) {
                        Text("历史记录")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = viewModel::createConversation) {
                    Text("新建对话")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    enabled = uiState.messages.isNotEmpty() || uiState.pendingImages.isNotEmpty(),
                    onClick = { showClearConfirm = true }
                ) {
                    Text("清空当前")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { message ->
                    ChatMessageBubble(
                        message = message,
                        onToolSelected = onToolSelected,
                        modifier = Modifier.padding(vertical = 4.dp)
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
                    onNewConversation = viewModel::createConversation,
                    onSelectConversation = viewModel::selectConversation,
                    onDeleteConversation = { conversationToDelete = it },
                    modifier = Modifier.width(240.dp)
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
                            onNewConversation = {
                                viewModel.createConversation()
                                scope.launch { drawerState.close() }
                            },
                            onSelectConversation = {
                                viewModel.selectConversation(it)
                                scope.launch { drawerState.close() }
                            },
                            onDeleteConversation = { conversationToDelete = it },
                            modifier = Modifier.width(280.dp)
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
    onNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (ChatConversation) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "历史记录",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = onNewConversation,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text("新建对话")
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp),
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = conversation.title,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        TextButton(onClick = onDelete) {
            Text("删除")
        }
    }
}
