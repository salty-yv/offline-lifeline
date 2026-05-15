package com.example.offlinelifeline.core.model

import com.example.offlinelifeline.agent.rag.GuideCitation

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val createdAtMillis: Long,
    val attachments: List<Attachment> = emptyList(),
    val toolRecommendations: List<ToolRecommendation> = emptyList(),
    /** 本次回答命中的本地指南引用，供 UI 展示"依据本地指南"来源 */
    val citations: List<GuideCitation> = emptyList(),
    val isFinal: Boolean = true
)
