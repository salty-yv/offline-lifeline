package com.example.offlinelifeline.core.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.example.offlinelifeline.core.common.AppDispatchers
import com.example.offlinelifeline.core.logging.DebugLogger
import com.example.offlinelifeline.device.battery.BatteryStatusProvider
import kotlinx.coroutines.withContext
import java.io.File

class DeviceDiagnosticsLogger(
    context: Context,
    private val batteryStatusProvider: BatteryStatusProvider,
    private val debugLogger: DebugLogger,
    private val dispatchers: AppDispatchers = AppDispatchers()
) {
    private val appContext = context.applicationContext

    suspend fun logSnapshot(reason: String) {
        withContext(dispatchers.io) {
            val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
            val runtime = Runtime.getRuntime()
            val battery = batteryStatusProvider.getStatus()
            val filesDir = appContext.filesDir
            val externalModelsDir = appContext.getExternalFilesDir("models")

            debugLogger.info(
                TAG,
                buildString {
                    append("Stability snapshot")
                    append(" reason=").append(reason)
                    append(" device=").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
                    append(" android=").append(Build.VERSION.RELEASE)
                    append(" sdk=").append(Build.VERSION.SDK_INT)
                    append(" abis=").append(Build.SUPPORTED_ABIS.joinToString("|"))
                    append(" battery=").append(battery.percent?.let { "$it%" } ?: "unknown")
                    append(" charging=").append(battery.isCharging)
                    append(" availMemMb=").append(memoryInfo.availMem.toMb())
                    append(" totalMemMb=").append(memoryInfo.totalMem.toMb())
                    append(" lowMemory=").append(memoryInfo.lowMemory)
                    append(" appUsedHeapMb=").append((runtime.totalMemory() - runtime.freeMemory()).toMb())
                    append(" appMaxHeapMb=").append(runtime.maxMemory().toMb())
                    append(" filesFreeMb=").append(filesDir.freeSpaceMb())
                    append(" cacheFreeMb=").append(appContext.cacheDir.freeSpaceMb())
                    append(" externalModelsDir=").append(externalModelsDir?.absolutePath ?: "unavailable")
                    append(" externalModelsFreeMb=").append(externalModelsDir?.freeSpaceMb() ?: -1L)
                }
            )
        }
    }

    private fun Long.toMb(): Long = this / (1024L * 1024L)

    private fun File.freeSpaceMb(): Long = usableSpace / (1024L * 1024L)

    private companion object {
        const val TAG = "DeviceDiagnostics"
    }
}
