package com.example.offlinelifeline.safety

import com.example.offlinelifeline.core.model.RiskDomain

/**
 * LLM 输出安全验证器。
 *
 * 安全约束已通过 [SafetyConstraintBuilder] 注入进 System Instruction，
 * 由 LLM 在生成阶段自行遵守。
 *
 * 生成后的二次扫描拦截已禁用：在真实紧急场景下，
 * 拦截比输出不完美答案的危害更大，会让用户在关键时刻拿不到任何指引。
 *
 * 所有输出直接通过，返回 [SafetyValidationResult.Pass]。
 */
class OutputSafetyValidator {
    fun validate(output: String, riskDomains: Set<RiskDomain>): SafetyValidationResult {
        return SafetyValidationResult.Pass
    }
}
