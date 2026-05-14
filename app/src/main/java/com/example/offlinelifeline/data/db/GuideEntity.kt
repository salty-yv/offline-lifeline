package com.example.offlinelifeline.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guides")
data class GuideEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val body: String,
    val tags: String,
    val updatedAtMillis: Long
)
