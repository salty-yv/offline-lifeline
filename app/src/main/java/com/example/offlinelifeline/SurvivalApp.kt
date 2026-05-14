package com.example.offlinelifeline

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.offlinelifeline.di.AppContainer
import com.example.offlinelifeline.ui.navigation.AppNavHost
import com.example.offlinelifeline.ui.navigation.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurvivalApp() {
    var selectedRoute by rememberSaveable { mutableStateOf(Route.Chat) }
    val context = LocalContext.current
    val appContainer = remember(context) { AppContainer(context) }

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
                onRouteSelected = { selectedRoute = it }
            )
        }
    ) { innerPadding ->
        AppNavHost(
            selectedRoute = selectedRoute,
            appContainer = appContainer,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
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
