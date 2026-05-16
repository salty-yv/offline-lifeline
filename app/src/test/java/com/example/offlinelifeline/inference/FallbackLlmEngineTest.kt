package com.example.offlinelifeline.inference

import com.example.offlinelifeline.core.model.ModelRuntimeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackLlmEngineTest {
    @Test
    fun sendMessageInitializesPrimaryBeforeSending() = runBlocking {
        val primary = InitializingFakeEngine()
        val fallback = InitializingFakeEngine()
        val engine = FallbackLlmEngine(primary, fallback)

        val chunks = engine.sendMessage(testRequest()).toList()

        assertTrue(primary.initializeCalled)
        assertEquals(listOf(InferenceChunk("ok", isFinal = true)), chunks)
    }

    @Test
    fun fallbackSuccessReportsReadyState() = runBlocking {
        val primary = InitializingFakeEngine(shouldFailInitialization = true)
        val fallback = InitializingFakeEngine()
        val engine = FallbackLlmEngine(primary, fallback)

        val result = engine.initialize()

        assertTrue(result.isSuccess)
        assertEquals(ModelRuntimeState.Ready, engine.runtimeState.value)
    }

    private fun testRequest() = InferenceRequest(
        text = "hello",
        systemInstruction = "",
        safetyInstruction = ""
    )

    private class InitializingFakeEngine(
        private val shouldFailInitialization: Boolean = false
    ) : LocalLlmEngine {
        private val state = MutableStateFlow<ModelRuntimeState>(ModelRuntimeState.NotChecked)
        var initializeCalled = false

        override val runtimeState: StateFlow<ModelRuntimeState> = state

        override suspend fun initialize(): Result<Unit> {
            initializeCalled = true
            return if (shouldFailInitialization) {
                state.value = ModelRuntimeState.Failed("init failed")
                Result.failure(IllegalStateException("init failed"))
            } else {
                state.value = ModelRuntimeState.Ready
                Result.success(Unit)
            }
        }

        override fun sendMessage(request: InferenceRequest): Flow<InferenceChunk> {
            check(state.value == ModelRuntimeState.Ready) {
                "sendMessage called before initialize"
            }
            return flowOf(InferenceChunk("ok", isFinal = true))
        }

        override suspend fun stopGeneration() {
            state.value = ModelRuntimeState.Ready
        }

        override suspend fun resetConversation() {
            state.value = ModelRuntimeState.Ready
        }

        override suspend fun release() {
            state.value = ModelRuntimeState.Released
        }
    }
}
