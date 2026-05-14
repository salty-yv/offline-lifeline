package com.example.offlinelifeline.core.model

enum class ToolType {
    SOS_FLASHLIGHT,
    SCREEN_SOS,
    BATTERY_SAVER_ADVICE,
    EMERGENCY_CARD,
    OFFLINE_GUIDE,
    DEBUG_LOG_EXPORT
}

data class ToolRecommendation(
    val toolType: ToolType,
    val reason: String,
    val priority: Int
)
