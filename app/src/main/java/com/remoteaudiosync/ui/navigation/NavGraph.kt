package com.remoteaudiosync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.remoteaudiosync.ui.NetworkViewModel
import com.remoteaudiosync.ui.screens.*

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val networkViewModel: NetworkViewModel = viewModel()
    NavHost(navController = navController, startDestination = "preloader") {
        composable("preloader") {
            PreloaderScreen(onComplete = {
                navController.navigate("dashboard") {
                    popUpTo("preloader") { inclusive = true }
                }
            })
        }
        composable("dashboard") {
            DashboardScreen(
                viewModel = networkViewModel,
                onNavigateToPairing = { navController.navigate("pairing") },
                onNavigateToMedia = { navController.navigate("media") },
                onNavigateToCalls = { navController.navigate("calls") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToDiagnostics = { navController.navigate("diagnostics") },
                onNavigateToPermissions = { navController.navigate("permissions") }
            )
        }
        composable("pairing") { NetworkScreen(onNavigateBack = { navController.popBackStack() }, viewModel = networkViewModel) }
        composable("media") { MediaScreen(onNavigateBack = { navController.popBackStack() }, viewModel = networkViewModel) }
        composable("calls") { CallsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("bluetooth") { BluetoothDeviceScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("permissions") { PermissionsScreen(onNavigateBack = { navController.popBackStack() }, viewModel = networkViewModel) }
        composable("settings") { SettingsScreen(onNavigateBack = { navController.popBackStack() }, viewModel = networkViewModel) }
        composable("diagnostics") { DiagnosticsScreen(onNavigateBack = { navController.popBackStack() }, viewModel = networkViewModel) }
    }
}
