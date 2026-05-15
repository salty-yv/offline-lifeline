package com.example.offlinelifeline.agent.rag

import com.example.offlinelifeline.data.db.GuideChunkFtsEntity

/**
 * 多路检索结果融合排序器。
 *
 * 把来自不同检索路径（全库 FTS、topic 定向 FTS、riskDomain 兜底）的结果
 * 合并去重，并按"出现在越多路径中得分越高"的思路排序，
 * 同分时以 topic 匹配优先。
 *
 * 该实现不依赖向量模型，仅用 chunkId 做去重和计数，轻量高效。
 */
object RankFusion {

    /**
     * 融合多路检索结果。
     *
     * @param resultLists    各路检索结果的列表（允许重复 chunkId）
     * @param preferredTopic 优先展示的 topic（当分数相同时排前面）
     * @param limit          最终返回结果数上限
     * @return 去重、排序后的 chunk 列表
     */
    fun merge(
        resultLists: List<List<GuideChunkFtsEntity>>,
        preferredTopic: String? = null,
        limit: Int = 5
    ): List<GuideChunkFtsEntity> {
        // chunkId → (entity, 命中路径数)
        val scoreMap = mutableMapOf<String, Pair<GuideChunkFtsEntity, Int>>()

        for (list in resultLists) {
            for (entity in list) {
                val existing = scoreMap[entity.chunkId]
                if (existing == null) {
                    scoreMap[entity.chunkId] = entity to 1
                } else {
                    scoreMap[entity.chunkId] = existing.first to (existing.second + 1)
                }
            }
        }

        return scoreMap.values
            .sortedWith(
                compareByDescending<Pair<GuideChunkFtsEntity, Int>> { (_, score) -> score }
                    .thenByDescending { (entity, _) ->
                        if (preferredTopic != null && entity.topic == preferredTopic) 1 else 0
                    }
            )
            .take(limit)
            .map { (entity, _) -> entity }
    }
}
