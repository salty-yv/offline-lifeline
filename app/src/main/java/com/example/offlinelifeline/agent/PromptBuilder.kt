package com.example.offlinelifeline.agent

import com.example.offlinelifeline.core.model.AgentContext
import com.example.offlinelifeline.core.model.ToolRecommendation

class PromptBuilder {

    /**
     * 构建发送给 LLM 的 System Instruction。
     *
     * 根据 [intent] 决定注入不同的输出格式要求：
     * - [UserIntent.EMERGENCY] / [UserIntent.UNKNOWN]：注入6段应急结构框架，要求追问处境
     * - [UserIntent.PROCEDURAL]：注入步骤列表格式，不追问电量/体力等应急信息
     */
    fun buildSystemInstruction(
        context: AgentContext,
        actionStructure: String,
        toolRecommendations: List<ToolRecommendation>,
        intent: UserIntent = UserIntent.UNKNOWN,
        imagePaths: List<String> = emptyList(),
        imageInputSupported: Boolean = false
    ): String {
        return buildString {
            appendLine("你是一个完全离线运行的自救助手。")
            appendLine("你不是医生、不是救援调度系统、不能替代专业救援。")
            appendLine("你的回答必须保守、短句、步骤化、可执行。")
            appendLine("信息不足时，先给立即行动，再问最多 3 个关键问题。")
            // 语言跟随：LLM 始终使用用户的输入语言回复
            appendLine("Always reply in the same language the user used. If the user wrote in English, reply in English. If the user wrote in Chinese, reply in Chinese.")
            // 纯文本约束：禁止 LaTeX、Markdown 特殊符号
            appendLine("Output plain text only. Do NOT use LaTeX, Markdown, or any special symbols such as $, \\, ^, →, ←, ✓, ★, or math notation like \\rightarrow or \\times. Use only numbered lists (1. 2. 3.) and plain dashes (-) for bullet points.")
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

            if (imagePaths.isNotEmpty()) {
                appendLine("[Image Input]")
                appendLine("User attached ${imagePaths.size} processed local image(s).")
                appendLine("Image files were compressed and re-encoded locally before inference.")
                if (imageInputSupported) {
                    appendLine("The current runtime may inspect the attached image content.")
                    appendLine("First describe only clearly visible content, then give conservative risk-oriented advice.")
                    appendLine("Do not make definitive medical diagnosis or definitive wild food safety judgment from the image.")
                } else {
                    appendLine("The current runtime cannot inspect image pixels.")
                    appendLine("Do not say you can see the image. Do not describe visual content.")
                    appendLine("Ask the user for a short text description of what is in the image, while still giving safe immediate advice based on their text.")
                }
                appendLine()
            }

            when (intent) {
                UserIntent.EMERGENCY, UserIntent.UNKNOWN -> {
                    // 应急场景：注入行动计划框架 + 工具推荐 + 6段结构要求
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
                    appendLine()

                    appendLine("[Required Output Format]")
                    appendLine("请严格按以下结构回答：")
                    appendLine("1. 先做这 3 步")
                    appendLine("2. 当前风险")
                    appendLine("3. 接下来几分钟")
                    appendLine("4. 不要做")
                    appendLine("5. 我还需要确认")
                    appendLine("6. 可使用的本地工具")
                }

                UserIntent.PROCEDURAL -> {
                    // 操作查询场景：不注入应急框架，改用步骤列表格式
                    appendLine("[Required Output Format]")
                    appendLine("用户在询问操作方法或制作步骤，请直接回答，不要套用应急框架。")
                    appendLine("请按以下格式回答：")
                    appendLine("1. 所需材料（如有）")
                    appendLine("2. 步骤（用编号列出，每步一行，简洁）")
                    appendLine("3. 注意事项（如有，不超过 3 条）")
                    appendLine("不要追问用户的电量、体力或装备，除非问题与处境直接相关。")
                }
            }
        }
    }

    fun buildSafetyInstruction(safetyInstruction: String): String = safetyInstruction
}
