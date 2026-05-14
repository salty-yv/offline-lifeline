package com.example.offlinelifeline.agent

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
    private val llmEngine: LocalLlmEngine
) {
    fun prepareResponse(
        userInput: String,
        history: List<ChatMessage>
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
        val intent = intentClassifier.classify(userInput)
        val questions = questionPlanner.planQuestions(context)
        val tools = toolRouter.recommendTools(risks, context)
        val actionStructure = actionPlanner.buildActionStructure(risks, context, questions)
        val systemInstruction = promptBuilder.buildSystemInstruction(
            context = context,
            actionStructure = actionStructure,
            toolRecommendations = tools,
            intent = intent
        )
        val safetyInstruction = promptBuilder.buildSafetyInstruction(
            safetyKernel.buildSafetyInstruction(risks)
        )

        return PreparedAgentResponse(
            request = InferenceRequest(
                text = userInput,
                systemInstruction = systemInstruction,
                safetyInstruction = safetyInstruction
            ),
            response = AgentResponse(
                text = actionStructure,
                riskDomains = risks,
                followUpQuestions = questions,
                toolRecommendations = tools
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
    }
}

data class PreparedAgentResponse(
    val request: InferenceRequest,
    val response: AgentResponse
) {
    val hasKnownRisk: Boolean
        get() = response.riskDomains.any { it != RiskDomain.UNKNOWN }
}
