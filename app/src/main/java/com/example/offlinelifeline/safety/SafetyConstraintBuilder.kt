package com.example.offlinelifeline.safety

import com.example.offlinelifeline.core.model.RiskDomain

class SafetyConstraintBuilder {
    fun buildSafetyInstruction(riskDomains: Set<RiskDomain>): String {
        val activeRules = SafetyRules.forRisks(riskDomains)

        return buildString {
            appendLine("你必须给出保守、可执行的建议。")
            appendLine("你不是医生、不是救援调度系统，不能替代专业救援。")
            appendLine("如果信息不足，先给立即行动，再问最多 3 个关键问题。")
            appendLine("本地工具只能推荐，不能声称已经自动执行。")

            if (activeRules.isNotEmpty()) {
                appendLine()
                appendLine("禁止动作：")
                activeRules
                    .flatMap { it.bannedPatterns }
                    .distinct()
                    .forEach { appendLine("- 不得建议或暗示：$it") }

                appendLine()
                appendLine("必要提醒：")
                activeRules
                    .flatMap { it.requiredReminders }
                    .distinct()
                    .forEach { appendLine("- $it") }
            }
        }.trim()
    }
}
