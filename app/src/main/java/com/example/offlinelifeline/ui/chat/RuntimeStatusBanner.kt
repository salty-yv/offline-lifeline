package com.example.offlinelifeline.ui.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.offlinelifeline.core.model.ModelRuntimeState
import com.example.offlinelifeline.ui.i18n.LocalAppStrings

@Composable
fun RuntimeStatusText(
    runtimeState: ModelRuntimeState,
    modelAssetState: ModelRuntimeState,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isEnglish = strings.languageTag.startsWith("en")
    val message = when {
        errorMessage != null -> compactErrorMessage(errorMessage, isEnglish)
        modelAssetState is ModelRuntimeState.Checking -> if (isEnglish) "Checking model" else "检查模型"
        modelAssetState is ModelRuntimeState.Missing -> if (isEnglish) "Model missing" else "模型缺失"
        modelAssetState is ModelRuntimeState.ChecksumFailed -> if (isEnglish) "Check failed" else "校验失败"
        runtimeState is ModelRuntimeState.Loading -> if (isEnglish) "Initializing" else "初始化模型"
        runtimeState is ModelRuntimeState.Failed -> if (isEnglish) "Model error" else "模型出错"
        runtimeState is ModelRuntimeState.Released -> if (isEnglish) "Model released" else "模型已释放"
        runtimeState is ModelRuntimeState.NotChecked -> if (isEnglish) "Preparing model" else "准备模型"
        else -> ""
    }

    Text(
        text = message,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        color = if (errorMessage != null || modelAssetState is ModelRuntimeState.ChecksumFailed) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

private fun compactErrorMessage(message: String, isEnglish: Boolean): String {
    return when {
        message.contains("清空", ignoreCase = true) -> if (isEnglish) "Cleared" else "已清空"
        message.contains("删除", ignoreCase = true) -> if (isEnglish) "Deleted" else "已删除"
        message.contains("停止", ignoreCase = true) || message.contains("stop", ignoreCase = true) ->
            if (isEnglish) "Stopped" else "已停止"
        message.contains("初始化", ignoreCase = true) || message.contains("initialize", ignoreCase = true) ->
            if (isEnglish) "Init failed" else "初始化失败"
        message.contains("图片", ignoreCase = true) || message.contains("image", ignoreCase = true) ->
            if (isEnglish) "Image issue" else "图片提示"
        message.contains("失败", ignoreCase = true) || message.contains("failed", ignoreCase = true) ->
            if (isEnglish) "Failed" else "操作失败"
        else -> message
    }
}
