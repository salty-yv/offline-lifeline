package com.example.offlinelifeline.data.repository

import com.example.offlinelifeline.data.db.GuideDao
import com.example.offlinelifeline.data.db.GuideEntity
import kotlinx.coroutines.flow.Flow

class GuideRepository(
    private val guideDao: GuideDao
) {
    fun observeGuides(): Flow<List<GuideEntity>> = guideDao.observeGuides()

    suspend fun getGuide(id: String): GuideEntity? = guideDao.getGuide(id)

    suspend fun seedDefaultGuidesIfNeeded() {
        if (guideDao.countGuides() == 0) {
            guideDao.upsertAll(DefaultGuideData.guides)
        }
    }

    suspend fun search(query: String): List<GuideEntity> {
        return if (query.isBlank()) {
            emptyList()
        } else {
            guideDao.search(query.trim())
        }
    }

    suspend fun seedGuides(guides: List<GuideEntity>) {
        guideDao.upsertAll(guides)
    }
}
