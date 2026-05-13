package com.rubensimon.ecolens.ui.screens.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rubensimon.ecolens.data.repository.UserRepository
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.utils.PointsManager
import com.rubensimon.ecolens.utils.rememberPlatformImagePicker
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch

/**
 * Pantalla de perfil de usuario.
 * 
 * Permite visualizar la información de un usuario (propio o ajeno), incluyendo
 * sus estadísticas, impacto ambiental, logros y actividad reciente.
 * También permite al usuario actual cambiar su foto de perfil.
 * 
 * @param userId ID del usuario a visualizar. Si es null, muestra el perfil del usuario actual.
 * @param onBackClick Acción al pulsar el botón de volver.
 * @param onSettingsClick Acción al pulsar el botón de configuración.
 */
@Composable
fun ProfileScreen(
    userId: String?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings = remember { Settings() }
    val isSelf = userId == null
    val currentUserId = remember { settings.getString("user_id", "") }
    val displayedUserId = userId ?: currentUserId
    
    var username by remember { mutableStateOf(com.rubensimon.ecolens.utils.HistoryManager.getCachedUsername(displayedUserId)) }
    var puntos by remember { mutableIntStateOf(if (isSelf) PointsManager.getPoints() else com.rubensimon.ecolens.utils.HistoryManager.getCachedPoints(displayedUserId)) }
    var totalScans by remember { mutableIntStateOf(if (isSelf) PointsManager.getTotalScans() else com.rubensimon.ecolens.utils.HistoryManager.getCachedScans(displayedUserId)) }
    var bio by remember { mutableStateOf(com.rubensimon.ecolens.utils.HistoryManager.getCachedBio(displayedUserId)) }
    var profilePicUrl by remember { mutableStateOf(com.rubensimon.ecolens.utils.HistoryManager.getCachedProfilePic(displayedUserId)) }
    var personalHistory by remember { mutableStateOf(com.rubensimon.ecolens.utils.HistoryManager.getCachedUserHistory(displayedUserId)) }
    var isLoading by remember { mutableStateOf(personalHistory.isEmpty()) }
    var isUploading by remember { mutableStateOf(false) }
    
    val imagePicker = rememberPlatformImagePicker { bytes ->
        scope.launch {
            isUploading = true
            val url = UserRepository().uploadProfilePicture(currentUserId, bytes)
            if (url != null) profilePicUrl = url
            isUploading = false
        }
    }

    LaunchedEffect(displayedUserId) {
        if (displayedUserId.isNotEmpty()) {
            isLoading = personalHistory.isEmpty()
            val repo = UserRepository()
            try {
                val user = repo.getUserById(displayedUserId)
                user?.let {
                    username = it.display_name ?: it.username
                    puntos = it.puntos
                    totalScans = it.total_scans
                    bio = it.bio ?: "Eco Enthusiast"
                    profilePicUrl = it.profile_picture_url
                    
                    // Actualizar cache
                    com.rubensimon.ecolens.utils.HistoryManager.cacheUserProfile(it)
                }
                
                val history = repo.getUserHistory(displayedUserId)
                if (history.isNotEmpty()) {
                    personalHistory = history
                    com.rubensimon.ecolens.utils.HistoryManager.cacheUserHistory(displayedUserId, history)
                }
            } catch (e: Exception) {
                println("Error loading profile: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    val nivel = if (isSelf) PointsManager.getLevel() else calcularNivel(puntos)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── TOP BAR ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp).background(EcoColors.CardPrimary.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = EcoColors.TextPrimary, modifier = Modifier.size(20.dp))
                }
                Text("Perfil", fontWeight = FontWeight.ExtraBold, color = EcoColors.TextPrimary, fontSize = 16.sp, letterSpacing = 1.sp)
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(40.dp).background(EcoColors.CardPrimary.copy(alpha = 0.4f), CircleShape)
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
                // ── AVATAR ──────────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        val currentProfileUrl = profilePicUrl
                        val isProfileAvatarEmpty = currentProfileUrl.isNullOrEmpty() || currentProfileUrl == "null" || currentProfileUrl.isBlank()
                        val profileAvatarGradient = remember(username) { EcoColors.getAvatarGradient(username) }

                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .background(profileAvatarGradient, CircleShape)
                                .clip(CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .clickable { if (isSelf && !isUploading) imagePicker.launchPicker() },
                            contentAlignment = Alignment.Center
                        ) {
                            // Capa 1: Letra al fondo
                            Text(
                                text = username.trim().replace("@", "").take(1).uppercase().ifEmpty { "U" }, 
                                fontSize = 48.sp, 
                                fontWeight = FontWeight.Black, 
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            // Capa 2: Imagen encima
                            if (!isProfileAvatarEmpty) {
                                AsyncImage(
                                    model = currentProfileUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            
                            if (isUploading) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = EcoColors.GlassAccent, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        if (isSelf) {
                            Surface(
                                modifier = Modifier.size(34.dp).offset(x = 2.dp, y = 2.dp).clickable { if (!isUploading) imagePicker.launchPicker() },
                                shape = CircleShape, color = EcoColors.GlassAccent, shadowElevation = 8.dp
                            ) {
                                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(username, fontSize = 24.sp, fontWeight = FontWeight.Black, color = EcoColors.TextPrimary)
                    Text(bio, fontSize = 13.sp, color = EcoColors.TextSecondary)
                    
                    Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        StatItem("Escaneos", totalScans.toString())
                        StatItem("Puntos", puntos.toString())
                    }
                }

                // ── IMPACT CARDS (Ajustadas para visibilidad) ────────────────
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ImpactCard("CO₂ Saved", "${(totalScans * 0.5f).toInt()}kg", Color(0xFF10B981), Modifier.weight(1f))
                    ImpactCard("Meta", "${(nivel * 50)}kg", Color(0xFF3B82F6), Modifier.weight(1f))
                    ImpactCard("Nivel", "$nivel", Color(0xFFF59E0B), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(32.dp))
                StyledHeader("TU HUELLA POSITIVA", Icons.Default.Public)
                DetailedImpactSection(totalScans)

                Spacer(modifier = Modifier.height(32.dp))
                StyledHeader("LOGROS DESBLOQUEADOS", Icons.Default.EmojiEvents)
                BadgesSection(totalScans, puntos, nivel)

                Spacer(modifier = Modifier.height(32.dp))
                StyledHeader("ACTIVIDAD RECIENTE", Icons.Default.History)

                if (personalHistory.isEmpty()) {
                    Text("Aún no hay actividad registrada.", color = EcoColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    personalHistory.take(10).forEach { act ->
                        ActivityRow(com.rubensimon.ecolens.data.models.social.HistoryItem(
                            user_id = act.user_id,
                            nombre = act.object_name,
                            objeto = act.object_name,
                            puntos = act.points,
                            emoji = com.rubensimon.ecolens.utils.HistoryManager.getEmojiForObject(act.object_name),
                            fecha = act.created_at ?: ""
                        ))
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

/**
 * Tarjeta que muestra un indicador de impacto ambiental rápido.
 * 
 * @param label Descripción del impacto (ej: "CO2 Saved").
 * @param value Valor numérico con unidades.
 * @param color Color temático para la tarjeta.
 */
@Composable
fun ImpactCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val isDark = EcoColors.isDark
    Surface(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (isDark) color.copy(alpha = 0.12f) else color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(label, fontSize = 10.sp, color = if (isDark) color else color.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = EcoColors.TextPrimary)
        }
    }
}

/**
 * Muestra un par de datos estadísticos (Etiqueta y Valor).
 */
@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = EcoColors.TextPrimary)
        Text(label, fontSize = 11.sp, color = EcoColors.TextSecondary)
    }
}

/**
 * Sección horizontal desplazable que muestra las insignias del usuario.
 * Las insignias no desbloqueadas aparecen con un candado y menor opacidad.
 */
@Composable
fun BadgesSection(scans: Int, puntos: Int, nivel: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val badges = listOf(
            Triple("🌱", "Primer Paso", scans >= 1),
            Triple("🔥", "En Racha", scans >= 7),
            Triple("♻️", "Eco-Guerrero", scans >= 100),
            Triple("🌍", "Guardián", puntos >= 1000),
            Triple("🏆", "Top Player", nivel >= 10)
        )
        badges.forEach { (emoji, name, unlocked) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(68.dp).background(if (unlocked) EcoColors.GlassAccent.copy(alpha = 0.15f) else EcoColors.CardPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 32.sp, modifier = Modifier.alpha(if (unlocked) 1f else 0.2f))
                    if (!unlocked) Icon(Icons.Default.Lock, null, tint = EcoColors.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(name, fontSize = 10.sp, color = if (unlocked) EcoColors.TextPrimary else EcoColors.TextSecondary, fontWeight = if (unlocked) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

/**
 * Sección detallada que calcula y muestra el ahorro de recursos (agua, luz, árboles).
 */
@Composable
fun DetailedImpactSection(scans: Int) {
    val waterSaved = scans * 75
    val energySaved = scans * 10
    val treesSaved = scans / 20.0
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ImpactDetailItem("💧", "Agua", "${waterSaved}L", "Ahorrada", Color(0xFF3B82F6), Modifier.weight(1f))
        ImpactDetailItem("💡", "Luz", "${energySaved}h", "Energía", Color(0xFFF59E0B), Modifier.weight(1f))
        ImpactDetailItem("🌳", "Bosque", "${treesSaved.toString().take(4)}", "Árboles", Color(0xFF10B981), Modifier.weight(1f))
    }
}

/**
 * Item detallado para la sección de impacto. Utiliza una [GlassCard].
 */
@Composable
fun ImpactDetailItem(emoji: String, title: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, cornerRadius = 24) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Black, color = color, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, color = EcoColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = EcoColors.TextSecondary, fontSize = 9.sp, maxLines = 1)
        }
    }
}

/**
 * Calcula el nivel del usuario basado en sus puntos acumulados.
 * 
 * Lógica:
 * - Nivel 1-4: Rangos fijos de puntos.
 * - Nivel 5+: Un nivel extra por cada 500 puntos.
 */
private fun calcularNivel(puntos: Int): Int = when {
    puntos < 100 -> 1
    puntos < 300 -> 2
    puntos < 600 -> 3
    puntos < 1000 -> 4
    puntos < 1500 -> 5
    else -> (puntos / 500) + 3
}
