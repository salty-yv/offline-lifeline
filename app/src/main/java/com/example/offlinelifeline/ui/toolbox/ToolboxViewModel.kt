package com.example.offlinelifeline.ui.toolbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.offlinelifeline.core.diagnostics.DeviceDiagnosticsLogger
import com.example.offlinelifeline.core.logging.DebugLogExporter
import com.example.offlinelifeline.device.battery.BatteryAdviceGenerator
import com.example.offlinelifeline.device.battery.BatteryStatusProvider
import com.example.offlinelifeline.device.flashlight.FlashlightController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ToolboxViewModel(
    private val flashlightController: FlashlightController,
    private val batteryStatusProvider: BatteryStatusProvider,
    private val batteryAdviceGenerator: BatteryAdviceGenerator,
    private val debugLogExporter: DebugLogExporter,
    private val deviceDiagnosticsLogger: DeviceDiagnosticsLogger
) : ViewModel() {
    private val _uiState = MutableStateFlow(ToolboxUiState())
    val uiState: StateFlow<ToolboxUiState> = _uiState

    private var sosJob: Job? = null

    init {
        refreshBatteryStatus()
        viewModelScope.launch {
            _uiState.update { it.copy(isTorchAvailable = flashlightController.isAvailable()) }
        }
    }

    fun refreshBatteryStatus() {
        val status = batteryStatusProvider.getStatus()
        _uiState.update {
            it.copy(
                batteryStatus = status,
                batteryAdvice = batteryAdviceGenerator.buildAdvice(status),
                errorMessage = null
            )
        }
    }

    fun setTorchEnabled(enabled: Boolean) {
        viewModelScope.launch {
            flashlightController.setTorchEnabled(enabled)
                .onSuccess {
                    _uiState.update {
                        it.copy(isTorchOn = enabled, errorMessage = null)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "闪光灯操作失败")
                    }
                }
        }
    }

    fun startSosFlash() {
        if (_uiState.value.isSosFlashing) return
        sosJob = viewModelScope.launch {
            _uiState.update { it.copy(isSosFlashing = true, errorMessage = null) }
            runCatching {
                while (true) {
                    flashSignal(short = true)
                    flashSignal(short = true)
                    flashSignal(short = true)
                    delay(450)
                    flashSignal(short = false)
                    flashSignal(short = false)
                    flashSignal(short = false)
                    delay(450)
                    flashSignal(short = true)
                    flashSignal(short = true)
                    flashSignal(short = true)
                    delay(1_200)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(errorMessage = throwable.message ?: "SOS 闪光失败")
                }
            }
        }
    }

    fun stopSosFlash() {
        sosJob?.cancel()
        sosJob = null
        viewModelScope.launch {
            flashlightController.setTorchEnabled(false)
            _uiState.update {
                it.copy(isSosFlashing = false, isTorchOn = false)
            }
        }
    }

    fun exportLogs() {
        viewModelScope.launch {
            debugLogExporter.exportText()
                .onSuccess { file ->
                    _uiState.update {
                        it.copy(exportedLogFile = file, errorMessage = null)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "Debug Log 导出失败")
                    }
                }
        }
    }

    fun recordStabilitySnapshot() {
        viewModelScope.launch {
            runCatching {
                deviceDiagnosticsLogger.logSnapshot("manual_toolbox")
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        stabilitySnapshotMessage = "Stability snapshot written to Debug Log",
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(errorMessage = throwable.message ?: "Stability snapshot failed")
                }
            }
        }
    }

    private suspend fun flashSignal(short: Boolean) {
        val onDuration = if (short) 220L else 650L
        flashlightController.setTorchEnabled(true).getOrThrow()
        _uiState.update { it.copy(isTorchOn = true) }
        delay(onDuration)
        flashlightController.setTorchEnabled(false).getOrThrow()
        _uiState.update { it.copy(isTorchOn = false) }
        delay(220)
    }

    override fun onCleared() {
        stopSosFlash()
        super.onCleared()
    }

    class Factory(
        private val flashlightController: FlashlightController,
        private val batteryStatusProvider: BatteryStatusProvider,
        private val batteryAdviceGenerator: BatteryAdviceGenerator,
        private val debugLogExporter: DebugLogExporter,
        private val deviceDiagnosticsLogger: DeviceDiagnosticsLogger
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ToolboxViewModel::class.java)) {
                return ToolboxViewModel(
                    flashlightController = flashlightController,
                    batteryStatusProvider = batteryStatusProvider,
                    batteryAdviceGenerator = batteryAdviceGenerator,
                    debugLogExporter = debugLogExporter,
                    deviceDiagnosticsLogger = deviceDiagnosticsLogger
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
