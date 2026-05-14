package com.example.offlinelifeline.core.model

sealed class ModelRuntimeState {
    data object NotChecked : ModelRuntimeState()
    data object Checking : ModelRuntimeState()
    data object Missing : ModelRuntimeState()
    data object ChecksumFailed : ModelRuntimeState()
    data object ReadyToLoad : ModelRuntimeState()
    data object Loading : ModelRuntimeState()
    data object Ready : ModelRuntimeState()
    data class Failed(val message: String) : ModelRuntimeState()
    data object Generating : ModelRuntimeState()
    data object Releasing : ModelRuntimeState()
    data object Released : ModelRuntimeState()
}
