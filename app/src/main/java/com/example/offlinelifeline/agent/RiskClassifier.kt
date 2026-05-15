package com.example.offlinelifeline.agent

import com.example.offlinelifeline.core.model.AgentContext
import com.example.offlinelifeline.core.model.RiskDomain

class RiskClassifier {
    fun classify(input: String, context: AgentContext): Set<RiskDomain> {
        val risks = linkedSetOf<RiskDomain>()

        // ── 导航 ──────────────────────────────────────────────────────────────
        if (input.containsAny("迷路", "走丢", "找不到路", "迷失", "方向不清",
                "lost", "i'm lost", "i am lost", "can't find", "no direction")) risks += RiskDomain.LOST

        // ── 创伤 / 外伤 ───────────────────────────────────────────────────────
        if (input.containsAny("脚扭", "扭伤", "崴脚", "捻挫", "摔伤", "疼痛", "伤口", "受伤", "关节",
                "injured", "injury", "hurt", "wound", "sprain", "twisted", "ankle")) risks += RiskDomain.INJURY
        if (input.containsAny("流血", "止不住血", "出血", "血流",
                "bleeding", "blood", "hemorrhage")) risks += RiskDomain.BLEEDING
        if (input.containsAny("烫伤", "烧伤", "起泡", "水泡",
                "burn", "burned", "blister", "scalded", "scald")) risks += RiskDomain.BURN
        if (input.containsAny("骨折", "变形", "不能动", "不能负重", "断骨",
                "fracture", "broken bone", "can't move", "can't walk")) risks += RiskDomain.FRACTURE
        if (input.containsAny("蛇", "毒蛇", "蛇咬", "被咬",
                "snake", "snakebite", "snake bite", "venom", "bitten by snake")) risks += RiskDomain.SNAKE_BITE
        if (input.containsAny("蘑菇", "野果", "误食", "中毒", "吃了不明",
                "mushroom", "poisoned", "poisoning", "toxic plant", "ate unknown")) risks += RiskDomain.POISONING

        // ── 过敏 / 休克（均映射 INJURY，触发 medical topic 检索） ───────────────
        if (input.containsAny("过敏", "荨麻疹", "喉咙紧", "过敏反应", "花粉", "食物过敏",
                "allergy", "allergic", "anaphylaxis", "hives", "rash", "epipen")) risks += RiskDomain.INJURY
        if (input.containsAny("休克", "意识模糊", "晕过去", "脉搏弱", "呼吸急促", "昏厥",
                "shock", "unconscious", "fainted", "weak pulse", "rapid breathing")) risks += RiskDomain.INJURY

        // ── 低温 / 失温 ───────────────────────────────────────────────────────
        if (input.containsAny("冷", "发抖", "湿透", "失温", "低温", "体温低", "暴雪", "大雪",
                "cold", "freezing", "hypothermia", "frostbite", "shivering", "soaked",
                "blizzard", "snowstorm")) risks += RiskDomain.HYPOTHERMIA

        // ── 高温 / 中暑 / 热射病 ──────────────────────────────────────────────
        if (input.containsAny("热", "头晕", "暴晒", "中暑", "热射病", "高热", "体温高", "极端高温",
                "heat", "heatstroke", "overheating", "dizzy", "sunstroke", "hot",
                "heat stroke", "hyperthermia")) risks += RiskDomain.HEATSTROKE

        // ── 脱水 / 缺水 ───────────────────────────────────────────────────────
        if (input.containsAny("渴", "没水", "脱水", "缺水", "找水", "水源", "没有水喝",
                "thirsty", "no water", "out of water", "dehydrated", "dehydration",
                "water shortage", "need water")) risks += RiskDomain.DEHYDRATION

        // ── 自然灾害 ──────────────────────────────────────────────────────────
        if (input.containsAny("洪水", "涨水", "河水暴涨", "水位上涨", "山洪", "泥石流", "被水困",
                "flood", "flooding", "rising water", "flash flood",
                "mudslide", "debris flow")) risks += RiskDomain.FLOOD
        if (input.containsAny("火", "烟", "烧", "火灾", "山火", "丛林火",
                "fire", "smoke", "burning", "wildfire", "forest fire")) risks += RiskDomain.FIRE
        if (input.containsAny("雷", "电闪", "打雷", "雷暴", "闪电",
                "lightning", "thunder", "thunderstorm")) risks += RiskDomain.THUNDERSTORM
        if (input.containsAny("地震", "余震", "楼塌", "塌方", "山体滑坡", "滑坡", "崩塌",
                "earthquake", "aftershock", "collapse", "landslide", "rockslide")) risks += RiskDomain.EARTHQUAKE

        // ── 设备 / 电量 / 求救信号 ───────────────────────────────────────────
        if (input.containsAny("没电", "低电量", "求救信号", "sos", "SOS", "发出求救", "信号",
                "low battery", "no battery", "battery dead", "battery dying",
                "rescue signal", "send sos")
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

