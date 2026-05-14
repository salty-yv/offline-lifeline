package com.example.offlinelifeline.core.logging

import com.example.offlinelifeline.data.db.DebugLogDao
import com.example.offlinelifeline.data.db.DebugLogEntity
import kotlinx.coroutines.flow.Flow

class DebugLogRepository(
    private val debugLogDao: DebugLogDao
) {
    fun observeLogs(): Flow<List<DebugLogEntity>> = debugLogDao.observeLogs()

    suspend fun writeLog(
        level: DebugLogLevel,
        tag: String,
        message: String,
        createdAtMillis: Long
    ) {
        debugLogDao.insert(
            DebugLogEntity(
                level = level.name,
                tag = tag,
                message = message,
                createdAtMillis = createdAtMillis
            )
        )
    }

    suspend fun clearLogs() {
        debugLogDao.clear()
    }
}
