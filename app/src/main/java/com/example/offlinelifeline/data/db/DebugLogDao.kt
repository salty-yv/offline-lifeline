package com.example.offlinelifeline.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DebugLogDao {
    @Query("SELECT * FROM debug_logs ORDER BY createdAtMillis DESC")
    fun observeLogs(): Flow<List<DebugLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DebugLogEntity)

    @Query("DELETE FROM debug_logs")
    suspend fun clear()
}
