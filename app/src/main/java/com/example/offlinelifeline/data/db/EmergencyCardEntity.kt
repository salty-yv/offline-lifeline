package com.example.offlinelifeline.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_cards")
data class EmergencyCardEntity(
    @PrimaryKey val id: Int = DEFAULT_ID,
    val name: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val chronicConditions: String = "",
    val medications: String = "",
    val emergencyContact: String = "",
    val notes: String = "",
    val hideSensitiveFields: Boolean = true,
    val updatedAtMillis: Long = 0L
) {
    companion object {
        const val DEFAULT_ID = 1
    }
}
