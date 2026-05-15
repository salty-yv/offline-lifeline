package com.example.offlinelifeline.inference.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.offlinelifeline.inference.ModelCatalog
import com.example.offlinelifeline.inference.ModelIntegrityChecker

/**
 * WorkManager Worker，负责在后台（含进程被杀、App 切至后台等场景）持续下载模型。
 *
 * 输入数据：
 *   - [KEY_MODEL_ID]：要下载的模型 ID（如 "e2b" / "e4b"）
 *
 * 通过 [ModelDownloadRepository] 的 Flow 向 UI 暴露进度；
 * Worker 本身仅负责触发下载和处理重试逻辑。
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

        return try {
            val repository = ModelDownloadRepository(
                context = applicationContext,
                integrityChecker = ModelIntegrityChecker()
            )
            repository.startDownload(manifest)

            val state = repository.getDownloadState(modelId).value
            if (state is ModelDownloadState.Completed) {
                Result.success()
            } else {
                // 下载未完成，允许 WorkManager 重试
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val WORK_NAME_PREFIX = "model_download_"

        fun workName(modelId: String) = "$WORK_NAME_PREFIX$modelId"
    }
}
