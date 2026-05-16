package com.example.offlinelifeline.inference.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.offlinelifeline.inference.ModelCatalog
import com.example.offlinelifeline.inference.ModelIntegrityChecker
import kotlinx.coroutines.CancellationException

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

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID)
            ?: return Result.failure()

        val manifest = ModelCatalog.findById(modelId)
            ?: return Result.failure()
        val preferMirror = inputData.getBoolean(KEY_PREFER_MIRROR, false)

        return try {
            val repository = ModelDownloadRepository(
                context = applicationContext,
                integrityChecker = ModelIntegrityChecker(),
                onStateChanged = { _, state ->
                    setProgress(progressDataFor(state))
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
