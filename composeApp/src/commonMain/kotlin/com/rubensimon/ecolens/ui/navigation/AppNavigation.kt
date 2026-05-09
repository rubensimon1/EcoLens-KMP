package com.rubensimon.ecolens.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rubensimon.ecolens.ui.screens.auth.OnboardingScreen
import com.russhwolf.settings.Settings
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rubensimon.ecolens.ui.screens.auth.LoginScreen
import com.rubensimon.ecolens.ui.screens.features.CollectionScreen
import com.rubensimon.ecolens.ui.screens.features.HistoryScreen
import com.rubensimon.ecolens.ui.screens.features.LeaderboardScreen
import com.rubensimon.ecolens.ui.screens.features.MapsScreen
import com.rubensimon.ecolens.ui.screens.features.RewardsScreen
import com.rubensimon.ecolens.ui.screens.features.ScanScreen
import com.rubensimon.ecolens.ui.screens.features.UpcyclingScreen
import com.rubensimon.ecolens.ui.screens.auth.OnboardingScreen
import com.rubensimon.ecolens.ui.screens.main.AiChatScreen
import com.rubensimon.ecolens.ui.screens.main.NotificationsScreen
import com.rubensimon.ecolens.ui.screens.main.MenuScreen
import com.rubensimon.ecolens.ui.screens.features.SddrScreen
import com.rubensimon.ecolens.ui.screens.main.ProfileScreen
import com.rubensimon.ecolens.ui.screens.main.SettingsScreen

/**
 * NavHost de EcoLens KMP.
 *
 * Reemplaza el sistema de Activities del proyecto Android original.
 * Cada Screen equivale a una Activity anterior.
 *
 * ### Equivalencias Activity → Screen
 * | Android Activity     | KMP Screen       |
 * |----------------------|------------------|
 * | LoginActivity        | LoginScreen      |
 * | MenuActivity         | MenuScreen       |
 * | MainActivity (scan)  | ScanScreen       |
 * | ProfileActivity      | ProfileScreen    |
 * | SettingsActivity     | SettingsScreen   |
 * | HistoryActivity      | HistoryScreen    |
 * | LeaderboardActivity  | LeaderboardScreen|
 * | RewardsActivity      | RewardsScreen    |
 * | MapsActivity         | MapsScreen       |
 * | CollectionActivity   | CollectionScreen |
 * | UpcyclingActivity    | UpcyclingScreen  |
 */

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Welcome.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { 1000 },
                animationSpec = androidx.compose.animation.core.tween(400)
            ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400))
        },
        exitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { -1000 },
                animationSpec = androidx.compose.animation.core.tween(400)
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400))
        },
        popEnterTransition = {
            androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { -1000 },
                animationSpec = androidx.compose.animation.core.tween(400)
            ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400))
        },
        popExitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { 1000 },
                animationSpec = androidx.compose.animation.core.tween(400)
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400))
        }
    ) {
        // ── Auth ────────────────────────────────────────────────────────────
        composable(Screen.Welcome.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Menu.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route)
                },
                startInWelcome = true
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Menu.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                startInWelcome = false
            )
        }

        // ── Main ────────────────────────────────────────────────────────────
        composable(Screen.Menu.route) {
            val settings = remember { Settings() }
            val userId = settings.getString("user_id", "")
            
            // Cargar notificaciones específicas de este usuario al entrar
            LaunchedEffect(userId) {
                com.rubensimon.ecolens.utils.NotificationManager.setUser(userId)
            }

            MenuScreen(
                userId = userId,
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onCameraClick = { navController.navigate(Screen.Scan.createRoute(false)) },
                onHistoryClick = { navController.navigate(Screen.History.route) },
                onMapsClick = { navController.navigate(Screen.Maps.createRoute("TODOS")) },
                onStatsClick = { navController.navigate(Screen.Leaderboard.route) },
                onAiChatClick = { navController.navigate(Screen.AiChat.route) },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onAchievementsClick = { navController.navigate(Screen.Rewards.route) },
                onFriendProfileClick = { userId ->
                    navController.navigate(Screen.FriendProfile.createRoute(userId))
                },
                onLeaderboardClick = {
                    navController.navigate(Screen.Leaderboard.route)
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onEcoDexClick = { navController.navigate(Screen.Collection.route) },
                onSddrClick = { navController.navigate(Screen.Sddr.route) }
            )
        }

        composable(
            route = Screen.Scan.route,
            arguments = listOf(navArgument("isSddr") { type = NavType.BoolType })
        ) { backStackEntry ->
            val isSddr = backStackEntry.arguments?.getBoolean("isSddr") ?: false
            ScanScreen(
                isSddr = isSddr,
                onBackClick = { navController.popBackStack() },
                onScanComplete = { _, _ -> navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                userId = null, // propio perfil
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.FriendProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStack ->
            val userId = backStack.arguments?.getString("userId")
            ProfileScreen(
                userId = userId,
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    com.rubensimon.ecolens.utils.NotificationManager.clearForLogout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Features ────────────────────────────────────────────────────────
        composable(Screen.History.route) {
            HistoryScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(
                onBackClick = { navController.popBackStack() },
                onProfileClick = { userId ->
                    navController.navigate(Screen.FriendProfile.createRoute(userId))
                }
            )
        }

        composable(Screen.Rewards.route) {
            RewardsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(
            route = Screen.Maps.route,
            arguments = listOf(navArgument("filter") { defaultValue = "TODOS" })
        ) { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filter") ?: "TODOS"
            MapsScreen(
                onBackClick = { navController.popBackStack() },
                initialFilter = filter
            )
        }

        composable(Screen.Collection.route) {
            CollectionScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Upcycling.route) {
            UpcyclingScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.AiChat.route) {
            // Aquí irá el AiChatScreen completo
            AiChatScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Notifications.route) {
            // Aquí irá el NotificationsScreen completo
            NotificationsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Sddr.route) {
            SddrScreen(
                onBackClick = { navController.popBackStack() },
                onScanClick = { navController.navigate(Screen.Scan.createRoute(true)) },
                onMapClick = { navController.navigate(Screen.Maps.createRoute("SDDR")) }
            )
        }
    }
}
