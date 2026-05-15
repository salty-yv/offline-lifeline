package com.example.offlinelifeline.data.db

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用启动时把 assets/databases/offline_guides.db 里的 chunk 数据
 * 导入主数据库的 guide_chunks_fts 表。
 *
 * ## 版本控制
 * 每次修改指南内容（新增/修改/删除 Markdown 文件后重新执行 build_guides_db.py）
 * 必须同步把 [GUIDE_DATA_VERSION] 加 1。
 *
 * 启动时若检测到 [GUIDE_DATA_VERSION] > SharedPreferences 里存储的版本号，
 * 则自动清空旧数据并重新从 asset DB 导入，确保已安装设备也能拿到最新内容。
 */
class GuideChunkSeeder(
    private val context: Context,
    private val guideChunkDao: GuideChunkDao
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 检查版本号并按需导入。
     * - 全新安装（chunk 为空）：直接导入
     * - 已安装但版本落后：清空旧数据后重新导入
     * - 版本一致：跳过，无任何 IO 操作
     *
     * 在协程中调用，IO 分发到 Dispatchers.IO。
     */
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        val savedVersion = prefs.getInt(KEY_VERSION, 0)

        val needsSeed = when {
            guideChunkDao.countChunks() == 0 -> true          // 全新安装
            savedVersion < GUIDE_DATA_VERSION -> true          // 内容有更新
            else -> false                                       // 版本一致，跳过
        }

        if (!needsSeed) return@withContext

        val assetDb = openAssetDb() ?: return@withContext
        try {
            // 先清空旧数据，再重新写入（保证幂等）
            guideChunkDao.clearAll()
            copyChunksFromAsset(assetDb)
            // 只有成功写入后才更新版本号
            prefs.edit().putInt(KEY_VERSION, GUIDE_DATA_VERSION).apply()
        } finally {
            assetDb.close()
        }
    }

    /**
     * 强制重新导入（调试用）。
     * 无论版本是否一致，都清空并重新导入。
     */
    suspend fun forceReseed() = withContext(Dispatchers.IO) {
        val assetDb = openAssetDb() ?: return@withContext
        try {
            guideChunkDao.clearAll()
            copyChunksFromAsset(assetDb)
            prefs.edit().putInt(KEY_VERSION, GUIDE_DATA_VERSION).apply()
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

    companion object {
        /**
         * ⚠️ 每次修改指南内容并重新运行 scripts/build_guides_db.py 后，
         * 必须把这个数字 +1，已安装的设备才会在下次启动时自动更新。
         *
         * 当前版本：1（初始版本，包含 15 篇基础指南）
         */
        const val GUIDE_DATA_VERSION = 1

        private const val PREFS_NAME = "guide_chunk_seeder"
        private const val KEY_VERSION = "guide_data_version"
    }
}
