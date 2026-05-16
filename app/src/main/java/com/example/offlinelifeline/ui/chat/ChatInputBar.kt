package com.example.offlinelifeline.ui.chat

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlinelifeline.R
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
    chatTextSizeSp: Int,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onPickImage: () -> Unit,
    onOpenCamera: () -> Unit,
    onRemoveImage: (Attachment.Image) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pendingImages.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (isFreeChatMode) {
                                strings.freeChatInputPlaceholder
                            } else {
                                strings.chatInputPlaceholder
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = chatTextSizeSp.sp)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = chatTextSizeSp.sp),
                    minLines = 1,
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) onSend()
                        }
                    ),
                    trailingIcon = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                enabled = !isGenerating && !isProcessingImage,
                                onClick = onOpenCamera,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_camera_24),
                                    contentDescription = strings.camera,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                enabled = !isGenerating && !isProcessingImage,
                                onClick = onPickImage,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_gallery_24),
                                    contentDescription = strings.gallery,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    enabled = !isGenerating
                )

                if (isGenerating) {
                    Button(
                        onClick = onStop,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_stop_24),
                            contentDescription = strings.stop
                        )
                    }
                } else {
                    Button(
                        onClick = onSend,
                        enabled = canSend,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_send_24),
                            contentDescription = strings.send
                        )
                    }
                }
            }
            if (isProcessingImage) {
                Text(
                    text = strings.processingImage,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PendingImagePreview(
    image: Attachment.Image,
    onRemove: () -> Unit
) {
    val bitmap = remember(image.localPath) {
        BitmapFactory.decodeFile(image.localPath)
    }

    Box(modifier = Modifier.size(76.dp)) {
        Card(
            modifier = Modifier
                .size(68.dp)
                .align(Alignment.BottomStart),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = LocalAppStrings.current.pendingImageContentDescription,
                    modifier = Modifier.size(68.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = LocalAppStrings.current.image,
                    modifier = Modifier
                        .size(68.dp)
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Surface(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.TopEnd),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            onClick = onRemove
        ) {
            Row(
                modifier = Modifier.size(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24),
                    contentDescription = LocalAppStrings.current.delete,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
