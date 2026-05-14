package com.example.offlinelifeline.ui.toolbox

import com.example.offlinelifeline.device.battery.BatteryStatus
import java.io.File

data class ToolboxUiState(
    val isTorchAvailable: Boolean = false,
    val isTorchOn: Boolean = false,
    val isSosFlashing: Boolean = false,
    val batteryStatus: BatteryStatus = BatteryStatus(percent = null, isCharging = false),
    val batteryAdvice: List<String> = emptyList(),
    val exportedLogFile: File? = null,
    val stabilitySnapshotMessage: String? = null,
    val errorMessage: String? = null
)
