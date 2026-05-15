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
import com.example.offlinelifeline.ui.i18n.LocalAppStrings

@Composable
fun RuntimeStatusBanner(
    runtimeState: ModelRuntimeState,
    modelAssetState: ModelRuntimeState,
    isGenerating: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val message = when {
        errorMessage != null -> errorMessage
        isGenerating -> strings.generatingReply
        modelAssetState is ModelRuntimeState.Checking -> strings.checkingRealModel
        modelAssetState is ModelRuntimeState.Missing -> strings.realModelMissing
        modelAssetState is ModelRuntimeState.ChecksumFailed -> strings.checksumFailed
        modelAssetState is ModelRuntimeState.ReadyToLoad && runtimeState is ModelRuntimeState.Ready -> strings.realModelReady
        modelAssetState is ModelRuntimeState.ReadyToLoad -> strings.realModelReadyToLoad
        runtimeState is ModelRuntimeState.Loading -> strings.mockModelLoading
        runtimeState is ModelRuntimeState.Ready -> strings.mockModelReady
        runtimeState is ModelRuntimeState.Failed -> runtimeState.message
        runtimeState is ModelRuntimeState.Released -> strings.mockModelReleased
        else -> strings.mockModelPending
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
