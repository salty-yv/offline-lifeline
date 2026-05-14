package com.example.offlinelifeline.safety

import com.example.offlinelifeline.core.model.RiskDomain

class SafetyKernel(
    private val constraintBuilder: SafetyConstraintBuilder = SafetyConstraintBuilder(),
    private val outputValidator: OutputSafetyValidator = OutputSafetyValidator()
) {
    fun buildSafetyInstruction(
        riskDomains: Set<RiskDomain>,
        hasImageInput: Boolean = false,
        imageInputSupported: Boolean = false
    ): String {
        return constraintBuilder.buildSafetyInstruction(
            riskDomains = riskDomains,
            hasImageInput = hasImageInput,
            imageInputSupported = imageInputSupported
        )
    }

    fun validate(output: String, riskDomains: Set<RiskDomain>): SafetyValidationResult {
        return outputValidator.validate(output, riskDomains)
    }

    fun buildFallbackResponse(reason: String, riskDomains: Set<RiskDomain>): String {
        val reminders = SafetyRules.forRisks(riskDomains)
            .flatMap { it.requiredReminders }
            .distinct()

        return buildString {
            appendLine("刚才的回答包含不安全内容，已被拦截。")
            appendLine()
            appendLine("先做这 3 步")
            appendLine("1. 停止高风险动作，先保证自己处在相对安全的位置。")
            appendLine("2. 如果有受伤、出血、呼吸困难、意识异常，尽快联系救援或让同伴求助。")
            appendLine("3. 降低手机亮度，保存电量，优先发送简短求救信息。")
            appendLine()
            appendLine("不要做")
            appendLine(reason)
            if (reminders.isNotEmpty()) {
                appendLine()
                appendLine("必要提醒")
                reminders.forEach { appendLine("- $it") }
            }
        }.trim()
    }
}
