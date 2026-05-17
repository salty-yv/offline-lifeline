package com.example.offlinelifeline.inference

import android.os.SystemClock
import com.example.offlinelifeline.core.common.AppDispatchers
import com.example.offlinelifeline.core.logging.DebugLogger
import com.example.offlinelifeline.core.model.ModelRuntimeState
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class LiteRtLmEngine(
    private val modelAssetManager: ModelAssetManager,
    private val debugLogger: DebugLogger,
    private val manifestProvider: suspend () -> ModelManifest = { ModelManifest.Default },
    private val dispatchers: AppDispatchers = AppDispatchers()
) : LocalLlmEngine {
    private val _runtimeState = MutableStateFlow<ModelRuntimeState>(ModelRuntimeState.NotChecked)
    override val runtimeState: StateFlow<ModelRuntimeState> = _runtimeState

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedModelId: String? = null
    private var loadedModelLocationKey: String? = null
    private var stopRequested = false
    private var imageInputEnabled = false
    private val engineMutex = Mutex()

    override val supportsImageInput: Boolean
        get() = imageInputEnabled

    override suspend fun initialize(): Result<Unit> = engineMutex.withLock {
        initializeLocked(manifestProvider())
    }

    private suspend fun initializeLocked(manifest: ModelManifest): Result<Unit> {
        val startedAt = SystemClock.elapsedRealtime()
        return try {
            _runtimeState.value = ModelRuntimeState.Checking
            val checkResult = modelAssetManager.prepareModelForRuntime(manifest)
            if (checkResult.runtimeState != ModelRuntimeState.ReadyToLoad) {
                _runtimeState.value = checkResult.runtimeState
                error(checkResult.message)
            }

            val modelLocation = checkResult.location ?: error("Model path is unavailable.")
            val modelLocationKey = modelAssetManager.modelLocationKey(modelLocation)
            if (
                _runtimeState.value == ModelRuntimeState.Ready &&
                loadedModelId == manifest.modelId &&
                loadedModelLocationKey == modelLocationKey
            ) {
                return Result.success(Unit)
            }
            if (
                loadedModelId == manifest.modelId &&
                loadedModelLocationKey == modelLocationKey &&
                engine != null &&
                conversation != null
            ) {
                _runtimeState.value = ModelRuntimeState.Ready
                return Result.success(Unit)
            }
            if (
                engine != null &&
                (loadedModelId != manifest.modelId || loadedModelLocationKey != modelLocationKey)
            ) {
                releaseLocked()
            }

            val modelPath = modelAssetManager.modelPathForRuntime(modelLocation)
                ?: error("Model path is unavailable.")

            _runtimeState.value = ModelRuntimeState.Loading
            debugLogger.info(
                TAG,
                "litertlm_initialize_start modelPath=$modelPath backend=CPU supportsImageInput=${manifest.supportsImageInput}"
            )

            withContext(dispatchers.io) {
                Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
                val createdEngine = if (manifest.supportsImageInput) {
                    runCatching {
                        createEngine(modelPath = modelPath, enableVision = true)
                    }.onSuccess {
                        imageInputEnabled = true
                        debugLogger.info(TAG, "litertlm_vision_backend_enabled backend=GPU maxNumImages=$MAX_IMAGES")
                    }.getOrElse { visionThrowable ->
                        debugLogger.warning(
                            TAG,
                            "litertlm_vision_backend_failed_retrying_text_only reason=${visionThrowable.message}. Check model accelerator allowlist for visionAccelerator=gpu."
                        )
                        imageInputEnabled = false
                        createEngine(modelPath = modelPath, enableVision = false)
                    }
                } else {
                    imageInputEnabled = false
                    createEngine(modelPath = modelPath, enableVision = false)
                }
                engine = createdEngine
                conversation = createdEngine.createConversation()
            }

            loadedModelId = manifest.modelId
            loadedModelLocationKey = modelLocationKey
            _runtimeState.value = ModelRuntimeState.Ready
            debugLogger.info(
                TAG,
                "litertlm_initialize_success modelId=${manifest.modelId} location=$modelLocationKey elapsedMs=${SystemClock.elapsedRealtime() - startedAt} supportsImageInput=$supportsImageInput"
            )
            Result.success(Unit)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            val failureMessage = "LiteRT-LM initialization failed for ${manifest.modelName}: ${throwable.message ?: throwable::class.java.simpleName}"
            _runtimeState.value = ModelRuntimeState.Failed(failureMessage)
            debugLogger.error(
                TAG,
                "litertlm_initialize_failed elapsedMs=${SystemClock.elapsedRealtime() - startedAt}",
                throwable
            )
            releaseLocked()
            Result.failure(IllegalStateException(failureMessage, throwable))
        }
    }

    override fun sendMessage(request: InferenceRequest): Flow<InferenceChunk> = flow {
        engineMutex.withLock {
            val manifest = manifestProvider()
            if (needsInitialization(manifest)) {
                initializeLocked(manifest).getOrThrow()
            }
            val activeConversation = conversation ?: error("LiteRT-LM conversation is not initialized")
            stopRequested = false
            _runtimeState.value = ModelRuntimeState.Generating

            val prompt = buildPrompt(request)
            val startedAt = SystemClock.elapsedRealtime()
            var firstTokenLatencyMs: Long? = null
            var chunkCount = 0
            var outputChars = 0

            try {
                val contents = buildContents(request, prompt)
                debugLogger.info(
                    TAG,
                    "litertlm_generation_start promptChars=${prompt.length} imageCount=${request.imagePaths.size} supportsImageInput=$supportsImageInput multimodal=${contents != null}"
                )
                val outputFlow = if (contents != null) {
                    activeConversation.sendMessageAsync(contents)
                } else {
                    activeConversation.sendMessageAsync(prompt)
                }
                outputFlow.collect { message ->
                    if (stopRequested) throw CancellationException("LiteRT-LM generation stopped")

                    val text = message.toString()
                    if (firstTokenLatencyMs == null && text.isNotEmpty()) {
                        firstTokenLatencyMs = SystemClock.elapsedRealtime() - startedAt
                        debugLogger.info(TAG, "litertlm_first_token latencyMs=$firstTokenLatencyMs")
                    }
                    chunkCount += 1
                    outputChars += text.length
                    emit(InferenceChunk(text = text, isFinal = false))
                }

                emit(InferenceChunk(text = "", isFinal = true))
                _runtimeState.value = ModelRuntimeState.Ready
                debugLogger.info(
                    TAG,
                    "litertlm_generation_success totalElapsedMs=${SystemClock.elapsedRealtime() - startedAt} firstTokenLatencyMs=${firstTokenLatencyMs ?: -1} chunks=$chunkCount outputChars=$outputChars"
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    _runtimeState.value = ModelRuntimeState.Ready
                    debugLogger.warning(
                        TAG,
                        "litertlm_generation_interrupted totalElapsedMs=${SystemClock.elapsedRealtime() - startedAt} firstTokenLatencyMs=${firstTokenLatencyMs ?: -1} chunks=$chunkCount outputChars=$outputChars"
                    )
                    throw throwable
                }

                _runtimeState.value = ModelRuntimeState.Failed(throwable.message ?: "LiteRT-LM generation failed")
                debugLogger.error(
                    TAG,
                    "litertlm_generation_failed totalElapsedMs=${SystemClock.elapsedRealtime() - startedAt} firstTokenLatencyMs=${firstTokenLatencyMs ?: -1} chunks=$chunkCount outputChars=$outputChars",
                    throwable
                )
                throw throwable
            }
        }
    }

    private suspend fun needsInitialization(manifest: ModelManifest): Boolean {
        if (_runtimeState.value != ModelRuntimeState.Ready || engine == null || conversation == null) {
            return true
        }
        if (loadedModelId != manifest.modelId) {
            return true
        }
        val currentLocationKey = modelAssetManager.currentModelLocationKey(manifest)
        return currentLocationKey == null || loadedModelLocationKey != currentLocationKey
    }

    override suspend fun stopGeneration() {
        stopRequested = true
        _runtimeState.value = ModelRuntimeState.Ready
        debugLogger.info(TAG, "litertlm_generation_stop_requested")
    }

    override suspend fun resetConversation() {
        engineMutex.withLock {
            val startedAt = SystemClock.elapsedRealtime()
            stopRequested = true
            conversation?.close()
            conversation = withContext(dispatchers.io) {
                engine?.createConversation()
            }
            _runtimeState.value = if (conversation == null) {
                ModelRuntimeState.Released
            } else {
                ModelRuntimeState.Ready
            }
            debugLogger.info(
                TAG,
                "litertlm_conversation_reset elapsedMs=${SystemClock.elapsedRealtime() - startedAt} state=${_runtimeState.value::class.simpleName}"
            )
        }
    }

    override suspend fun release() {
        engineMutex.withLock {
            releaseLocked()
        }
    }

    private suspend fun releaseLocked() {
        val startedAt = SystemClock.elapsedRealtime()
        _runtimeState.value = ModelRuntimeState.Releasing
        runCatching {
            withContext(dispatchers.io) {
                conversation?.close()
                conversation = null
                engine?.close()
                engine = null
                loadedModelId = null
                loadedModelLocationKey = null
            }
            modelAssetManager.releaseOpenExternalModelDescriptors()
        }.onFailure { throwable ->
            debugLogger.error(TAG, "litertlm_release_failed elapsedMs=${SystemClock.elapsedRealtime() - startedAt}", throwable)
            throw throwable
        }
        _runtimeState.value = ModelRuntimeState.Released
        debugLogger.info(TAG, "litertlm_release_success elapsedMs=${SystemClock.elapsedRealtime() - startedAt}")
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

    private suspend fun buildContents(request: InferenceRequest, prompt: String): Contents? {
        if (request.imagePaths.isEmpty() || !supportsImageInput) return null

        val imageContents = request.imagePaths
            .take(MAX_IMAGES)
            .mapNotNull { path ->
                runCatching {
                    Content.ImageBytes(File(path).readBytes())
                }.onFailure { throwable ->
                    debugLogger.warning(TAG, "litertlm_image_read_failed path=$path reason=${throwable.message}")
                }.getOrNull()
            }

        if (imageContents.isEmpty()) return null

        return Contents.of(
            listOf(Content.Text(prompt)) + imageContents
        )
    }

    private fun createEngine(modelPath: String, enableVision: Boolean): Engine {
        val config = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU(),
            visionBackend = if (enableVision) Backend.GPU() else null,
            maxNumImages = if (enableVision) MAX_IMAGES else null,
            cacheDir = modelAssetManager.cacheDirPath
        )
        val createdEngine = Engine(config)
        return try {
            createdEngine.initialize()
            createdEngine
        } catch (throwable: Throwable) {
            runCatching { createdEngine.close() }
            throw throwable
        }
    }

    private companion object {
        const val TAG = "LiteRtLmEngine"
        const val MAX_IMAGES = 3
    }
}
