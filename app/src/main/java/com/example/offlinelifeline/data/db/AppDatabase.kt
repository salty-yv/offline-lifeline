package com.example.offlinelifeline.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 应用主数据库。
 *
 * 版本历史：
 * - v1 → v2：新增 chat_conversations 表，chat_messages 加 conversationId 列（见 AppContainer）
 * - v2 → v3：新增 guide_chunks、guide_chunks_fts（FTS4）表，支持本地 RAG 检索
 *
 * 首次安装时通过 Room.createFromAsset("databases/offline_guides.db") 加载内置数据库。
 * 升级时通过 Migration 平滑迁移，不会清除已有数据。
 */
@Database(
    entities = [
        ChatConversationEntity::class,
        ChatMessageEntity::class,
        DebugLogEntity::class,
        EmergencyCardEntity::class,
        GuideEntity::class,
        GuideChunkEntity::class,
        GuideChunkFtsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun debugLogDao(): DebugLogDao
    abstract fun emergencyCardDao(): EmergencyCardDao
    abstract fun guideDao(): GuideDao
    abstract fun guideChunkDao(): GuideChunkDao
}
