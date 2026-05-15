package com.example.offlinelifeline.agent

import com.example.offlinelifeline.core.model.AgentContext
import com.example.offlinelifeline.core.model.ToolRecommendation

class PromptBuilder {

    /**
     * 构建发送给 LLM 的 System Instruction。
     *
     * 根据 [intent] 决定注入不同的输出格式要求：
     * - [UserIntent.EMERGENCY] / [UserIntent.UNKNOWN]：注入6段应急参考框架，格式为建议而非强制
     * - [UserIntent.FOLLOW_UP]：不注入任何格式要求，LLM 自然延续上下文作答
     * - [UserIntent.FREE_CHAT]：完全不注入格式和行动计划，仅保留 RAG 上下文和语言要求
     * - [UserIntent.PROCEDURAL]：注入步骤列表格式，不追问电量/体力等应急信息
     */

    fun buildSystemInstruction(
        context: AgentContext,
        actionStructure: String,
        toolRecommendations: List<ToolRecommendation>,
        intent: UserIntent = UserIntent.UNKNOWN,
        imagePaths: List<String> = emptyList(),
        imageInputSupported: Boolean = false,
        languageTag: String = "zh-CN",
        ragContext: String = ""          // 本地指南 chunk 拼接文本，空字符串表示无命中
    ): String {
        val useEnglish = languageTag.startsWith("en")
        return buildString {
            // ── 角色定义：FREE_CHAT 使用宽松通用助理人设，其余模式用应急助手人设 ──
            if (intent == UserIntent.FREE_CHAT) {
                appendLine("你是一个离线运行的智能助手，内置户外急救和野外生存知识库。")
                appendLine("你不是医生，不能替代专业救援，但可以自由地交流和回答各类问题。")
            } else {
                appendLine("你是一个完全离线运行的自救助手。")
                appendLine("你不是医生、不是救援调度系统、不能替代专业救援。")
                appendLine("你的回答必须保守、短句、步骤化、可执行。")
                appendLine("信息不足时，先给立即行动，再问最多 3 个关键问题。")
            }
            appendLine("[Reply Language]")
            appendLine(if (useEnglish) "English" else "Simplified Chinese")
            appendLine("Always reply in the reply language above, even if the user input uses another language.")
            // 纯文本约束：禁止 LaTeX、Markdown 特殊符号
            appendLine("Output plain text only. Do NOT use LaTeX, Markdown, or any special symbols such as $, \\, ^, →, ←, ✓, ★, or math notation like \\rightarrow or \\times. Use only numbered lists (1. 2. 3.) and plain dashes (-) for bullet points.")
            appendLine("Do NOT start your response with filler phrases such as '保持冷静', '不要慌', '深呼吸', '首先，保持冷静', 'Stay calm', 'Don't panic', or any reassurance opener. Every sentence must be a concrete, actionable instruction.")
            appendLine()

            appendLine("[Known Context]")
            if (context.knownFacts.isEmpty()) {
                appendLine(if (useEnglish) "No confirmed facts yet." else "暂无已确认事实。")
            } else {
                context.knownFacts.forEach { (key, value) ->
                    appendLine("- $key: $value")
                }
            }
            appendLine()

            // ── 本地指南 RAG 上下文 ──────────────────────────────────────────────
            // 仅在有命中内容时注入，避免浪费 context 窗口
            if (ragContext.isNotBlank()) {
                appendLine("[Local Guide Context]")
                appendLine("以下是软件内置的本地指南片段，直接来自离线知识库：")
                appendLine(ragContext)
                appendLine()
                appendLine(if (useEnglish) {
                    "Priority: Base your answer on the Local Guide Context above. " +
                    "If it does not cover the situation, do not invent specific medical steps."
                } else {
                    "优先依据以上本地指南回答。如果本地指南没有覆盖，不要编造具体医疗操作。"
                })
                appendLine()
            }

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
                        appendLine(if (useEnglish) "No recommended tools." else "暂无推荐工具。")
                    } else {
                        toolRecommendations.forEach {
                            appendLine("- ${it.toolType.name}: ${it.reason}")
                        }
                    }
                    appendLine()

                    appendLine("[Output Format Reference]")
                    if (useEnglish) {
                        appendLine("This is a reference framework, not a rigid template. Adapt based on what the user actually asked.")
                        appendLine("If the situation is simple or the user asked a direct question, answer directly in 2-3 sentences.")
                        appendLine("If the situation is complex and unclear, you may use this structure as a guide:")
                        appendLine("- Immediate actions (most critical first)")
                        appendLine("- Current risks (only if not already obvious)")
                        appendLine("- Next steps")
                        appendLine("- What NOT to do (only if there's a real danger of wrong action)")
                        appendLine("- Questions to ask (only the 1-2 most critical unknowns)")
                        appendLine("Never force all sections if they are not needed.")
                    } else {
                        appendLine("以下是参考框架，不是必须套用的模板。根据用户实际提问灵活调整。")
                        appendLine("如果情况简单或用户问的是具体问题，直接用 2-3 句话回答即可。")
                        appendLine("如果处境复杂且信息不足，可参考以下结构组织回答：")
                        appendLine("- 立即行动（最重要的放第一）")
                        appendLine("- 当前风险（仅当用户未提及时才列出）")
                        appendLine("- 接下来要做的")
                        appendLine("- 不要做的（仅当存在明显错误操作风险时才列出）")
                        appendLine("- 需要确认的问题（最多问 1-2 个最关键的）")
                        appendLine("不需要的部分直接省略，不要强行凑满所有段落。")
                    }
                }

