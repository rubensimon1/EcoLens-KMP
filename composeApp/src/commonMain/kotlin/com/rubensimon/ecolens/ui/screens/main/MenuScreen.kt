package com.rubensimon.ecolens.ui.screens.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rubensimon.ecolens.data.models.social.HistoryItem as SocialHistoryItem
import com.rubensimon.ecolens.data.network.SupabaseClientProvider
import com.rubensimon.ecolens.data.repository.UserRepository
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.utils.*
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.PostgresAction.Insert
import io.github.jan.supabase.realtime.PostgresAction.Update
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone

@Composable
fun MenuScreen(
    userId: String,
    onProfileClick: () -> Unit,
    onCameraClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMapsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onEcoDexClick: () -> Unit,
    onAiChatClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onFriendProfileClick: (String) -> Unit,
    onLeaderboardClick: () -> Unit,
    onSddrClick: () -> Unit
) {
    var points by remember { mutableIntStateOf(PointsManager.getPoints()) }
    var todayScansCount by remember { mutableIntStateOf(0) }
    var profilePicUrl by remember { mutableStateOf(HistoryManager.getCachedProfilePic(userId)) }
    var username by remember { mutableStateOf(HistoryManager.getCachedUsername(userId)) }
    var dailyScans by remember { mutableStateOf<List<SocialHistoryItem>>(emptyList()) }
    var weeklyScansCount by remember { mutableStateOf<List<Int>>(List(7) { 0 }) }
    var globalActivity by remember { mutableStateOf(HistoryManager.globalActivityCache) }
    var userAvatars by remember { mutableStateOf(HistoryManager.avatarCache.toMap()) }
    var isCommunityLoading by remember { mutableStateOf(HistoryManager.globalActivityCache.isEmpty()) }
    
    val userRepo = remember { UserRepository() }
    val scope = rememberCoroutineScope()
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var selectedDate by remember { mutableStateOf(today) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val refreshData: suspend () -> Unit = {
                // 1. Cargar datos locales INMEDIATAMENTE para respuesta instantánea
                points = PointsManager.getPoints()
                val itemsToday = HistoryManager.getHistoryForDate(today.dayOfMonth, today.monthNumber)
                todayScansCount = itemsToday.size
                dailyScans = HistoryManager.getHistoryForDate(selectedDate.dayOfMonth, selectedDate.monthNumber)
                
                val startOfWeek = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
                weeklyScansCount = (0..6).map { offset ->
                    val d = startOfWeek.plus(offset, DateTimeUnit.DAY)
                    HistoryManager.getHistoryForDate(d.dayOfMonth, d.monthNumber).size
                }

                // 2. Intentar sincronización con red en background
                try {
                    PointsManager.loadFromSupabase(userId)
                    HistoryManager.loadFromDatabase(userId)
                    
                    // 3. Actualizar UI con datos remotos si hubo cambios
                    points = PointsManager.getPoints()
                    val user = userRepo.getUserById(userId)
                    if (user != null) {
                        profilePicUrl = user.profile_picture_url
                        username = user.display_name ?: user.username
                        HistoryManager.cacheUserProfile(user)
                    }
                    
                    val itemsUpdated = HistoryManager.getHistoryForDate(today.dayOfMonth, today.monthNumber)
                    todayScansCount = itemsUpdated.size
                    dailyScans = HistoryManager.getHistoryForDate(selectedDate.dayOfMonth, selectedDate.monthNumber)
                    
                    weeklyScansCount = (0..6).map { offset ->
                        val d = startOfWeek.plus(offset, DateTimeUnit.DAY)
                        HistoryManager.getHistoryForDate(d.dayOfMonth, d.monthNumber).size
                    }
                } catch (e: Exception) {
                    println("Error refreshData (Offline): ${e.message}")
                }
            }
            
            // Cargar datos al iniciar
            launch { refreshData() }
            
            // Detector de retorno a la pantalla (OnResume equivalente en KMP)
            // Esto asegura que al volver de Escanear, el gráfico se actualice.
            launch {
                SupabaseClientProvider.client.realtime.connect() // Asegurar conexión
                // También podemos disparar refreshData periódicamente o al detectar foco
            }

            // Suscripción a cambios en tiempo real
            val channel = SupabaseClientProvider.client.realtime.channel("menu-sync")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "usuarios" }.onEach { if (it is Update) refreshData() }.launchIn(this)
            channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "historial_escaneos" }.onEach { if (it is Insert) refreshData() }.launchIn(this)
            channel.subscribe()

            // Cargar comunidad de forma asíncrona y optimizada (una sola consulta join)
            launch {
                try {
                    // Si el cache está vacío, mostramos el esqueleto de carga
                    if (HistoryManager.globalActivityCache.isEmpty()) {
                        isCommunityLoading = true
                    }
                    
                    val activitiesWithUsers = userRepo.getGlobalActivityWithProfiles(10)
                    
                    if (activitiesWithUsers.isNotEmpty()) {
                        // Actualizar Cache Global
                        val newAvatars = activitiesWithUsers.associate { it.second.id to it.second.profile_picture_url }
                        HistoryManager.avatarCache.putAll(newAvatars)
                        
                        val newActivity = activitiesWithUsers.map { (act, user) ->
                            SocialHistoryItem(
                                user_id = act.user_id,
                                nombre = user.display_name ?: user.username,
                                objeto = act.object_name,
                                puntos = act.points,
                                emoji = HistoryManager.getEmojiForObject(act.object_name),
                                fecha = act.created_at ?: ""
                            )
                        }
                        HistoryManager.globalActivityCache = newActivity
                        
                        // Actualizar UI
                        userAvatars = HistoryManager.avatarCache.toMap()
                        globalActivity = newActivity
                    }
                } catch (e: Exception) { 
                    println("Error comunidad: ${e.message}")
                } finally {
                    isCommunityLoading = false
                }
            }
        }
    }

    val unreadNotifications by com.rubensimon.ecolens.utils.NotificationManager.unreadCount.collectAsState()
    val hasNewNotifications = unreadNotifications > 0

    var isOnline by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Detector de conexión simple: intentamos refrescar cada 5 segundos
        while(true) {
            val wasOffline = !isOnline
            try {
                // Un ping rápido a Supabase para verificar conexión real
                SupabaseClientProvider.client.from("usuarios").select {
                    limit(1)
                }
                isOnline = true
                // Si acabamos de recuperar la conexión, sincronizamos pendientes
                if (wasOffline) {
                    HistoryManager.syncPendingItems()
                }
            } catch (e: Exception) {
                isOnline = false
            }
            kotlinx.coroutines.delay(5000)
        }
    }

    // ── Refresco automático al volver a la pantalla ──
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    // Refrescar solo localmente para rapidez
                    points = PointsManager.getPoints()
                    val itemsToday = HistoryManager.getHistoryForDate(today.dayOfMonth, today.monthNumber)
                    todayScansCount = itemsToday.size
                    dailyScans = HistoryManager.getHistoryForDate(selectedDate.dayOfMonth, selectedDate.monthNumber)
                    
                    val startOfWeek = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
                    weeklyScansCount = (0..6).map { offset ->
                        val d = startOfWeek.plus(offset, DateTimeUnit.DAY)
                        HistoryManager.getHistoryForDate(d.dayOfMonth, d.monthNumber).size
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        var screenLoaded by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { screenLoaded = true }
        
        AnimatedVisibility(
            visible = screenLoaded,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 100 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                ) {
                    // ── HEADER ORBE Y ANILLO DE NIVEL ──────────────
                // ── Lógica de Nivel con Animación de Reinicio (Tipo Juego) ──
                val targetLevel = calcularNivel(points)
                var displayedLevel by remember { mutableStateOf(targetLevel) }
                val progressAnimatable = remember { androidx.compose.animation.core.Animatable(0f) }

                LaunchedEffect(points) {
                    val currentLevelInAnim = calcularNivel(points)
                    
                    if (currentLevelInAnim > displayedLevel) {
                        // 1. Completar el círculo actual hasta el 100%
                        progressAnimatable.animateTo(1f, animationSpec = tween(800, easing = LinearEasing))
                        // 2. Cambiar nivel visual y resetear círculo a 0
                        displayedLevel = currentLevelInAnim
                        progressAnimatable.snapTo(0f)
                    }
                    
                    // 3. Animar hasta el progreso real actual
                    val base = getBaseLevelPoints(currentLevelInAnim)
                    val next = getNextLevelPoints(currentLevelInAnim)
                    val actualProgress = ((points - base).toFloat() / (next - base)).coerceIn(0f, 1f)
                    progressAnimatable.animateTo(actualProgress, animationSpec = tween(1200, easing = FastOutSlowInEasing))
                }
                
                val animatedProgress = progressAnimatable.value

                // ── Lógica de Saludo Dinámico ──
                val greeting = remember {
                    val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).hour
                    when (now) {
                        in 5..12 -> "¡Buenos días,"
                        in 13..20 -> "¡Buenas tardes,"
                        else -> "¡Buenas noches,"
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar Orbe con anillo
                        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = EcoColors.CardPrimary, strokeWidth = 3.dp)
                            CircularProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxSize(), color = EcoColors.GlassAccent, strokeWidth = 3.dp, strokeCap = StrokeCap.Round)
                            
                            val currentProfilePic = profilePicUrl 
                            val isHeaderAvatarEmpty = currentProfilePic.isNullOrEmpty() || currentProfilePic == "null" || currentProfilePic.isBlank()
                            val headerAvatarGradient = remember(username) { EcoColors.getAvatarGradient(username) }
                            
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(headerAvatarGradient, CircleShape)
                                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    .clip(CircleShape)
                                    .clickable { onProfileClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                // Capa 1: Letra al fondo
                                Text(
                                    text = username.trim().replace("@", "").take(1).uppercase().ifEmpty { "U" },
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )

                                // Capa 2: Imagen encima
                                if (!isHeaderAvatarEmpty) {
                                    AsyncImage(
                                        model = currentProfilePic,
                                        contentDescription = "Perfil",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            
                            // Nivel flotante
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .size(20.dp)
                                    .background(EcoColors.GlassAccent, CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$displayedLevel", 
                                    color = Color.White, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("$greeting $username!", color = EcoColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("Hoy", color = EcoColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedLevelBadge(points, todayScansCount)
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Icono de Notificaciones con Badge dinámico
                        Box {
                            IconButton(
                                onClick = { 
                                    onNotificationsClick() 
                                }, 
                                modifier = Modifier.size(40.dp).background(EcoColors.CardPrimary.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(Icons.Default.Notifications, null, tint = EcoColors.TextPrimary, modifier = Modifier.size(20.dp))
                            }
                            // Solo dibujamos el badge si hay algo nuevo
                            if (hasNewNotifications) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = 2.dp)
                                        .size(10.dp)
                                        .background(Color(0xFFFF5252), CircleShape)
                                        .border(1.5.dp, EcoColors.BackgroundDark, CircleShape)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        IconButton(
                            onClick = { onSettingsClick() }, 
                            modifier = Modifier.size(40.dp).background(EcoColors.CardPrimary.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, null, tint = EcoColors.TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

            // ── CALENDARIO (Actualiza la fecha y la lista de ese día) ────
            WeeklyCalendar(selectedDate) { date -> 
                selectedDate = date 
                dailyScans = HistoryManager.getHistoryForDate(date.dayOfMonth, date.monthNumber)
            }

            // ── TRANSICIÓN ANIMADA ENTRE VISTAS ─────────────────────────
            AnimatedContent(
                targetState = selectedDate == today,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(200))
                },
                modifier = Modifier.animateContentSize()
            ) { isToday ->
                if (isToday) {
                    // ── VISTA DE HOY (DASHBOARD COMPLETO) ───────────────────────
                    Column {
                        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                            MissionCard(todayScansCount)
                        }

                        DailyProgressBar(todayScansCount)

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // ── ACCESO AL ASISTENTE IA (NUEVO) ───────────────────────
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth().shimmerEffect(durationMillis = 5000),
                                backgroundColor = EcoColors.CardPrimary.copy(alpha = 0.4f),
                                cornerRadius = 24,
                                onClick = onAiChatClick
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(44.dp).background(EcoColors.GlassAccent.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, null, tint = EcoColors.GlassAccent, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Eco-Asistente IA", fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary, fontSize = 15.sp)
                                        Text("Pregúntame cómo reciclar o ideas de upcycling", color = EcoColors.TextSecondary, fontSize = 11.sp)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = EcoColors.TextSecondary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // ── ACCESO A LA COMPETICIÓN (RANKING) ───────────────────
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = EcoColors.CardPrimary.copy(alpha = 0.4f),
                                cornerRadius = 24,
                                onClick = onLeaderboardClick
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(44.dp).background(Color(0xFFFFD700).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Competición Global", fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary, fontSize = 15.sp)
                                        Text("Mira quién lidera el ranking de hoy", color = EcoColors.TextSecondary, fontSize = 11.sp)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = EcoColors.TextSecondary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        StyledSectionTitle("Eco-Comunidad", Icons.Default.Groups)
                        
                        if (isCommunityLoading) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(3) { 
                                    SkeletonBox(modifier = Modifier.width(180.dp).height(110.dp), shape = RoundedCornerShape(24.dp))
                                }
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(globalActivity) { item ->
                                    CommunityItem(item, userAvatars[item.user_id]) { targetUserId ->
                                        // Solo navegamos si es el perfil de OTRA persona
                                        if (targetUserId != userId) {
                                            onFriendProfileClick(targetUserId)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        StyledSectionTitle("Tus Herramientas", Icons.Default.AutoAwesome)
                        
                        // ── FLOATING ORBS PARA HERRAMIENTAS ────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToolOrb("Mapa", Icons.Default.Map) { onMapsClick() }
                            ToolOrb("Eco-Dex", Icons.Default.Backpack) { onEcoDexClick() }
                            
                            // Gran Orbe Pulsante de Escaneo
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onCameraClick() }.offset(y = (-8).dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .background(Brush.radialGradient(listOf(Color(0xFF76D7C4), Color(0xFF2ECC71))), CircleShape)
                                        .border(4.dp, EcoColors.GlassAccent.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Escanear", color = EcoColors.GlassAccent, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                            
                            ToolOrb("Eco-Retorno", Icons.Default.Autorenew) { onSddrClick() }
                            ToolOrb("Logros", Icons.Default.EmojiEvents) { onAchievementsClick() }
                        }
                        
                        // ── GRÁFICO ANIMADO (HOY: AL FINAL) ───────────────────────
                        Spacer(modifier = Modifier.height(32.dp))
                        StyledSectionTitle("Actividad Semanal", Icons.Default.ShowChart)
                        AnimatedWeeklyLineChart(weeklyScansCount, selectedDate.dayOfWeek.ordinal)
                    }
                } else {
                    // ── VISTA DE HISTORIAL DIARIO ───────────────────────────────
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // ── GRÁFICO ANIMADO (OTRO DÍA: AL PRINCIPIO) ───────────────────────
                        StyledSectionTitle("Actividad Semanal", Icons.Default.ShowChart)
                        AnimatedWeeklyLineChart(weeklyScansCount, selectedDate.dayOfWeek.ordinal)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        if (dailyScans.isNotEmpty()) {
                            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                StyledSectionTitle("Tus escaneos de este día", Icons.Default.History)
                                Spacer(modifier = Modifier.height(8.dp))
                                dailyScans.forEach { scan ->
                                    ActivityRow(scan)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sin escaneos este día",
                                    fontSize = 14.sp,
                                    color = EcoColors.TextSecondary.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Espacio para la ModernBottomBar de App.kt
            Spacer(modifier = Modifier.height(140.dp))
        }

        // ── BANNER SIN CONEXIÓN (NUEVO) ──────────────────────────
        AnimatedVisibility(
            visible = !isOnline,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
        ) {
            GlassCard(
                backgroundColor = Color(0xFFFF5252).copy(alpha = 0.9f),
                cornerRadius = 50,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudOff, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sin conexión - Modo Offline activo", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
}
}

// ── COMPONENTES ESPECÍFICOS DEL MENÚ ────────────────────────────────────

@Composable
fun StyledSectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    Brush.linearGradient(listOf(EcoColors.GlassAccent, Color(0xFF34D399))),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = EcoColors.TextPrimary,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
fun WeeklyCalendar(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val days = remember {
        val start = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        (0..6).map { start.plus(it - start.dayOfWeek.ordinal, DateTimeUnit.DAY) }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { date ->
            val isSelected = date == selectedDate
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onDateSelected(date) }.padding(4.dp)
            ) {
                Text(date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp, color = EcoColors.TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(if (isSelected) EcoColors.GlassAccent else Color.Transparent)
                        .then(if (!isSelected) Modifier.border(1.dp, EcoColors.TextSecondary.copy(alpha = 0.2f), CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(date.dayOfMonth.toString(), color = if (isSelected) Color.White else EcoColors.TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MissionCard(scans: Int) {
    val shape = RoundedCornerShape(50.dp)
    GlassCard(
        modifier = Modifier.clip(shape).shimmerEffect(),
        backgroundColor = EcoColors.GlassAccent.copy(alpha = 0.9f), 
        cornerRadius = 50, 
        onClick = {}
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            // Icono translúcido gigante
            Icon(
                Icons.Default.Recycling, 
                contentDescription = null, 
                tint = Color.White.copy(alpha = 0.15f), 
                modifier = Modifier.size(150.dp).align(Alignment.CenterEnd).offset(x = 30.dp, y = 10.dp)
            )
            
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalArrangement = Arrangement.Center) {
                val isCompleted = scans >= 3
                Text("Misión del día", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isCompleted) "¡Misión completada!" else "Escanea 3 objetos", 
                    color = Color.White, 
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Si está completado, mostramos un gran CHECK, si no, las papeleras
                if (isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(28.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("¡Buen trabajo, ruben!", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                    }
                } else {
                    // Botellas luminosas
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) { index ->
                            val isLit = index < scans
                            val color = if (isLit) Color.White else Color.White.copy(alpha = 0.3f)
                            Icon(
                                Icons.Default.LocalDrink,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityItem(item: SocialHistoryItem, avatarUrl: String?, onProfileClick: (String) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    GlassCard(
        modifier = Modifier.width(180.dp).clip(shape).shimmerEffect(durationMillis = 5000), 
        cornerRadius = 24,
        onClick = { onProfileClick(item.user_id) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isAvatarEmpty = avatarUrl.isNullOrEmpty() || avatarUrl == "null" || avatarUrl.isBlank()
                val avatarGradient = remember(item.nombre) { EcoColors.getAvatarGradient(item.nombre) }
                
                // Avatar o Letra Inicial
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(avatarGradient, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Capa 1: Letra siempre al fondo
                    Text(
                        text = item.nombre.trim().replace("@", "").take(1).uppercase().ifEmpty { "U" },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    // Capa 2: Imagen encima
                    if (!isAvatarEmpty) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("@${item.nombre}", fontSize = 12.sp, color = EcoColors.GlassAccent, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(item.objeto, fontWeight = FontWeight.Black, fontSize = 15.sp, color = EcoColors.TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("+${item.puntos} pts", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(TimeUtils.getTimeAgo(item.fecha), color = EcoColors.TextSecondary.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun DailyProgressBar(scans: Int) {
    val progress = (scans / 3f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1500, easing = FastOutSlowInEasing)
    )
    
    val shape = RoundedCornerShape(32.dp)
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(shape).shimmerEffect(durationMillis = 4000), 
        cornerRadius = 32
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = EcoColors.TextSecondary.copy(alpha = 0.1f),
                    strokeWidth = 6.dp
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = EcoColors.GlassAccent,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                Text("$scans/3", fontWeight = FontWeight.Black, fontSize = 16.sp, color = EcoColors.TextPrimary)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Progreso de Hoy", fontWeight = FontWeight.Black, fontSize = 18.sp, color = EcoColors.TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(if (scans >= 3) "¡Misión diaria completada!" else "Te faltan ${3 - scans} objetos para el bonus", fontSize = 13.sp, color = EcoColors.TextSecondary)
            }
            if (scans >= 3) {
                Box(modifier = Modifier.size(40.dp).background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun ToolOrb(title: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.size(56.dp).background(EcoColors.CardPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = EcoColors.GlassAccent, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, color = EcoColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnimatedWeeklyLineChart(weeklyData: List<Int>, selectedDayIndex: Int) {
    // Ajustamos la escala para que sea menos sensible. 
    // Usamos un mínimo de 15 para que 3-4 escaneos no llenen todo el gráfico.
    // Además añadimos un margen del 20% arriba para que los puntos no toquen el borde.
    val actualMax = weeklyData.maxOrNull() ?: 0
    val maxScans = (actualMax.toFloat().coerceAtLeast(15f) * 1.2f)
    val days = listOf("L", "M", "X", "J", "V", "S", "D")
    
    var animationPlayed by remember { mutableStateOf(false) }
    val pathProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
    )
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), cornerRadius = 32) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val lineColor = EcoColors.GlassAccent
                val fillColor = EcoColors.GlassAccent.copy(alpha = 0.2f)
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val spacePerPoint = width / (weeklyData.size - 1).coerceAtLeast(1)
                    val path = Path()
                    val fillPath = Path()
                    
                    val points = weeklyData.mapIndexed { index, scans ->
                        val x = index * spacePerPoint
                        val y = height - (height * (scans.toFloat() / maxScans))
                        Offset(x, y)
                    }
                    
                    if (points.isNotEmpty()) {
                        path.moveTo(points.first().x, points.first().y)
                        fillPath.moveTo(points.first().x, height)
                        fillPath.lineTo(points.first().x, points.first().y)
                        
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                            val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                            
                            path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                            fillPath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                        }
                        
                        fillPath.lineTo(points.last().x, height)
                        fillPath.close()
                        
                        clipRect(right = width * pathProgress) {
                            drawPath(path = fillPath, brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)))
                            drawPath(path = path, color = lineColor, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        
                        points.forEachIndexed { index, point ->
                            if (point.x <= width * pathProgress) {
                                val isSelected = index == selectedDayIndex
                                val dotRadius = if (isSelected) 7.dp.toPx() else 4.dp.toPx()
                                val dotColor = if (isSelected) Color(0xFFFF5722) else lineColor
                                drawCircle(color = dotColor, radius = dotRadius, center = point)
                                drawCircle(color = Color.White, radius = dotRadius / 2, center = point)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEachIndexed { index, day ->
                    val isSelected = index == selectedDayIndex
                    Text(
                        text = day, 
                        fontSize = 13.sp, 
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) Color(0xFFFF5722) else EcoColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedLevelBadge(points: Int, todayScansCount: Int) {
    val level = calcularNivel(points)
    val basePoints = getBaseLevelPoints(level)
    val nextPoints = getNextLevelPoints(level)
    
    val currentProgress = (points - basePoints).coerceAtLeast(0)
    val requiredProgress = (nextPoints - basePoints).coerceAtLeast(1)
    val progressRatio = (currentProgress.toFloat() / requiredProgress.toFloat()).coerceIn(0f, 1f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(1500, easing = FastOutSlowInEasing)
    )

    val hasFire = todayScansCount >= 3 // Racha de fuego si completa la misión diaria
    val fireColor = if (hasFire) Color(0xFFFF5722) else EcoColors.TextSecondary.copy(alpha = 0.3f)
    val fireBgColor = if (hasFire) Color(0xFFFF5722).copy(alpha = 0.15f) else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(EcoColors.CardPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(20.dp).background(fireBgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Whatshot, contentDescription = null, tint = fireColor, modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        
        Column(verticalArrangement = Arrangement.Center) {
            Text("Nivel $level", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(EcoColors.TextSecondary.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFB74D), Color(0xFFFF5722))
                            )
                        )
                )
            }
        }
    }
}

private fun calcularNivel(puntos: Int): Int = when {
    puntos < 100 -> 1
    puntos < 300 -> 2
    puntos < 600 -> 3
    puntos < 1000 -> 4
    puntos < 1500 -> 5
    else -> (puntos / 500) + 3
}

private fun getNextLevelPoints(level: Int): Int = when (level) {
    1 -> 100
    2 -> 300
    3 -> 600
    4 -> 1000
    else -> (level - 2) * 500
}

private fun getBaseLevelPoints(level: Int): Int = when (level) {
    1 -> 0
    2 -> 100
    3 -> 300
    4 -> 600
    5 -> 1000
    else -> (level - 3) * 500
}
