package com.example.offlinelifeline.inference

import android.content.Context
import android.os.SystemClock
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
        val startedAt = SystemClock.elapsedRealtime()
        debugLogger?.info(TAG, "model_check_start name=${manifest.modelName} version=${manifest.modelVersion}")

        val location = findModelLocation(manifest)
        if (location == null) {
            return ModelAssetCheckResult(
                manifest = manifest,
                location = null,
                runtimeState = ModelRuntimeState.Missing,
                message = "Model file is missing. Offline guides and tools are still available."
            ).also {
                debugLogger?.warning(TAG, "model_check_missing elapsedMs=${SystemClock.elapsedRealtime() - startedAt}")
            }
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
                message = "Model file passed integrity checks."
            ).also {
                debugLogger?.info(
                    TAG,
                    buildString {
                        append("model_check_valid")
                        append(" location=").append(location.description)
                        append(" expectedSizeBytes=").append(manifest.expectedSizeBytes)
                        append(" elapsedMs=").append(SystemClock.elapsedRealtime() - startedAt)
                    }
                )
            }

            ModelIntegrityResult.Missing -> ModelAssetCheckResult(
                manifest = manifest,
                location = null,
                runtimeState = ModelRuntimeState.Missing,
                message = "Model file is missing. Offline guides and tools are still available."
            ).also {
                debugLogger?.warning(TAG, "model_check_missing elapsedMs=${SystemClock.elapsedRealtime() - startedAt}")
            }

            is ModelIntegrityResult.SizeMismatch -> ModelAssetCheckResult(
                manifest = manifest,
                location = location,
                runtimeState = ModelRuntimeState.ChecksumFailed,
                message = "Model file size does not match the manifest."
            ).also {
                debugLogger?.error(
                    TAG,
                    "model_check_size_mismatch actual=${integrityResult.actualSizeBytes} expected=${integrityResult.expectedSizeBytes} elapsedMs=${SystemClock.elapsedRealtime() - startedAt}"
                )
            }

            is ModelIntegrityResult.ChecksumMismatch -> ModelAssetCheckResult(
                manifest = manifest,
                location = location,
                runtimeState = ModelRuntimeState.ChecksumFailed,
                message = "Model SHA-256 does not match the manifest."
            ).also {
                debugLogger?.error(
                    TAG,
                    "model_check_checksum_mismatch actual=${integrityResult.actualSha256} expected=${integrityResult.expectedSha256} elapsedMs=${SystemClock.elapsedRealtime() - startedAt}"
                )
            }
        }
    }

    suspend fun prepareModelForRuntime(
        manifest: ModelManifest = ModelManifest.Default
    ): ModelAssetCheckResult {
        val checkResult = checkModel(manifest)
        val location = checkResult.location
        if (checkResult.runtimeState != ModelRuntimeState.ReadyToLoad || location !is ModelAssetLocation.AssetLocation) {
            return checkResult
        }

        return withContext(dispatchers.io) {
            val copiedFile = copyAssetModelToFile(location.assetName)
            when (val integrityResult = integrityChecker.verifyFile(copiedFile, manifest)) {
                ModelIntegrityResult.Valid -> checkResult.copy(
                    location = ModelAssetLocation.FileLocation(copiedFile),
                    message = "Asset model was copied to app storage and passed integrity checks."
                )

                ModelIntegrityResult.Missing -> checkResult.copy(
                    location = null,
                    runtimeState = ModelRuntimeState.Missing,
                    message = "Model file is missing. Offline guides and tools are still available."
                )

                is ModelIntegrityResult.SizeMismatch -> checkResult.copy(
                    location = ModelAssetLocation.FileLocation(copiedFile),
                    runtimeState = ModelRuntimeState.ChecksumFailed,
                    message = "Model file size does not match the manifest."
                )

                is ModelIntegrityResult.ChecksumMismatch -> checkResult.copy(
                    location = ModelAssetLocation.FileLocation(copiedFile),
                    runtimeState = ModelRuntimeState.ChecksumFailed,
                    message = "Model SHA-256 does not match the manifest."
                )
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

    private fun copyAssetModelToFile(assetName: String): File {
        val modelDir = File(appContext.filesDir, "models").apply { mkdirs() }
        val targetFile = modelDir.resolve(assetName)
        if (targetFile.exists() && targetFile.length() > 0L) {
            return targetFile
        }

        val tmpFile = modelDir.resolve("$assetName.asset.tmp")
        appContext.assets.open(assetName).use { input ->
            tmpFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        if (!tmpFile.renameTo(targetFile)) {
            tmpFile.copyTo(targetFile, overwrite = true)
            tmpFile.delete()
        }
        return targetFile
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
