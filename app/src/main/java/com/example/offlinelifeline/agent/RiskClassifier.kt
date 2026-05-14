package com.example.offlinelifeline.agent

import com.example.offlinelifeline.core.model.AgentContext
import com.example.offlinelifeline.core.model.RiskDomain

class RiskClassifier {
    fun classify(input: String, context: AgentContext): Set<RiskDomain> {
        val risks = linkedSetOf<RiskDomain>()

        if (input.containsAny("迷路", "走丢", "找不到路", "迷失", "方向不清",
                "lost", "i'm lost", "i am lost", "can't find", "no direction")) risks += RiskDomain.LOST
        if (input.containsAny("脚扭", "扭伤", "摔伤", "疼", "伤口", "受伤",
                "injured", "injury", "hurt", "wound", "sprain", "twisted")) risks += RiskDomain.INJURY
        if (input.containsAny("流血", "止不住血", "出血",
                "bleeding", "blood", "hemorrhage")) risks += RiskDomain.BLEEDING
        if (input.containsAny("烫伤", "烧伤", "起泡",
                "burn", "burned", "blister", "scalded")) risks += RiskDomain.BURN
        if (input.containsAny("骨折", "变形", "不能动", "不能负重",
                "fracture", "broken bone", "can't move", "can't walk")) risks += RiskDomain.FRACTURE
        if (input.containsAny("蛇", "咬", "毒蛇",
                "snake", "snakebite", "snake bite", "venom", "bitten")) risks += RiskDomain.SNAKE_BITE
        if (input.containsAny("蘑菇", "野果", "吃了", "误食", "中毒",
                "mushroom", "poisoned", "poisoning", "ate", "eaten", "toxic")) risks += RiskDomain.POISONING
        if (input.containsAny("冷", "发抖", "湿透", "失温",
                "cold", "freezing", "hypothermia", "frostbite", "shivering", "soaked")) risks += RiskDomain.HYPOTHERMIA
        if (input.containsAny("热", "头晕", "暴晒", "中暑",
                "heat", "heatstroke", "overheating", "dizzy", "sunstroke", "hot")) risks += RiskDomain.HEATSTROKE
        if (input.containsAny("渴", "没水", "脱水",
                "thirsty", "no water", "out of water", "dehydrated", "dehydration")) risks += RiskDomain.DEHYDRATION
        if (input.containsAny("洪水", "涨水", "河水", "水位",
                "flood", "flooding", "rising water", "river")) risks += RiskDomain.FLOOD
        if (input.containsAny("火", "烟", "烧", "火灾",
                "fire", "smoke", "burning", "wildfire")) risks += RiskDomain.FIRE
        if (input.containsAny("雷", "电闪", "打雷", "雷暴",
                "lightning", "thunder", "thunderstorm")) risks += RiskDomain.THUNDERSTORM
        if (input.containsAny("地震", "余震", "楼塌", "塌方",
                "earthquake", "aftershock", "collapse", "landslide")) risks += RiskDomain.EARTHQUAKE
        if (input.containsAny("没电", "低电量", "low battery", "no battery", "battery dead", "battery dying")
            || (context.batteryPercent != null && context.batteryPercent <= 20)) {
            risks += RiskDomain.LOW_BATTERY
        }
        if (input.containsAny("天黑", "夜里", "晚上", "快黑", "夜晚",
                "dark", "night", "getting dark", "nightfall", "sunset")) risks += RiskDomain.NIGHT

        return risks.ifEmpty { setOf(RiskDomain.UNKNOWN) }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { contains(it, ignoreCase = true) }
    }
}

