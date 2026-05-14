package com.example.offlinelifeline.ui.chat

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.core.model.ModelRuntimeState

@Composable
fun RuntimeStatusBanner(
    runtimeState: ModelRuntimeState,
    modelAssetState: ModelRuntimeState,
    isGenerating: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val message = when {
        errorMessage != null -> errorMessage
        isGenerating -> "正在生成回复..."
        modelAssetState is ModelRuntimeState.Checking -> "正在检查真实模型文件..."
        modelAssetState is ModelRuntimeState.Missing -> "真实模型不可用，可使用 Mock 对话、离线指南和工具箱。"
        modelAssetState is ModelRuntimeState.ChecksumFailed -> "模型校验失败，已阻止真实模型加载；仍可使用 Mock 和离线工具。"
        modelAssetState is ModelRuntimeState.ReadyToLoad && runtimeState is ModelRuntimeState.Ready -> "真实模型已就绪，LiteRT-LM 引擎已加载。"
        modelAssetState is ModelRuntimeState.ReadyToLoad -> "真实模型文件已就绪，正在准备 LiteRT-LM 引擎。"
        runtimeState is ModelRuntimeState.Loading -> "正在初始化 Mock 模型..."
        runtimeState is ModelRuntimeState.Ready -> "Mock 模型已就绪"
        runtimeState is ModelRuntimeState.Failed -> runtimeState.message
        runtimeState is ModelRuntimeState.Released -> "Mock 模型已释放"
        else -> "Mock 模型待初始化"
    }

    val containerColor = if (errorMessage != null || modelAssetState is ModelRuntimeState.ChecksumFailed) {
        MaterialTheme.colorScheme.errorContainer
    } else if (modelAssetState is ModelRuntimeState.Missing) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = if (errorMessage != null || modelAssetState is ModelRuntimeState.ChecksumFailed) {
        MaterialTheme.colorScheme.onErrorContainer
    } else if (modelAssetState is ModelRuntimeState.Missing) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 1.dp
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
