package com.example.offlinelifeline.safety

import com.example.offlinelifeline.core.model.RiskDomain

data class SafetyRule(
    val riskDomain: RiskDomain,
    val bannedPatterns: List<String>,
    val requiredReminders: List<String>
)
