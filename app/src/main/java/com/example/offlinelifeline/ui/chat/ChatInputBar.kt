package com.example.offlinelifeline.ui.chat

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.core.model.Attachment
import com.example.offlinelifeline.ui.i18n.LocalAppStrings

@Composable
fun ChatInputBar(
    text: String,
    pendingImages: List<Attachment.Image>,
    isProcessingImage: Boolean,
    canSend: Boolean,
    isGenerating: Boolean,
    isFreeChatMode: Boolean = false,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onPickImage: () -> Unit,
    onOpenCamera: () -> Unit,
    onRemoveImage: (Attachment.Image) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pendingImages.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            ) {
                items(
                    items = pendingImages,
                    key = { it.localPath }
                ) { image ->
                    PendingImagePreview(
                        image = image,
                        onRemove = { onRemoveImage(image) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                enabled = !isGenerating && !isProcessingImage,
                onClick = onOpenCamera,
                label = { Text(strings.camera) }
            )
            AssistChip(
                enabled = !isGenerating && !isProcessingImage,
                onClick = onPickImage,
                label = { Text(strings.gallery) }
            )
            if (isProcessingImage) {
                Text(
                    text = strings.processingImage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (isFreeChatMode) strings.freeChatInputPlaceholder
                        else strings.chatInputPlaceholder
                    )
                },
                minLines = 1,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSend) onSend()
                    }
                ),
                enabled = !isGenerating
            )

            if (isGenerating) {
                TextButton(onClick = onStop) {
                    Text(strings.stop)
                }
            } else {
                Button(
                    onClick = onSend,
                    enabled = canSend
                ) {
                    Text(strings.send)
                }
            }
        }
    }
}

@Composable
private fun PendingImagePreview(
    image: Attachment.Image,
    onRemove: () -> Unit
) {
    val strings = LocalAppStrings.current
    val bitmap = remember(image.localPath) {
        BitmapFactory.decodeFile(image.localPath)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = strings.pendingImageContentDescription,
                    modifier = Modifier.size(58.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = strings.image,
                    modifier = Modifier.size(58.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onRemove) {
                Text(strings.delete)
            }
        }
    }
}
