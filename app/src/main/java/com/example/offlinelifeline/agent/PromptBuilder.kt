package com.example.offlinelifeline.agent

import com.example.offlinelifeline.core.model.AgentContext
import com.example.offlinelifeline.core.model.ToolRecommendation

class PromptBuilder {
    fun buildSystemInstruction(
        context: AgentContext,
        actionStructure: String,
        toolRecommendations: List<ToolRecommendation>
    ): String {
        return buildString {
            appendLine("你是一个完全离线运行的自救助手。")
            appendLine("你不是医生、不是救援调度系统、不能替代专业救援。")
            appendLine("你的回答必须保守、短句、步骤化、可执行。")
            appendLine("信息不足时，先给立即行动，再问最多 3 个关键问题。")
            appendLine()
            appendLine("[Known Context]")
            if (context.knownFacts.isEmpty()) {
                appendLine("暂无已确认事实。")
            } else {
                context.knownFacts.forEach { (key, value) ->
                    appendLine("- $key: $value")
                }
            }
            appendLine()
            appendLine("[Agent Action Plan]")
            appendLine(actionStructure)
            appendLine()
            appendLine("[Available Local Tools]")
            if (toolRecommendations.isEmpty()) {
                appendLine("暂无推荐工具。")
            } else {
                toolRecommendations.forEach {
                    appendLine("- ${it.toolType.name}: ${it.reason}")
                }
            }
        }
    }

    fun buildSafetyInstruction(safetyInstruction: String): String = safetyInstruction
}
