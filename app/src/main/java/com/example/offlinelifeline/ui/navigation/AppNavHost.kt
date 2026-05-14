package com.example.offlinelifeline.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.core.model.ToolType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.offlinelifeline.di.AppContainer
import com.example.offlinelifeline.ui.chat.ChatScreen
import com.example.offlinelifeline.ui.chat.ChatViewModel
import com.example.offlinelifeline.ui.emergencycard.EmergencyCardScreen
import com.example.offlinelifeline.ui.emergencycard.EmergencyCardViewModel
import com.example.offlinelifeline.ui.guide.GuideScreen
import com.example.offlinelifeline.ui.guide.GuideViewModel
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
    when (selectedRoute) {
        Route.Chat -> {
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.Factory(
                    chatRepository = appContainer.chatRepository,
                    llmEngine = appContainer.localLlmEngine,
                    survivalAgent = appContainer.survivalAgent,
                    modelAssetManager = appContainer.modelAssetManager,
                    imagePreprocessor = appContainer.imagePreprocessor
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

        else -> PlaceholderScreen(
            title = selectedRoute.title,
            body = selectedRoute.placeholderText,
            modifier = modifier
        )
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = body,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
