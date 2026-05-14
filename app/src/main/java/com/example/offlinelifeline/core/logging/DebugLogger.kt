package com.example.offlinelifeline.core.logging

import com.example.offlinelifeline.core.common.TimeProvider

class DebugLogger(
    private val repository: DebugLogRepository,
    private val timeProvider: TimeProvider = TimeProvider.System
) {
    suspend fun info(tag: String, message: String) {
        repository.writeLog(
            level = DebugLogLevel.INFO,
            tag = tag,
            message = message,
            createdAtMillis = timeProvider.nowMillis()
        )
    }

    suspend fun warning(tag: String, message: String) {
        repository.writeLog(
            level = DebugLogLevel.WARNING,
            tag = tag,
            message = message,
            createdAtMillis = timeProvider.nowMillis()
        )
    }

    suspend fun error(tag: String, message: String, throwable: Throwable? = null) {
        repository.writeLog(
            level = DebugLogLevel.ERROR,
            tag = tag,
            message = buildString {
                append(message)
                throwable?.message?.let { append(": ").append(it) }
            },
            createdAtMillis = timeProvider.nowMillis()
        )
    }
}

enum class DebugLogLevel {
    INFO,
    WARNING,
    ERROR
}
