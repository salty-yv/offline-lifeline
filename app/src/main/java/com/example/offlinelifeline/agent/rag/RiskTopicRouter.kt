package com.example.offlinelifeline.agent.rag

import com.example.offlinelifeline.core.model.RiskDomain

/**
 * 把已识别的 [RiskDomain] 映射到指南 topic 大类。
 *
 * topic 与 Markdown Front Matter 中的 topic 字段保持一致：
 * - medical    ← 医疗类风险
 * - navigation ← 导航/迷路类
 * - disaster   ← 自然灾害类
 * - device     ← 设备/电量类
 *
 * 返回 null 表示无对应 topic，检索时将退化为全库 FTS 搜索。
 */
object RiskTopicRouter {

    fun topicFor(domain: RiskDomain): String? = when (domain) {
        RiskDomain.BLEEDING,
        RiskDomain.BURN,
        RiskDomain.INJURY,
        RiskDomain.FRACTURE,
        RiskDomain.SNAKE_BITE,
        RiskDomain.POISONING,
        RiskDomain.HYPOTHERMIA,
        RiskDomain.HEATSTROKE,
        RiskDomain.DEHYDRATION -> "medical"

        RiskDomain.LOST -> "navigation"

        RiskDomain.FLOOD,
        RiskDomain.FIRE,
        RiskDomain.THUNDERSTORM,
        RiskDomain.EARTHQUAKE -> "disaster"

        RiskDomain.LOW_BATTERY,
        RiskDomain.NIGHT -> "device"

        RiskDomain.UNKNOWN -> null
    }

    /**
     * 给一批 RiskDomain 选出优先级最高的那个 topic。
     * 优先顺序：medical > disaster > navigation > device。
     */
    fun primaryTopicFor(domains: Set<RiskDomain>): String? {
        val priority = listOf("medical", "disaster", "navigation", "device")
        val topics = domains.mapNotNull { topicFor(it) }.toSet()
        return priority.firstOrNull { it in topics }
    }
}
