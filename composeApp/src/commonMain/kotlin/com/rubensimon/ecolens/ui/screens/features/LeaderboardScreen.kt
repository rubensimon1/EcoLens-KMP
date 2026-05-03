package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.data.models.items.LeaderboardItem
import com.rubensimon.ecolens.data.repository.UserRepository
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.utils.PointsManager
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch

/**
 * Pantalla de ranking — migrada de LeaderboardActivity.
 *
 * Carga el top 10 de Supabase y destaca al usuario actual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val settings = remember { Settings() }
    val currentUsername = remember { settings.getString("username", "") }
    var leaderboard by remember { mutableStateOf<List<LeaderboardItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar top usuarios
    LaunchedEffect(Unit) {
        scope.launch {
            val users = UserRepository().getTopUsers(10)
            leaderboard = users.mapIndexed { index, user ->
                LeaderboardItem(
                    userId = user.id,
                    rank = index + 1,
                    name = user.display_name ?: user.username,
                    points = user.puntos,
                    isCurrentUser = user.username == currentUsername
                )
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(EcoColors.BackgroundDark).statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Text("🏆 Ranking Global", color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = EcoColors.TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EcoColors.BackgroundDark,
                        titleContentColor = EcoColors.TextPrimary,
                        navigationIconContentColor = EcoColors.TextPrimary
                    )
                )
            }
        },
        containerColor = EcoColors.BackgroundDark
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EcoColors.GlassAccent)
            }
        } else if (leaderboard.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(EcoColors.CardPrimary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌍", fontSize = 64.sp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Aún no hay nadie más aquí",
                    color = EcoColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "¡Invita a tus amigos a unirse al reto y competid por ser el más sostenible!",
                    color = EcoColors.TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                GlassButton(
                    onClick = { /* Lógica de invitar */ },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Invitar amigos")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                itemsIndexed(leaderboard) { _, item ->
                    LeaderboardItemCard(item, onClick = { onProfileClick(item.userId) })
                }
            }
        }
    }
}

@Composable
private fun LeaderboardItemCard(item: LeaderboardItem, onClick: () -> Unit) {
    val rankColor = when (item.rank) {
        1 -> Color(0xFFFFD700) // Oro
        2 -> Color(0xFFC0C0C0) // Plata
        3 -> Color(0xFFCD7F32) // Bronce
        else -> EcoColors.TextSecondary
    }
    
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        backgroundColor = if (item.isCurrentUser) EcoColors.GlassAccent.copy(alpha = 0.15f) else EcoColors.CardBackground,
        cornerRadius = 24
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Posición con trofeo para top 3
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (item.rank <= 3) rankColor.copy(alpha = 0.2f) else EcoColors.CardPrimary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.rank <= 3) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = rankColor,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "${item.rank}",
                            color = rankColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = item.name + if (item.isCurrentUser) " (tú)" else "",
                        color = EcoColors.TextPrimary,
                        fontWeight = if (item.isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    if (item.isCurrentUser) {
                        Text("¡Buen trabajo!", color = EcoColors.GlassAccent, fontSize = 11.sp)
                    }
                }
            }
            Text(
                text = "${item.points} pts",
                color = if (item.rank <= 3) rankColor else EcoColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
