package com.example.offlinelifeline.ui.chat

import com.example.offlinelifeline.core.model.ChatMessage
import com.example.offlinelifeline.core.model.Attachment
import com.example.offlinelifeline.core.model.ModelRuntimeState

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val pendingImages: List<Attachment.Image> = emptyList(),
    val isProcessingImage: Boolean = false,
    val isGenerating: Boolean = false,
    val runtimeState: ModelRuntimeState = ModelRuntimeState.NotChecked,
    val modelAssetState: ModelRuntimeState = ModelRuntimeState.NotChecked,
    val errorMessage: String? = null
) {
    val canSend: Boolean
        get() = (inputText.isNotBlank() || pendingImages.isNotEmpty()) && !isGenerating && !isProcessingImage
}
