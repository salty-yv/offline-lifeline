package com.example.offlinelifeline.core.model

data class AgentContext(
    val riskDomains: Set<RiskDomain> = emptySet(),
    val knownFacts: Map<String, String> = emptyMap(),
    val missingFacts: List<String> = emptyList(),
    val batteryPercent: Int? = null,
    val userCanMove: Boolean? = null,
    val hasWater: Boolean? = null,
    val hasWarmClothes: Boolean? = null
)
