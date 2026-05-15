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

@Composable
fun ModelRowWithState(
    manifest: ModelManifest,
    downloadState: ModelDownloadState,
    modelAvailability: ModelAssetCheckResult?,
    isActive: Boolean,
    onDownload: (ModelManifest) -> Unit,
    onCancel: (ModelManifest) -> Unit,
    onSwitch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    text = modelSizeLabel(manifest),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLocalAvailable) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "模型已可用",
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

        when (downloadState) {
            is ModelDownloadState.Idle, is ModelDownloadState.Failed -> {
                if (isLocalAvailable) {
                    LocalModelSwitchContent(
                        manifest = manifest,
                        isActive = isActive,
                        locationDescription = modelAvailability?.location?.description,
                        onSwitch = onSwitch
                    )
                } else {
                    val label = if (downloadState is ModelDownloadState.Failed) "重新下载" else "下载"
                    Button(
                        onClick = { onDownload(manifest) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label)
                    }
                    if (downloadState is ModelDownloadState.Failed) {
                        Text(
                            text = "失败：${downloadState.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            is ModelDownloadState.Queued -> {
                Text(
                    text = "等待下载...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                CancelButton(onClick = { onCancel(manifest) })
            }

            is ModelDownloadState.Downloading -> {
                val progress = downloadState.progressFraction
                Text(
                    text = downloadProgressText(downloadState),
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
                CancelButton(onClick = { onCancel(manifest) })
            }

            is ModelDownloadState.Paused -> {
                Text(
                    text = "已暂停，点击继续下载",
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
                    ) { Text("继续") }
                    OutlinedButton(
                        onClick = { onCancel(manifest) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("取消") }
                }
            }

            is ModelDownloadState.Completed -> {
                LocalModelSwitchContent(
                    manifest = manifest,
                    isActive = isActive,
                    locationDescription = modelAvailability?.location?.description,
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
    onSwitch: (String) -> Unit
) {
    if (!locationDescription.isNullOrBlank()) {
        Text(
            text = "已检测到本地模型：${shortLocation(locationDescription)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (!isActive) {
        Button(
            onClick = { onSwitch(manifest.modelId) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("切换到此模型") }
    } else {
        Text(
            text = "当前正在使用",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CancelButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text("取消")
    }
}

private fun modelSizeLabel(manifest: ModelManifest): String {
    return when {
        manifest.expectedSizeBytes > 0 -> "约 ${formatGb(manifest.expectedSizeBytes)} GB"
        manifest.modelId == "e2b" -> "约 2 GB"
        else -> "约 4 GB"
    }
}

private fun downloadProgressText(state: ModelDownloadState.Downloading): String {
    val progress = state.progressFraction
    if (progress < 0f) {
        return "正在连接..."
    }

    val downloaded = formatGb(state.downloadedBytes)
    val total = state.totalBytes.takeIf { it > 0 }?.let(::formatGb)
    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()

    return if (total != null) {
        "$percent%  $downloaded GB / $total GB"
    } else {
        "$downloaded GB 已下载"
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
