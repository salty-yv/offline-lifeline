package com.example.offlinelifeline.di

import android.content.Context
import androidx.room.Room
import com.example.offlinelifeline.agent.ActionPlanner
import com.example.offlinelifeline.agent.ContextManager
import com.example.offlinelifeline.agent.IntentClassifier
import com.example.offlinelifeline.agent.PromptBuilder
import com.example.offlinelifeline.agent.QuestionPlanner
import com.example.offlinelifeline.agent.RiskClassifier
import com.example.offlinelifeline.agent.SurvivalAgent
import com.example.offlinelifeline.agent.ToolRouter
import com.example.offlinelifeline.core.logging.DebugLogRepository
import com.example.offlinelifeline.core.logging.DebugLogger
import com.example.offlinelifeline.data.datastore.SettingsStore
import com.example.offlinelifeline.data.db.AppDatabase
import com.example.offlinelifeline.data.repository.ChatRepository
import com.example.offlinelifeline.data.repository.EmergencyCardRepository
import com.example.offlinelifeline.data.repository.GuideRepository
import com.example.offlinelifeline.inference.FallbackLlmEngine
import com.example.offlinelifeline.inference.LiteRtLmEngine
import com.example.offlinelifeline.inference.LocalLlmEngine
import com.example.offlinelifeline.inference.ModelAssetManager
import com.example.offlinelifeline.inference.ModelIntegrityChecker
import com.example.offlinelifeline.inference.MockLlmEngine
import com.example.offlinelifeline.safety.SafetyKernel

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            DATABASE_NAME
        ).build()
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(database.chatDao())
    }

    val debugLogRepository: DebugLogRepository by lazy {
        DebugLogRepository(database.debugLogDao())
    }

    val debugLogger: DebugLogger by lazy {
        DebugLogger(debugLogRepository)
    }

    val emergencyCardRepository: EmergencyCardRepository by lazy {
        EmergencyCardRepository(database.emergencyCardDao())
    }

    val guideRepository: GuideRepository by lazy {
        GuideRepository(database.guideDao())
    }

    val settingsStore: SettingsStore by lazy {
        SettingsStore(appContext)
    }

    private val mockLlmEngine: LocalLlmEngine by lazy {
        MockLlmEngine()
    }

    val modelIntegrityChecker: ModelIntegrityChecker by lazy {
        ModelIntegrityChecker()
    }

    val modelAssetManager: ModelAssetManager by lazy {
        ModelAssetManager(
            context = appContext,
            integrityChecker = modelIntegrityChecker,
            debugLogger = debugLogger
        )
    }

    private val liteRtLmEngine: LocalLlmEngine by lazy {
        LiteRtLmEngine(
            modelAssetManager = modelAssetManager,
            debugLogger = debugLogger
        )
    }

    val localLlmEngine: LocalLlmEngine by lazy {
        FallbackLlmEngine(
            primary = liteRtLmEngine,
            fallback = mockLlmEngine
        )
    }

    val survivalAgent: SurvivalAgent by lazy {
        SurvivalAgent(
            contextManager = ContextManager(),
            riskClassifier = RiskClassifier(),
            intentClassifier = IntentClassifier(),
            questionPlanner = QuestionPlanner(),
            actionPlanner = ActionPlanner(),
            toolRouter = ToolRouter(),
            promptBuilder = PromptBuilder(),
            safetyKernel = SafetyKernel(),
            llmEngine = localLlmEngine
        )
    }

    private companion object {
        const val DATABASE_NAME = "offline_lifeline.db"
    }
}
