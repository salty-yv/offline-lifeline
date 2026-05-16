package com.example.offlinelifeline.inference.download

import android.content.Context
import com.example.offlinelifeline.inference.ModelIntegrityChecker
import com.example.offlinelifeline.inference.ModelIntegrityResult
import com.example.offlinelifeline.inference.ModelManifest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

class ModelDownloadRepository(
    private val context: Context,
    private val integrityChecker: ModelIntegrityChecker = ModelIntegrityChecker(),
    private val onStateChanged: suspend (String, ModelDownloadState) -> Unit = { _, _ -> }
) {
    private val stateFlows = mutableMapOf<String, MutableStateFlow<ModelDownloadState>>()
    private val cancelledDownloads = mutableSetOf<String>()
    private val activeCalls = mutableMapOf<String, Call>()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .build()
    }

    fun getDownloadState(modelId: String): StateFlow<ModelDownloadState> {
        return getOrCreateStateFlow(modelId).asStateFlow()
    }

    fun setDownloadState(modelId: String, state: ModelDownloadState) {
        getOrCreateStateFlow(modelId).value = state
    }

    suspend fun startDownload(
        manifest: ModelManifest,
        preferMirror: Boolean = false
    ) {
        val stateFlow = getOrCreateStateFlow(manifest.modelId)
        val mutex = downloadMutexFor(manifest.modelId)
        if (mutex.isLocked) {
            emitState(manifest.modelId, stateFlow, ModelDownloadState.Queued)
        }
        mutex.withLock {
            synchronized(cancelledDownloads) {
                cancelledDownloads.remove(manifest.modelId)
            }
            startDownloadLocked(manifest, preferMirror)
        }
    }

    private suspend fun startDownloadLocked(
        manifest: ModelManifest,
        preferMirror: Boolean
    ) {
        val stateFlow = getOrCreateStateFlow(manifest.modelId)
        val modelDir = getModelDir()
        val targetFile = File(modelDir, manifest.fileName)
        val tmpFile = File(modelDir, "${manifest.fileName}.tmp")

        if (targetFile.exists()) {
            emitState(
                manifest.modelId,
                stateFlow,
                ModelDownloadState.Downloading(
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length(),
                    progressFraction = 1f
                )
            )
            verifyAndEmit(stateFlow, targetFile, manifest)
            return
        }

        emitState(
            manifest.modelId,
            stateFlow,
            ModelDownloadState.Downloading(
                downloadedBytes = tmpFile.length(),
                totalBytes = -1L,
                progressFraction = -1f
            )
        )

        withContext(Dispatchers.IO) {
            try {
                downloadFromAvailableUrl(manifest, stateFlow, tmpFile, preferMirror)

                moveIntoPlace(tmpFile, targetFile)
                if (!targetFile.exists()) {
                    emitState(
                        manifest.modelId,
                        stateFlow,
                        ModelDownloadState.Failed("Downloaded model file was not created")
                    )
                    return@withContext
                }

                verifyAndEmit(stateFlow, targetFile, manifest)
            } catch (e: CancellationException) {
                synchronized(activeCalls) {
                    activeCalls.remove(manifest.modelId)
                }?.cancel()
                emitState(
                    manifest.modelId,
                    stateFlow,
                    if (wasCancelledByUser(manifest.modelId)) {
                        ModelDownloadState.Idle
                    } else {
                        ModelDownloadState.Paused
                    }
                )
                throw e
            } catch (e: UnknownHostException) {
                emitState(
                    manifest.modelId,
                    stateFlow,
                    ModelDownloadState.Failed("Cannot connect to the model download server")
                )
            } catch (e: SocketTimeoutException) {
                emitState(
                    manifest.modelId,
                    stateFlow,
                    ModelDownloadState.Failed("Connection timed out while downloading the model")
                )
            } catch (e: IOException) {
                emitState(
                    manifest.modelId,
                    stateFlow,
                    if (wasCancelledByUser(manifest.modelId)) {
                        ModelDownloadState.Idle
                    } else {
                        ModelDownloadState.Failed(e.message ?: "Network error while downloading the model")
                    }
                )
            } catch (e: Exception) {
                emitState(
                    manifest.modelId,
                    stateFlow,
                    ModelDownloadState.Failed(e.message ?: "Unknown model download error")
                )
            }
        }
    }

    private suspend fun emitState(
        modelId: String,
        stateFlow: MutableStateFlow<ModelDownloadState>,
        state: ModelDownloadState
    ) {
        stateFlow.value = state
        onStateChanged(modelId, state)
    }

    fun pauseDownload(modelId: String) {
        val stateFlow = getOrCreateStateFlow(modelId)
        if (stateFlow.value is ModelDownloadState.Downloading) {
            stateFlow.value = ModelDownloadState.Paused
        }
    }

    fun cancelDownload(modelId: String, manifest: ModelManifest) {
        synchronized(cancelledDownloads) {
            cancelledDownloads.add(modelId)
        }
        synchronized(activeCalls) {
            activeCalls.remove(modelId)
        }?.cancel()
        getOrCreateStateFlow(modelId).value = ModelDownloadState.Idle
        val tmpFile = File(getModelDir(), "${manifest.fileName}.tmp")
        tmpFile.delete()
    }

    fun getDownloadedFile(manifest: ModelManifest): File? {
        val file = File(getModelDir(), manifest.fileName)
        return if (file.exists()) file else null
    }

    suspend fun refreshDownloadedState(manifest: ModelManifest) {
        val file = getDownloadedFile(manifest) ?: return
        val stateFlow = getOrCreateStateFlow(manifest.modelId)
        verifyAndEmit(stateFlow, file, manifest)
    }

    private fun getModelDir(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getOrCreateStateFlow(modelId: String): MutableStateFlow<ModelDownloadState> {
        return stateFlows.getOrPut(modelId) {
            MutableStateFlow(ModelDownloadState.Idle)
        }
    }

    private fun wasCancelledByUser(modelId: String): Boolean {
        return synchronized(cancelledDownloads) {
            cancelledDownloads.remove(modelId)
        }
    }

    private fun downloadUrls(
        manifest: ModelManifest,
        preferMirror: Boolean
    ): List<String> {
        val primaryUrls = if (preferMirror) {
            listOfNotNull(manifest.fallbackUrl, manifest.downloadUrl)
        } else {
            listOfNotNull(manifest.downloadUrl, manifest.fallbackUrl)
        }
        return primaryUrls.distinct()
    }

    private suspend fun downloadFromAvailableUrl(
        manifest: ModelManifest,
        stateFlow: MutableStateFlow<ModelDownloadState>,
        tmpFile: File,
        preferMirror: Boolean
    ) {
        var lastFailure: Exception? = null
        for (url in downloadUrls(manifest, preferMirror)) {
            currentCoroutineContext().ensureActive()
            try {
                downloadFromUrl(manifest.modelId, url, stateFlow, tmpFile)
                return
            } catch (e: UnknownHostException) {
                lastFailure = e
            } catch (e: SocketTimeoutException) {
                lastFailure = e
            } catch (e: IOException) {
                if (isCancelledByUser(manifest.modelId)) {
                    throw e
                }
                lastFailure = e
            }
        }
        throw lastFailure ?: IOException("No model download URL is available")
    }

    private suspend fun downloadFromUrl(
        modelId: String,
        url: String,
        stateFlow: MutableStateFlow<ModelDownloadState>,
        tmpFile: File
    ) {
        var resumeOffset = if (tmpFile.exists()) tmpFile.length() else 0L
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "OfflineLifeline/1.0 Android")

        if (resumeOffset > 0) {
            requestBuilder.header("Range", "bytes=$resumeOffset-")
        }

        val call = httpClient.newCall(requestBuilder.build())
        synchronized(activeCalls) {
            activeCalls[modelId] = call
        }

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${response.message}")
                }

                val appending = resumeOffset > 0 && response.code == HTTP_PARTIAL_CONTENT
                if (resumeOffset > 0 && !appending) {
                    tmpFile.delete()
                    resumeOffset = 0L
                }

                val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
                val totalBytes = when {
                    appending && contentLength > 0 -> resumeOffset + contentLength
                    contentLength > 0 -> contentLength
                    else -> -1L
                }
                val body = response.body ?: throw IOException("Empty response body")

                FileOutputStream(tmpFile, appending).use { out ->
                    body.byteStream().use { inputStream ->
                        val buffer = ByteArray(BUFFER_SIZE_BYTES)
                        var downloadedBytes = resumeOffset
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = inputStream.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            downloadedBytes += read
                            val fraction = if (totalBytes > 0) {
                                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                -1f
                            }
                            emitState(
                                modelId,
                                stateFlow,
                                ModelDownloadState.Downloading(
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    progressFraction = fraction
                                )
                            )
                        }
                    }
                }
            }
        } finally {
            synchronized(activeCalls) {
                if (activeCalls[modelId] === call) {
                    activeCalls.remove(modelId)
                }
            }
        }
    }

    private suspend fun verifyAndEmit(
        stateFlow: MutableStateFlow<ModelDownloadState>,
        file: File,
        manifest: ModelManifest
    ) {
        val result = integrityChecker.verifyFile(file, manifest)
        when (result) {
            is ModelIntegrityResult.Valid -> {
                emitState(manifest.modelId, stateFlow, ModelDownloadState.Completed(manifest))
            }

            else -> {
                file.delete()
                emitState(
                    manifest.modelId,
                    stateFlow,
                    ModelDownloadState.Failed("Model integrity check failed: $result")
                )
            }
        }
    }

    private fun moveIntoPlace(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: IOException) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun isCancelledByUser(modelId: String): Boolean {
        return synchronized(cancelledDownloads) {
            cancelledDownloads.contains(modelId)
        }
    }

    private companion object {
        const val BUFFER_SIZE_BYTES = 256 * 1024
        const val HTTP_PARTIAL_CONTENT = 206
        val downloadMutexes = mutableMapOf<String, Mutex>()

        fun downloadMutexFor(modelId: String): Mutex {
            return synchronized(downloadMutexes) {
                downloadMutexes.getOrPut(modelId) { Mutex() }
            }
        }
    }
}
