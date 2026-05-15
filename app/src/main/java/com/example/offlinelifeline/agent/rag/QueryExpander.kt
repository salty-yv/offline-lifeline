package com.example.offlinelifeline.agent.rag

/**
 * 本地固定词表的查询扩展器。
 *
 * 不联网、不需要词向量模型，基于预设的风险词 → 相关词映射表
 * 把用户口语化输入扩展成 FTS MATCH 可用的 OR 查询字符串。
 *
 * 示例：
 * ```
 * 输入  : "我腿上一直流血止不住"
 * 扩展后: ["流血", "出血", "止血", "压迫", "伤口", "绷带", "休克"]
 * FTS  : "\"流血\" OR \"出血\" OR \"止血\" OR \"压迫\" OR \"伤口\""
 * ```
 */
object QueryExpander {

    /** 核心风险词 → 相关检索词映射表（中英双语） */
    private val expansions: Map<String, List<String>> = mapOf(

        // ── 医疗类 ───────────────────────────────────────────────────────────
        "出血"    to listOf("流血", "止血", "压迫", "绷带", "伤口", "休克"),
        "流血"    to listOf("出血", "止血", "压迫", "绷带", "伤口"),
        "止血"    to listOf("出血", "流血", "压迫", "伤口"),
        "bleeding" to listOf("blood", "wound", "pressure", "bandage", "tourniquet", "hemorrhage"),

        "骨折"    to listOf("扭伤", "变形", "疼痛", "固定", "不能动"),
        "扭伤"    to listOf("骨折", "脚踝", "疼痛", "固定", "冷敷"),
        "fracture" to listOf("sprain", "break", "immobilize", "pain"),

        "烧伤"    to listOf("烫伤", "水泡", "降温", "起泡", "热源"),
        "烫伤"    to listOf("烧伤", "水泡", "降温", "冲洗"),
        "burn"    to listOf("blister", "cooling", "scald", "heat"),

        "失温"    to listOf("低温", "发抖", "湿透", "保暖", "寒冷"),
        "发抖"    to listOf("失温", "寒冷", "保暖", "低温"),
        "hypothermia" to listOf("freezing", "shivering", "wet", "cold", "warmth"),

        "中暑"    to listOf("高温", "暴晒", "头晕", "降温", "补水"),
        "暴晒"    to listOf("中暑", "高温", "降温"),
        "heatstroke" to listOf("heat", "sunstroke", "dizzy", "cooling", "hydration"),

        "脱水"    to listOf("口渴", "没水", "补水", "电解质"),
        "没水"    to listOf("脱水", "口渴", "补水"),
        "dehydration" to listOf("thirsty", "water", "hydration", "electrolyte"),

        "蛇咬"    to listOf("毒蛇", "咬伤", "固定", "肿胀", "毒液"),
        "毒蛇"    to listOf("蛇咬", "咬伤", "固定"),
        "snake"   to listOf("bite", "venom", "swelling", "immobilize"),

        "误食"    to listOf("蘑菇", "野果", "中毒", "催吐", "腹痛"),
        "蘑菇"    to listOf("误食", "中毒", "野果"),
        "poisoning" to listOf("mushroom", "toxic", "vomit", "stomach"),

        // ── 过敏 ─────────────────────────────────────────────────────────────
        "过敏"    to listOf("荨麻疹", "肿胀", "呼吸困难", "过敏反应", "肾上腺素", "休克"),
        "过敏反应" to listOf("过敏", "肿胀", "呼吸困难", "肾上腺素"),
        "荨麻疹"  to listOf("过敏", "皮疹", "过敏反应"),
        "allergy" to listOf("allergic", "anaphylaxis", "hives", "rash", "epinephrine", "antihistamine"),
        "anaphylaxis" to listOf("allergy", "allergic", "epinephrine", "shock"),

        // ── 休克 ─────────────────────────────────────────────────────────────
        "休克"    to listOf("意识模糊", "血压低", "脉搏弱", "保暖", "抬高双腿", "急救"),
        "昏厥"    to listOf("休克", "意识模糊", "血压低"),
        "shock"   to listOf("unconscious", "low blood pressure", "weak pulse", "raise legs", "warmth"),

        // ── 扭伤（RICE 原则）────────────────────────────────────────────────
        "崴脚"    to listOf("扭伤", "脚踝", "肿胀", "冷敷", "固定"),
        "捻挫"    to listOf("扭伤", "骨折", "肿胀", "冷敷"),
        "sprain"  to listOf("ankle", "twist", "swelling", "ice", "compress", "rice"),

        // ── 热射病 ───────────────────────────────────────────────────────────
        "热射病"  to listOf("中暑", "高热", "意识不清", "降温", "冰敷", "急救"),
        "高热"    to listOf("热射病", "中暑", "降温", "冷却"),
        "heat stroke" to listOf("heatstroke", "hyperthermia", "cooling", "ice", "unconscious"),

        // ── 极端高温 ─────────────────────────────────────────────────────────
        "极端高温" to listOf("中暑", "热射病", "降温", "补水", "遮阴"),
        "heat"    to listOf("heatstroke", "sunstroke", "cooling", "shade", "hydration"),

        // ── 低温 ─────────────────────────────────────────────────────────────
        "低温"    to listOf("失温", "保暖", "干衣", "发抖", "寒冷"),
        "体温低"  to listOf("失温", "低温", "保暖"),
        "cold"    to listOf("hypothermia", "frostbite", "warmth", "shelter", "dry clothes"),

        // ── 暴雪 ─────────────────────────────────────────────────────────────
        "暴雪"    to listOf("大雪", "失温", "保暖", "避险", "遮蔽"),
        "大雪"    to listOf("暴雪", "失温", "保暖"),
        "blizzard" to listOf("snowstorm", "hypothermia", "shelter", "warmth", "visibility"),
        "snowstorm" to listOf("blizzard", "snow", "cold", "shelter", "hypothermia"),

        // ── 山体滑坡 ─────────────────────────────────────────────────────────
        "山体滑坡" to listOf("滑坡", "塌方", "撤离", "崩塌", "地震"),
        "滑坡"    to listOf("山体滑坡", "崩塌", "撤离", "余震"),
        "崩塌"    to listOf("山体滑坡", "滑坡", "塌方"),
        "landslide" to listOf("rockslide", "collapse", "evacuate", "debris", "earthquake"),
        "rockslide" to listOf("landslide", "collapse", "debris"),

        // ── 泥石流 ───────────────────────────────────────────────────────────
        "泥石流"  to listOf("山洪", "洪水", "地质灾害", "逃生", "溪谷"),
        "山洪"    to listOf("泥石流", "洪水", "涨水"),
        "mudslide" to listOf("debris flow", "flood", "evacuate", "river", "landslide"),

        // ── 缺水 / 水源 ───────────────────────────────────────────────────────
        "缺水"    to listOf("脱水", "口渴", "找水", "水源", "补水"),
        "找水"    to listOf("缺水", "水源", "脱水"),
        "水源"    to listOf("缺水", "找水", "补水"),
        "water shortage" to listOf("no water", "dehydration", "find water", "thirsty"),

        // ── 求救信号 ─────────────────────────────────────────────────────────
        "求救信号" to listOf("SOS", "信号", "闪光灯", "标记", "屏幕", "求救"),
        "rescue signal" to listOf("SOS", "signal", "flashlight", "screen", "help"),

        // ── 导航类 ───────────────────────────────────────────────────────────
        "迷路"    to listOf("找不到路", "方向", "求救", "信号", "原地等待"),
        "找不到路" to listOf("迷路", "方向", "求救"),
        "lost"    to listOf("navigation", "direction", "signal", "rescue", "shelter"),

        // ── 灾害类 ───────────────────────────────────────────────────────────
        "洪水"    to listOf("涨水", "河流", "涉水", "转移", "水位"),
        "涨水"    to listOf("洪水", "水位", "河流"),
        "flood"   to listOf("water", "rising", "river", "evacuate"),

        "火灾"    to listOf("烟", "撤离", "逃生", "山火"),
        "火"      to listOf("烟", "燃烧", "撤离"),
        "fire"    to listOf("smoke", "evacuate", "escape", "wildfire"),

        "雷电"    to listOf("打雷", "闪电", "雷暴", "避险"),
        "打雷"    to listOf("雷电", "闪电", "避险"),
        "lightning" to listOf("thunder", "thunderstorm", "shelter"),

        "地震"    to listOf("余震", "坍塌", "建筑", "掩埋"),
        "余震"    to listOf("地震", "建筑", "坍塌"),
        "earthquake" to listOf("aftershock", "collapse", "building"),

        // ── 设备类 ───────────────────────────────────────────────────────────
        "没电"    to listOf("低电量", "省电", "飞行模式", "亮度"),
        "低电量"  to listOf("没电", "省电", "飞行模式"),
        "battery" to listOf("low", "power", "save", "airplane mode"),

        "求救"    to listOf("SOS", "信号", "闪光灯", "标记", "屏幕"),
        "SOS"     to listOf("求救", "信号", "闪光灯"),
        "rescue"  to listOf("SOS", "signal", "flashlight", "help"),
    )

    /**
     * 把用户原始输入扩展为相关检索词列表（去重，保留原始输入）。
     *
     * @param raw 用户原始输入字符串
     * @return 包含原始词和扩展词的列表
     */
    fun expandTerms(raw: String): List<String> {
        val normalized = raw.trim()
        val terms = mutableSetOf<String>()
        terms += normalized

        expansions.forEach { (key, values) ->
            if (normalized.contains(key, ignoreCase = true)) {
                terms += key
                terms += values
            }
        }

        return terms.filter { it.isNotBlank() }
    }

    /**
     * 把检索词列表拼成 FTS4 MATCH 用的 OR 查询字符串。
     *
     * 每个词用双引号包裹，避免 FTS 把多字词拆开匹配。
     *
     * @param terms 检索词列表
     * @return 格式如 `"出血" OR "止血" OR "压迫"` 的字符串
     */
    fun toFtsOrQuery(terms: List<String>): String =
        terms
            .filter { it.isNotBlank() }
            .map { it.replace("\"", "") }   // 防止注入破坏 FTS 语法
            .distinct()
            .joinToString(" OR ") { "\"$it\"" }
}
