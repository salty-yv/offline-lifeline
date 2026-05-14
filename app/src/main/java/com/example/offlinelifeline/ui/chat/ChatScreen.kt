package com.example.offlinelifeline.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.offlinelifeline.core.model.ToolType

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onToolSelected: (ToolType) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var showCamera by remember { mutableStateOf(false) }
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

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        RuntimeStatusBanner(
            runtimeState = uiState.runtimeState,
            modelAssetState = uiState.modelAssetState,
            isGenerating = uiState.isGenerating,
            errorMessage = uiState.errorMessage
        )

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
