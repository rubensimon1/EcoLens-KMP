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
    data object Scan : Screen("scan/{isSddr}") {
        fun createRoute(isSddr: Boolean) = "scan/$isSddr"
    }
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object History : Screen("history")
    data object Leaderboard : Screen("leaderboard")
    data object Rewards : Screen("rewards")
    data object Maps : Screen("maps?filter={filter}") {
        fun createRoute(filter: String = "TODOS") = "maps?filter=$filter"
    }
    data object Collection : Screen("collection")
    data object Upcycling : Screen("upcycling")
    data object Onboarding : Screen("onboarding")
    data object AiChat : Screen("ai_chat")
    data object Notifications : Screen("notifications")
    data object Sddr : Screen("sddr")
    
    data object FriendProfile : Screen("friend_profile/{userId}") {
        fun createRoute(userId: String) = "friend_profile/$userId"
    }
}
