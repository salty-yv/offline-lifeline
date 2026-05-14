package com.example.offlinelifeline.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["conversationId"])]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(defaultValue = "'legacy'")
    val conversationId: String,
    val role: String,
    val text: String,
    val createdAtMillis: Long,
    val isFinal: Boolean
)
