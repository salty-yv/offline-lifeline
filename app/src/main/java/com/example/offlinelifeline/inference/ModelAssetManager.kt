package com.example.offlinelifeline.inference

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.Os
import com.example.offlinelifeline.core.common.AppDispatchers
import com.example.offlinelifeline.core.logging.DebugLogger
import com.example.offlinelifeline.core.model.ModelRuntimeState
import kotlinx.coroutines.withContext
import java.io.File

class ModelAssetManager(
    context: Context,
    private val integrityChecker: ModelIntegrityChecker,
    private val debugLogger: DebugLogger? = null,
    private val externalModelProvider: suspend (String) -> ExternalModelReference? = { null },
    private val dispatchers: AppDispatchers = AppDispatchers()
) {
    private val appContext = context.applicationContext
    private val externalModelDescriptors = mutableMapOf<String, ParcelFileDescriptor>()

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
            is ModelAssetLocation.ExternalUriLocation -> verifyUri(location.uri, manifest)
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

    suspend fun identifyExternalModel(uri: Uri): ModelAssetCheckResult {
        val startedAt = SystemClock.elapsedRealtime()
        debugLogger?.info(TAG, "model_external_identify_start uri=$uri")

        return withContext(dispatchers.io) {
            val displayName = queryDisplayName(uri) ?: uri.toString()
            val sizeBytes = querySizeBytes(uri)
            val candidates = ModelCatalog.all.filter { manifest ->
                sizeBytes <= 0L || manifest.expectedSizeBytes <= 0L || manifest.expectedSizeBytes == sizeBytes
            }.ifEmpty { ModelCatalog.all }

            candidates.forEach { manifest ->
                if (verifyUri(uri, manifest) == ModelIntegrityResult.Valid) {
                    if (resolveDirectReadablePath(uri) == null) {
                        return@withContext ModelAssetCheckResult(
                            manifest = manifest,
                            location = null,
                            runtimeState = ModelRuntimeState.Failed("Selected ${manifest.modelName} passed hash check, but Android only exposed it as a content URI. LiteRT-LM needs a directly readable file path. Move the model to Android/data/com.example.offlinelifeline/files/models or choose a location the app can read as a real file path."),
                            message = "Selected ${manifest.modelName} passed hash check, but Android only exposed it as a content URI. LiteRT-LM needs a directly readable file path. Move the model to Android/data/com.example.offlinelifeline/files/models or choose a location the app can read as a real file path."
                        )
                    }
                    takePersistableReadPermission(uri)
                    return@withContext ModelAssetCheckResult(
                        manifest = manifest,
                        location = ModelAssetLocation.ExternalUriLocation(
                            uri = uri,
                            displayName = displayName,
                            modelId = manifest.modelId
                        ),
                        runtimeState = ModelRuntimeState.ReadyToLoad,
                        message = "Selected model matched ${manifest.modelName}."
                    ).also {
                        debugLogger?.info(
                            TAG,
                            "model_external_identify_valid modelId=${manifest.modelId} displayName=$displayName elapsedMs=${SystemClock.elapsedRealtime() - startedAt}"
                        )
                    }
                }
            }

            ModelAssetCheckResult(
                manifest = ModelManifest.Default,
                location = null,
                runtimeState = ModelRuntimeState.ChecksumFailed,
                message = "Selected file does not match any known model."
            )
        }
    }

    suspend fun moveSelectedModelIntoAppStorage(uri: Uri): ModelAssetCheckResult {
        val startedAt = SystemClock.elapsedRealtime()
        debugLogger?.info(TAG, "model_move_start uri=$uri")

        return withContext(dispatchers.io) {
            val sizeBytes = querySizeBytes(uri)
            val candidates = ModelCatalog.all.filter { manifest ->
                sizeBytes <= 0L || manifest.expectedSizeBytes <= 0L || manifest.expectedSizeBytes == sizeBytes
            }.ifEmpty { ModelCatalog.all }

            candidates.forEach { manifest ->
                if (verifyUri(uri, manifest) == ModelIntegrityResult.Valid) {
                    return@withContext moveVerifiedModelIntoAppStorage(uri, manifest, startedAt)
                }
            }

            ModelAssetCheckResult(
                manifest = ModelManifest.Default,
                location = null,
                runtimeState = ModelRuntimeState.ChecksumFailed,
                message = "Selected file does not match any known model."
            )
        }
    }

    fun modelPathForRuntime(location: ModelAssetLocation): String {
        return when (location) {
            is ModelAssetLocation.FileLocation -> location.file.absolutePath
            is ModelAssetLocation.AssetLocation -> error("Asset model must be copied to a file before LiteRT-LM initialization")
            is ModelAssetLocation.ExternalUriLocation -> openExternalModelPath(location)
        }
    }

    fun modelLocationKey(location: ModelAssetLocation): String {
        return when (location) {
            is ModelAssetLocation.FileLocation -> "file:${location.file.absolutePath}"
            is ModelAssetLocation.AssetLocation -> "asset:${location.assetName}"
            is ModelAssetLocation.ExternalUriLocation -> "external:${location.uri}"
        }
    }

    suspend fun currentModelLocationKey(manifest: ModelManifest): String? {
        return findModelLocation(manifest)?.let(::modelLocationKey)
    }

    fun releaseOpenExternalModelDescriptors() {
        synchronized(externalModelDescriptors) {
            externalModelDescriptors.values.forEach { descriptor ->
                runCatching { descriptor.close() }
            }
            externalModelDescriptors.clear()
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

            externalModelProvider(manifest.modelId)?.let { externalModel ->
                return@withContext ModelAssetLocation.ExternalUriLocation(
                    uri = Uri.parse(externalModel.uriString),
                    displayName = externalModel.displayName ?: externalModel.uriString,
                    modelId = manifest.modelId
                )
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

    private suspend fun moveVerifiedModelIntoAppStorage(
        uri: Uri,
        manifest: ModelManifest,
        startedAt: Long
    ): ModelAssetCheckResult {
        val modelDir = File(appContext.filesDir, "models").apply { mkdirs() }
        val targetFile = modelDir.resolve(manifest.fileName)
        val tmpFile = modelDir.resolve("${manifest.fileName}.move.tmp")

        val movedDirectly = moveDirectReadableFile(uri, targetFile)
        if (!movedDirectly) {
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            val input = appContext.contentResolver.openInputStream(uri)
                ?: return ModelAssetCheckResult(
                    manifest = manifest,
                    location = null,
                    runtimeState = ModelRuntimeState.Missing,
                    message = "Selected model file could not be opened."
                )
            input.use { source ->
                tmpFile.outputStream().use { target ->
                    source.copyTo(target)
                }
            }

            when (val tmpIntegrity = integrityChecker.verifyFile(tmpFile, manifest)) {
                ModelIntegrityResult.Valid -> moveFileIntoPlace(tmpFile, targetFile)
                ModelIntegrityResult.Missing -> {
                    tmpFile.delete()
                    return ModelAssetCheckResult(
                        manifest = manifest,
                        location = null,
                        runtimeState = ModelRuntimeState.Missing,
                        message = "Selected model file could not be copied."
                    )
                }
                is ModelIntegrityResult.SizeMismatch -> {
                    tmpFile.delete()
                    return ModelAssetCheckResult(
                        manifest = manifest,
                        location = null,
                        runtimeState = ModelRuntimeState.ChecksumFailed,
                        message = "Copied model size does not match ${manifest.modelName}."
                    )
                }
                is ModelIntegrityResult.ChecksumMismatch -> {
                    tmpFile.delete()
                    return ModelAssetCheckResult(
                        manifest = manifest,
                        location = null,
                        runtimeState = ModelRuntimeState.ChecksumFailed,
                        message = "Copied model SHA-256 does not match ${manifest.modelName}."
                    )
                }
            }
        }

        val finalIntegrity = integrityChecker.verifyFile(targetFile, manifest)
        if (finalIntegrity != ModelIntegrityResult.Valid) {
            return ModelAssetCheckResult(
                manifest = manifest,
                location = ModelAssetLocation.FileLocation(targetFile),
                runtimeState = ModelRuntimeState.ChecksumFailed,
                message = "Moved model did not pass final integrity check."
            )
        }

        val sourceDeleted = movedDirectly || deleteSourceDocument(uri)
        val message = if (sourceDeleted) {
            "Moved ${manifest.modelName} into app model storage."
        } else {
            "Imported ${manifest.modelName} into app model storage. Android did not allow deleting the original file, so you can remove it manually to free space."
        }

        return ModelAssetCheckResult(
            manifest = manifest,
            location = ModelAssetLocation.FileLocation(targetFile),
            runtimeState = ModelRuntimeState.ReadyToLoad,
            message = message
        ).also {
            debugLogger?.info(
                TAG,
                "model_move_success modelId=${manifest.modelId} target=${targetFile.absolutePath} sourceDeleted=$sourceDeleted elapsedMs=${SystemClock.elapsedRealtime() - startedAt}"
            )
        }
    }

    private fun moveDirectReadableFile(uri: Uri, targetFile: File): Boolean {
        val sourcePath = resolveDirectReadablePath(uri) ?: return false
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists() || !sourceFile.isFile || !sourceFile.canRead()) {
            return false
        }
        if (runCatching { sourceFile.canonicalPath == targetFile.canonicalPath }.getOrDefault(false)) {
            return true
        }
        targetFile.parentFile?.mkdirs()
        if (targetFile.exists() && !targetFile.delete()) {
            return false
        }
        return sourceFile.renameTo(targetFile)
    }

    private fun moveFileIntoPlace(sourceFile: File, targetFile: File) {
        targetFile.parentFile?.mkdirs()
        if (targetFile.exists()) {
            targetFile.delete()
        }
        if (!sourceFile.renameTo(targetFile)) {
            sourceFile.copyTo(targetFile, overwrite = true)
            sourceFile.delete()
        }
    }

    private suspend fun verifyUri(uri: Uri, manifest: ModelManifest): ModelIntegrityResult {
        return withContext(dispatchers.io) {
            val sizeBytes = runCatching { querySizeBytes(uri) }.getOrDefault(-1L)
            if (manifest.expectedSizeBytes > 0 && sizeBytes > 0 && sizeBytes != manifest.expectedSizeBytes) {
                return@withContext ModelIntegrityResult.SizeMismatch(
                    actualSizeBytes = sizeBytes,
                    expectedSizeBytes = manifest.expectedSizeBytes
                )
            }

            runCatching {
                integrityChecker.verifyStream(
                    openStream = {
                        appContext.contentResolver.openInputStream(uri)
                            ?: error("Unable to open selected model file")
                    },
                    manifest = manifest
                )
            }.getOrElse {
                ModelIntegrityResult.Missing
            }
        }
    }

    private fun openExternalModelPath(location: ModelAssetLocation.ExternalUriLocation): String {
        resolveDirectReadablePath(location.uri)?.let { return it }

        error(
            "External model '${location.displayName}' is not exposed as a directly readable file path. " +
                "LiteRT-LM cannot initialize from Android content URI storage. " +
                "Move it to Android/data/com.example.offlinelifeline/files/models or choose a directly readable file."
        )
    }

    private fun resolveDirectReadablePath(uri: Uri): String? {
        val descriptor = appContext.contentResolver.openFileDescriptor(uri, "r")
            ?: error("Unable to open selected model file")
        val fdPath = "/proc/self/fd/${descriptor.fd}"
        val directPath = runCatching { Os.readlink(fdPath) }.getOrNull()
        if (!directPath.isNullOrBlank()) {
            val directFile = File(directPath)
            if (directFile.exists() && directFile.isFile && directFile.canRead()) {
                runCatching { descriptor.close() }
                return directFile.absolutePath
            }
        }
        runCatching { descriptor.close() }
        return null
    }

    private fun takePersistableReadPermission(uri: Uri) {
        try {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers grant only a session permission. The model will remain usable
            // until Android revokes that URI permission.
        }
    }

    private fun deleteSourceDocument(uri: Uri): Boolean {
        return runCatching {
            DocumentsContract.deleteDocument(appContext.contentResolver, uri)
        }.getOrDefault(false)
    }

    private fun querySizeBytes(uri: Uri): Long {
        runCatching {
            appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                if (descriptor.length > 0L) {
                    return descriptor.length
                }
            }
        }

        runCatching {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst()) {
                    return cursor.getLong(sizeIndex)
                }
            }
        }
        return -1L
    }

    private fun queryDisplayName(uri: Uri): String? {
        runCatching {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return null
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

data class ExternalModelReference(
    val uriString: String,
    val displayName: String?
)

sealed class ModelAssetLocation {
    data class FileLocation(val file: File) : ModelAssetLocation()
    data class AssetLocation(val assetName: String) : ModelAssetLocation()
    data class ExternalUriLocation(
        val uri: Uri,
        val displayName: String,
        val modelId: String
    ) : ModelAssetLocation()

    val description: String
        get() = when (this) {
            is FileLocation -> file.absolutePath
            is AssetLocation -> "assets/$assetName"
            is ExternalUriLocation -> displayName
        }
}

fun ModelAssetLocation.absoluteModelPath(): String {
    return when (this) {
        is ModelAssetLocation.FileLocation -> file.absolutePath
        is ModelAssetLocation.AssetLocation -> error("Asset model must be copied to a file before LiteRT-LM initialization")
        is ModelAssetLocation.ExternalUriLocation -> error("External URI models must be opened through ModelAssetManager")
    }
}
