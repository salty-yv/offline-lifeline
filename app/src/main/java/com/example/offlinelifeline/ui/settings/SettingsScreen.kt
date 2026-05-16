package com.example.offlinelifeline.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.inference.ModelCatalog
import com.example.offlinelifeline.ui.components.LifelineTopBar
import com.example.offlinelifeline.ui.i18n.LocalAppStrings
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val settings by viewModel.settings.collectAsState()
    val modelStrings = remember(strings.languageTag) {
        ModelManagementStrings.forLanguage(strings.languageTag)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LifelineTopBar(title = strings.settingsTitle)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.languageSectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.languageSectionBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LanguageOption(
                        title = strings.simplifiedChinese,
                        tag = "zh-CN",
                        selectedTag = settings.languageTag,
                        onLanguageSelected = viewModel::setLanguageTag
                    )
                    LanguageOption(
                        title = strings.english,
                        tag = "en-US",
                        selectedTag = settings.languageTag,
                        onLanguageSelected = viewModel::setLanguageTag
                    )
                }
            }
        }

            item {
                ChatTextSizeCard(
                    languageTag = strings.languageTag,
                    chatTextSizeSp = settings.chatTextSizeSp,
                    onChatTextSizeChanged = viewModel::setChatTextSizeSp
                )
            }

            item {
                Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = modelStrings.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = modelStrings.recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = viewModel::refreshAllModelAvailability,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(modelStrings.rescanLocalModels)
                    }
                    HorizontalDivider()

                    ModelCatalog.all.forEachIndexed { index, manifest ->
                        val downloadState by viewModel
                            .getDownloadState(manifest.modelId)
                            .collectAsState()
                        val modelAvailability by viewModel
                            .getModelAvailability(manifest.modelId)
                            .collectAsState()

                        ModelRowWithState(
                            manifest = manifest,
                            downloadState = downloadState,
                            modelAvailability = modelAvailability,
                            strings = modelStrings,
                            isActive = settings.activeModelId == manifest.modelId,
                            onDownload = viewModel::enqueueDownload,
                            onCancel = viewModel::cancelDownload,
                            onSwitch = viewModel::switchActiveModel
                        )

                        if (index < ModelCatalog.all.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ChatTextSizeCard(
    languageTag: String,
    chatTextSizeSp: Int,
    onChatTextSizeChanged: (Int) -> Unit
) {
    val isEnglish = languageTag.startsWith("en")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Chat text size" else "对话字号",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isEnglish) {
                            "Adjust the message text size on the chat page."
                        } else {
                            "调整对话页面消息文字的大小。"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${chatTextSizeSp}sp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value = chatTextSizeSp.toFloat(),
                onValueChange = { onChatTextSizeChanged(it.roundToInt()) },
                valueRange = 14f..22f,
                steps = 7
            )
        }
    }
}

@Composable
private fun LanguageOption(
    title: String,
    tag: String,
    selectedTag: String,
    onLanguageSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        RadioButton(
            selected = selectedTag == tag,
            onClick = { onLanguageSelected(tag) }
        )
    }
}
