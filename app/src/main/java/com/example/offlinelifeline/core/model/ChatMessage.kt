package com.example.offlinelifeline.core.model

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val createdAtMillis: Long,
    val attachments: List<Attachment> = emptyList(),
    val isFinal: Boolean = true
)
