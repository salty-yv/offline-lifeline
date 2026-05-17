package com.example.offlinelifeline.data.repository

import com.example.offlinelifeline.core.model.ChatMessage
import com.example.offlinelifeline.core.model.ChatConversation
import com.example.offlinelifeline.core.model.ChatRole
import com.example.offlinelifeline.data.db.ChatDao
import com.example.offlinelifeline.data.db.ChatConversationEntity
import com.example.offlinelifeline.data.db.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao
) {
    fun observeConversations(): Flow<List<ChatConversation>> {
        return chatDao.observeConversations().map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        return chatDao.observeMessages(conversationId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun getMessages(conversationId: String): List<ChatMessage> {
        return chatDao.getMessages(conversationId).map { it.toModel() }
    }

    suspend fun ensureActiveConversation(): ChatConversation {
        return chatDao.getLatestConversation()?.toModel() ?: createConversation()
    }

    suspend fun createConversation(title: String = DEFAULT_TITLE): ChatConversation {
        val now = System.currentTimeMillis()
        val conversation = ChatConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        chatDao.insertConversation(conversation)
        return conversation.toModel()
    }

    suspend fun saveMessage(conversationId: String, message: ChatMessage) {
        chatDao.insert(message.toEntity(conversationId))
        val conversation = chatDao.getConversation(conversationId)
        if (conversation != null) {
            val title = if (message.role == ChatRole.USER && conversation.title == DEFAULT_TITLE) {
                message.text.toConversationTitle()
            } else {
                conversation.title
            }
            chatDao.updateConversationMetadata(conversationId, title, message.createdAtMillis)
        }
    }

    suspend fun clearMessages(conversationId: String) {
        chatDao.clear(conversationId)
        if (chatDao.getConversation(conversationId) == null) return
        chatDao.updateConversationMetadata(
            conversationId = conversationId,
            title = DEFAULT_TITLE,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun deleteConversation(conversationId: String): ChatConversation {
        chatDao.clear(conversationId)
        chatDao.deleteConversation(conversationId)
        return ensureActiveConversation()
    }

    companion object {
        const val DEFAULT_TITLE = ChatConversation.DEFAULT_TITLE
    }
}

private fun ChatConversationEntity.toModel(): ChatConversation {
    return ChatConversation(
        id = id,
        title = title,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

private fun ChatMessageEntity.toModel(): ChatMessage {
    val parsedRole = runCatching { ChatRole.valueOf(role) }.getOrDefault(ChatRole.SYSTEM)
    return ChatMessage(
        id = id,
        role = parsedRole,
        text = text,
        createdAtMillis = createdAtMillis,
        isFinal = isFinal
    )
}

private fun ChatMessage.toEntity(conversationId: String): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name,
        text = text,
        createdAtMillis = createdAtMillis,
        isFinal = isFinal
    )
}

private fun String.toConversationTitle(): String {
    val normalized = replace(Regex("\\s+"), " ").trim()
    val source = normalized.ifBlank { "图片对话" }
    return if (source.length <= 24) source else source.take(24) + "..."
}
