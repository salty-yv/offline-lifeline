package com.example.offlinelifeline.core.model

data class ChatConversation(
    val id: String,
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    val hasDefaultTitle: Boolean
        get() = title == DEFAULT_TITLE

    companion object {
        const val DEFAULT_TITLE = "新对话"
    }
}
