package com.example.offlinelifeline.inference.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.offlinelifeline.R
import com.example.offlinelifeline.inference.ModelCatalog
import com.example.offlinelifeline.inference.ModelIntegrityChecker
import kotlinx.coroutines.CancellationException
import java.util.Locale

/**
 * WorkManager Worker，负责在后台（含进程被杀、App 切至后台等场景）持续下载模型。
 *
 * 输入数据：
 *   - [KEY_MODEL_ID]：要下载的模型 ID（如 "e2b" / "e4b"）
 *
 * 通过 WorkManager progress 向 UI 暴露进度；
 * Worker 负责触发下载、处理重试逻辑，并在进程存活期间持续更新进度。
 */
class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private var lastForegroundBytes = -1L
    private var lastForegroundPercent = -1
    private var lastForegroundState: String? = null

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID)
            ?: return Result.failure()

        val manifest = ModelCatalog.findById(modelId)
            ?: return Result.failure()
        val preferMirror = inputData.getBoolean(KEY_PREFER_MIRROR, false)

        return try {
            setForeground(createForegroundInfo(manifest.modelName, ModelDownloadState.Queued))

            val repository = ModelDownloadRepository(
                context = applicationContext,
                integrityChecker = ModelIntegrityChecker(),
                onStateChanged = { _, state ->
                    setProgress(progressDataFor(state))
                    if (shouldUpdateForeground(state)) {
                        setForeground(createForegroundInfo(manifest.modelName, state))
                    }
                }
            )
            repository.startDownload(manifest, preferMirror = preferMirror)

            val state = repository.getDownloadState(modelId).value
            if (state is ModelDownloadState.Completed) {
                Result.success()
            } else {
                // 下载未完成，允许 WorkManager 重试
                Result.retry()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun shouldUpdateForeground(state: ModelDownloadState): Boolean {
        val stateKey = state::class.java.name
        if (lastForegroundState != stateKey) {
            lastForegroundState = stateKey
            if (state !is ModelDownloadState.Downloading) return true
        }

        if (state !is ModelDownloadState.Downloading) return false

        val percent = progressPercent(state)
        val downloadedDelta = state.downloadedBytes - lastForegroundBytes
        val shouldUpdate = lastForegroundBytes < 0 ||
            (percent >= 0 && percent != lastForegroundPercent) ||
            downloadedDelta >= FOREGROUND_UPDATE_BYTES

        if (shouldUpdate) {
            lastForegroundBytes = state.downloadedBytes
            lastForegroundPercent = percent
        }
        return shouldUpdate
    }

    private fun createForegroundInfo(
        modelName: String,
        state: ModelDownloadState
    ): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_settings_24)
            .setContentTitle("正在下载离线模型")
            .setContentText(notificationText(modelName, state))
            .setOngoing(state is ModelDownloadState.Downloading || state is ModelDownloadState.Queued)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent())
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "取消",
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
            )
            .apply {
                when (state) {
                    is ModelDownloadState.Downloading -> {
                        val percent = progressPercent(state)
                        if (percent >= 0) {
                            setProgress(100, percent, false)
                        } else {
                            setProgress(0, 0, true)
                        }
                    }

                    ModelDownloadState.Queued,
                    ModelDownloadState.Paused -> setProgress(0, 0, true)

                    else -> setProgress(0, 0, false)
                }
            }
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existingChannel != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "离线模型下载",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "保持大模型下载在息屏和后台时继续运行"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun openAppIntent(): PendingIntent? {
        val intent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?: return null
        return PendingIntent.getActivity(
            applicationContext,
            OPEN_APP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notificationText(modelName: String, state: ModelDownloadState): String {
        return when (state) {
            is ModelDownloadState.Downloading -> {
                val percent = progressPercent(state)
                val downloaded = formatGb(state.downloadedBytes)
                val total = state.totalBytes.takeIf { it > 0 }?.let(::formatGb)
                when {
                    percent >= 0 && total != null -> "$modelName  $percent%  $downloaded GB / $total GB"
                    else -> "$modelName  已下载 $downloaded GB"
                }
            }

            is ModelDownloadState.Completed -> "$modelName 下载完成，正在校验"
            is ModelDownloadState.Failed -> "$modelName 下载失败：${state.reason}"
            ModelDownloadState.Paused -> "$modelName 下载已暂停"
            ModelDownloadState.Queued -> "$modelName 等待开始下载"
            ModelDownloadState.Idle -> "$modelName 等待下载"
        }
    }

    private fun progressPercent(state: ModelDownloadState.Downloading): Int {
        return if (state.progressFraction >= 0f) {
            (state.progressFraction.coerceIn(0f, 1f) * 100).toInt()
        } else {
            -1
        }
    }

    private fun formatGb(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format(Locale.US, "%.2f", gb)
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_PREFER_MIRROR = "prefer_mirror"
        const val KEY_STATE = "state"
        const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_PROGRESS_FRACTION = "progress_fraction"
        const val KEY_FAILURE_REASON = "failure_reason"
        const val WORK_NAME_PREFIX = "model_download_"

        const val STATE_QUEUED = "queued"
        const val STATE_DOWNLOADING = "downloading"
        const val STATE_COMPLETED = "completed"
        const val STATE_PAUSED = "paused"
        const val STATE_FAILED = "failed"
        private const val CHANNEL_ID = "offline_model_downloads"
        private const val NOTIFICATION_ID = 1002
        private const val OPEN_APP_REQUEST_CODE = 1003
        private const val FOREGROUND_UPDATE_BYTES = 16L * 1024L * 1024L

        fun workName(modelId: String) = "$WORK_NAME_PREFIX$modelId"

        fun progressDataFor(state: ModelDownloadState): Data {
            return when (state) {
                is ModelDownloadState.Downloading -> workDataOf(
                    KEY_STATE to STATE_DOWNLOADING,
                    KEY_DOWNLOADED_BYTES to state.downloadedBytes,
                    KEY_TOTAL_BYTES to state.totalBytes,
                    KEY_PROGRESS_FRACTION to state.progressFraction
                )

                is ModelDownloadState.Completed -> workDataOf(KEY_STATE to STATE_COMPLETED)
                is ModelDownloadState.Failed -> workDataOf(
                    KEY_STATE to STATE_FAILED,
                    KEY_FAILURE_REASON to state.reason
                )

                ModelDownloadState.Queued -> workDataOf(KEY_STATE to STATE_QUEUED)
                ModelDownloadState.Paused -> workDataOf(KEY_STATE to STATE_PAUSED)
                ModelDownloadState.Idle -> Data.EMPTY
            }
        }
    }
}
