package com.example.offlinelifeline.ui.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.data.db.GuideEntity

@Composable
fun GuideScreen(
    viewModel: GuideViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedGuide = uiState.selectedGuide

    if (selectedGuide != null) {
        GuideDetailScreen(
            guide = selectedGuide,
            onBack = viewModel::closeDetail,
            modifier = modifier
        )
    } else {
        GuideListScreen(
            uiState = uiState,
            onQueryChanged = viewModel::onQueryChanged,
            onGuideSelected = viewModel::selectGuide,
            modifier = modifier
        )
    }
}

@Composable
private fun GuideListScreen(
    uiState: GuideUiState,
    onQueryChanged: (String) -> Unit,
    onGuideSelected: (GuideEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("搜索指南") }
        )

        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.weight(1f))
            uiState.errorMessage != null -> MessageState(
                text = uiState.errorMessage,
                modifier = Modifier.weight(1f)
            )
            uiState.visibleGuides.isEmpty() -> MessageState(
                text = "没有找到匹配的离线指南",
                modifier = Modifier.weight(1f)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.visibleGuides,
                    key = { it.id }
                ) { guide ->
                    GuideListItem(
                        guide = guide,
                        onClick = { onGuideSelected(guide) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideListItem(
    guide: GuideEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = guide.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = guide.summary,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                guide.tags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .take(3)
                    .forEach { tag ->
                        AssistChip(
                            onClick = onClick,
                            label = { Text(tag) }
                        )
                    }
            }
        }
    }
}

@Composable
private fun GuideDetailScreen(
    guide: GuideEntity,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Button(onClick = onBack) {
                Text("返回列表")
            }
        }

        item {
            Text(
                text = guide.title,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = guide.summary,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = guide.body,
                modifier = Modifier.padding(top = 18.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageState(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            modifier = Modifier.widthIn(max = 320.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
