package com.example.offlinelifeline.inference

import com.example.offlinelifeline.core.model.ModelRuntimeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FallbackLlmEngine(
    private val primary: LocalLlmEngine,
    private val fallback: LocalLlmEngine
) : LocalLlmEngine {
    private val _runtimeState = MutableStateFlow<ModelRuntimeState>(ModelRuntimeState.NotChecked)
    override val runtimeState: StateFlow<ModelRuntimeState> = _runtimeState

    private var activeEngine: LocalLlmEngine = primary
    private val initializeMutex = Mutex()
    override val supportsImageInput: Boolean
        get() = activeEngine.supportsImageInput

    override suspend fun initialize(): Result<Unit> = initializeMutex.withLock {
        val primaryResult = primary.initialize()
        if (primaryResult.isSuccess) {
            activeEngine = primary
            _runtimeState.value = ModelRuntimeState.Ready
            return@withLock primaryResult
        }

        val primaryState = primary.runtimeState.value
        _runtimeState.value = primaryState
        val fallbackResult = fallback.initialize()
        if (fallbackResult.isSuccess) {
            activeEngine = fallback
            _runtimeState.value = ModelRuntimeState.Ready
            return@withLock Result.success(Unit)
        }

        _runtimeState.value = ModelRuntimeState.Failed(
            fallbackResult.exceptionOrNull()?.message
                ?: primaryResult.exceptionOrNull()?.message
                ?: "模型初始化失败"
        )
        return@withLock fallbackResult
    }

    override fun sendMessage(request: InferenceRequest): Flow<InferenceChunk> = flow {
        if (activeEngine.runtimeState.value != ModelRuntimeState.Ready) {
            initialize().getOrThrow()
        }
        activeEngine.sendMessage(request).collect { chunk ->
            emit(chunk)
        }
    }

    override suspend fun stopGeneration() {
        activeEngine.stopGeneration()
    }

    override suspend fun resetConversation() {
        activeEngine.resetConversation()
    }

    override suspend fun release() {
        activeEngine.release()
    }
}
