package com.example.offlinelifeline.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offlinelifeline.agent.SurvivalAgent
import com.example.offlinelifeline.core.common.TimeProvider
import com.example.offlinelifeline.core.model.ChatMessage
import com.example.offlinelifeline.core.model.ChatRole
import com.example.offlinelifeline.core.model.ModelRuntimeState
import com.example.offlinelifeline.data.repository.ChatRepository
import com.example.offlinelifeline.inference.LocalLlmEngine
import com.example.offlinelifeline.inference.ModelAssetManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val llmEngine: LocalLlmEngine,
    private val survivalAgent: SurvivalAgent,
    private val modelAssetManager: ModelAssetManager,
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

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isGenerating) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = text,
            createdAtMillis = timeProvider.nowMillis()
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
                isGenerating = true,
                errorMessage = null
            )
        }

        generationJob = viewModelScope.launch {
            chatRepository.saveMessage(userMessage)

            val preparedResponse = survivalAgent.prepareResponse(
                userInput = text,
                history = _uiState.value.messages.filter { it.id != assistantMessage.id }
            )

            var assistantText = ""
            survivalAgent.sendMessage(preparedResponse)
                .onEach { chunk ->
                    assistantText += chunk.text
                    updateAssistantMessage(
                        assistantMessage.copy(
                            text = assistantText,
                            isFinal = chunk.isFinal
                        )
                    )

                    if (chunk.isFinal) {
                        chatRepository.saveMessage(
                            assistantMessage.copy(
                                text = assistantText,
                                isFinal = true
                            )
                        )
                        _uiState.update { it.copy(isGenerating = false) }
                    }
                }
                .catch { throwable ->
                    updateAssistantMessage(assistantMessage.copy(text = assistantText))
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

    class Factory(
        private val chatRepository: ChatRepository,
        private val llmEngine: LocalLlmEngine,
        private val survivalAgent: SurvivalAgent,
        private val modelAssetManager: ModelAssetManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(chatRepository, llmEngine, survivalAgent, modelAssetManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
