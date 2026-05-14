package com.example.offlinelifeline.agent

import com.example.offlinelifeline.core.model.AgentContext
import com.example.offlinelifeline.core.model.RiskDomain

class RiskClassifier {
    fun classify(input: String, context: AgentContext): Set<RiskDomain> {
        val risks = linkedSetOf<RiskDomain>()

        if (input.containsAny("迷路", "走丢", "找不到路", "迷失", "方向不清")) risks += RiskDomain.LOST
        if (input.containsAny("脚扭", "扭伤", "摔伤", "疼", "伤口", "受伤")) risks += RiskDomain.INJURY
        if (input.containsAny("流血", "止不住血", "出血")) risks += RiskDomain.BLEEDING
        if (input.containsAny("烫伤", "烧伤", "起泡")) risks += RiskDomain.BURN
        if (input.containsAny("骨折", "变形", "不能动", "不能负重")) risks += RiskDomain.FRACTURE
        if (input.containsAny("蛇", "咬", "毒蛇")) risks += RiskDomain.SNAKE_BITE
        if (input.containsAny("蘑菇", "野果", "吃了", "误食", "中毒")) risks += RiskDomain.POISONING
        if (input.containsAny("冷", "发抖", "湿透", "失温")) risks += RiskDomain.HYPOTHERMIA
        if (input.containsAny("热", "头晕", "暴晒", "中暑")) risks += RiskDomain.HEATSTROKE
        if (input.containsAny("渴", "没水", "脱水")) risks += RiskDomain.DEHYDRATION
        if (input.containsAny("洪水", "涨水", "河水", "水位")) risks += RiskDomain.FLOOD
        if (input.containsAny("火", "烟", "烧", "火灾")) risks += RiskDomain.FIRE
        if (input.containsAny("雷", "电闪", "打雷", "雷暴")) risks += RiskDomain.THUNDERSTORM
        if (input.containsAny("地震", "余震", "楼塌", "塌方")) risks += RiskDomain.EARTHQUAKE
        if (input.containsAny("没电", "低电量") || (context.batteryPercent != null && context.batteryPercent <= 20)) {
            risks += RiskDomain.LOW_BATTERY
        }
        if (input.containsAny("天黑", "夜里", "晚上", "快黑", "夜晚")) risks += RiskDomain.NIGHT

        return risks.ifEmpty { setOf(RiskDomain.UNKNOWN) }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { contains(it, ignoreCase = true) }
    }
}
