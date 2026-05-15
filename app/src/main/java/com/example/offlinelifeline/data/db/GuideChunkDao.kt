package com.example.offlinelifeline.data.db

import androidx.room.Dao
import androidx.room.Query

/**
 * RAG 指南分块的数据访问接口。
 *
 * 所有检索均基于 FTS4 虚拟表 [GuideChunkFtsEntity]，
 * 支持 OR 关键词查询和 topic 定向过滤。
 *
 * 查询字符串由上层 [com.example.offlinelifeline.agent.rag.QueryExpander] 构造，
 * 格式为：`"关键词1" OR "关键词2" OR "关键词3"`。
 */
@Dao
interface GuideChunkDao {

    /**
     * 全库 FTS 检索，不限 topic。
     *
     * @param ftsQuery FTS MATCH 查询字符串，例如 `"出血" OR "止血" OR "压迫"`
     * @param limit 最多返回结果数
     */
    @Query(
        """
        SELECT * FROM guide_chunks_fts
        WHERE guide_chunks_fts MATCH :ftsQuery
        LIMIT :limit
        """
    )
    suspend fun searchFts(ftsQuery: String, limit: Int): List<GuideChunkFtsEntity>

    /**
     * Topic 定向 FTS 检索，在指定大类（medical / navigation / disaster / device）中搜索。
     *
     * @param ftsQuery FTS MATCH 查询字符串
     * @param topic    大类，例如 "medical"
     * @param limit    最多返回结果数
     */
    @Query(
        """
        SELECT * FROM guide_chunks_fts
        WHERE guide_chunks_fts MATCH :ftsQuery
          AND topic = :topic
        LIMIT :limit
        """
    )
    suspend fun searchFtsByTopic(
        ftsQuery: String,
        topic: String,
        limit: Int
    ): List<GuideChunkFtsEntity>

    /**
     * 根据 riskDomain 直接返回全部相关 chunk，用于 RiskDomain 路由兜底。
     *
     * @param riskDomain RiskDomain 枚举名称字符串，例如 "BLEEDING"
     * @param limit      最多返回结果数
     */
    @Query(
        """
        SELECT * FROM guide_chunks_fts
        WHERE riskDomain = :riskDomain
        LIMIT :limit
        """
    )
    suspend fun getByRiskDomain(riskDomain: String, limit: Int): List<GuideChunkFtsEntity>

    /**
     * 根据 chunkId 精确获取单个 chunk（调试、测试用）。
     */
    @Query("SELECT * FROM guide_chunks_fts WHERE chunkId = :chunkId LIMIT 1")
    suspend fun getChunkById(chunkId: String): GuideChunkFtsEntity?

    /**
     * 获取当前 FTS 表的 chunk 总数（可用于启动时数据健康检查）。
     */
    @Query("SELECT COUNT(*) FROM guide_chunks_fts")
    suspend fun countChunks(): Int
}
