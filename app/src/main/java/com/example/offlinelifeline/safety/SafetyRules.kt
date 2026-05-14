package com.example.offlinelifeline.safety

import com.example.offlinelifeline.core.model.RiskDomain

internal object SafetyRules {
    private val rules = listOf(
        SafetyRule(
            riskDomain = RiskDomain.POISONING,
            bannedPatterns = listOf("可以吃", "能吃", "安全食用", "尝一口", "继续食用"),
            requiredReminders = listOf("不要继续食用，保留样本或照片，尽快求助")
        ),
        SafetyRule(
            riskDomain = RiskDomain.SNAKE_BITE,
            bannedPatterns = listOf("吸出蛇毒", "吸蛇毒", "切开伤口", "放血", "用嘴吸"),
            requiredReminders = listOf("减少活动，固定患肢，尽快求救")
        ),
        SafetyRule(
            riskDomain = RiskDomain.BLEEDING,
            bannedPatterns = listOf("继续赶路", "继续冒险移动", "放血"),
            requiredReminders = listOf("压迫止血，尽快求救")
        ),
        SafetyRule(
            riskDomain = RiskDomain.HYPOTHERMIA,
            bannedPatterns = listOf("喝酒保暖", "饮酒保暖"),
            requiredReminders = listOf("保持干燥、避风、保温")
        ),
        SafetyRule(
            riskDomain = RiskDomain.HEATSTROKE,
            bannedPatterns = listOf("继续暴晒", "坚持晒着"),
            requiredReminders = listOf("降温、补水、避免继续暴晒")
        ),
        SafetyRule(
            riskDomain = RiskDomain.FLOOD,
            bannedPatterns = listOf("涉水穿越", "强行过河", "趟过洪水"),
            requiredReminders = listOf("不要涉水强行穿越")
        ),
        SafetyRule(
            riskDomain = RiskDomain.THUNDERSTORM,
            bannedPatterns = listOf("躲在大树下", "躲到孤立大树下"),
            requiredReminders = listOf("远离孤立大树、金属物和开阔高地")
        ),
        SafetyRule(
            riskDomain = RiskDomain.LOW_BATTERY,
            bannedPatterns = listOf("持续长时间对话", "一直开着屏幕"),
            requiredReminders = listOf("降亮度、关高耗电功能、减少连续对话")
        ),
        SafetyRule(
            riskDomain = RiskDomain.LOST,
            bannedPatterns = listOf("盲目移动", "随便找路"),
            requiredReminders = listOf("停止盲目移动，保存体力，制造求救信号")
        )
    )

    fun forRisks(riskDomains: Set<RiskDomain>): List<SafetyRule> {
        return rules.filter { it.riskDomain in riskDomains }
    }
}
