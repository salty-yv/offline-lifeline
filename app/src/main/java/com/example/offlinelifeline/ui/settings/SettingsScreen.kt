package com.example.offlinelifeline.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.core.model.ModelRuntimeState
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
    val aboutStrings = remember(strings.languageTag) {
        AboutSettingsStrings.forLanguage(strings.languageTag)
    }
    val externalModelSelection by viewModel.externalModelSelection.collectAsState()
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val localModelFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let(viewModel::selectExternalModel)
        }
    }

    if (showAbout) {
        BackHandler {
            showAbout = false
        }
        AboutDetailScreen(
            strings = aboutStrings,
            onBack = { showAbout = false },
            modifier = modifier
        )
        return
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
                    ExternalModelPickerRow(
                        languageTag = strings.languageTag,
                        selectionState = externalModelSelection?.runtimeState,
                        message = externalModelSelection?.message,
                        onChoose = {
                            localModelFilePicker.launch(createModelFilePickerIntent())
                        }
                    )
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

            item {
                AboutSettingsEntryCard(
                    strings = aboutStrings,
                    onClick = { showAbout = true }
                )
            }
    }
}
}

@Composable
private fun AboutSettingsEntryCard(
    strings: AboutSettingsStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = strings.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = strings.entryDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = ">",
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AboutDetailScreen(
    strings: AboutSettingsStrings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionCode = remember(packageInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }
    }
    val versionName = packageInfo.versionName ?: strings.unknown
    val openUrl: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LifelineTopBar(
            title = strings.title,
            navigationIcon = Icons.Default.ArrowBack,
            navigationContentDescription = strings.back,
            onNavigationClick = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.appDisplayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = strings.tagline,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${strings.version} $versionName ($versionCode)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                AboutSection(title = strings.appInfoTitle) {
                    AboutRow(label = strings.appName, value = strings.appDisplayName)
                    AboutRow(label = strings.version, value = "$versionName ($versionCode)")
                    AboutRow(label = strings.packageName, value = context.packageName)
                    AboutRow(label = strings.license, value = strings.openSource)
                }
            }

            item {
                AboutSection(title = strings.linksTitle) {
                    AboutRow(
                        label = strings.authorHomepage,
                        value = AUTHOR_HOMEPAGE_URL,
                        onClick = { openUrl(AUTHOR_HOMEPAGE_URL) }
                    )
                    HorizontalDivider()
                    AboutRow(
                        label = strings.projectHomepage,
                        value = PROJECT_HOMEPAGE_URL,
                        onClick = { openUrl(PROJECT_HOMEPAGE_URL) }
                    )
                }
            }

            item {
                AboutSection(title = strings.privacyTitle) {
                    Text(
                        text = strings.privacyBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun AboutRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (onClick != null) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (onClick != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private data class AboutSettingsStrings(
    val title: String,
    val entryDescription: String,
    val back: String,
    val tagline: String,
    val appInfoTitle: String,
    val linksTitle: String,
    val privacyTitle: String,
    val privacyBody: String,
    val appName: String,
    val appDisplayName: String,
    val version: String,
    val packageName: String,
    val license: String,
    val openSource: String,
    val authorHomepage: String,
    val projectHomepage: String,
    val unknown: String
) {
    companion object {
        fun forLanguage(languageTag: String): AboutSettingsStrings {
            return if (languageTag.startsWith("en")) en() else zh()
        }

        private fun zh() = AboutSettingsStrings(
            title = "关于",
            entryDescription = "版本、项目主页、作者主页和本地隐私说明",
            back = "返回设置",
            tagline = "一个面向离线应急场景的本地生存助手。",
            appInfoTitle = "应用信息",
            linksTitle = "相关链接",
            privacyTitle = "隐私与离线说明",
            privacyBody = "对话、个人信息卡和本地工具数据默认保存在本机。应用优先使用离线指南、本地工具和设备端模型能力，不会自动上传你的个人信息。",
            appName = "应用名称",
            appDisplayName = "Offline Lifeline",
            version = "版本号",
            packageName = "包名",
            license = "授权",
            openSource = "开源项目",
            authorHomepage = "作者主页",
            projectHomepage = "项目主页",
            unknown = "未知"
        )

        private fun en() = AboutSettingsStrings(
            title = "About",
            entryDescription = "Version, project links, author profile, and local privacy notes",
            back = "Back to settings",
            tagline = "A local survival assistant for offline emergency situations.",
            appInfoTitle = "App information",
            linksTitle = "Links",
            privacyTitle = "Privacy and offline notes",
            privacyBody = "Chats, emergency card details, and local tool data are stored on this device by default. The app prioritizes offline guides, local tools, and on-device model features. It does not automatically upload your personal information.",
            appName = "App name",
            appDisplayName = "Offline Lifeline",
            version = "Version",
            packageName = "Package",
            license = "License",
            openSource = "Open-source project",
            authorHomepage = "Author homepage",
            projectHomepage = "Project homepage",
            unknown = "Unknown"
        )
    }
}

private const val AUTHOR_HOMEPAGE_URL = "https://github.com/salty-yv"
private const val PROJECT_HOMEPAGE_URL = "https://github.com/salty-yv/offline-lifeline"

private fun createModelFilePickerIntent(): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        putExtra(
            Intent.EXTRA_MIME_TYPES,
            arrayOf(
                "application/octet-stream",
                "application/x-lm",
                "*/*"
            )
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    }
}

@Composable
private fun ExternalModelPickerRow(
    languageTag: String,
    selectionState: ModelRuntimeState?,
    message: String?,
    onChoose: () -> Unit
) {
    val isEnglish = languageTag.startsWith("en")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isEnglish) "Use model from phone storage" else "使用手机存储里的模型",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (isEnglish) {
                "Pick a downloaded .litertlm file. The app checks its hash, identifies E2B or E4B, and moves it into app model storage."
            } else {
                "选择已经下载好的 .litertlm 文件，应用会校验哈希并自动判断是 E2B 还是 E4B，然后移动到应用模型目录。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = onChoose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isEnglish) "Move model file" else "移动模型文件")
        }
        if (selectionState == ModelRuntimeState.Checking) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (!message.isNullOrBlank()) {
            val isError = selectionState is ModelRuntimeState.Failed ||
                selectionState == ModelRuntimeState.ChecksumFailed ||
                selectionState == ModelRuntimeState.Missing
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
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
