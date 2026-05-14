package com.example.offlinelifeline.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_conversations ORDER BY updatedAtMillis DESC")
    fun observeConversations(): Flow<List<ChatConversationEntity>>

    @Query("SELECT * FROM chat_conversations ORDER BY updatedAtMillis DESC")
    suspend fun getConversations(): List<ChatConversationEntity>

    @Query("SELECT * FROM chat_conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversation(conversationId: String): ChatConversationEntity?

    @Query("SELECT * FROM chat_conversations ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun getLatestConversation(): ChatConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ChatConversationEntity)

    @Query("UPDATE chat_conversations SET title = :title, updatedAtMillis = :updatedAtMillis WHERE id = :conversationId")
    suspend fun updateConversationMetadata(conversationId: String, title: String, updatedAtMillis: Long)

    @Query("DELETE FROM chat_conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAtMillis ASC")
    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAtMillis ASC")
    suspend fun getMessages(conversationId: String): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun countMessages(conversationId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun clear(conversationId: String)
}
