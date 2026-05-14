package com.example.offlinelifeline.inference

import com.example.offlinelifeline.core.model.ModelRuntimeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

class MockLlmEngine(
    private val behavior: MockLlmBehavior = MockLlmBehavior.Normal
) : LocalLlmEngine {
    private val _runtimeState = MutableStateFlow<ModelRuntimeState>(ModelRuntimeState.NotChecked)
    override val runtimeState: StateFlow<ModelRuntimeState> = _runtimeState

    @Volatile
    private var stopRequested = false

    override suspend fun initialize(): Result<Unit> {
        _runtimeState.value = ModelRuntimeState.Loading
        delay(500)
        return if (behavior == MockLlmBehavior.FailInitialization) {
            _runtimeState.value = ModelRuntimeState.Failed("Mock 模型加载失败")
            Result.failure(IllegalStateException("Mock 模型加载失败"))
        } else {
            _runtimeState.value = ModelRuntimeState.Ready
            Result.success(Unit)
        }
    }

    override fun sendMessage(request: InferenceRequest): Flow<InferenceChunk> = flow {
        if (behavior == MockLlmBehavior.FailGeneration) {
            _runtimeState.value = ModelRuntimeState.Failed("Mock 生成失败")
            throw IllegalStateException("Mock 生成失败")
        }

        stopRequested = false
        _runtimeState.value = ModelRuntimeState.Generating

        val chunks = buildMockChunks(request)
        chunks.forEachIndexed { index, chunk ->
            delay(100)
            if (stopRequested) {
                _runtimeState.value = ModelRuntimeState.Ready
                return@flow
            }
            emit(InferenceChunk(text = chunk, isFinal = index == chunks.lastIndex))
        }

        _runtimeState.value = ModelRuntimeState.Ready
    }

    override suspend fun stopGeneration() {
        stopRequested = true
        _runtimeState.value = ModelRuntimeState.Ready
    }

    override suspend fun resetConversation() {
        stopRequested = true
        _runtimeState.value = ModelRuntimeState.Ready
    }

    override suspend fun release() {
        stopRequested = true
        _runtimeState.value = ModelRuntimeState.Released
    }

    private fun buildMockChunks(request: InferenceRequest): List<String> {
        val agentPlan = request.systemInstruction
            .substringAfter("[Agent Action Plan]", missingDelimiterValue = "")
            .substringBefore("[Available Local Tools]")
            .trim()
        val tools = request.systemInstruction
            .substringAfter("[Available Local Tools]", missingDelimiterValue = "")
            .trim()

        val response = if (agentPlan.isNotBlank()) {
            buildString {
                append(agentPlan)
                if (tools.isNotBlank()) {
                    append("\n\n可使用的本地工具\n")
                    append(tools)
                }
            }
        } else {
            val normalizedInput = request.text.trim()
            buildString {
                append("先保持原地安全，减少不必要操作。")
                append("\n\n你刚才说：")
                append(normalizedInput)
                append("\n\n当前为 Mock 输出。")
            }
        }

        return response
            .chunked(14)
            .map { it }
    }
}

enum class MockLlmBehavior {
    Normal,
    FailInitialization,
    FailGeneration
}
