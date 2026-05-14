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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.offlinelifeline.di.AppContainer
import com.example.offlinelifeline.ui.chat.ChatScreen
import com.example.offlinelifeline.ui.chat.ChatViewModel
import com.example.offlinelifeline.ui.guide.GuideScreen
import com.example.offlinelifeline.ui.guide.GuideViewModel

@Composable
fun AppNavHost(
    selectedRoute: Route,
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
                    modelAssetManager = appContainer.modelAssetManager
                )
            )
            ChatScreen(
                viewModel = chatViewModel,
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
