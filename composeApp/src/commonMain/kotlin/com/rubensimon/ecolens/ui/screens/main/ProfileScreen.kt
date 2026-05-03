package com.rubensimon.ecolens.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rubensimon.ecolens.data.repository.UserRepository
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.utils.PointsManager
import com.rubensimon.ecolens.utils.rememberPlatformImagePicker
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Pantalla de perfil — migrada de ProfileActivity.
 *
 * Muestra estadísticas, impacto ambiental, logros y foto de perfil.
 * Si [userId] es null → perfil propio. Si no → perfil de otro usuario.
 *
 * La selección de foto es un `expect fun` en PlatformImagePicker porque
 * ActivityResultContracts solo existe en Android.
 */
@Composable
fun ProfileScreen(
    userId: String?,         // null = propio perfil
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings = remember { Settings() }

    val isSelf = userId == null
    val currentUserId = remember { settings.getString("user_id", "") }
    val displayedUserId = userId ?: currentUserId
    val localUsername = remember { settings.getString("username", "EcoUser") }

    var username by remember { mutableStateOf(localUsername) }
    var puntos by remember { mutableIntStateOf(0) }
    var totalScans by remember { mutableIntStateOf(0) }
    var bio by remember { mutableStateOf("Eco Enthusiast") }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var personalHistory by remember { mutableStateOf<List<com.rubensimon.ecolens.data.models.social.HistoryItemModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    
    val imagePicker = rememberPlatformImagePicker { bytes ->
        scope.launch {
            isUploading = true
            val url = UserRepository().uploadProfilePicture(currentUserId, bytes)
            if (url != null) {
                profilePicUrl = url
            }
            isUploading = false
        }
    }

    LaunchedEffect(displayedUserId) {
        val id = displayedUserId
        if (id != null && id.isNotEmpty()) {
            isLoading = true
            scope.launch {
                val repo = UserRepository()
                val user = repo.getUserById(id)
                user?.let {
                    username = it.display_name ?: it.username
                    puntos = it.puntos
                    totalScans = it.total_scans
                    bio = it.bio ?: "Eco Enthusiast"
                    profilePicUrl = it.profile_picture_url
                }
                
                // Cargar actividad real
                personalHistory = repo.getUserHistory(id)
                isLoading = false
            }
        }
    }


    val nivel = if (isSelf) PointsManager.getLevel() else calcularNivel(puntos)
    val co2Saved = totalScans * 0.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoColors.BackgroundDark)
            .statusBarsPadding()
    ) {
        // ... (resto del código de la Top Bar)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp).background(EcoColors.CardPrimary, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = EcoColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Text("Perfil", fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary, fontSize = 16.sp)
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp).background(EcoColors.CardPrimary, CircleShape)
            ) {
                Icon(Icons.Default.Settings, null, tint = EcoColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // ── Avatar & Name ─────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color(0xFF1E293B), CircleShape) // Color sólido (Slate 800)
                            .clip(CircleShape)
                            .clickable { if (isSelf && !isUploading) imagePicker.launchPicker() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePicUrl != null && profilePicUrl!!.isNotEmpty()) {
                            AsyncImage(
                                model = profilePicUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                username.take(1).uppercase(), 
                                fontSize = 40.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = EcoColors.TextPrimary
                            )
                        }
                        
                        if (isUploading) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = EcoColors.GlassAccent, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                    if (isSelf) {
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .offset(x = 4.dp, y = 4.dp)
                                .clickable { if (!isUploading) imagePicker.launchPicker() },
                            shape = CircleShape,
                            color = EcoColors.GlassAccent,
                            shadowElevation = 4.dp
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(username, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary)
                Text(bio, fontSize = 13.sp, color = EcoColors.TextSecondary)
            }

            // ── Follow Stats ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("Escaneos", fontSize = 12.sp, color = EcoColors.TextSecondary)
                    Text("$totalScans", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("Puntos", fontSize = 12.sp, color = EcoColors.TextSecondary)
                    Text("$puntos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Impact Cards (Estilo imagen 2) ───────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ImpactCard("CO₂ Saved", "${(totalScans * 0.5f).toInt()}kg", Color(0xFF34D399), Modifier.weight(1f))
                ImpactCard("Goal", "${(nivel * 50)}kg", Color(0xFF60A5FA), Modifier.weight(1f))
                ImpactCard("Level", "$nivel", Color(0xFFFBBF24), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Detailed Impact Section (Nueva sección 4) ────────────────
            Text("Tu Huella Positiva", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EcoColors.TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            DetailedImpactSection(totalScans)

            Spacer(modifier = Modifier.height(24.dp))

            // ── Logros / Badges ──────────────────────────────────────────
            Text("Logros desbloqueados", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EcoColors.TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val badges = buildBadges(totalScans, puntos, nivel)
                badges.forEach { badge ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    if (badge.unlocked) EcoColors.GlassAccent.copy(alpha = 0.2f) 
                                    else EcoColors.CardPrimary.copy(alpha = 0.5f), 
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                badge.emoji, 
                                fontSize = 32.sp,
                                modifier = Modifier.alpha(if (badge.unlocked) 1f else 0.3f)
                            )
                            if (!badge.unlocked) {
                                Icon(Icons.Default.Lock, null, tint = EcoColors.TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            badge.name, 
                            fontSize = 11.sp, 
                            color = if (badge.unlocked) EcoColors.TextPrimary else EcoColors.TextSecondary,
                            fontWeight = if (badge.unlocked) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Ecological Activity List (Estilo imagen 2) ────────────────
            Text("Actividad ecológica", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EcoColors.TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            if (personalHistory.isEmpty()) {
                Text(
                    "Aún no hay actividad registrada. ¡Empieza a escanear!",
                    color = EcoColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                personalHistory.take(10).forEach { item ->
                    ActivityItem(
                        title = "Escaneo de ${item.object_name}",
                        time = formatHistoryDate(item.created_at),
                        icon = when {
                            item.object_name.contains("Plástico", true) -> Icons.Default.Recycling
                            item.object_name.contains("Vidrio", true) -> Icons.Default.Public
                            else -> Icons.Default.Eco
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun ImpactCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = EcoColors.TextPrimary)
        }
    }
}

@Composable
fun ActivityItem(title: String, time: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(EcoColors.CardPrimary, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = EcoColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary, fontSize = 14.sp)
            Text(time, color = EcoColors.TextSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = EcoColors.CardPrimary, modifier = Modifier.size(20.dp))
    }
}

private data class Badge(val emoji: String, val name: String, val unlocked: Boolean)

private fun buildBadges(scans: Int, puntos: Int, nivel: Int) = listOf(
    Badge("🌱", "Primer Paso", scans >= 1),
    Badge("🔥", "En Racha", scans >= 7),
    Badge("♻️", "Eco-Guerrero", scans >= 100),
    Badge("🌍", "Guardián", puntos >= 1000),
    Badge("🏆", "Top Player", nivel >= 10),
    Badge("⚡", "Veloz", scans >= 50)
)

private fun calcularNivel(puntos: Int): Int = when {
    puntos < 100 -> 1
    puntos < 300 -> 2
    puntos < 600 -> 3
    puntos < 1000 -> 4
    puntos < 1500 -> 5
    else -> (puntos / 500) + 3
}

private fun nextLevelPoints(nivel: Int): Int = when (nivel) {
    1 -> 100; 2 -> 300; 3 -> 600; 4 -> 1000; 5 -> 1500
    else -> nivel * 500
}

@Composable
private fun DetailedImpactSection(scans: Int) {
    val waterSaved = scans * 75 // 75L por objeto
    val energySaved = scans * 10 // 10h de bombilla
    val treesSaved = scans / 20.0 // 1 árbol cada 20 objetos

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ImpactDetailItem("💧 Agua", "${waterSaved}L", "Ahorrada", Color(0xFF60A5FA), Modifier.weight(1f))
        ImpactDetailItem("💡 Luz", "${energySaved}h", "Energía", Color(0xFFFBBF24), Modifier.weight(1f))
        ImpactDetailItem("🌳 Bosque", "${treesSaved.toString().take(4)}", "Árboles", Color(0xFF34D399), Modifier.weight(1f))
    }
}

@Composable
private fun ImpactDetailItem(
    emoji: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
            Text(label, color = EcoColors.TextSecondary, fontSize = 10.sp)
        }
    }
}

private fun prevLevelPoints(nivel: Int): Int = when (nivel) {
    1 -> 0; 2 -> 100; 3 -> 300; 4 -> 600; 5 -> 1000
    else -> (nivel - 1) * 500
}

private fun formatHistoryDate(createdAt: String?): String {
    if (createdAt == null) return "Recientemente"
    return try {
        // Formato simplificado para el perfil
        createdAt.substringBefore("T").replace("-", "/")
    } catch (e: Exception) {
        "Hace poco"
    }
}
