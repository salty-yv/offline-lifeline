package com.example.offlinelifeline.ui.toolbox

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.offlinelifeline.R
import com.example.offlinelifeline.core.model.ToolType
import com.example.offlinelifeline.ui.components.LifelineCard
import com.example.offlinelifeline.ui.components.LifelineIconBubble
import com.example.offlinelifeline.ui.components.LifelineTopBar
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

    BackHandler(enabled = showScreenSos) {
        showScreenSos = false
    }

    BackHandler(enabled = panel != ToolboxPanel.Home) {
        if (panel == ToolboxPanel.Flashlight) {
            viewModel.stopSosFlash()
        }
        panel = ToolboxPanel.Home
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LifelineTopBar(title = strings.routeToolbox)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { ToolCard(R.drawable.ic_tool_flashlight_24, strings.sosFlashlightTitle, strings.sosFlashlightBody, onOpenFlashlight) }
            item { ToolCard(R.drawable.ic_tool_sos_24, strings.screenSosTitle, strings.screenSosBody, onOpenScreenSos) }
            item { ToolCard(R.drawable.ic_tool_battery_24, strings.batteryAdviceTitle, strings.batteryAdviceBody, onOpenBattery) }
            item { ToolCard(R.drawable.ic_nav_card_24, strings.emergencyCardEditorTitle, strings.emergencyCardToolBody, onOpenEmergencyCard) }
            item { ToolCard(R.drawable.ic_tool_log_24, strings.debugLogTitle, strings.debugLogBody, onOpenDebugLog) }
        }
    }
}

@Composable
private fun ToolCard(
    iconRes: Int,
    title: String,
    body: String,
    onClick: () -> Unit
) {
    LifelineCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                LifelineIconBubble(modifier = Modifier.size(48.dp)) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.titleMedium,
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
        Surface(
            modifier = Modifier.size(200.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (uiState.isTorchOn || uiState.isSosFlashing) "SOS" else "OFF",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (uiState.isSosFlashing) strings.running else strings.notRunning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LifelineCard {
            Text(
                text = "${strings.flashlightStatus}: ${if (uiState.isTorchOn) strings.flashlightOn else strings.flashlightOff}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${strings.sosFlashStatus}: ${if (uiState.isSosFlashing) strings.running else strings.notRunning}",
                style = MaterialTheme.typography.bodyLarge
            )
            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    enabled = uiState.isTorchAvailable && !uiState.isSosFlashing,
                    onClick = { withCameraPermission { onTorchChanged(!uiState.isTorchOn) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.isTorchOn) strings.turnOff else strings.turnOn)
                }
                Button(
                    enabled = uiState.isTorchAvailable,
                    onClick = {
                        withCameraPermission {
                            if (uiState.isSosFlashing) onStopSos() else onStartSos()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.isSosFlashing) strings.stopSos else strings.startSos)
                }
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
        val percentText = uiState.batteryStatus.percent?.let { "$it%" } ?: "--"
        Surface(
            modifier = Modifier.size(160.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = percentText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (uiState.batteryStatus.isCharging) strings.charging else strings.notCharging,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        LifelineCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.currentBattery,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onRefresh) {
                    Text(strings.refresh)
                }
            }
        }
        LifelineCard(containerColor = androidx.compose.ui.graphics.Color(0xFFFCE7F3)) {
            Text(
                text = strings.batteryAdviceTitle,
                style = MaterialTheme.typography.titleMedium
            )
            strings.batteryAdvice(uiState.batteryStatus).forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}. $item",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        LifelineCard {
            Text(
                text = strings.debugLogNote,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onRecordSnapshot,
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.recordSnapshot)
            }
            Button(
                onClick = onExport,
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.exportTxt)
            }
        }
        LifelineCard {
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
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LifelineTopBar(
            title = title,
            navigationIcon = Icons.Default.ArrowBack,
            navigationContentDescription = strings.backToToolbox,
            onNavigationClick = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
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
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "S O S",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Tap anywhere to stop",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
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
