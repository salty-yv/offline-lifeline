package com.example.offlinelifeline.core.model

import com.example.offlinelifeline.agent.rag.GuideCitation

/**
 * Agent 处理完用户输入后返回的响应对象。
 *
 * [citations] 为本次回答引用的本地指南片段列表，
 * 供 UI 在答案下方展示"依据本地指南"来源，
 * 默认为空列表（无 RAG 命中或 RAG 服务未初始化时）。
 */
data class AgentResponse(
    val text: String,
    val riskDomains: Set<RiskDomain>,
    val followUpQuestions: List<String>,
    val toolRecommendations: List<ToolRecommendation>,
    val citations: List<GuideCitation> = emptyList()
)
