package com.example.offlinelifeline.agent

/**
 * 用户意图分类器。
 *
 * 将用户输入区分为三类：
 * - [UserIntent.EMERGENCY]：用户描述当前危险处境（首次求助），需要应急行动计划框架。
 * - [UserIntent.PROCEDURAL]：用户询问如何完成某件事，需要操作步骤。
 * - [UserIntent.FOLLOW_UP]：已有对话上下文的追问或补充，LLM 应自然延续而非重新套框架。
 * - [UserIntent.UNKNOWN]：无法判断，降级为 EMERGENCY 处理，确保求救场景不漏判。
 *
 * 使用轻量规则分类，避免额外推理调用带来的延迟和电量消耗。
 */
enum class UserIntent {
    EMERGENCY,   // 首次求救 / 新的应急处境描述
    PROCEDURAL,  // 操作步骤 / 制作方法查询
    FOLLOW_UP,   // 对话追问 / 补充信息，上下文已建立
    FREE_CHAT,   // 自由对话：无格式约束，仅注入 RAG 上下文
    UNKNOWN      // 无法判断，降级为 EMERGENCY 处理
}

class IntentClassifier {

    /**
     * @param input 用户当前输入
     * @param previousTurns 已有的对话轮次（user 消息数量），0 表示首轮
     */
    fun classify(input: String, previousTurns: Int = 0): UserIntent {
        val lowerInput = input.lowercase()
        val hasHardEmergency = HARD_EMERGENCY_KEYWORDS.any { lowerInput.contains(it) }
        val hasEmergency = EMERGENCY_KEYWORDS.any { lowerInput.contains(it) }
        val hasProcedural = PROCEDURAL_KEYWORDS.any { lowerInput.contains(it) }
        val isShortFollowUp = input.trim().length <= FOLLOW_UP_MAX_LENGTH

        return when {
            // 无论轮次，含强烈求救词（救命/出血/骨折等）始终走 EMERGENCY
            hasHardEmergency -> UserIntent.EMERGENCY

            // 已有上下文 + 短句/追问词 → 判断为追问，让 LLM 自然延续
            previousTurns >= 1 && (isShortFollowUp || FOLLOW_UP_KEYWORDS.any { lowerInput.contains(it) }) ->
                UserIntent.FOLLOW_UP

            // 操作步骤类（在追问判断之后，避免追问被步骤格式打断）
            hasProcedural -> UserIntent.PROCEDURAL

            // 普通紧急词，首轮走 EMERGENCY，非首轮且没有新情况词走 FOLLOW_UP
            hasEmergency -> if (previousTurns == 0) UserIntent.EMERGENCY else UserIntent.FOLLOW_UP

            else -> if (previousTurns == 0) UserIntent.UNKNOWN else UserIntent.FOLLOW_UP
        }
    }

    private companion object {
        /**
         * 强烈求救词：无论轮次都触发 EMERGENCY 框架。
         * 只保留真正危及生命的高置信词，减少误判。
         */
        val HARD_EMERGENCY_KEYWORDS = listOf(
            // 中文
            "救命", "大量出血", "骨折", "失温", "溺水", "昏迷", "呼吸困难", "胸痛",
            "中毒", "蛇咬", "烧伤", "误食", "脱水严重",
            // 英文
            "mayday", "sos", "drowning", "unconscious", "heavy bleeding",
            "chest pain", "can't breathe", "fracture", "snakebite", "snake bite",
            "hypothermia", "heatstroke", "severe dehydration"
        )

        /** 普通紧急词：首轮触发 EMERGENCY，非首轮有新处境才触发 */
        val EMERGENCY_KEYWORDS = listOf(
            // 中文
            "迷路", "走丢", "找不到路", "受伤", "出血", "帮我",
            "中暑", "没电", "快没电", "低电量", "被困", "找不到出口",
            "脚扭", "地震", "洪水", "火灾", "雷暴", "头晕",
            // 英文
            "i'm lost", "i am lost", "injured", "bleeding", "help me",
            "help", "low battery", "no battery", "trapped", "stuck", "can't move",
            "earthquake", "flood", "fire", "lightning", "poisoned", "poisoning",
            "dehydrated", "dizzy", "burn", "burned", "i need help", "emergency",
            "frostbite", "out of water", "no water", "getting dark"
        )

        /** 操作步骤 / 制作方法关键词（中英双语） */
        val PROCEDURAL_KEYWORDS = listOf(
            // 中文
            "怎么", "如何", "方法", "步骤", "制造", "制作",
            "搭建", "建造", "处理方法", "教我", "教一下",
            "怎样", "怎样才能", "能不能教", "流程", "做法",
            "制备", "生火", "净化", "过滤", "包扎", "固定", "缝合",
            // 英文
            "how to", "how do i", "how can i", "how do you",
            "what is the way", "steps to", "guide to", "tutorial",
            "teach me", "show me", "method", "procedure", "process",
            "make a", "build a", "create a", "construct", "purify",
            "filter", "bandage", "splint", "start a fire", "make fire"
        )

        /** 追问特征词：含这些词时明显是在延续对话 */
        val FOLLOW_UP_KEYWORDS = listOf(
            // 中文
            "然后呢", "接下来", "那之后", "你说的", "你提到", "刚才", "刚说",
            "再说一下", "能详细", "详细说", "补充", "我知道了", "好的", "明白",
            "那如果", "但如果", "如果", "万一", "假设", "另外",
            "还有", "还需要", "继续", "下一步", "第几步",
            // 英文
            "then what", "what next", "after that", "you said", "you mentioned",
            "more detail", "can you explain", "got it", "okay", "understood",
            "what if", "but if", "also", "and then", "next step", "continue"
        )

        /** 短于此字数的输入，在有上下文时推测为追问 */
        const val FOLLOW_UP_MAX_LENGTH = 25
    }
}
