package com.example.offlinelifeline.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GuideDao {
    @Query("SELECT * FROM guides ORDER BY title ASC")
    fun observeGuides(): Flow<List<GuideEntity>>

    @Query("SELECT * FROM guides WHERE id = :id")
    suspend fun getGuide(id: String): GuideEntity?

    @Query(
        """
        SELECT * FROM guides
        WHERE title LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY title ASC
        """
    )
    suspend fun search(query: String): List<GuideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(guides: List<GuideEntity>)
}
