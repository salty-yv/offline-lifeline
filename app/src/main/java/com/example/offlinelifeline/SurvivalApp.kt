package com.example.offlinelifeline

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.core.model.ToolType
import com.example.offlinelifeline.data.datastore.AppSettings
import com.example.offlinelifeline.di.AppContainer
import com.example.offlinelifeline.ui.i18n.LocalAppStrings
import com.example.offlinelifeline.ui.i18n.appStringsFor
import com.example.offlinelifeline.ui.navigation.AppNavHost
import com.example.offlinelifeline.ui.navigation.Route

@Composable
fun SurvivalApp() {
    var selectedRoute by rememberSaveable { mutableStateOf(Route.Chat) }
    var selectedTool by rememberSaveable { mutableStateOf<ToolType?>(null) }
    var routeBackStack by rememberSaveable { mutableStateOf(listOf(Route.Chat)) }
    var showExitConfirm by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val appContainer = remember(context) { AppContainer(context) }
    val settings by appContainer.settingsStore.settings.collectAsState(initial = AppSettings())
    val strings = appStringsFor(settings.languageTag)
    val isEnglish = strings.languageTag.startsWith("en")

    fun navigateTo(route: Route, toolType: ToolType? = null) {
        selectedTool = toolType
        selectedRoute = route
        if (routeBackStack.lastOrNull() != route) {
            routeBackStack = routeBackStack + route
        }
    }

    fun navigateBackOneLevel() {
        selectedTool = null
        val newStack = if (routeBackStack.size > 1) {
            routeBackStack.dropLast(1)
        } else {
            listOf(Route.Chat)
        }
        routeBackStack = newStack
        selectedRoute = newStack.lastOrNull() ?: Route.Chat
    }

    LaunchedEffect(Unit) {
        appContainer.deviceDiagnosticsLogger.logSnapshot("app_start")
        // 首次启动时把 asset DB 里的 guide chunk 数据导入主数据库（幂等，已有数据则跳过）
        appContainer.guideChunkSeeder.seedIfNeeded()
    }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        BackHandler {
            if (selectedRoute == Route.Chat) {
                showExitConfirm = true
            } else {
                navigateBackOneLevel()
            }
        }

        if (showExitConfirm) {
            AlertDialog(
                onDismissRequest = { showExitConfirm = false },
                title = { Text(if (isEnglish) "Exit app?" else "退出应用？") },
                text = {
                    Text(
                        if (isEnglish) {
                            "Do you want to close Offline Lifeline?"
                        } else {
                            "确定要退出 Offline Lifeline 吗？"
                        }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitConfirm = false
                            (context as? Activity)?.finish()
                        }
                    ) {
                        Text(strings.exit)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirm = false }) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                SurvivalBottomBar(
                    selectedRoute = selectedRoute,
                    onRouteSelected = {
                        navigateTo(it)
                    }
                )
            }
        ) { innerPadding ->
            AppNavHost(
                selectedRoute = selectedRoute,
                selectedTool = selectedTool,
                onToolSelected = { toolType ->
                    navigateTo(toolType.toRoute(), toolType)
                },
                appContainer = appContainer,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

private fun ToolType.toRoute(): Route {
    return when (this) {
        ToolType.EMERGENCY_CARD -> Route.EmergencyCard
        ToolType.OFFLINE_GUIDE -> Route.Guide
        ToolType.SOS_FLASHLIGHT,
        ToolType.SCREEN_SOS,
        ToolType.BATTERY_SAVER_ADVICE,
        ToolType.DEBUG_LOG_EXPORT -> Route.Toolbox
    }
}

@Composable
private fun SurvivalBottomBar(
    selectedRoute: Route,
    onRouteSelected: (Route) -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 18.dp),
            shape = RoundedCornerShape(40.dp),
            color = MaterialTheme.colorScheme.outline
        ) {
            Surface(
                modifier = Modifier.padding(1.dp),
                shape = RoundedCornerShape(39.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Route.entries.forEach { route ->
                        val selected = selectedRoute == route
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            shape = RoundedCornerShape(30.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            onClick = { onRouteSelected(route) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val contentColor = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Icon(
                                    painter = painterResource(route.iconRes()),
                                    contentDescription = route.title(strings),
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = route.navLabel(strings),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Route.navLabel(strings: com.example.offlinelifeline.ui.i18n.AppStrings): String {
    return when (this) {
        Route.Chat -> if (strings.languageTag.startsWith("en")) "CHAT" else strings.routeChat
        Route.Toolbox -> if (strings.languageTag.startsWith("en")) "TOOLBOX" else strings.routeToolbox
        Route.Guide -> if (strings.languageTag.startsWith("en")) "GUIDE" else strings.routeGuide
        Route.EmergencyCard -> if (strings.languageTag.startsWith("en")) "SOS" else strings.routeEmergencyCard
        Route.Settings -> if (strings.languageTag.startsWith("en")) "SETTINGS" else strings.routeSettings
    }
}

private fun Route.iconRes(): Int {
    return when (this) {
        Route.Chat -> R.drawable.ic_nav_chat_24
        Route.Toolbox -> R.drawable.ic_nav_tools_24
        Route.Guide -> R.drawable.ic_nav_guide_24
        Route.EmergencyCard -> R.drawable.ic_nav_card_24
        Route.Settings -> R.drawable.ic_nav_settings_24
    }
}
