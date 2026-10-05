package com.example.spaceplan.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.spaceplan.ui.SpacePlanViewModel
import com.example.spaceplan.ui.about.AboutScreen
import com.example.spaceplan.ui.calculator.CalculatorScreen
import com.example.spaceplan.ui.history.HistoryScreen
import com.example.spaceplan.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

enum class Destination(val route: String, val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    CALCULATOR("calculator", "Calculator", Icons.Filled.Calculate, Icons.Outlined.Calculate),
    HISTORY("history", "History", Icons.Filled.History, Icons.Outlined.History),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    ABOUT("about", "About", Icons.Filled.Info, Icons.Outlined.Info),
}

@Composable
fun SpacePlanApp(viewModel: SpacePlanViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateTopLevel(destination.route) },
                        icon = { Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.CALCULATOR.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            composable(Destination.CALCULATOR.route) {
                CalculatorScreen(viewModel = viewModel, showMessage = showMessage)
            }
            composable(Destination.HISTORY.route) {
                HistoryScreen(
                    viewModel = viewModel,
                    onOpen = { record ->
                        viewModel.restore(record)
                        navController.navigateTopLevel(Destination.CALCULATOR.route)
                    },
                    showMessage = showMessage,
                )
            }
            composable(Destination.SETTINGS.route) {
                SettingsScreen(viewModel = viewModel, showMessage = showMessage)
            }
            composable(Destination.ABOUT.route) {
                AboutScreen()
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
