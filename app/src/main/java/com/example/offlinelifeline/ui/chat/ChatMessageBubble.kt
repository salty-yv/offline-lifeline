package com.example.offlinelifeline.ui.chat

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.core.model.Attachment
import com.example.offlinelifeline.core.model.ChatMessage
import com.example.offlinelifeline.core.model.ChatRole
import com.example.offlinelifeline.core.model.ToolRecommendation
import com.example.offlinelifeline.core.model.ToolType

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onToolSelected: (ToolType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == ChatRole.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.widthIn(max = 340.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = bubbleColor,
                contentColor = textColor,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (message.text.isBlank()) "..." else message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            val imageAttachments = message.attachments.filterIsInstance<Attachment.Image>()
            if (imageAttachments.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = imageAttachments,
                        key = { it.localPath }
                    ) { image ->
                        MessageImagePreview(image)
                    }
                }
            }

            if (!isUser && message.toolRecommendations.isNotEmpty()) {
                ToolRecommendationList(
                    recommendations = message.toolRecommendations,
                    onToolSelected = onToolSelected,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageImagePreview(image: Attachment.Image) {
    val bitmap = remember(image.localPath) {
        BitmapFactory.decodeFile(image.localPath)
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "attached image",
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "图片",
                modifier = Modifier.padding(18.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ToolRecommendationList(
    recommendations: List<ToolRecommendation>,
    onToolSelected: (ToolType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "推荐工具",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        recommendations.forEach { recommendation ->
            AssistChip(
                onClick = { onToolSelected(recommendation.toolType) },
                label = { Text(recommendation.toolType.displayName()) }
            )
        }
    }
}

private fun ToolType.displayName(): String {
    return when (this) {
        ToolType.SOS_FLASHLIGHT -> "SOS 闪光灯"
        ToolType.SCREEN_SOS -> "屏幕 SOS"
        ToolType.BATTERY_SAVER_ADVICE -> "电量保护建议"
        ToolType.EMERGENCY_CARD -> "个人应急信息卡"
        ToolType.OFFLINE_GUIDE -> "离线指南"
        ToolType.DEBUG_LOG_EXPORT -> "Debug Log 导出"
    }
}
