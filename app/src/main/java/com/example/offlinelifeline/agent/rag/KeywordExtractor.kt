package com.example.offlinelifeline.agent.rag

/**
 * 轻量级关键词提取器。
 *
 * 不依赖任何 NLP 库，通过停用词过滤和最小长度限制
 * 从用户输入中提取出有实际检索价值的词。
 *
 * 策略：
 * 1. 按空格和标点分词（中文按字符长度兜底，不做真正分词）
 * 2. 过滤停用词（助词、语气词、代词等）
 * 3. 过滤长度 < 2 的词
 * 4. 保留最多 [MAX_KEYWORDS] 个词
 */
object KeywordExtractor {

    private const val MAX_KEYWORDS = 8

    /** 中文常见停用词表（助词、语气词、代词、副词等） */
    private val stopWords = setOf(
        "的", "了", "和", "与", "在", "我", "你", "他", "她", "它",
        "是", "有", "不", "也", "就", "都", "而", "但", "很", "更",
        "最", "非常", "一点", "一些", "这", "那", "这个", "那个",
        "怎么", "什么", "如何", "为什么", "哪里", "哪", "吗", "呢",
        "啊", "吧", "嗯", "哦", "好", "行", "可以", "能不能",
        "我的", "我们", "你们", "他们", "现在", "然后", "接着",
        "还有", "已经", "可能", "应该", "需要", "一直", "一下",
        // 英文停用词
        "i", "me", "my", "we", "you", "he", "she", "it", "they",
        "is", "am", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would",
        "could", "should", "may", "might", "a", "an", "the",
        "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "about", "as", "into",
        "very", "really", "just", "now", "so", "not", "no",
        "what", "how", "why", "where", "when", "who", "this", "that"
    )

    /**
     * 从用户输入中提取关键词。
     *
     * @param input 用户原始输入
     * @return 过滤后的关键词列表，最多 [MAX_KEYWORDS] 个
     */
    fun extract(input: String): List<String> {
        if (input.isBlank()) return emptyList()

        // 按空格、标点、分隔符切分
        val tokens = input
            .split(Regex("[\\s，。！？、；：,.!?;:·…—\\-/\\\\()（）\\[\\]【】]+"))
            .filter { it.isNotBlank() }

        val keywords = mutableListOf<String>()

        for (token in tokens) {
            if (token.length < 2) continue
            if (token.lowercase() in stopWords) continue
            keywords += token
            if (keywords.size >= MAX_KEYWORDS) break
        }

        // 如果切分后没有有效词（纯中文长句未被切开），退回到整句
        if (keywords.isEmpty() && input.trim().length >= 2) {
            return listOf(input.trim())
        }

        return keywords
    }
}