                UserIntent.FOLLOW_UP -> {
                    // 追问场景：完全不注入格式约束，让 LLM 基于上下文自然作答
                    if (useEnglish) {
                        appendLine("The user is continuing the conversation based on previous context.")
                        appendLine("Answer naturally and directly. Do NOT restart the emergency framework.")
                        appendLine("Refer back to what was already discussed and build on it.")
                        appendLine("If the user's question is simple, answer in 1-2 sentences.")
                    } else {
                        appendLine("用户正在基于之前对话的上下文继续提问或补充信息。")
                        appendLine("请自然地延续对话，直接回答用户当前的问题，不要重新套应急框架。")
                        appendLine("可以引用之前已知的情况（如位置、伤情、装备），体现对话的连贯性。")
                        appendLine("如果问题简单，1-2 句话直接回答即可。")
                    }
                }

                UserIntent.PROCEDURAL -> {
                    // 操作查询场景：不注入应急框架，改用步骤列表格式
                    appendLine("[Required Output Format]")
                    if (useEnglish) {
                        appendLine("The user is asking for a procedure or how-to steps. Answer directly without the emergency framework.")
                        appendLine("Use this format:")
                        appendLine("1. Materials needed, if any")
                        appendLine("2. Steps, numbered and concise")
                        appendLine("3. Notes, if any, no more than 3")
                        appendLine("Do not ask about battery, strength, or gear unless directly relevant.")
                    } else {
                        appendLine("用户在询问操作方法或制作步骤，请直接回答，不要套用应急框架。")
                        appendLine("请按以下格式回答：")
                        appendLine("1. 所需材料（如有）")
                        appendLine("2. 步骤（用编号列出，每步一行，简洁）")
                        appendLine("3. 注意事项（如有，不超过 3 条）")
                        appendLine("不要追问用户的电量、体力或装备，除非问题与处境直接相关。")
                    }
                }
                UserIntent.FREE_CHAT -> {
                    // 自由对话：完全不注入任何格式和行动计划约束
                    // RAG 上下文已在上方 [Local Guide Context] 节い进入，这里只告知 LLM 延续对话即可
                    if (useEnglish) {
                        appendLine("You are in free chat mode. The user is having a casual or exploratory conversation.")
                        appendLine("Answer naturally and conversationally. You may reference the Local Guide Context above if it is helpful.")
                        appendLine("Do not apply any emergency framework, numbered structure, or fixed output template.")
                    } else {
                        appendLine("当前处于自由对话模式。用户在进行随意性或探索性的提问。")
                        appendLine("请自然地以对话语气回答。如果上方有本地指南内容，可以引用但不强要。")
                        appendLine("不要套用应急框架、编号结构或任何固定模板。")
                    }
                }
            }
        }
    }

    fun buildSafetyInstruction(safetyInstruction: String): String = safetyInstruction
}
