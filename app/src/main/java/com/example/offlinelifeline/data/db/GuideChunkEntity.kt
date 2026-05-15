package com.example.offlinelifeline.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * RAG 检索用的指南分块实体。
 *
 * 每篇指南（[GuideEntity]）按 Markdown 二级标题拆分为多个 chunk，
 * 供 Agent 在调用 LLM 前进行本地全文检索，再将相关片段注入 Prompt。
 *
 * 数据由构建期脚本 scripts/build_guides_db.py 生成，
 * 随 APK 内置于 assets/databases/offline_guides.db，无需运行时联网。
 */
@Entity(
    tableName = "guide_chunks",
    indices = [
        Index("guideId"),
        Index("topic"),
        Index("riskDomain")
    ]
)
data class GuideChunkEntity(
    /** 唯一片段 ID，格式：{guideId}_{三位序号}，例如 "bleeding_001"。 */
    @PrimaryKey val chunkId: String,

    /** 对应整篇指南的 ID，例如 "bleeding"。 */
    val guideId: String,

    /** 大类 topic，与 Markdown Front Matter 一致，例如 "medical"、"disaster"。 */
    val topic: String,

    /** 主要风险领域，对应 [com.example.offlinelifeline.core.model.RiskDomain] 枚举名称。 */
    val riskDomain: String,

    /** 指南标题，例如 "出血"。 */
    val title: String,

    /** 该 chunk 在指南中的标题路径，例如 "出血 > 先做这 3 步"。 */
    val headingPath: String,

    /** 真正注入 LLM Prompt 的短文本，150-500 中文字左右。 */
    val body: String,

    /** 逗号分隔的标签，用于 FTS 补偿中文分词不足，例如 "出血,止血,伤口,压迫"。 */
    val tags: String,

    /** 内容优先级，1-5，高风险内容优先级更高。 */
    val priority: Int,

    /** 该 chunk 在同篇指南中的顺序索引（从 0 开始）。 */
    val chunkIndex: Int,

    /** 内容版本号，方便测试追踪 chunk 的迭代历史。 */
    val contentVersion: Int,

    /** 内容生成时间戳（毫秒）。 */
    val updatedAtMillis: Long
)
