package com.rubensimon.ecolens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rubensimon.ecolens.ui.components.CustomIcons
import com.rubensimon.ecolens.ui.components.EcoColors
import com.rubensimon.ecolens.ui.navigation.AppNavigation
import com.russhwolf.settings.Settings
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.platform.LocalDensity

@Composable
fun App(startDestination: String = "welcome") {
    val isSystemDark = isSystemInDarkTheme()
    val settings = remember { Settings() }
    val density = LocalDensity.current
    
    val initialRoute = remember {
        val savedUserId = settings.getString("user_id", "")
        if (savedUserId.isNotEmpty()) "menu" else "welcome"
    }

    LaunchedEffect(Unit) {
        val currentPref = settings.getBoolean("dark_mode", isSystemDark)
        EcoColors.updateTheme(currentPref)
    }

    LaunchedEffect(isSystemDark) {
        val currentPref = settings.getBoolean("dark_mode", isSystemDark)
        EcoColors.updateTheme(currentPref)
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != null && currentRoute in listOf("menu", "collection", "rewards", "profile", "settings", "history", "leaderboard", "maps", "upcycling")
    val edgeWidthPx = with(density) { 40.dp.toPx() }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                ModernBottomBar(navController, currentRoute)
            }
        },
        containerColor = EcoColors.BackgroundDark
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EcoColors.BackgroundDark
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(navController) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (change.previousPosition.x < edgeWidthPx && dragAmount > 15) {
                                if (navController.previousBackStackEntry != null) {
                                    change.consume()
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
            ) {
                AppNavigation(
                    navController = navController,
                    startDestination = initialRoute
                )
            }
        }
    }
}

@Composable
fun ModernBottomBar(navController: NavHostController, currentRoute: String?) {
    val isDark = EcoColors.isDark
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(34.dp),
            color = if (isDark) Color(0xFF1A1A1A).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f),
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 0.5.dp,
                color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val items = listOf(
                    Triple("Inicio", "menu", Icons.Default.Home),
                    Triple("Eco-Dex", "collection", CustomIcons.Backpack),
                    Triple("Premios", "rewards", CustomIcons.Trophy),
                    Triple("Perfil", "profile", Icons.Default.Person)
                )

                items.forEach { item ->
                    val label = item.first
                    val route = item.second
                    val icon = item.third
                    
                    val selected = currentRoute == route
                    val accentColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                    
                    IconButton(
                        onClick = {
                            if (!selected) {
                                navController.navigate(route) {
                                    popUpTo("menu") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) accentColor else (if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.3f)),
                            modifier = Modifier.size(if (selected) 28.dp else 24.dp)
                        )
                    }
                }
            }
        }
    }
}
