package com.example.offlinelifeline.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyCardDao {
    @Query("SELECT * FROM emergency_cards WHERE id = :id")
    fun observeCard(id: Int = EmergencyCardEntity.DEFAULT_ID): Flow<EmergencyCardEntity?>

    @Query("SELECT * FROM emergency_cards WHERE id = :id")
    suspend fun getCard(id: Int = EmergencyCardEntity.DEFAULT_ID): EmergencyCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: EmergencyCardEntity)
}
