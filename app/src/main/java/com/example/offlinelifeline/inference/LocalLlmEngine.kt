package com.example.offlinelifeline.inference

import com.example.offlinelifeline.core.model.ModelRuntimeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LocalLlmEngine {
    val runtimeState: StateFlow<ModelRuntimeState>

    val supportsImageInput: Boolean
        get() = false

    suspend fun initialize(): Result<Unit>

    fun sendMessage(request: InferenceRequest): Flow<InferenceChunk>

    suspend fun stopGeneration()

    suspend fun resetConversation()

    suspend fun release()
}
