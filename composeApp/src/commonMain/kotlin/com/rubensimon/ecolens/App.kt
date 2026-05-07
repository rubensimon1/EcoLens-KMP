package com.rubensimon.ecolens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rubensimon.ecolens.ui.components.CustomIcons
import com.rubensimon.ecolens.ui.components.EcoColors
import com.rubensimon.ecolens.ui.components.fadingEdge
import com.rubensimon.ecolens.ui.navigation.AppNavigation
import com.russhwolf.settings.Settings
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory

@Composable
fun App(startDestination: String = "welcome") {
    // ── Configuración de Coil (Carga de imágenes de red) ──
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }

    // ── Gestión del Tema ──
    val isSystemDark = isSystemInDarkTheme()
    val settings = remember { Settings() }

    val initialRoute = remember {
        val savedUserId = settings.getString("user_id", "")
        if (savedUserId.isNotEmpty()) "menu" else "welcome"
    }

    // Inicializar tema al arrancar
    LaunchedEffect(Unit) {
        val currentPref = settings.getBoolean("dark_mode", isSystemDark)
        EcoColors.updateTheme(currentPref)
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != null && currentRoute in listOf("menu", "collection", "rewards", "profile")


    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                ModernBottomBar(navController, currentRoute)
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        // ── FONDO GLOBAL PREMIUM ──────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            val isDark = EcoColors.isDark
            val infiniteTransition = rememberInfiniteTransition()
            val orbOffset by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 100f,
                animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
            )

            // Fondo base
            Box(modifier = Modifier.fillMaxSize().background(if (isDark) Color(0xFF001A1A) else Color(0xFFE0FFF0)))

            // Orbes de luz
            Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
                val primaryOrbColor = if (isDark) Color(0xFF2ECC71).copy(alpha = 0.15f) else Color(0xFF2ECC71).copy(alpha = 0.1f)
                val secondaryOrbColor = if (isDark) Color(0xFF008080).copy(alpha = 0.2f) else Color(0xFF008080).copy(alpha = 0.15f)

                drawCircle(
                    brush = Brush.radialGradient(colors = listOf(primaryOrbColor, Color.Transparent)),
                    radius = 450.dp.toPx(),
                    center = Offset(size.width * 0.8f + orbOffset, size.height * 0.2f)
                )
                drawCircle(
                    brush = Brush.radialGradient(colors = listOf(secondaryOrbColor, Color.Transparent)),
                    radius = 400.dp.toPx(),
                    center = Offset(size.width * 0.1f - orbOffset, size.height * 0.7f)
                )
            }

            // Contenido de la navegación con efecto de desvanecimiento global
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .then(if (showBottomBar) Modifier.fadingEdge() else Modifier)
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
                    val accentColor = if (isDark) Color(0xFF76D7C4) else Color(0xFF2ECC71)
                    
                    val animatedSize by animateDpAsState(targetValue = if (selected) 30.dp else 24.dp)
                    val animatedColor by animateColorAsState(targetValue = if (selected) accentColor else (if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.3f)))
                    
                    IconButton(
                        onClick = {
                            if (!selected) {
                                navController.navigate(route) {
                                    // Navega a la raíz del grafo para evitar acumular pantallas
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    // Evita múltiples copias de la misma pantalla
                                    launchSingleTop = true
                                    // Restaura el estado (scroll, etc) si ya existía
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = animatedColor,
                            modifier = Modifier.size(animatedSize)
                        )
                    }
                }
            }
        }
    }
}
