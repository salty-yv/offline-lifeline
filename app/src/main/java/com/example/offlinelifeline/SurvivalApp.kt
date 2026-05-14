package com.example.offlinelifeline

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.offlinelifeline.core.model.ToolType
import com.example.offlinelifeline.di.AppContainer
import com.example.offlinelifeline.ui.navigation.AppNavHost
import com.example.offlinelifeline.ui.navigation.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurvivalApp() {
    var selectedRoute by rememberSaveable { mutableStateOf(Route.Chat) }
    var selectedTool by rememberSaveable { mutableStateOf<ToolType?>(null) }
    val context = LocalContext.current
    val appContainer = remember(context) { AppContainer(context) }

    LaunchedEffect(Unit) {
        appContainer.deviceDiagnosticsLogger.logSnapshot("app_start")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(selectedRoute.title) }
            )
        },
        bottomBar = {
            SurvivalBottomBar(
                selectedRoute = selectedRoute,
                onRouteSelected = {
                    selectedTool = null
                    selectedRoute = it
                }
            )
        }
    ) { innerPadding ->
        AppNavHost(
            selectedRoute = selectedRoute,
            selectedTool = selectedTool,
            onToolSelected = { toolType ->
                selectedTool = toolType
                selectedRoute = toolType.toRoute()
            },
            appContainer = appContainer,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
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
    NavigationBar {
        Route.entries.forEach { route ->
            NavigationBarItem(
                selected = selectedRoute == route,
                onClick = { onRouteSelected(route) },
                icon = { Text(route.iconLabel) },
                label = { Text(route.title) }
            )
        }
    }
}
