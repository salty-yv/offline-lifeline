package com.example.offlinelifeline.agent

import com.example.offlinelifeline.core.model.AgentContext
import com.example.offlinelifeline.core.model.RiskDomain

class ActionPlanner {
    fun buildActionStructure(
        riskDomains: Set<RiskDomain>,
        context: AgentContext,
        followUpQuestions: List<String> = emptyList(),
        languageTag: String = "zh-CN"
    ): String {
        if (languageTag.startsWith("en")) {
            return buildEnglishActionStructure(riskDomains, context)
        }
        return buildString {
            appendLine("先做这 3 步")
            appendLine("1. 停下来，先确认脚下、头顶和周围没有立即危险。")
            appendLine("2. 降低手机亮度，保存电量，只保留必要通信和定位能力。")
            appendLine("3. 如果受伤，先坐稳或靠稳，避免继续加重伤势。")
            appendLine()

            appendLine("当前风险")
            appendLine(riskDomains.formatRiskNames())
            appendLine()

            appendLine("接下来几分钟")
            appendLine(buildNextMinutes(riskDomains, context))
            appendLine()

            appendLine("不要做")
            appendLine(buildDoNot(riskDomains))
            appendLine()

            appendLine("我还需要确认")
            if (followUpQuestions.isEmpty()) {
                appendLine("暂时没有必须追问的信息。")
            } else {
                followUpQuestions.forEachIndexed { index, question ->
                    appendLine("${index + 1}. $question")
                }
            }
            appendLine()

            appendLine("什么时候必须求救")
            appendLine("如果出现大量出血、意识模糊、呼吸困难、胸痛、无法移动、体温快速下降，尽快联系救援或让同伴求助。")
        }.trim()
    }

    private fun buildEnglishActionStructure(
        riskDomains: Set<RiskDomain>,
        context: AgentContext
    ): String {
        return buildString {
            appendLine("Do these 3 things first")
            appendLine("1. Stop and check that there is no immediate danger underfoot, overhead, or nearby.")
            appendLine("2. Lower phone brightness and preserve power for essential communication and location use.")
            appendLine("3. If you are injured, sit or brace yourself before moving again.")
            appendLine()

            appendLine("Current risks")
            appendLine(riskDomains.joinToString(", ") { it.name })
            appendLine()

            appendLine("Next few minutes")
            appendLine(buildEnglishNextMinutes(riskDomains, context))
            appendLine()

            appendLine("Do not")
            appendLine(buildEnglishDoNot(riskDomains))
            appendLine()

            appendLine("When to seek help immediately")
            appendLine("If there is heavy bleeding, confusion, trouble breathing, chest pain, inability to move, or rapidly dropping body temperature, contact rescue services or ask a companion for help as soon as possible.")
        }.trim()
    }

    private fun buildNextMinutes(riskDomains: Set<RiskDomain>, context: AgentContext): String {
        val lines = mutableListOf<String>()
        if (RiskDomain.LOST in riskDomains) {
            lines += "不要盲目赶路，选择相对安全、避风、容易被看到的位置。"
        }
        if (RiskDomain.INJURY in riskDomains || RiskDomain.FRACTURE in riskDomains) {
            lines += "减少受伤部位负重，能固定就先固定，疼痛明显时不要强行行走。"
        }
        if (RiskDomain.LOW_BATTERY in riskDomains || (context.batteryPercent != null && context.batteryPercent <= 20)) {
            lines += "先准备一条包含位置、伤情、电量的短求救信息，减少连续对话。"
        }
        if (RiskDomain.NIGHT in riskDomains) {
            lines += "天黑前优先保暖、避风、制造明显求救标记。"
        }
        return lines.ifEmpty { listOf("先稳定处境，再补充关键信息。") }.joinToString("\n")
    }

    private fun buildDoNot(riskDomains: Set<RiskDomain>): String {
        val lines = mutableListOf("不要为了找路继续消耗体力或电量。")
        if (RiskDomain.INJURY in riskDomains || RiskDomain.FRACTURE in riskDomains) {
            lines += "不要强行负重行走。"
        }
        if (RiskDomain.LOW_BATTERY in riskDomains) {
            lines += "不要长时间亮屏、反复刷新或连续生成长回答。"
        }
        if (RiskDomain.NIGHT in riskDomains) {
            lines += "不要在看不清路况时下坡、过河或穿越复杂地形。"
        }
        return lines.joinToString("\n")
    }

    private fun buildEnglishNextMinutes(riskDomains: Set<RiskDomain>, context: AgentContext): String {
        val lines = mutableListOf<String>()
        if (RiskDomain.LOST in riskDomains) {
            lines += "Do not keep walking blindly. Choose a safer, sheltered place that is easier to see."
        }
        if (RiskDomain.INJURY in riskDomains || RiskDomain.FRACTURE in riskDomains) {
            lines += "Reduce weight on the injured area. Stabilize it if possible and do not force walking through strong pain."
        }
        if (RiskDomain.LOW_BATTERY in riskDomains || (context.batteryPercent != null && context.batteryPercent <= 20)) {
            lines += "Prepare one short help message with location clues, injuries, and battery level. Avoid long repeated chats."
        }
        if (RiskDomain.NIGHT in riskDomains) {
            lines += "Before it gets darker, prioritize warmth, wind protection, and a visible help signal."
        }
        return lines.ifEmpty { listOf("Stabilize the situation first, then add the key missing details.") }.joinToString("\n")
    }

    private fun buildEnglishDoNot(riskDomains: Set<RiskDomain>): String {
        val lines = mutableListOf("Do not spend strength or battery wandering to look for a route.")
        if (RiskDomain.INJURY in riskDomains || RiskDomain.FRACTURE in riskDomains) {
            lines += "Do not force weight onto an injured limb."
        }
        if (RiskDomain.LOW_BATTERY in riskDomains) {
            lines += "Do not keep the screen bright or generate long answers repeatedly."
        }
        if (RiskDomain.NIGHT in riskDomains) {
            lines += "Do not go downhill, cross water, or move through complex terrain when visibility is poor."
        }
        return lines.joinToString("\n")
    }

    private fun Set<RiskDomain>.formatRiskNames(): String {
        return joinToString("、") { it.name }
    }
}
