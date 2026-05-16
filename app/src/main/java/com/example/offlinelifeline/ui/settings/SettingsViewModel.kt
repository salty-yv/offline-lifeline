package com.example.offlinelifeline.ui.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.offlinelifeline.data.datastore.AppSettings
import com.example.offlinelifeline.data.datastore.SettingsStore
import com.example.offlinelifeline.core.model.ModelRuntimeState
import com.example.offlinelifeline.inference.ModelAssetCheckResult
import com.example.offlinelifeline.inference.ModelAssetManager
import com.example.offlinelifeline.inference.ModelCatalog
import com.example.offlinelifeline.inference.ModelManifest
import com.example.offlinelifeline.inference.download.ModelDownloadRepository
import com.example.offlinelifeline.inference.download.ModelDownloadState
import com.example.offlinelifeline.inference.download.ModelDownloadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val modelDownloadRepository: ModelDownloadRepository,
    private val modelAssetManager: ModelAssetManager,
    private val workManager: WorkManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    private val availabilityFlows = mutableMapOf<String, MutableStateFlow<ModelAssetCheckResult?>>()
    private val workObservers = mutableMapOf<String, WorkObserverRegistration>()

    init {
        ModelCatalog.all.forEach(::observeDownloadWork)
        refreshAllModelAvailability()
    }

    fun getDownloadState(modelId: String): StateFlow<ModelDownloadState> {
        return modelDownloadRepository.getDownloadState(modelId)
    }

    fun getModelAvailability(modelId: String): StateFlow<ModelAssetCheckResult?> {
        return getOrCreateAvailabilityFlow(modelId).asStateFlow()
    }

    fun refreshAllModelAvailability() {
        ModelCatalog.all.forEach { manifest ->
            viewModelScope.launch {
                refreshModelAvailability(manifest)
            }
        }
    }

    fun setLanguageTag(tag: String) {
        viewModelScope.launch { settingsStore.setLanguageTag(tag) }
    }

    fun setChatTextSizeSp(sizeSp: Int) {
        viewModelScope.launch { settingsStore.setChatTextSizeSp(sizeSp) }
    }

    fun enqueueDownload(manifest: ModelManifest) {
        val current = modelDownloadRepository.getDownloadState(manifest.modelId).value
        if (current is ModelDownloadState.Downloading || current is ModelDownloadState.Queued) return

        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(
                workDataOf(ModelDownloadWorker.KEY_MODEL_ID to manifest.modelId)
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        modelDownloadRepository.setDownloadState(manifest.modelId, ModelDownloadState.Queued)
        workManager.enqueueUniqueWork(
            ModelDownloadWorker.workName(manifest.modelId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelDownload(manifest: ModelManifest) {
        workManager.cancelUniqueWork(ModelDownloadWorker.workName(manifest.modelId))
        modelDownloadRepository.cancelDownload(manifest.modelId, manifest)
    }

    fun switchActiveModel(modelId: String) {
        viewModelScope.launch {
            val manifest = ModelCatalog.findById(modelId) ?: return@launch
            val cachedCheckResult = getOrCreateAvailabilityFlow(modelId).value
            if (cachedCheckResult?.runtimeState == ModelRuntimeState.ReadyToLoad) {
                settingsStore.setActiveModelId(modelId)
                return@launch
            }

            val checkResult = refreshModelAvailability(manifest)
            if (checkResult.runtimeState == ModelRuntimeState.ReadyToLoad) {
                settingsStore.setActiveModelId(modelId)
            } else {
                modelDownloadRepository.setDownloadState(
                    modelId,
                    ModelDownloadState.Failed(checkResult.message)
                )
            }
        }
    }

    private suspend fun refreshModelAvailability(manifest: ModelManifest): ModelAssetCheckResult {
        getOrCreateAvailabilityFlow(manifest.modelId).value = ModelAssetCheckResult(
            manifest = manifest,
            location = null,
            runtimeState = ModelRuntimeState.Checking,
            message = "Checking model integrity..."
        )
        val checkResult = modelAssetManager.checkModel(manifest)
        getOrCreateAvailabilityFlow(manifest.modelId).value = checkResult
        return checkResult
    }

    private fun getOrCreateAvailabilityFlow(modelId: String): MutableStateFlow<ModelAssetCheckResult?> {
        return availabilityFlows.getOrPut(modelId) {
            MutableStateFlow(null)
        }
    }

    private fun observeDownloadWork(manifest: ModelManifest) {
        val modelId = manifest.modelId
        if (workObservers.containsKey(modelId)) return

        val liveData = workManager.getWorkInfosForUniqueWorkLiveData(
            ModelDownloadWorker.workName(modelId)
        )
        val observer = Observer<List<WorkInfo>> { workInfos ->
            val workInfo = workInfos.firstOrNull() ?: return@Observer
            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                viewModelScope.launch {
                    val checkResult = refreshModelAvailability(manifest)
                    val verifiedState = if (checkResult.runtimeState == ModelRuntimeState.ReadyToLoad) {
                        ModelDownloadState.Completed(manifest)
                    } else {
                        ModelDownloadState.Failed(checkResult.message)
                    }
                    modelDownloadRepository.setDownloadState(modelId, verifiedState)
                }
                return@Observer
            }

            val downloadState = workInfo.toModelDownloadState(manifest)
            modelDownloadRepository.setDownloadState(modelId, downloadState)
        }

        liveData.observeForever(observer)
        workObservers[modelId] = WorkObserverRegistration(liveData, observer)
    }

    private fun WorkInfo.toModelDownloadState(manifest: ModelManifest): ModelDownloadState {
        return when (state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED -> ModelDownloadState.Queued
            WorkInfo.State.RUNNING -> progress.toModelDownloadState(manifest)
            WorkInfo.State.SUCCEEDED -> ModelDownloadState.Queued
            WorkInfo.State.FAILED -> ModelDownloadState.Failed(
                progress.getString(ModelDownloadWorker.KEY_FAILURE_REASON)
                    ?: "Model download failed"
            )
            WorkInfo.State.CANCELLED -> ModelDownloadState.Idle
        }
    }

    private fun androidx.work.Data.toModelDownloadState(manifest: ModelManifest): ModelDownloadState {
        return when (getString(ModelDownloadWorker.KEY_STATE)) {
            ModelDownloadWorker.STATE_DOWNLOADING -> ModelDownloadState.Downloading(
                downloadedBytes = getLong(ModelDownloadWorker.KEY_DOWNLOADED_BYTES, 0L),
                totalBytes = getLong(ModelDownloadWorker.KEY_TOTAL_BYTES, -1L),
                progressFraction = getFloat(ModelDownloadWorker.KEY_PROGRESS_FRACTION, -1f)
            )
            ModelDownloadWorker.STATE_COMPLETED -> ModelDownloadState.Completed(manifest)
            ModelDownloadWorker.STATE_FAILED -> ModelDownloadState.Failed(
                getString(ModelDownloadWorker.KEY_FAILURE_REASON) ?: "Model download failed"
            )
            ModelDownloadWorker.STATE_PAUSED -> ModelDownloadState.Paused
            else -> ModelDownloadState.Queued
        }
    }

    override fun onCleared() {
        workObservers.values.forEach { (liveData, observer) ->
            liveData.removeObserver(observer)
        }
        workObservers.clear()
        super.onCleared()
    }

    private data class WorkObserverRegistration(
        val liveData: LiveData<List<WorkInfo>>,
        val observer: Observer<List<WorkInfo>>
    )

    class Factory(
        private val settingsStore: SettingsStore,
        private val modelDownloadRepository: ModelDownloadRepository,
        private val modelAssetManager: ModelAssetManager,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                settingsStore = settingsStore,
                modelDownloadRepository = modelDownloadRepository,
                modelAssetManager = modelAssetManager,
                workManager = WorkManager.getInstance(context)
            ) as T
        }
    }
}
