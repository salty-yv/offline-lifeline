package com.example.offlinelifeline.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        RuntimeStatusBanner(
            runtimeState = uiState.runtimeState,
            modelAssetState = uiState.modelAssetState,
            isGenerating = uiState.isGenerating,
            errorMessage = uiState.errorMessage
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            items(
                items = uiState.messages,
                key = { it.id }
            ) { message ->
                ChatMessageBubble(
                    message = message,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        HorizontalDivider()

        ChatInputBar(
            text = uiState.inputText,
            canSend = uiState.canSend,
            isGenerating = uiState.isGenerating,
            onTextChanged = viewModel::onInputChanged,
            onSend = viewModel::sendMessage,
            onStop = viewModel::stopGeneration
        )
    }
}
