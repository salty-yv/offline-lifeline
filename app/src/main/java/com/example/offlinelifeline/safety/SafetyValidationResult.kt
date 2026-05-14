package com.example.offlinelifeline.safety

sealed class SafetyValidationResult {
    data object Pass : SafetyValidationResult()
    data class Blocked(val reason: String) : SafetyValidationResult()
    data class NeedsRewrite(
        val reason: String,
        val rewriteInstruction: String
    ) : SafetyValidationResult()
}
