package com.example.offlinelifeline.core.logging

import android.content.Context
import com.example.offlinelifeline.core.common.AppDispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class DebugLogExporter(
    context: Context,
    private val repository: DebugLogRepository,
    private val dispatchers: AppDispatchers = AppDispatchers()
) {
    private val appContext = context.applicationContext

    suspend fun exportText(): Result<File> {
        return withContext(dispatchers.io) {
            runCatching {
                val logs = repository.observeLogs().first()
                val dir = File(appContext.getExternalFilesDir(null), "debug-logs").apply { mkdirs() }
                val file = File(dir, "offlifeline-debug-${System.currentTimeMillis()}.txt")
                file.writeText(
                    logs.joinToString(separator = "\n") { log ->
                        "${log.createdAtMillis} [${log.level}] ${log.tag}: ${log.message}"
                    },
                    Charsets.UTF_8
                )
                file
            }
        }
    }
}
