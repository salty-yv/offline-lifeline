package com.example.offlinelifeline.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offlinelifeline.agent.SurvivalAgent
import com.example.offlinelifeline.core.common.TimeProvider
import com.example.offlinelifeline.core.model.Attachment
import com.example.offlinelifeline.core.model.ChatMessage
import com.example.offlinelifeline.core.model.ChatRole
import com.example.offlinelifeline.core.model.ModelRuntimeState
import com.example.offlinelifeline.data.datastore.SettingsStore
import com.example.offlinelifeline.data.repository.ChatRepository
import com.example.offlinelifeline.inference.LocalLlmEngine
import com.example.offlinelifeline.inference.ModelCatalog
import com.example.offlinelifeline.inference.ModelAssetManager
import com.example.offlinelifeline.inference.ModelManifest
import com.example.offlinelifeline.device.image.ImagePreprocessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.io.File

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val llmEngine: LocalLlmEngine,
    private val survivalAgent: SurvivalAgent,
    private val modelAssetManager: ModelAssetManager,
    private val imagePreprocessor: ImagePreprocessor,
    private val settingsStore: SettingsStore,
    private val timeProvider: TimeProvider = TimeProvider.System
) : ViewModel() {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(ChatUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<ChatUiState> = _uiState

    private var generationJob: Job? = null
    private var modelRefreshJob: Job? = null
    private var activeModelId: String? = null
    private var languageTag: String = "zh-CN"

    init {
        viewModelScope.launch {
            val activeConversation = chatRepository.ensureActiveConversation()
            _uiState.update {
                it.copy(
                    selectedConversationId = activeConversation.id,
                    messages = chatRepository.getMessages(activeConversation.id)
                )
            }
        }

        viewModelScope.launch {
            chatRepository.observeConversations().collect { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
            }
        }

        viewModelScope.launch {
            llmEngine.runtimeState.collect { runtimeState ->
                _uiState.update { it.copy(runtimeState = runtimeState) }
            }
        }

        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                languageTag = settings.languageTag
                if (activeModelId != settings.activeModelId) {
                    activeModelId = settings.activeModelId
                    modelRefreshJob?.cancel()
                    modelRefreshJob = launch {
                        refreshSelectedModel(settings.activeModelId)
                    }
                }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun toggleFreeChatMode() {
        _uiState.update { it.copy(isFreeChatMode = !it.isFreeChatMode) }
    }

    fun createCameraRawFile(): File = imagePreprocessor.createCameraRawFile()

    fun addImageFromUri(uri: Uri) {
        processImage { imagePreprocessor.processUri(uri) }
    }

    fun addImageFromCameraFile(file: File) {
        processImage { imagePreprocessor.processFile(file) }
    }

    fun removePendingImage(image: Attachment.Image) {
        imagePreprocessor.deleteProcessedImage(image)
        _uiState.update { state ->
            state.copy(pendingImages = state.pendingImages.filterNot { it.localPath == image.localPath })
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val conversationId = state.selectedConversationId ?: return
        val text = state.inputText.trim().ifBlank {
            if (state.pendingImages.isNotEmpty()) {
                if (languageTag.startsWith("en")) {
                    "I added an on-site image. Please give conservative text-based advice first."
                } else {
                    "我添加了一张现场图片，请先按保守原则给出文字辅助建议。"
                }
            } else {
                ""
            }
        }
        val pendingImages = state.pendingImages
        if (text.isBlank() || state.isGenerating || state.isProcessingImage) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = text,
            createdAtMillis = timeProvider.nowMillis(),
            attachments = pendingImages
        )
        val assistantMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.ASSISTANT,
            text = "",
            createdAtMillis = timeProvider.nowMillis(),
            isFinal = false
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + assistantMessage,
                inputText = "",
                pendingImages = emptyList(),
                isGenerating = true,
                errorMessage = null
            )
        }

        generationJob = viewModelScope.launch {
            chatRepository.saveMessage(conversationId, userMessage)

            val preparedResponse = survivalAgent.prepareResponse(
                userInput = text,
                history = _uiState.value.messages.filter { it.id != assistantMessage.id },
                imagePaths = pendingImages.map { it.localPath },
                languageTag = languageTag,
                forceIntent = if (state.isFreeChatMode) com.example.offlinelifeline.agent.UserIntent.FREE_CHAT else null
            )
            val assistantMessageWithTools = assistantMessage.copy(
                toolRecommendations = preparedResponse.response.toolRecommendations,
                citations = preparedResponse.response.citations
            )
            updateAssistantMessage(assistantMessageWithTools)

            var assistantText = ""
            survivalAgent.sendMessage(preparedResponse)
                .onEach { chunk ->
                    assistantText += chunk.text
                    updateAssistantMessage(
                        assistantMessageWithTools.copy(
                            text = assistantText,
                            isFinal = chunk.isFinal
                        )
                    )

                    if (chunk.isFinal) {
                        chatRepository.saveMessage(
                            conversationId,
                            assistantMessageWithTools.copy(
                                text = assistantText,
                                isFinal = true
                            )
                        )
                        _uiState.update { it.copy(isGenerating = false) }
                    }
                }
                .catch { throwable ->
                    updateAssistantMessage(assistantMessageWithTools.copy(text = assistantText))
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = throwable.message ?: "生成失败"
                        )
                    }
                }
                .collect()

            _uiState.update { state ->
                if (state.isGenerating) state.copy(isGenerating = false) else state
            }
        }
    }

    fun createConversation() {
        generationJob?.cancel()
        generationJob = null
        _uiState.value.pendingImages.forEach(imagePreprocessor::deleteProcessedImage)
        viewModelScope.launch {
            llmEngine.stopGeneration()
            llmEngine.resetConversation()
            val conversation = chatRepository.createConversation()
            _uiState.update {
                it.copy(
                    selectedConversationId = conversation.id,
                    messages = emptyList(),
                    inputText = "",
                    pendingImages = emptyList(),
                    isProcessingImage = false,
                    isGenerating = false,
                    errorMessage = null
                )
            }
        }
    }

    fun selectConversation(conversationId: String) {
        if (_uiState.value.selectedConversationId == conversationId) return
        generationJob?.cancel()
        generationJob = null
        _uiState.value.pendingImages.forEach(imagePreprocessor::deleteProcessedImage)
        viewModelScope.launch {
            llmEngine.stopGeneration()
            llmEngine.resetConversation()
            _uiState.update {
                it.copy(
                    selectedConversationId = conversationId,
                    messages = chatRepository.getMessages(conversationId),
                    inputText = "",
                    pendingImages = emptyList(),
                    isProcessingImage = false,
                    isGenerating = false,
                    errorMessage = null
                )
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        val isDeletingActive = _uiState.value.selectedConversationId == conversationId
        if (isDeletingActive) {
            generationJob?.cancel()
            generationJob = null
            _uiState.value.pendingImages.forEach(imagePreprocessor::deleteProcessedImage)
        }
        viewModelScope.launch {
            if (isDeletingActive) {
                llmEngine.stopGeneration()
                llmEngine.resetConversation()
            }
            val nextConversation = chatRepository.deleteConversation(conversationId)
            if (isDeletingActive) {
                _uiState.update {
                    it.copy(
                        selectedConversationId = nextConversation.id,
                        messages = chatRepository.getMessages(nextConversation.id),
                        inputText = "",
                        pendingImages = emptyList(),
                        isProcessingImage = false,
                        isGenerating = false,
                        errorMessage = "对话已删除。"
                    )
                }
            }
        }
    }

    fun stopGeneration() {
        if (!_uiState.value.isGenerating) return

        generationJob?.cancel()
        generationJob = null
        viewModelScope.launch {
            llmEngine.stopGeneration()
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    errorMessage = "本次生成已停止，未完成回答不会写入历史。"
                )
            }
        }
    }

    fun clearConversation() {
        val conversationId = _uiState.value.selectedConversationId ?: return
        generationJob?.cancel()
        generationJob = null
        _uiState.value.pendingImages.forEach(imagePreprocessor::deleteProcessedImage)
        viewModelScope.launch {
            llmEngine.stopGeneration()
            chatRepository.clearMessages(conversationId)
            llmEngine.resetConversation()
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    inputText = "",
                    pendingImages = emptyList(),
                    isProcessingImage = false,
                    isGenerating = false,
                    errorMessage = "对话已清空，上下文已重置。"
                )
            }
        }
    }

    override fun onCleared() {
        generationJob?.cancel()
        modelRefreshJob?.cancel()
        viewModelScope.launch {
            llmEngine.release()
        }
        super.onCleared()
    }

    private suspend fun refreshSelectedModel(modelId: String) {
        val manifest = ModelCatalog.findById(modelId) ?: ModelManifest.Default
        _uiState.update { it.copy(modelAssetState = ModelRuntimeState.Checking, errorMessage = null) }
        val modelCheck = modelAssetManager.checkModel(manifest)
        _uiState.update { it.copy(modelAssetState = modelCheck.runtimeState) }

        llmEngine.initialize()
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(errorMessage = throwable.message ?: "模型初始化失败")
                }
            }
    }

    private fun updateAssistantMessage(message: ChatMessage) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map {
                    if (it.id == message.id) message else it
                }
            )
        }
    }

    private fun processImage(process: suspend () -> Result<Attachment.Image>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingImage = true, errorMessage = null) }
            process()
                .onSuccess { image ->
                    _uiState.update { state ->
                        if (state.pendingImages.size >= MAX_PENDING_IMAGES) {
                            imagePreprocessor.deleteProcessedImage(image)
                            return@update state.copy(
                                isProcessingImage = false,
                                errorMessage = "最多只能附加 $MAX_PENDING_IMAGES 张图片"
                            )
                        }
                        state.copy(
                            pendingImages = state.pendingImages + image,
                            isProcessingImage = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isProcessingImage = false,
                            errorMessage = throwable.message ?: "图片处理失败"
                        )
                    }
                }
        }
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val llmEngine: LocalLlmEngine,
        private val survivalAgent: SurvivalAgent,
        private val modelAssetManager: ModelAssetManager,
        private val imagePreprocessor: ImagePreprocessor,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(
                    chatRepository = chatRepository,
                    llmEngine = llmEngine,
                    survivalAgent = survivalAgent,
                    modelAssetManager = modelAssetManager,
                    imagePreprocessor = imagePreprocessor,
                    settingsStore = settingsStore
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    private companion object {
        const val MAX_PENDING_IMAGES = 3
    }
}
