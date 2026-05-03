package com.rubensimon.ecolens.ui.navigation

/**
 * Rutas de navegación de EcoLens KMP.
 *
 * Reemplaza el sistema de Activities + Intents del proyecto Android original.
 * Usa Compose Navigation con NavController (Compose Multiplatform).
 */
sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")
    data object Menu : Screen("menu")
    data object Scan : Screen("scan")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object History : Screen("history")
    data object Leaderboard : Screen("leaderboard")
    data object Rewards : Screen("rewards")
    data object Maps : Screen("maps")
    data object Collection : Screen("collection")
    data object Upcycling : Screen("upcycling")
    data object Onboarding : Screen("onboarding")
    
    data object FriendProfile : Screen("friend_profile/{userId}") {
        fun createRoute(userId: String) = "friend_profile/$userId"
    }
}
