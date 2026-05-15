package com.example.offlinelifeline.ui.chat

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp
import com.example.offlinelifeline.core.model.Attachment
import com.example.offlinelifeline.core.model.ChatMessage
import com.example.offlinelifeline.core.model.ChatRole
import com.example.offlinelifeline.core.model.ToolRecommendation
import com.example.offlinelifeline.core.model.ToolType
import com.example.offlinelifeline.ui.i18n.LocalAppStrings

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onToolSelected: (ToolType) -> Unit,
    chatTextSizeSp: Int,
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
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = chatTextSizeSp.sp)
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
    val strings = LocalAppStrings.current
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
                contentDescription = strings.attachedImageContentDescription,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = strings.image,
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
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = strings.recommendedTools,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        recommendations.forEach { recommendation ->
            AssistChip(
                onClick = { onToolSelected(recommendation.toolType) },
                label = { Text(strings.toolName(recommendation.toolType)) }
            )
        }
    }
}
