package com.example.offlinelifeline.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.core.model.ModelRuntimeState
import com.example.offlinelifeline.inference.ModelAssetCheckResult
import com.example.offlinelifeline.inference.ModelManifest
import com.example.offlinelifeline.inference.download.ModelDownloadState
import java.util.Locale

data class ModelManagementStrings(
    val title: String,
    val recommendation: String,
    val rescanLocalModels: String,
    val availableContentDescription: String,
    val download: String,
    val retryDownload: String,
    val failurePrefix: String,
    val queued: String,
    val paused: String,
    val resume: String,
    val cancel: String,
    val localModelDetected: String,
    val switchToModel: String,
    val currentlyUsing: String,
    val approx2Gb: String,
    val approx4Gb: String,
    val approxPrefix: String,
    val connecting: String,
    val checkingModel: String,
    val downloadedSuffix: String
) {
    companion object {
        fun forLanguage(languageTag: String): ModelManagementStrings {
            return if (languageTag.startsWith("en")) en() else zh()
        }

        private fun zh() = ModelManagementStrings(
            title = "离线模型管理",
            recommendation = "建议在 Wi-Fi 环境下下载大模型",
            rescanLocalModels = "重新检测本地模型",
            availableContentDescription = "模型已可用",
            download = "下载",
            retryDownload = "重新下载",
            failurePrefix = "失败：",
            queued = "等待下载...",
            paused = "已暂停，点击继续下载",
            resume = "继续",
            cancel = "取消",
            localModelDetected = "已检测到本地模型：",
            switchToModel = "切换到此模型",
            currentlyUsing = "当前正在使用",
            approx2Gb = "约 2 GB",
            approx4Gb = "约 4 GB",
            approxPrefix = "约 ",
            connecting = "正在连接...",
            checkingModel = "正在校验模型...",
            downloadedSuffix = " 已下载"
        )

        private fun en() = ModelManagementStrings(
            title = "Offline model management",
            recommendation = "Use Wi-Fi for large model downloads",
            rescanLocalModels = "Rescan local models",
            availableContentDescription = "Model available",
            download = "Download",
            retryDownload = "Retry download",
            failurePrefix = "Failed: ",
            queued = "Waiting to download...",
            paused = "Paused. Tap continue to resume.",
            resume = "Continue",
            cancel = "Cancel",
            localModelDetected = "Local model found: ",
            switchToModel = "Switch to this model",
            currentlyUsing = "Currently in use",
            approx2Gb = "About 2 GB",
            approx4Gb = "About 4 GB",
            approxPrefix = "About ",
            connecting = "Connecting...",
            checkingModel = "Checking model...",
            downloadedSuffix = " downloaded"
        )
    }
}

@Composable
fun ModelRowWithState(
    manifest: ModelManifest,
    downloadState: ModelDownloadState,
    modelAvailability: ModelAssetCheckResult?,
    strings: ModelManagementStrings,
    isActive: Boolean,
    onDownload: (ModelManifest) -> Unit,
    onCancel: (ModelManifest) -> Unit,
    onSwitch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isChecking = modelAvailability?.runtimeState == ModelRuntimeState.Checking
    val isLocalAvailable = downloadState is ModelDownloadState.Completed ||
        modelAvailability?.runtimeState == ModelRuntimeState.ReadyToLoad

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = manifest.modelName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = modelSizeLabel(manifest, strings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLocalAvailable) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = strings.availableContentDescription,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isChecking) {
            Text(
                text = strings.checkingModel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else when (downloadState) {
            is ModelDownloadState.Idle, is ModelDownloadState.Failed -> {
                if (isLocalAvailable) {
                    LocalModelSwitchContent(
                        manifest = manifest,
                        isActive = isActive,
                        locationDescription = modelAvailability?.location?.description,
                        strings = strings,
                        onSwitch = onSwitch
                    )
                } else {
                    val label = if (downloadState is ModelDownloadState.Failed) strings.retryDownload else strings.download
                    Button(
                        onClick = { onDownload(manifest) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label)
                    }
                    if (downloadState is ModelDownloadState.Failed) {
                        Text(
                            text = "${strings.failurePrefix}${downloadState.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            is ModelDownloadState.Queued -> {
                Text(
                    text = strings.queued,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                CancelButton(strings = strings, onClick = { onCancel(manifest) })
            }

            is ModelDownloadState.Downloading -> {
                val progress = downloadState.progressFraction
                Text(
                    text = downloadProgressText(downloadState, strings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                CancelButton(strings = strings, onClick = { onCancel(manifest) })
            }

            is ModelDownloadState.Paused -> {
                Text(
                    text = strings.paused,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onDownload(manifest) },
                        modifier = Modifier.weight(1f)
                    ) { Text(strings.resume) }
                    OutlinedButton(
                        onClick = { onCancel(manifest) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text(strings.cancel) }
                }
            }

            is ModelDownloadState.Completed -> {
                LocalModelSwitchContent(
                    manifest = manifest,
                    isActive = isActive,
                    locationDescription = modelAvailability?.location?.description,
                    strings = strings,
                    onSwitch = onSwitch
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun LocalModelSwitchContent(
    manifest: ModelManifest,
    isActive: Boolean,
    locationDescription: String?,
    strings: ModelManagementStrings,
    onSwitch: (String) -> Unit
) {
    if (!locationDescription.isNullOrBlank()) {
        Text(
            text = "${strings.localModelDetected}${shortLocation(locationDescription)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (!isActive) {
        Button(
            onClick = { onSwitch(manifest.modelId) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(strings.switchToModel) }
    } else {
        Text(
            text = strings.currentlyUsing,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CancelButton(
    strings: ModelManagementStrings,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text(strings.cancel)
    }
}

private fun modelSizeLabel(
    manifest: ModelManifest,
    strings: ModelManagementStrings
): String {
    return when {
        manifest.expectedSizeBytes > 0 -> "${strings.approxPrefix}${formatGb(manifest.expectedSizeBytes)} GB"
        manifest.modelId == "e2b" -> strings.approx2Gb
        else -> strings.approx4Gb
    }
}

private fun downloadProgressText(
    state: ModelDownloadState.Downloading,
    strings: ModelManagementStrings
): String {
    val progress = state.progressFraction
    if (progress < 0f) {
        return strings.connecting
    }

    val downloaded = formatGb(state.downloadedBytes)
    val total = state.totalBytes.takeIf { it > 0 }?.let(::formatGb)
    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()

    return if (total != null) {
        "$percent%  $downloaded GB / $total GB"
    } else {
        "$downloaded GB${strings.downloadedSuffix}"
    }
}

private fun shortLocation(description: String): String {
    return if (description.length <= 48) {
        description
    } else {
        "..." + description.takeLast(45)
    }
}

private fun formatGb(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return String.format(Locale.US, "%.2f", gb)
}
