package com.example.offlinelifeline.data.repository

import com.example.offlinelifeline.data.db.EmergencyCardDao
import com.example.offlinelifeline.data.db.EmergencyCardEntity
import kotlinx.coroutines.flow.Flow

class EmergencyCardRepository(
    private val emergencyCardDao: EmergencyCardDao
) {
    fun observeCard(): Flow<EmergencyCardEntity?> = emergencyCardDao.observeCard()

    suspend fun getCard(): EmergencyCardEntity? = emergencyCardDao.getCard()

    suspend fun saveCard(card: EmergencyCardEntity) {
        emergencyCardDao.upsert(card.copy(id = EmergencyCardEntity.DEFAULT_ID))
    }
}
