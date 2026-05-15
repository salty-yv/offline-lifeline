package com.example.offlinelifeline.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.offlinelifeline.agent.ActionPlanner
import com.example.offlinelifeline.agent.ContextManager
import com.example.offlinelifeline.agent.IntentClassifier
import com.example.offlinelifeline.agent.PromptBuilder
import com.example.offlinelifeline.agent.QuestionPlanner
import com.example.offlinelifeline.agent.RiskClassifier
import com.example.offlinelifeline.agent.SurvivalAgent
import com.example.offlinelifeline.agent.ToolRouter
import com.example.offlinelifeline.core.diagnostics.DeviceDiagnosticsLogger
import com.example.offlinelifeline.core.logging.DebugLogRepository
import com.example.offlinelifeline.core.logging.DebugLogExporter
import com.example.offlinelifeline.core.logging.DebugLogger
import com.example.offlinelifeline.data.datastore.SettingsStore
import com.example.offlinelifeline.data.db.AppDatabase
import com.example.offlinelifeline.data.db.GuideChunkSeeder
import com.example.offlinelifeline.data.repository.ChatRepository
import com.example.offlinelifeline.data.repository.EmergencyCardRepository
import com.example.offlinelifeline.data.repository.GuideRepository
import com.example.offlinelifeline.inference.FallbackLlmEngine
import com.example.offlinelifeline.inference.LiteRtLmEngine
import com.example.offlinelifeline.inference.LocalLlmEngine
import com.example.offlinelifeline.inference.ModelAssetManager
import com.example.offlinelifeline.inference.ModelIntegrityChecker
import com.example.offlinelifeline.inference.MockLlmEngine
import com.example.offlinelifeline.device.battery.BatteryAdviceGenerator
import com.example.offlinelifeline.device.battery.BatteryStatusProvider
import com.example.offlinelifeline.device.flashlight.FlashlightController
import com.example.offlinelifeline.device.image.ImagePreprocessor
import com.example.offlinelifeline.safety.SafetyKernel

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
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

    val debugLogExporter: DebugLogExporter by lazy {
        DebugLogExporter(appContext, debugLogRepository)
    }

    val flashlightController: FlashlightController by lazy {
        FlashlightController(appContext)
    }

    val batteryStatusProvider: BatteryStatusProvider by lazy {
        BatteryStatusProvider(appContext)
    }

    val batteryAdviceGenerator: BatteryAdviceGenerator by lazy {
        BatteryAdviceGenerator()
    }

    val imagePreprocessor: ImagePreprocessor by lazy {
        ImagePreprocessor(appContext)
    }

    val deviceDiagnosticsLogger: DeviceDiagnosticsLogger by lazy {
        DeviceDiagnosticsLogger(
            context = appContext,
            batteryStatusProvider = batteryStatusProvider,
            debugLogger = debugLogger
        )
    }

    val emergencyCardRepository: EmergencyCardRepository by lazy {
        EmergencyCardRepository(database.emergencyCardDao())
    }

    val guideRepository: GuideRepository by lazy {
        GuideRepository(database.guideDao())
    }

    val guideChunkDao by lazy {
        database.guideChunkDao()
    }

    val guideChunkSeeder by lazy {
        GuideChunkSeeder(appContext, guideChunkDao)
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_conversations (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "ALTER TABLE chat_messages ADD COLUMN conversationId TEXT NOT NULL DEFAULT 'legacy'"
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO chat_conversations (
                        id,
                        title,
                        createdAtMillis,
                        updatedAtMillis
                    )
                    SELECT
                        'legacy',
                        '旧对话',
                        MIN(createdAtMillis),
                        MAX(createdAtMillis)
                    FROM chat_messages
                    WHERE EXISTS (SELECT 1 FROM chat_messages)
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chat_messages_conversationId ON chat_messages(conversationId)"
                )
            }
        }

        /**
         * v2 → v3：新增 guide_chunks 普通表和 guide_chunks_fts FTS4 虚拟表。
         * 用于 Agent 本地 RAG 检索；表数据由 assets/databases/offline_guides.db 内置提供。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS guide_chunks (
                        chunkId         TEXT NOT NULL PRIMARY KEY,
                        guideId         TEXT NOT NULL,
                        topic           TEXT NOT NULL,
                        riskDomain      TEXT NOT NULL,
                        title           TEXT NOT NULL,
                        headingPath     TEXT NOT NULL,
                        body            TEXT NOT NULL,
                        tags            TEXT NOT NULL,
                        priority        INTEGER NOT NULL,
                        chunkIndex      INTEGER NOT NULL,
                        contentVersion  INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_chunk_guide  ON guide_chunks(guideId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_chunk_topic  ON guide_chunks(topic)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_chunk_domain ON guide_chunks(riskDomain)"
                )
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS guide_chunks_fts
                    USING fts4(
                        chunkId,
                        guideId,
                        topic,
                        riskDomain,
                        title,
                        headingPath,
                        body,
                        tags
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
