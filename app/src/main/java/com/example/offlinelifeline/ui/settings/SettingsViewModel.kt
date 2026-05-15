package com.example.offlinelifeline.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
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
import kotlinx.coroutines.Job
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

    private val downloadJobs = mutableMapOf<String, Job>()
    private val availabilityFlows = mutableMapOf<String, MutableStateFlow<ModelAssetCheckResult?>>()

    init {
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
                modelDownloadRepository.refreshDownloadedState(manifest)
                refreshModelAvailability(manifest)
            }
        }
    }

    fun setLanguageTag(tag: String) {
        viewModelScope.launch { settingsStore.setLanguageTag(tag) }
    }

    fun enqueueDownload(manifest: ModelManifest) {
        val current = modelDownloadRepository.getDownloadState(manifest.modelId).value
        if (current is ModelDownloadState.Downloading || current is ModelDownloadState.Queued) return

        workManager.cancelUniqueWork(ModelDownloadWorker.workName(manifest.modelId))
        val job = viewModelScope.launch {
            modelDownloadRepository.startDownload(manifest)
            refreshModelAvailability(manifest)
        }
        downloadJobs[manifest.modelId] = job
    }

    fun cancelDownload(manifest: ModelManifest) {
        modelDownloadRepository.cancelDownload(manifest.modelId, manifest)
        downloadJobs[manifest.modelId]?.cancel()
        downloadJobs.remove(manifest.modelId)
        workManager.cancelUniqueWork(ModelDownloadWorker.workName(manifest.modelId))
    }

    fun switchActiveModel(modelId: String) {
        viewModelScope.launch {
            val manifest = ModelCatalog.findById(modelId) ?: return@launch
            val checkResult = refreshModelAvailability(manifest)
            if (checkResult.runtimeState == ModelRuntimeState.ReadyToLoad) {
                settingsStore.setActiveModelId(modelId)
            }
        }
    }

    private suspend fun refreshModelAvailability(manifest: ModelManifest): ModelAssetCheckResult {
        val checkResult = modelAssetManager.checkModel(manifest)
        getOrCreateAvailabilityFlow(manifest.modelId).value = checkResult
        return checkResult
    }

    private fun getOrCreateAvailabilityFlow(modelId: String): MutableStateFlow<ModelAssetCheckResult?> {
        return availabilityFlows.getOrPut(modelId) {
            MutableStateFlow(null)
        }
    }

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
