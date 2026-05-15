package com.example.offlinelifeline.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.offlinelifeline.core.model.ToolType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.offlinelifeline.di.AppContainer
import com.example.offlinelifeline.ui.chat.ChatScreen
import com.example.offlinelifeline.ui.chat.ChatViewModel
import com.example.offlinelifeline.ui.emergencycard.EmergencyCardScreen
import com.example.offlinelifeline.ui.emergencycard.EmergencyCardViewModel
import com.example.offlinelifeline.ui.guide.GuideScreen
import com.example.offlinelifeline.ui.guide.GuideViewModel
import com.example.offlinelifeline.ui.settings.SettingsScreen
import com.example.offlinelifeline.ui.settings.SettingsViewModel
import com.example.offlinelifeline.ui.toolbox.ToolboxScreen
import com.example.offlinelifeline.ui.toolbox.ToolboxViewModel

@Composable
fun AppNavHost(
    selectedRoute: Route,
    selectedTool: ToolType?,
    onToolSelected: (ToolType) -> Unit,
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    when (selectedRoute) {
        Route.Chat -> {
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.Factory(
                    chatRepository = appContainer.chatRepository,
                    llmEngine = appContainer.localLlmEngine,
                    survivalAgent = appContainer.survivalAgent,
                    modelAssetManager = appContainer.modelAssetManager,
                    imagePreprocessor = appContainer.imagePreprocessor,
                    settingsStore = appContainer.settingsStore
                )
            )
            ChatScreen(
                viewModel = chatViewModel,
                onToolSelected = onToolSelected,
                modifier = modifier
            )
        }

        Route.Toolbox -> {
            val toolboxViewModel: ToolboxViewModel = viewModel(
                factory = ToolboxViewModel.Factory(
                    flashlightController = appContainer.flashlightController,
                    batteryStatusProvider = appContainer.batteryStatusProvider,
                    batteryAdviceGenerator = appContainer.batteryAdviceGenerator,
                    debugLogExporter = appContainer.debugLogExporter,
                    deviceDiagnosticsLogger = appContainer.deviceDiagnosticsLogger
                )
            )
            val emergencyCardViewModel: EmergencyCardViewModel = viewModel(
                key = "toolbox-emergency-card",
                factory = EmergencyCardViewModel.Factory(
                    repository = appContainer.emergencyCardRepository
                )
            )
            ToolboxScreen(
                viewModel = toolboxViewModel,
                emergencyCardViewModel = emergencyCardViewModel,
                selectedTool = selectedTool,
                modifier = modifier
            )
        }

        Route.Guide -> {
            val guideViewModel: GuideViewModel = viewModel(
                factory = GuideViewModel.Factory(
                    guideRepository = appContainer.guideRepository
                )
            )
            GuideScreen(
                viewModel = guideViewModel,
                modifier = modifier
            )
        }

        Route.EmergencyCard -> {
            val emergencyCardViewModel: EmergencyCardViewModel = viewModel(
                key = "main-emergency-card",
                factory = EmergencyCardViewModel.Factory(
                    repository = appContainer.emergencyCardRepository
                )
            )
            EmergencyCardScreen(
                viewModel = emergencyCardViewModel,
                modifier = modifier
            )
        }

        Route.Settings -> {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    settingsStore = appContainer.settingsStore,
                    modelDownloadRepository = appContainer.modelDownloadRepository,
                    modelAssetManager = appContainer.modelAssetManager,
                    context = context
                )
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                modifier = modifier
            )
        }
    }
}
