package com.example.offlinelifeline.safety

import com.example.offlinelifeline.core.model.RiskDomain

class OutputSafetyValidator {
    fun validate(output: String, riskDomains: Set<RiskDomain>): SafetyValidationResult {
        val normalized = output.lowercase()
        val activeRules = SafetyRules.forRisks(riskDomains)

        activeRules.forEach { rule ->
            val unsafePattern = rule.bannedPatterns.firstOrNull { pattern ->
                normalized.containsUnsafePattern(pattern.lowercase())
            }
            if (unsafePattern != null) {
                return SafetyValidationResult.Blocked(
                    reason = "输出包含危险建议：$unsafePattern"
                )
            }
        }

        val missingReminder = activeRules
            .flatMap { it.requiredReminders }
            .firstOrNull { reminder ->
                !output.contains(reminder)
            }

        return if (missingReminder != null && riskDomains.any { it != RiskDomain.UNKNOWN }) {
            SafetyValidationResult.NeedsRewrite(
                reason = "缺少必要提醒：$missingReminder",
                rewriteInstruction = "请注意：$missingReminder"
            )
        } else {
            SafetyValidationResult.Pass
        }
    }

    private fun String.containsUnsafePattern(pattern: String): Boolean {
        var searchIndex = 0
        while (true) {
            val index = indexOf(pattern, startIndex = searchIndex)
            if (index == -1) return false
            val prefixStart = (index - NEGATION_WINDOW).coerceAtLeast(0)
            val prefix = substring(prefixStart, index)
            val negated = NegationWords.any { prefix.contains(it) }
            if (!negated) return true
            searchIndex = index + pattern.length
        }
    }

    private companion object {
        const val NEGATION_WINDOW = 8
        val NegationWords = listOf("不要", "不得", "禁止", "不能", "别", "避免", "不应")
    }
}
