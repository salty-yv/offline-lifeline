package com.example.offlinelifeline.inference

import com.example.offlinelifeline.core.common.AppDispatchers
import com.example.offlinelifeline.core.logging.DebugLogger
import com.example.offlinelifeline.core.model.ModelRuntimeState
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class LiteRtLmEngine(
    private val modelAssetManager: ModelAssetManager,
    private val debugLogger: DebugLogger,
    private val dispatchers: AppDispatchers = AppDispatchers()
) : LocalLlmEngine {
    private val _runtimeState = MutableStateFlow<ModelRuntimeState>(ModelRuntimeState.NotChecked)
    override val runtimeState: StateFlow<ModelRuntimeState> = _runtimeState

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var stopRequested = false

    override suspend fun initialize(): Result<Unit> {
        if (_runtimeState.value == ModelRuntimeState.Ready) {
            return Result.success(Unit)
        }

        return runCatching {
            _runtimeState.value = ModelRuntimeState.Checking
            val checkResult = modelAssetManager.checkModel()
            if (checkResult.runtimeState != ModelRuntimeState.ReadyToLoad) {
                _runtimeState.value = checkResult.runtimeState
                error(checkResult.message)
            }

            val modelPath = checkResult.location?.absoluteModelPath()
                ?: error("模型路径不可用")

            _runtimeState.value = ModelRuntimeState.Loading
            debugLogger.info(TAG, "Initializing LiteRT-LM engine: $modelPath")

            withContext(dispatchers.io) {
                Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    cacheDir = modelAssetManager.cacheDirPath
                )
                val createdEngine = Engine(config)
                createdEngine.initialize()
                engine = createdEngine
                conversation = createdEngine.createConversation()
            }

            _runtimeState.value = ModelRuntimeState.Ready
            debugLogger.info(TAG, "LiteRT-LM engine ready")
        }.onFailure { throwable ->
            _runtimeState.value = ModelRuntimeState.Failed(throwable.message ?: "LiteRT-LM 初始化失败")
            debugLogger.error(TAG, "LiteRT-LM initialization failed", throwable)
            release()
        }
    }

    override fun sendMessage(request: InferenceRequest): Flow<InferenceChunk> = flow {
        val activeConversation = conversation ?: error("LiteRT-LM 会话未初始化")
        stopRequested = false
        _runtimeState.value = ModelRuntimeState.Generating

        val prompt = buildPrompt(request)
        try {
            activeConversation.sendMessageAsync(prompt).collect { message ->
                if (stopRequested) throw CancellationException("LiteRT-LM generation stopped")
                emit(InferenceChunk(text = message.toString(), isFinal = false))
            }
            emit(InferenceChunk(text = "", isFinal = true))
            _runtimeState.value = ModelRuntimeState.Ready
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                _runtimeState.value = ModelRuntimeState.Ready
                throw throwable
            }
            _runtimeState.value = ModelRuntimeState.Failed(throwable.message ?: "LiteRT-LM 生成失败")
            debugLogger.error(TAG, "LiteRT-LM generation failed", throwable)
            throw throwable
        }
    }

    override suspend fun stopGeneration() {
        stopRequested = true
        _runtimeState.value = ModelRuntimeState.Ready
        debugLogger.info(TAG, "LiteRT-LM generation stop requested")
    }

    override suspend fun resetConversation() {
        stopGeneration()
        conversation?.close()
        conversation = withContext(dispatchers.io) {
            engine?.createConversation()
        }
        _runtimeState.value = if (conversation == null) {
            ModelRuntimeState.Released
        } else {
            ModelRuntimeState.Ready
        }
    }

    override suspend fun release() {
        _runtimeState.value = ModelRuntimeState.Releasing
        withContext(dispatchers.io) {
            conversation?.close()
            conversation = null
            engine?.close()
            engine = null
        }
        _runtimeState.value = ModelRuntimeState.Released
        debugLogger.info(TAG, "LiteRT-LM resources released")
    }

    private fun buildPrompt(request: InferenceRequest): String {
        return buildString {
            appendLine(request.systemInstruction)
            appendLine()
            appendLine(request.safetyInstruction)
            appendLine()
            appendLine("[User Message]")
            append(request.text)
        }
    }

    private companion object {
        const val TAG = "LiteRtLmEngine"
    }
}
