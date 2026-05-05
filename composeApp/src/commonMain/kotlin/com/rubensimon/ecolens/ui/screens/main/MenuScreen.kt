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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.ui.components.CustomIcons
import com.rubensimon.ecolens.utils.PointsManager
import com.russhwolf.settings.Settings
import kotlinx.datetime.*
import com.rubensimon.ecolens.utils.HistoryManager
import com.rubensimon.ecolens.utils.TimeUtils
import com.rubensimon.ecolens.utils.getTimeAgo
import com.rubensimon.ecolens.data.repository.UserRepository
import androidx.compose.ui.text.style.TextAlign

/**
 * Dashboard principal — equivalente a MenuActivity del Android original.
 *
 * Muestra la mascota virtual, puntos, saludo al usuario y
 * el grid de accesos a todas las funcionalidades.
 */
@Composable
fun MenuScreen(
    onScanClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onMapsClick: () -> Unit,
    onCollectionClick: () -> Unit,
    onUpcyclingClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val settings = remember { Settings() }
    val username = remember { settings.getString("username", "EcoUser") }
    var puntos by remember { mutableIntStateOf(PointsManager.getPoints()) }
    var totalScans by remember { mutableIntStateOf(PointsManager.getTotalScans()) }
    
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val today = now.date
    var selectedDate by remember { mutableStateOf(today) }
    val isTodaySelected = selectedDate == today
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isLoadingData by remember { mutableStateOf(false) }

    // Estado para historial del día seleccionado
    var dailyScans by remember { mutableStateOf<List<com.rubensimon.ecolens.data.models.social.HistoryItem>>(emptyList()) }
    var dailyRedemptions by remember { mutableStateOf<List<com.rubensimon.ecolens.data.models.social.RedemptionModel>>(emptyList()) }
    
    // Datos del gráfico semanal — se recalculan con cada refresh
    var weeklyScansData by remember { mutableStateOf(List(7) { 0 }) }
    
    val userRepo = remember { UserRepository() }
    var globalFeedItems by remember { mutableStateOf<List<com.rubensimon.ecolens.data.models.social.HistoryItemModel>>(emptyList()) }
    var usernamesMap by remember { mutableStateOf<Map<String, String>>(emptyList<Pair<String, String>>().toMap()) }

    // Refrescar al entrar o volver a la pantalla
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(refreshTrigger) {
        val userId = settings.getString("user_id", "")
        if (userId.isNotEmpty()) {
            isLoadingData = true
            // ── Sincronización Inicial con Supabase ──
            PointsManager.loadFromSupabase(userId)
            HistoryManager.loadFromDatabase(userId)
            
            // ── Actualizar datos locales tras sincronización ──
            puntos = PointsManager.getPoints()
            totalScans = PointsManager.getTotalScans()
            
            // ── Recalcular gráfico semanal ──
            val firstDay = today.plus(-(today.dayOfWeek.ordinal), DateTimeUnit.DAY)
            weeklyScansData = (0..6).map { i ->
                val date = firstDay.plus(i, DateTimeUnit.DAY)
                HistoryManager.getHistoryForDate(date.dayOfMonth, date.monthNumber).size
            }
            
            // ── Carga de Feed Global ──
            val activities = userRepo.getGlobalActivity(10)
            if (activities.isNotEmpty()) {
                val userIds = activities.map { it.user_id }.distinct()
                usernamesMap = userRepo.getUsernamesMap(userIds)
                globalFeedItems = activities
            }
            isLoadingData = false
        }
    }

    LaunchedEffect(selectedDate) {
        if (!isTodaySelected) {
            dailyScans = HistoryManager.getHistoryForDate(selectedDate.dayOfMonth, selectedDate.monthNumber)
            val userId = settings.getString("user_id", "")
            if (userId.isNotEmpty()) {
                val isoDate = "${selectedDate.year}-${selectedDate.monthNumber.toString().padStart(2, '0')}-${selectedDate.dayOfMonth.toString().padStart(2, '0')}"
                dailyRedemptions = UserRepository().getRedemptionsForDate(userId, isoDate)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoColors.BackgroundDark)
            .safeDrawingPadding()
    ) {
        // ── Top Bar (Estilo imagen 1) ──────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(EcoColors.CardPrimary, CircleShape).clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(username.take(1).uppercase(), fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Hola, $username", fontSize = 14.sp, color = EcoColors.TextSecondary)
                    
                    val dayName = when(selectedDate.dayOfWeek) {
                        kotlinx.datetime.DayOfWeek.MONDAY -> "Lun"
                        kotlinx.datetime.DayOfWeek.TUESDAY -> "Mar"
                        kotlinx.datetime.DayOfWeek.WEDNESDAY -> "Mie"
                        kotlinx.datetime.DayOfWeek.THURSDAY -> "Jue"
                        kotlinx.datetime.DayOfWeek.FRIDAY -> "Vie"
                        kotlinx.datetime.DayOfWeek.SATURDAY -> "Sab"
                        kotlinx.datetime.DayOfWeek.SUNDAY -> "Dom"
                        else -> ""
                    }
                    val monthName = when(selectedDate.month) {
                        kotlinx.datetime.Month.JANUARY -> "Ene"
                        kotlinx.datetime.Month.FEBRUARY -> "Feb"
                        kotlinx.datetime.Month.MARCH -> "Mar"
                        kotlinx.datetime.Month.APRIL -> "Abr"
                        kotlinx.datetime.Month.MAY -> "May"
                        kotlinx.datetime.Month.JUNE -> "Jun"
                        kotlinx.datetime.Month.JULY -> "Jul"
                        kotlinx.datetime.Month.AUGUST -> "Ago"
                        kotlinx.datetime.Month.SEPTEMBER -> "Sep"
                        kotlinx.datetime.Month.OCTOBER -> "Oct"
                        kotlinx.datetime.Month.NOVEMBER -> "Nov"
                        kotlinx.datetime.Month.DECEMBER -> "Dic"
                        else -> ""
                    }
                    Text("$dayName, ${selectedDate.dayOfMonth} $monthName", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Racha de días
                if (PointsManager.getStreak() > 0) {
                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("🔥", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${PointsManager.getStreak()}", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Badge de Nivel + Barra de progreso
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 12.dp)) {
                    Surface(
                        color = EcoColors.Success.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Success.copy(alpha = 0.4f))
                    ) {
                        Text(
                            "Nivel ${PointsManager.getLevel()}", 
                            color = if (EcoColors.isDark) EcoColors.Success else Color(0xFF065F46), 
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Progress Bar del nivel
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (EcoColors.isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(PointsManager.getProgressToNextLevel())
                                .fillMaxHeight()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF34D399), Color(0xFF10B981))
                                    )
                                )
                        )
                    }
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(40.dp).background(EcoColors.CardPrimary, CircleShape)
                ) {
                    Icon(Icons.Default.Settings, null, tint = EcoColors.TextPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // ── Card Principal: Reto o Resumen ──────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(32.dp),
                color = if (isTodaySelected) EcoColors.GlassAccent else EcoColors.CardPrimary
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dailyGoal = PointsManager.getDailyGoal()
                    val dailyScansCount = PointsManager.getDailyScans()
                    val isMissionDone = dailyScansCount >= dailyGoal

                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isTodaySelected) "Misión del día" else "Resumen del día", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text(
                            if (isTodaySelected) {
                                if (isMissionDone) "¡Misión\nCompletada! 🎉"
                                else "Escanea $dailyGoal\nobjetos ($dailyScansCount/$dailyGoal)"
                            } else {
                                "Resumen del\n${selectedDate.dayOfMonth} de ${selectedDate.month}"
                            }, 
                            color = Color.White, 
                            fontSize = 22.sp, 
                            fontWeight = FontWeight.ExtraBold, 
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (isTodaySelected) {
                            Surface(
                                color = if (isMissionDone) Color.White else Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Text(
                                    if (isMissionDone) "COMPLETADO ✅" else "+100 pts extra", 
                                    color = if (isMissionDone) EcoColors.GlassAccent else Color.White, 
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(if (isTodaySelected) "♻️" else "📅", fontSize = 80.sp, modifier = Modifier.offset(y = 10.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // ── Daily Mission (Nueva sección 1) ───────────────────────
            DailyMissionCard()

            Spacer(modifier = Modifier.height(24.dp))

            // ── Weekly Strip ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val firstDayOfWeek = today.plus(-(today.dayOfWeek.ordinal), kotlinx.datetime.DateTimeUnit.DAY)
                val dayNames = listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom")
                
                dayNames.forEachIndexed { index, day ->
                    val date = firstDayOfWeek.plus(index, kotlinx.datetime.DateTimeUnit.DAY)
                    val isSelected = date == selectedDate
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) (if (EcoColors.isDark) Color.White else Color.Black) else Color.Transparent)
                            .clickable { selectedDate = date }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(day, color = if (isSelected) (if (EcoColors.isDark) Color.Black else Color.White) else EcoColors.TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${date.dayOfMonth}", color = if (isSelected) (if (EcoColors.isDark) Color.Black else Color.White) else EcoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isTodaySelected) {
                // ── MODO HOY: Eco-Comunidad (Novedad) ───────────────────────────
                Text("Eco-Comunidad", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = EcoColors.TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (globalFeedItems.isEmpty()) {
                        Surface(
                            color = EcoColors.CardPrimary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, EcoColors.TextSecondary.copy(alpha = 0.1f))
                        ) {
                            Text("✨ ¡Bienvenido! Empieza a reciclar hoy.", modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = EcoColors.TextPrimary)
                        }
                    } else {
                        globalFeedItems.forEach { activity ->
                            val user = usernamesMap[activity.user_id] ?: "EcoUser"
                            val emoji = if (activity.action_type == "reward") "🎁" else "🌱"
                            val timeAgo = com.rubensimon.ecolens.utils.TimeUtils.getTimeAgo(activity.created_at)
                            
                            // Color dinámico según puntos
                            val color = when {
                                activity.points > 35 -> Color(0xFFAF52DE) // Púrpura (Épico)
                                activity.points > 25 -> Color(0xFFFF9500) // Naranja (Genial)
                                activity.points > 15 -> Color(0xFF34C759) // Verde (Bueno)
                                else -> Color(0xFF007AFF) // Azul (Estándar)
                            }

                            Surface(
                                color = color.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(emoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("@$user", fontWeight = FontWeight.Bold, color = color, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(timeAgo, color = EcoColors.TextSecondary, fontSize = 9.sp)
                                        }
                                        Text("${activity.object_name} (+${activity.points})", color = EcoColors.TextPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // ── MODO HOY: Herramientas ────────────────────────────────────────
                Text("Tus herramientas", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = EcoColors.TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FeatureCard(
                        title = "Escanear",
                        subtitle = "Reciclar ahora",
                        icon = androidx.compose.material.icons.Icons.Default.Camera,
                        color = EcoColors.Success, 
                        onClick = onScanClick,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        title = "Eco-Dex",
                        subtitle = "Tu colección",
                        icon = CustomIcons.Backpack,
                        color = Color(0xFFFBBF24), // Ambar
                        onClick = onCollectionClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FeatureCard(
                        title = "Ranking",
                        subtitle = "Liderazgo",
                        emoji = "🏆",
                        color = Color(0xFF60A5FA), // Azul cielo
                        onClick = onLeaderboardClick,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        title = "Mapa",
                        subtitle = "Puntos cercanos",
                        emoji = "📍",
                        color = Color(0xFF34D399), // Esmeralda
                        onClick = onMapsClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                FeatureCard(
                    title = "Upcycling DIY",
                    subtitle = "Ideas creativas para reutilizar",
                    emoji = "💡",
                    color = Color(0xFFF472B6), // Rosa
                    onClick = onUpcyclingClick,
                    modifier = Modifier.fillMaxWidth(),
                    isWide = true
                )

                Spacer(modifier = Modifier.height(32.dp))
                ActivityChart(data = weeklyScansData)
            } else {
                // ── MODO HISTORIAL: Resumen del día seleccionado ─────────────────
                Text("Actividad de este día", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = EcoColors.TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                if (dailyScans.isEmpty() && dailyRedemptions.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😴", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No hubo actividad este día", color = EcoColors.TextSecondary, textAlign = TextAlign.Center)
                        }
                    }
                }

                if (dailyScans.isNotEmpty()) {
                    Text("Escaneos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EcoColors.TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    dailyScans.forEach { item ->
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.nombre, color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold)
                                    Text(item.fecha, color = EcoColors.TextSecondary, fontSize = 12.sp)
                                }
                                Text("+${item.puntos}", color = EcoColors.GlassGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (dailyRedemptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Recompensas canjeadas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EcoColors.TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    dailyRedemptions.forEach { red ->
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🎁", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Cupón Canjeado", color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold)
                                    Text(red.fechaCanje?.substringBefore("T") ?: "", color = EcoColors.TextSecondary, fontSize = 12.sp)
                                }
                                Text("-??", color = Color.Red.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    emoji: String? = null,
    icon: ImageVector? = null,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWide: Boolean = false
) {
    Surface(
        modifier = modifier
            .height(if (isWide) 100.dp else 160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp),
        color = color.copy(alpha = 0.15f) // Estilo sutil de la imagen
    ) {
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(56.dp).background(color.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    if (icon != null) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                    } else if (emoji != null) {
                        Text(emoji, fontSize = 28.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
                    Text(subtitle, color = EcoColors.TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Surface(
                    color = color.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (icon != null) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                        } else if (emoji != null) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
                Text(subtitle, color = EcoColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ActivityChart(data: List<Int>, modifier: Modifier = Modifier) {
    val maxVal = data.maxOfOrNull { it }?.coerceAtLeast(1) ?: 1
    val dayNames = listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom")
    val todayIndex = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.ordinal
    
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Tu Impacto Semanal", color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Evolución de tu compromiso", color = EcoColors.TextSecondary, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(vertical = 8.dp)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (data.size - 1)
                    
                    // Ajustamos el máximo para que nunca llegue arriba del todo (dejamos un 40% de margen)
                    val rawMax = data.maxOfOrNull { it }?.coerceAtLeast(1) ?: 1
                    val chartMax = rawMax * 1.6f 
                    
                    val points = data.mapIndexed { index, value ->
                        val x = index * spacing
                        
                        // Lógica 100% progresiva:
                        // Si no hay puntos o es el futuro, al suelo (0).
                        val y = if (index > todayIndex || value == 0) {
                            height 
                        } else {
                            // Proporcionalidad pura con "suelo" de escala para evitar saltos bruscos
                            // Usamos al menos 5 como base de escala máxima para que 1 solo scan no llene el gráfico
                            val displayMax = maxVal.coerceAtLeast(5)
                            val ratio = value.toFloat() / displayMax.toFloat()
                            height - (ratio * height * 0.8f)
                        }
                        
                        androidx.compose.ui.geometry.Offset(x, y)
                    }

                    val path = androidx.compose.ui.graphics.Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                // Curva suave entre puntos
                                val p0 = points[i - 1]
                                val p1 = points[i]
                                val controlPoint1 = androidx.compose.ui.geometry.Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
                                val controlPoint2 = androidx.compose.ui.geometry.Offset(p0.x + (p1.x - p0.x) / 2, p1.y)
                                cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                            }
                        }
                    }

                    // Dibujar el degradado bajo la línea
                    val fillPath = androidx.compose.ui.graphics.Path().apply {
                        addPath(path)
                        lineTo(points.last().x, height)
                        lineTo(points.first().x, height)
                        close()
                    }
                    
                    drawPath(
                        path = fillPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(EcoColors.GlassAccent.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )

                    // Dibujar la línea principal
                    drawPath(
                        path = path,
                        color = EcoColors.GlassAccent,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                    
                    // Dibujar punto en el día actual
                    if (todayIndex < points.size) {
                        drawCircle(
                            color = EcoColors.GlassAccent,
                            radius = 6.dp.toPx(),
                            center = points[todayIndex]
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = points[todayIndex]
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayNames.forEachIndexed { index, day ->
                    Text(
                        day, 
                        color = if (index == todayIndex) EcoColors.TextPrimary else EcoColors.TextSecondary, 
                        fontSize = 10.sp, 
                        fontWeight = if (index == todayIndex) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
@Composable
private fun DailyMissionCard() {
    val dailyScans = PointsManager.getDailyScans()
    val goal = PointsManager.getDailyGoal()
    val progress = (dailyScans.toFloat() / goal.toFloat()).coerceAtMost(1f)
    val isCompleted = dailyScans >= goal

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isCompleted) Color(0xFF10B981).copy(alpha = 0.15f) else EcoColors.CardPrimary.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isCompleted) Color(0xFF10B981).copy(alpha = 0.2f) 
                        else EcoColors.GlassAccent.copy(alpha = 0.15f), 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Flag,
                    null,
                    tint = if (isCompleted) Color(0xFF10B981) else EcoColors.GlassAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isCompleted) "¡Misión completada!" else "Misión Diaria",
                        fontWeight = FontWeight.Bold,
                        color = EcoColors.TextPrimary,
                        fontSize = 15.sp
                    )
                    if (isCompleted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✨", fontSize = 14.sp)
                    }
                }
                Text(
                    if (isCompleted) "Has ganado el bono de hoy" else "Recicla $goal objetos hoy ($dailyScans/$goal)",
                    color = EcoColors.TextSecondary,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = if (isCompleted) Color(0xFF10B981) else EcoColors.GlassAccent,
                    trackColor = if (EcoColors.isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}
