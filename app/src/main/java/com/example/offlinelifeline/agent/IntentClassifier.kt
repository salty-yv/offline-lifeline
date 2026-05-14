package com.example.offlinelifeline.agent

/**
 * 用户意图分类器。
 *
 * 将用户输入区分为两类：
 * - [UserIntent.EMERGENCY]：用户描述当前危险处境，需要应急行动计划。
 * - [UserIntent.PROCEDURAL]：用户询问如何完成某件事，需要操作步骤。
 * - [UserIntent.UNKNOWN]：无法判断，降级为 EMERGENCY 处理，确保求救场景不漏判。
 *
 * 使用轻量规则分类，避免额外推理调用带来的延迟和电量消耗。
 */
enum class UserIntent {
    EMERGENCY,   // 求救 / 应急处境描述
    PROCEDURAL,  // 操作步骤 / 制作方法查询
    UNKNOWN      // 无法判断，降级为 EMERGENCY 处理
}

class IntentClassifier {

    fun classify(input: String): UserIntent {
        val emergencyScore = EMERGENCY_KEYWORDS.count { input.contains(it) }
        val proceduralScore = PROCEDURAL_KEYWORDS.count { input.contains(it) }

        return when {
            // 同时出现求救词时，优先判断为 EMERGENCY（如"我受伤了，怎么止血？"）
            emergencyScore > 0 -> UserIntent.EMERGENCY
            proceduralScore > 0 -> UserIntent.PROCEDURAL
            else -> UserIntent.UNKNOWN
        }
    }

    private companion object {
        /** 求救 / 应急处境关键词（中英双语） */
        val EMERGENCY_KEYWORDS = listOf(
            // 中文
            "迷路", "走丢", "找不到路", "受伤", "出血", "帮我", "怎么办",
            "救命", "失温", "中暑", "蛇咬", "没电", "快没电", "低电量",
            "被困", "找不到出口", "脚扭", "骨折", "烧伤", "溺水",
            "地震", "洪水", "火灾", "雷暴", "中毒", "误食", "脱水",
            "头晕", "昏迷", "呼吸困难", "胸痛", "大量出血",
            // 英文
            "i'm lost", "i am lost", "lost", "injured", "bleeding", "help me",
            "help", "hypothermia", "heatstroke", "snake bite", "snakebite",
            "low battery", "no battery", "trapped", "stuck", "can't move",
            "earthquake", "flood", "fire", "lightning", "poisoned", "poisoning",
            "dehydrated", "dizzy", "unconscious", "chest pain", "drowning",
            "broken", "fracture", "burn", "burned", "i need help", "emergency",
            "frostbite", "out of water", "no water", "getting dark", "night"
        )

        /** 操作步骤 / 制作方法关键词（中英双语） */
        val PROCEDURAL_KEYWORDS = listOf(
            // 中文
            "怎么", "如何", "方法", "步骤", "制造", "制作",
            "搭建", "建造", "处理方法", "教我", "教一下",
            "怎样", "怎样才能", "能不能教", "流程", "做法",
            "制备", "搭", "建", "造", "生火", "净化", "过滤",
            "包扎", "固定", "缝合",
            // 英文
            "how to", "how do i", "how can i", "how do you",
            "what is the way", "steps to", "guide to", "tutorial",
            "teach me", "show me", "method", "procedure", "process",
            "make a", "build a", "create a", "construct", "purify",
            "filter", "bandage", "splint", "start a fire", "make fire"
        )
    }
}
