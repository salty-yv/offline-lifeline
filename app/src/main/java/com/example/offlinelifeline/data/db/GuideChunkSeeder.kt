package com.example.offlinelifeline.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用首次启动（或 guide_chunks_fts 为空）时，
 * 把 assets/databases/offline_guides.db 里的 chunk 数据
 * 批量导入主数据库的 guide_chunks_fts 表。
 *
 * 这样主数据库只需正常走 Room Migration 构建，
 * chunk 内容独立由 asset DB 提供，两者完全解耦。
 */
class GuideChunkSeeder(
    private val context: Context,
    private val guideChunkDao: GuideChunkDao
) {
    /**
     * 如果 guide_chunks_fts 为空则执行导入，否则直接返回。
     * 在协程中调用，IO 分发到 Dispatchers.IO。
     */
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        if (guideChunkDao.countChunks() > 0) return@withContext

        val assetDb = openAssetDb() ?: return@withContext
        try {
            copyChunksFromAsset(assetDb)
        } finally {
            assetDb.close()
        }
    }

    /**
     * 把 assets/databases/offline_guides.db 复制到缓存目录并以只读方式打开。
     * 返回 null 表示 asset 文件不存在或读取失败。
     */
    private fun openAssetDb(): SQLiteDatabase? {
        return try {
            // 每次都重新从 assets 复制，保证使用最新版本
            val cacheFile = context.getDatabasePath("offline_guides_asset_cache.db")
            cacheFile.parentFile?.mkdirs()
            context.assets.open("databases/offline_guides.db").use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            SQLiteDatabase.openDatabase(
                cacheFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 遍历 asset DB 的 guide_chunks_fts 表，
     * 逐行转换为 [GuideChunkFtsEntity] 并批量插入主数据库。
     */
    private suspend fun copyChunksFromAsset(assetDb: SQLiteDatabase) {
        val cursor = assetDb.rawQuery(
            "SELECT chunkId, guideId, topic, riskDomain, title, headingPath, body, tags FROM guide_chunks_fts",
            null
        )

        val batch = mutableListOf<GuideChunkFtsEntity>()

        cursor.use {
            while (it.moveToNext()) {
                batch += GuideChunkFtsEntity(
                    chunkId     = it.getString(0),
                    guideId     = it.getString(1),
                    topic       = it.getString(2),
                    riskDomain  = it.getString(3),
                    title       = it.getString(4),
                    headingPath = it.getString(5),
                    body        = it.getString(6),
                    tags        = it.getString(7)
                )
            }
        }

        if (batch.isNotEmpty()) {
            guideChunkDao.insertAll(batch)
        }
    }
}
