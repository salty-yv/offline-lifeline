package com.example.offlinelifeline.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debug_logs")
data class DebugLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val level: String,
    val tag: String,
    val message: String,
    val createdAtMillis: Long
)
