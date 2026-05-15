package com.example.offlinelifeline.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.offlinelifeline.data.datastore.AppSettings
import com.example.offlinelifeline.data.datastore.SettingsStore
import com.example.offlinelifeline.inference.ModelManifest
import com.example.offlinelifeline.inference.download.ModelDownloadRepository
import com.example.offlinelifeline.inference.download.ModelDownloadState
import com.example.offlinelifeline.inference.download.ModelDownloadWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val modelDownloadRepository: ModelDownloadRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    private val downloadJobs = mutableMapOf<String, Job>()

    fun getDownloadState(modelId: String): StateFlow<ModelDownloadState> {
        return modelDownloadRepository.getDownloadState(modelId)
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
        viewModelScope.launch { settingsStore.setActiveModelId(modelId) }
    }

    class Factory(
        private val settingsStore: SettingsStore,
        private val modelDownloadRepository: ModelDownloadRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                settingsStore = settingsStore,
                modelDownloadRepository = modelDownloadRepository,
                workManager = WorkManager.getInstance(context)
            ) as T
        }
    }
}
