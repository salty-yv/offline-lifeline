package com.example.offlinelifeline.data.repository

import com.example.offlinelifeline.core.model.ChatMessage
import com.example.offlinelifeline.core.model.ChatRole
import com.example.offlinelifeline.data.db.ChatDao
import com.example.offlinelifeline.data.db.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val chatDao: ChatDao
) {
    fun observeMessages(): Flow<List<ChatMessage>> {
        return chatDao.observeMessages().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun getMessages(): List<ChatMessage> {
        return chatDao.getMessages().map { it.toModel() }
    }

    suspend fun saveMessage(message: ChatMessage) {
        chatDao.insert(message.toEntity())
    }

    suspend fun clearMessages() {
        chatDao.clear()
    }
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

private fun ChatMessage.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        role = role.name,
        text = text,
        createdAtMillis = createdAtMillis,
        isFinal = isFinal
    )
}
