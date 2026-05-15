package com.example.offlinelifeline.data.db

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 虚拟表实体，镜像 [GuideChunkEntity] 的可检索字段。
 *
 * 使用 @Fts4 而非 @Fts5 以保证在 Android API 26+ 全系设备上的兼容性。
 *
 * 查询方式：
 * ```sql
 * SELECT * FROM guide_chunks_fts
 * WHERE guide_chunks_fts MATCH '"出血" OR "止血" OR "压迫"'
 * ```
 *
 * 注意：FTS 表不支持普通索引，排序和过滤依赖 MATCH 语法和查询构造。
 */
@Fts4
@Entity(tableName = "guide_chunks_fts")
data class GuideChunkFtsEntity(
    val chunkId: String,
    val guideId: String,
    val topic: String,
    val riskDomain: String,
    val title: String,
    val headingPath: String,
    val body: String,
    val tags: String
)
