package com.example.offlinelifeline.agent

import com.example.offlinelifeline.core.model.AgentContext
import com.example.offlinelifeline.core.model.ChatMessage
import com.example.offlinelifeline.core.model.ChatRole

class ContextManager {
    fun buildContext(messages: List<ChatMessage>): AgentContext {
        val userText = messages
            .filter { it.role == ChatRole.USER }
            .joinToString(separator = "\n") { it.text }

        val batteryPercent = BatteryRegex.find(userText)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.coerceIn(0, 100)

        val knownFacts = buildMap {
            batteryPercent?.let { put("battery_percent", "$it%") }
            inferCanMove(userText)?.let { put("user_can_move", it.toString()) }
            inferHasWater(userText)?.let { put("has_water", it.toString()) }
            inferHasWarmClothes(userText)?.let { put("has_warm_clothes", it.toString()) }
        }

        val missingFacts = buildList {
            if (batteryPercent == null) add("battery_percent")
            if (inferCanMove(userText) == null) add("user_can_move")
            if (inferHasWater(userText) == null) add("has_water")
            if (inferHasWarmClothes(userText) == null) add("has_warm_clothes")
        }

        return AgentContext(
            knownFacts = knownFacts,
            missingFacts = missingFacts,
            batteryPercent = batteryPercent,
            userCanMove = inferCanMove(userText),
            hasWater = inferHasWater(userText),
            hasWarmClothes = inferHasWarmClothes(userText)
        )
    }

    fun summarizeIfNeeded(messages: List<ChatMessage>): List<ChatMessage> {
        return if (messages.size <= MAX_CONTEXT_MESSAGES) {
            messages
        } else {
            messages.takeLast(MAX_CONTEXT_MESSAGES)
        }
    }

    private fun inferCanMove(text: String): Boolean? {
        return when {
            text.containsAny(
                "不能动", "走不了", "无法移动", "不能移动", "不能负重",
                "can't move", "cannot move", "can't walk", "unable to walk",
                "immobile", "can't stand"
            ) -> false
            text.containsAny(
                "能走", "可以走", "能移动", "还能走", "能缓慢移动",
                "can walk", "able to walk", "can move", "can still walk", "slowly walking"
            ) -> true
            else -> null
        }
    }

    private fun inferHasWater(text: String): Boolean? {
        return when {
            text.containsAny(
                "没水", "没有水", "无水",
                "no water", "out of water", "no drinking water", "ran out of water"
            ) -> false
            text.containsAny(
                "有水", "带了水", "水壶", "矿泉水",
                "have water", "got water", "water bottle", "bottled water", "canteen"
            ) -> true
            else -> null
        }
    }

    private fun inferHasWarmClothes(text: String): Boolean? {
        return when {
            text.containsAny(
                "没带衣服", "没有保暖", "没保暖",
                "no warm clothes", "no jacket", "nothing warm", "no gear"
            ) -> false
            text.containsAny(
                "保暖", "外套", "冲锋衣", "毯子", "睡袋", "干衣服",
                "jacket", "coat", "blanket", "sleeping bag", "warm clothes", "warm gear"
            ) -> true
            else -> null
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { contains(it, ignoreCase = true) }
    }

    private companion object {
        const val MAX_CONTEXT_MESSAGES = 20
        val BatteryRegex = Regex("""(\d{1,3})\s*%""")
    }
}
