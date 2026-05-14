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
import com.example.offlinelifeline.data.repository.ChatRepository
import com.example.offlinelifeline.inference.LocalLlmEngine
import com.example.offlinelifeline.inference.ModelAssetManager
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
    private val timeProvider: TimeProvider = TimeProvider.System
) : ViewModel() {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(ChatUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<ChatUiState> = _uiState

    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(messages = chatRepository.getMessages()) }
            _uiState.update { it.copy(modelAssetState = ModelRuntimeState.Checking) }
            val modelCheck = modelAssetManager.checkModel()
            _uiState.update { it.copy(modelAssetState = modelCheck.runtimeState) }

            llmEngine.initialize()
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "模型初始化失败")
                    }
                }
        }

        viewModelScope.launch {
            llmEngine.runtimeState.collect { runtimeState ->
                _uiState.update { it.copy(runtimeState = runtimeState) }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
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
        val text = state.inputText.trim().ifBlank {
            if (state.pendingImages.isNotEmpty()) "我添加了一张现场图片，请先按保守原则给出文字辅助建议。" else ""
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
            chatRepository.saveMessage(userMessage)

            val preparedResponse = survivalAgent.prepareResponse(
                userInput = text,
                history = _uiState.value.messages.filter { it.id != assistantMessage.id },
                imagePaths = pendingImages.map { it.localPath }
            )
            val assistantMessageWithTools = assistantMessage.copy(
                toolRecommendations = preparedResponse.response.toolRecommendations
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

    override fun onCleared() {
        generationJob?.cancel()
        viewModelScope.launch {
            llmEngine.release()
        }
        super.onCleared()
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
        private val imagePreprocessor: ImagePreprocessor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(chatRepository, llmEngine, survivalAgent, modelAssetManager, imagePreprocessor) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    private companion object {
        const val MAX_PENDING_IMAGES = 3
    }
}
