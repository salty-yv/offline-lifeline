package com.example.offlinelifeline.core.model

data class AgentResponse(
    val text: String,
    val riskDomains: Set<RiskDomain>,
    val followUpQuestions: List<String>,
    val toolRecommendations: List<ToolRecommendation>
)
