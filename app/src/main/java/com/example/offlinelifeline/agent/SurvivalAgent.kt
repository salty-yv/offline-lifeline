package com.example.offlinelifeline.agent

import com.example.offlinelifeline.agent.rag.GuideCitation
import com.example.offlinelifeline.agent.rag.GuideRetrievalService
import com.example.offlinelifeline.core.model.AgentResponse
import com.example.offlinelifeline.core.model.ChatMessage
import com.example.offlinelifeline.core.model.ChatRole
import com.example.offlinelifeline.core.model.RiskDomain
import com.example.offlinelifeline.inference.InferenceChunk
import com.example.offlinelifeline.inference.InferenceRequest
import com.example.offlinelifeline.inference.LocalLlmEngine
import com.example.offlinelifeline.safety.SafetyKernel
import com.example.offlinelifeline.safety.SafetyValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class SurvivalAgent(
    private val contextManager: ContextManager,
    private val riskClassifier: RiskClassifier,
    private val intentClassifier: IntentClassifier,
    private val questionPlanner: QuestionPlanner,
    private val actionPlanner: ActionPlanner,
    private val toolRouter: ToolRouter,
    private val promptBuilder: PromptBuilder,
    private val safetyKernel: SafetyKernel,
    private val llmEngine: LocalLlmEngine,
    /** RAG 服务，可空：为 null 时跳过检索，保持向后兼容 */
    private val guideRetrievalService: GuideRetrievalService? = null
) {
    /**
     * 准备 Agent 响应（协程，内含 RAG 检索 IO 操作）。
     *
     * RAG 检索在这里完成，结果注入 systemInstruction 后交给 [sendMessage] 送给 LLM。
     */
    suspend fun prepareResponse(
        userInput: String,
        history: List<ChatMessage>,
        imagePaths: List<String> = emptyList(),
        languageTag: String = "zh-CN"
    ): PreparedAgentResponse {
        val scopedHistory = contextManager.summarizeIfNeeded(history)
        val contextMessages = scopedHistory + ChatMessage(
            id = "current-user-input",
            role = ChatRole.USER,
            text = userInput,
            createdAtMillis = 0L
        )
        val baseContext = contextManager.buildContext(contextMessages)
        val risks = riskClassifier.classify(userInput, baseContext)
        val context = baseContext.copy(riskDomains = risks)
        // 历史中 USER 消息的数量即为已完成的对话轮次，用于区分首轮和追问
        val previousTurns = history.count { it.role == ChatRole.USER }
        val intent = intentClassifier.classify(userInput, previousTurns)
        val questions = questionPlanner.planQuestions(context)
        val tools = toolRouter.recommendTools(risks, context, languageTag)
        val actionStructure = actionPlanner.buildActionStructure(risks, context, questions, languageTag)

        // ── RAG 检索：在调用 LLM 前先拿本地 chunk ──────────────────────────────
        val ragChunks = guideRetrievalService
            ?.retrieve(userInput, risks, limit = RAG_CHUNK_LIMIT)
            .orEmpty()

        val ragContext = if (ragChunks.isNotEmpty()) {
            GuideRetrievalService.buildLocalGuideContext(ragChunks)
        } else ""

        val citations: List<GuideCitation> = if (ragChunks.isNotEmpty()) {
            GuideRetrievalService.buildCitations(ragChunks)
        } else emptyList()

        val systemInstruction = promptBuilder.buildSystemInstruction(
            context = context,
            actionStructure = actionStructure,
            toolRecommendations = tools,
            intent = intent,
            imagePaths = imagePaths,
            imageInputSupported = llmEngine.supportsImageInput,
            languageTag = languageTag,
            ragContext = ragContext           // ← 注入本地指南上下文
        )
        val safetyInstruction = promptBuilder.buildSafetyInstruction(
            safetyKernel.buildSafetyInstruction(
                riskDomains = risks,
                hasImageInput = imagePaths.isNotEmpty(),
                imageInputSupported = llmEngine.supportsImageInput
            )
        )

        return PreparedAgentResponse(
            request = InferenceRequest(
                text = userInput,
                imagePaths = imagePaths,
                systemInstruction = systemInstruction,
                safetyInstruction = safetyInstruction
            ),
            response = AgentResponse(
                text = actionStructure,
                riskDomains = risks,
                followUpQuestions = questions,
                toolRecommendations = tools,
                citations = citations          // ← 引用来源传回 UI
            )
        )
    }

    fun sendMessage(preparedResponse: PreparedAgentResponse): Flow<InferenceChunk> = flow {
        val rawOutput = StringBuilder()

        llmEngine.sendMessage(preparedResponse.request).collect { chunk ->
            rawOutput.append(chunk.text)
        }

        val finalOutput = when (val validation = safetyKernel.validate(rawOutput.toString(), preparedResponse.response.riskDomains)) {
            SafetyValidationResult.Pass -> rawOutput.toString()
            is SafetyValidationResult.Blocked -> safetyKernel.buildFallbackResponse(
                reason = validation.reason,
                riskDomains = preparedResponse.response.riskDomains
            )
            is SafetyValidationResult.NeedsRewrite -> rawOutput
                .appendLine()
                .appendLine()
                .appendLine("安全补充")
                .append(validation.rewriteInstruction)
                .toString()
        }

        val chunks = finalOutput.chunked(SAFE_CHUNK_SIZE)
        chunks.forEachIndexed { index, text ->
            emit(
                InferenceChunk(
                    text = text,
                    isFinal = index == chunks.lastIndex
                )
            )
        }
    }

    private companion object {
        const val SAFE_CHUNK_SIZE = 14
        /** 每次最多注入 5 个 chunk，避免 context 窗口过大 */
        const val RAG_CHUNK_LIMIT = 5
    }
}

data class PreparedAgentResponse(
    val request: InferenceRequest,
    val response: AgentResponse
) {
    val hasKnownRisk: Boolean
        get() = response.riskDomains.any { it != RiskDomain.UNKNOWN }
}
