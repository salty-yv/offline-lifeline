package com.example.offlinelifeline.agent.rag

import com.example.offlinelifeline.core.model.RiskDomain
import com.example.offlinelifeline.data.db.GuideChunkDao
import com.example.offlinelifeline.data.db.GuideChunkFtsEntity

/**
 * 本地指南 RAG 检索服务。
 *
 * 采用"三路召回 + 融合排序"策略：
 *
 * ```
 * 路径 1：关键词提取 + FTS 全库搜索
 * 路径 2：QueryExpander 扩展词 + FTS 全库搜索
 * 路径 3：riskDomain 定向检索（兜底，保证至少有相关内容）
 *         ↓
 * RankFusion 去重、排序
 *         ↓
 * 返回 top-N chunk，供 PromptBuilder 注入 LLM Prompt
 * ```
 *
 * 所有检索均在本地 SQLite FTS4 表上进行，不联网。
 */
class GuideRetrievalService(
    private val dao: GuideChunkDao
) {
    /**
     * 执行检索，返回与用户输入最相关的 chunk 列表。
     *
     * @param userInput   用户原始输入文本
     * @param riskDomains 已由 RiskClassifier 识别的风险域集合
     * @param limit       最终返回数量上限，默认 5
     * @return 排序后的相关 chunk 列表，可能为空（但概率极低）
     */
    suspend fun retrieve(
        userInput: String,
        riskDomains: Set<RiskDomain>,
        limit: Int = 5
    ): List<GuideChunkFtsEntity> {

        val preferredTopic = RiskTopicRouter.primaryTopicFor(riskDomains)

        // ── 路径 1：提取关键词 → FTS 全库 ────────────────────────────────────
        val keywords = KeywordExtractor.extract(userInput)
        val keywordQuery = QueryExpander.toFtsOrQuery(keywords)
        val keywordHits = if (keywordQuery.isNotBlank()) {
            dao.searchFts(keywordQuery, limit = 8)
        } else emptyList()

        // ── 路径 2：扩展词 → FTS 全库 ────────────────────────────────────────
        val expandedTerms = QueryExpander.expandTerms(userInput)
        val expandedQuery = QueryExpander.toFtsOrQuery(expandedTerms)
        val expandedHits = if (expandedQuery.isNotBlank()) {
            if (preferredTopic != null) {
                // 优先在 topic 内检索，召回更精准
                dao.searchFtsByTopic(expandedQuery, preferredTopic, limit = 8)
            } else {
                dao.searchFts(expandedQuery, limit = 8)
            }
        } else emptyList()

        // ── 路径 3：riskDomain 兜底 ──────────────────────────────────────────
        // 即使 FTS 完全没命中，也能通过 riskDomain 返回最相关的指南
        val domainHits = riskDomains
            .filter { it != RiskDomain.UNKNOWN }
            .take(2)                                      // 最多取两个 domain 兜底
            .flatMap { domain ->
                dao.getByRiskDomain(domain.name, limit = 4)
            }

        // ── 融合排序 ──────────────────────────────────────────────────────────
        val merged = RankFusion.merge(
            resultLists = listOf(keywordHits, expandedHits, domainHits),
            preferredTopic = preferredTopic,
            limit = limit
        )

        // ── 最终兜底：如果所有路径都没命中，返回 domainHits 前几条 ──────────
        return merged.ifEmpty {
            domainHits.distinctBy { it.chunkId }.take(limit)
        }
    }

    companion object {
        /**
         * 把检索结果格式化为可注入 Prompt 的文本块。
         *
         * 格式：
         * ```
         * 来源：出血 > 先做这 3 步
         * 内容：用干净布料直接压住出血点。能做到时让伤处高于心脏...
         * ```
         */
        fun buildLocalGuideContext(chunks: List<GuideChunkFtsEntity>): String =
            chunks.joinToString("\n\n") { chunk ->
                "来源：${chunk.title} > ${chunk.headingPath.substringAfterLast(" > ")}\n内容：${chunk.body}"
            }

        /**
         * 把检索结果转换为引用信息列表，供 AgentResponse.citations 使用。
         */
        fun buildCitations(chunks: List<GuideChunkFtsEntity>): List<GuideCitation> =
            chunks.map { chunk ->
                GuideCitation(
                    guideId = chunk.guideId,
                    chunkId = chunk.chunkId,
                    title = chunk.title,
                    headingPath = chunk.headingPath
                )
            }
    }
}

/**
 * 本地指南引用信息，用于在 UI 展示"依据本地指南"来源。
 */
data class GuideCitation(
    val guideId: String,
    val chunkId: String,
    val title: String,
    val headingPath: String
)
