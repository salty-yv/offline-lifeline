package com.example.offlinelifeline.inference

import android.content.Context
import com.example.offlinelifeline.core.common.AppDispatchers
import com.example.offlinelifeline.core.logging.DebugLogger
import com.example.offlinelifeline.core.model.ModelRuntimeState
import kotlinx.coroutines.withContext
import java.io.File

class ModelAssetManager(
    context: Context,
    private val integrityChecker: ModelIntegrityChecker,
    private val debugLogger: DebugLogger? = null,
    private val dispatchers: AppDispatchers = AppDispatchers()
) {
    private val appContext = context.applicationContext
    val cacheDirPath: String
        get() = File(appContext.cacheDir, "litertlm").apply { mkdirs() }.absolutePath

    suspend fun checkModel(
        manifest: ModelManifest = ModelManifest.Default
    ): ModelAssetCheckResult {
        debugLogger?.info(TAG, "Checking model asset: ${manifest.modelName} ${manifest.modelVersion}")

        val location = findModelLocation(manifest)
            ?: return ModelAssetCheckResult(
                manifest = manifest,
                location = null,
                runtimeState = ModelRuntimeState.Missing,
                message = "模型文件不存在，可使用离线指南、工具箱和 Mock 模式。"
            ).also {
                debugLogger?.warning(TAG, it.message)
            }

        val integrityResult = when (location) {
            is ModelAssetLocation.FileLocation -> integrityChecker.verifyFile(location.file, manifest)
            is ModelAssetLocation.AssetLocation -> integrityChecker.verifyStream(
                openStream = { appContext.assets.open(location.assetName) },
                manifest = manifest
            )
        }

        return when (integrityResult) {
            ModelIntegrityResult.Valid -> ModelAssetCheckResult(
                manifest = manifest,
                location = location,
                runtimeState = ModelRuntimeState.ReadyToLoad,
                message = "模型文件已通过基础检查。"
            ).also {
                debugLogger?.info(TAG, "${it.message} ${location.description}")
            }

            ModelIntegrityResult.Missing -> ModelAssetCheckResult(
                manifest = manifest,
                location = null,
                runtimeState = ModelRuntimeState.Missing,
                message = "模型文件不存在，可使用离线指南、工具箱和 Mock 模式。"
            ).also {
                debugLogger?.warning(TAG, it.message)
            }

            is ModelIntegrityResult.SizeMismatch -> ModelAssetCheckResult(
                manifest = manifest,
                location = location,
                runtimeState = ModelRuntimeState.ChecksumFailed,
                message = "模型大小不匹配：实际 ${integrityResult.actualSizeBytes}，期望 ${integrityResult.expectedSizeBytes}。"
            ).also {
                debugLogger?.error(TAG, it.message)
            }

            is ModelIntegrityResult.ChecksumMismatch -> ModelAssetCheckResult(
                manifest = manifest,
                location = location,
                runtimeState = ModelRuntimeState.ChecksumFailed,
                message = "模型 SHA-256 不匹配，已阻止真实模型加载。"
            ).also {
                debugLogger?.error(TAG, "${it.message} actual=${integrityResult.actualSha256}")
            }
        }
    }

    private suspend fun findModelLocation(manifest: ModelManifest): ModelAssetLocation? {
        return withContext(dispatchers.io) {
            val fileNames = listOf(manifest.fileName) + manifest.alternateFileNames
            val fileCandidates = listOfNotNull(
                appContext.getExternalFilesDir("models"),
                File(appContext.filesDir, "models")
            ).flatMap { dir ->
                fileNames.map { fileName -> dir.resolve(fileName) }
            }

            fileCandidates.firstOrNull { it.exists() && it.isFile }?.let {
                return@withContext ModelAssetLocation.FileLocation(it)
            }

            fileNames.firstNotNullOfOrNull { fileName ->
                val assetExists = runCatching {
                    appContext.assets.open(fileName).close()
                }.isSuccess
                if (assetExists) ModelAssetLocation.AssetLocation(fileName) else null
            }
        }
    }

    private companion object {
        const val TAG = "ModelAssetManager"
    }
}

data class ModelAssetCheckResult(
    val manifest: ModelManifest,
    val location: ModelAssetLocation?,
    val runtimeState: ModelRuntimeState,
    val message: String
)

sealed class ModelAssetLocation {
    data class FileLocation(val file: File) : ModelAssetLocation()
    data class AssetLocation(val assetName: String) : ModelAssetLocation()

    val description: String
        get() = when (this) {
            is FileLocation -> file.absolutePath
            is AssetLocation -> "assets/$assetName"
        }
}

fun ModelAssetLocation.absoluteModelPath(): String {
    return when (this) {
        is ModelAssetLocation.FileLocation -> file.absolutePath
        is ModelAssetLocation.AssetLocation -> error("Asset model must be copied to a file before LiteRT-LM initialization")
    }
}
