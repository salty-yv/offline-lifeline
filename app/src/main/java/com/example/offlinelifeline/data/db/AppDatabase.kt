package com.example.offlinelifeline.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        DebugLogEntity::class,
        EmergencyCardEntity::class,
        GuideEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun debugLogDao(): DebugLogDao
    abstract fun emergencyCardDao(): EmergencyCardDao
    abstract fun guideDao(): GuideDao
}
