package com.example.offlinelifeline.ui.toolbox

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.offlinelifeline.core.model.ToolType
import com.example.offlinelifeline.ui.emergencycard.EmergencyCardScreen
import com.example.offlinelifeline.ui.emergencycard.EmergencyCardViewModel
import com.example.offlinelifeline.ui.i18n.LocalAppStrings

@Composable
fun ToolboxScreen(
    viewModel: ToolboxViewModel,
    emergencyCardViewModel: EmergencyCardViewModel,
    selectedTool: ToolType?,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var panel by rememberSaveable { mutableStateOf(ToolboxPanel.Home) }
    var showScreenSos by remember { mutableStateOf(false) }

    if (showScreenSos) {
        ScreenSosDialog(onDismiss = { showScreenSos = false })
    }

    LaunchedEffect(selectedTool) {
        when (selectedTool) {
            ToolType.SOS_FLASHLIGHT -> panel = ToolboxPanel.Flashlight
            ToolType.SCREEN_SOS -> showScreenSos = true
            ToolType.BATTERY_SAVER_ADVICE -> {
                viewModel.refreshBatteryStatus()
                panel = ToolboxPanel.Battery
            }
            ToolType.DEBUG_LOG_EXPORT -> panel = ToolboxPanel.DebugLog
            ToolType.EMERGENCY_CARD -> panel = ToolboxPanel.EmergencyCard
            ToolType.OFFLINE_GUIDE,
            null -> Unit
        }
    }

    when (panel) {
        ToolboxPanel.Home -> ToolboxHome(
            onOpenFlashlight = { panel = ToolboxPanel.Flashlight },
            onOpenScreenSos = { showScreenSos = true },
            onOpenBattery = {
                viewModel.refreshBatteryStatus()
                panel = ToolboxPanel.Battery
            },
            onOpenEmergencyCard = { panel = ToolboxPanel.EmergencyCard },
            onOpenDebugLog = { panel = ToolboxPanel.DebugLog },
            modifier = modifier
        )
        ToolboxPanel.Flashlight -> FlashlightTool(
            uiState = uiState,
            onBack = {
                viewModel.stopSosFlash()
                panel = ToolboxPanel.Home
            },
            onTorchChanged = viewModel::setTorchEnabled,
            onStartSos = viewModel::startSosFlash,
            onStopSos = viewModel::stopSosFlash,
            modifier = modifier
        )
        ToolboxPanel.Battery -> BatteryTool(
            uiState = uiState,
            onBack = { panel = ToolboxPanel.Home },
            onRefresh = viewModel::refreshBatteryStatus,
            modifier = modifier
        )
        ToolboxPanel.EmergencyCard -> EmergencyCardScreen(
            viewModel = emergencyCardViewModel,
            modifier = modifier
        )
        ToolboxPanel.DebugLog -> DebugLogTool(
            uiState = uiState,
            onBack = { panel = ToolboxPanel.Home },
            onExport = viewModel::exportLogs,
            onRecordSnapshot = viewModel::recordStabilitySnapshot,
            modifier = modifier
        )
    }
}

@Composable
private fun ToolboxHome(
    onOpenFlashlight: () -> Unit,
    onOpenScreenSos: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenEmergencyCard: () -> Unit,
    onOpenDebugLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ToolCard(strings.sosFlashlightTitle, strings.sosFlashlightBody, onOpenFlashlight) }
        item { ToolCard(strings.screenSosTitle, strings.screenSosBody, onOpenScreenSos) }
        item { ToolCard(strings.batteryAdviceTitle, strings.batteryAdviceBody, onOpenBattery) }
        item { ToolCard(strings.emergencyCardEditorTitle, strings.emergencyCardToolBody, onOpenEmergencyCard) }
        item { ToolCard(strings.debugLogTitle, strings.debugLogBody, onOpenDebugLog) }
    }
}

@Composable
private fun ToolCard(
    title: String,
    body: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FlashlightTool(
    uiState: ToolboxUiState,
    onBack: () -> Unit,
    onTorchChanged: (Boolean) -> Unit,
    onStartSos: () -> Unit,
    onStopSos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onTorchChanged(true)
    }

    fun withCameraPermission(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose { onStopSos() }
    }

    ToolPanelScaffold(title = strings.sosFlashlightTitle, onBack = onBack, modifier = modifier) {
        Text("${strings.flashlightStatus}: ${if (uiState.isTorchOn) strings.flashlightOn else strings.flashlightOff}")
        Text("${strings.sosFlashStatus}: ${if (uiState.isSosFlashing) strings.running else strings.notRunning}")
        uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                enabled = uiState.isTorchAvailable && !uiState.isSosFlashing,
                onClick = { withCameraPermission { onTorchChanged(!uiState.isTorchOn) } }
            ) {
                Text(if (uiState.isTorchOn) strings.turnOff else strings.turnOn)
            }
            Button(
                enabled = uiState.isTorchAvailable,
                onClick = {
                    withCameraPermission {
                        if (uiState.isSosFlashing) onStopSos() else onStartSos()
                    }
                }
            ) {
                Text(if (uiState.isSosFlashing) strings.stopSos else strings.startSos)
            }
        }
    }
}

@Composable
private fun BatteryTool(
    uiState: ToolboxUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    ToolPanelScaffold(title = strings.batteryAdviceTitle, onBack = onBack, modifier = modifier) {
        Text(
            text = "${strings.currentBattery}: ${uiState.batteryStatus.percent?.let { "$it%" } ?: strings.unknown}"
        )
        Text(
            text = if (uiState.batteryStatus.isCharging) strings.charging else strings.notCharging
        )
        Button(onClick = onRefresh) {
            Text(strings.refresh)
        }
        strings.batteryAdvice(uiState.batteryStatus).forEachIndexed { index, item ->
            Text("${index + 1}. $item")
        }
    }
}

@Composable
private fun DebugLogTool(
    uiState: ToolboxUiState,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onRecordSnapshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    ToolPanelScaffold(title = strings.debugLogTitle, onBack = onBack, modifier = modifier) {
        Text(strings.debugLogNote)
        Button(onClick = onRecordSnapshot) {
            Text(strings.recordSnapshot)
        }
        Button(onClick = onExport) {
            Text(strings.exportTxt)
        }
        uiState.stabilitySnapshotMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
        uiState.exportedLogFile?.let {
            Text(strings.exported(it.absolutePath))
        }
        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ToolPanelScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(strings.backToToolbox)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun ScreenSosDialog(
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        val originalKeepScreenOn = view.keepScreenOn
        val originalBrightness = activity?.window?.attributes?.screenBrightness
        view.keepScreenOn = true
        activity?.window?.attributes = activity?.window?.attributes?.apply {
            screenBrightness = 1f
        }
        onDispose {
            view.keepScreenOn = originalKeepScreenOn
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = originalBrightness ?: -1f
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "SOS",
                    color = Color.Red,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "HELP",
                    color = Color.Black,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onDismiss) {
                    Text(strings.exit)
                }
            }
        }
    }
}

private enum class ToolboxPanel {
    Home,
    Flashlight,
    Battery,
    EmergencyCard,
    DebugLog
}
