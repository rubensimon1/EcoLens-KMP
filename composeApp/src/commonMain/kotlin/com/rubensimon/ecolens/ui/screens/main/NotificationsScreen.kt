package com.rubensimon.ecolens.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.ui.components.EcoColors
import com.rubensimon.ecolens.ui.components.GlassCard
import com.rubensimon.ecolens.ui.components.GlassBackground

data class EcoNotification(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val time: String,
    val isRead: Boolean = false
)

@Composable
fun NotificationsScreen(onBackClick: () -> Unit) {
    var dailyReminderEnabled by remember { mutableStateOf(true) }
    
    val notifications = remember {
        listOf(
            EcoNotification("¡Nuevo Récord!", "Has superado tu media de escaneos semanales. ¡Sigue así!", Icons.Default.EmojiEvents, "Hace 2h", isRead = false),
            EcoNotification("Logro Desbloqueado", "Has alcanzado el Nivel 2. ¡Felicidades!", Icons.Default.Stars, "Ayer", isRead = false),
            EcoNotification("Comunidad", "@admin ha compartido una nueva idea de upcycling.", Icons.Default.Groups, "Hace 2 días", isRead = true)
        )
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, null, tint = EcoColors.TextPrimary)
                }
                Text("Notificaciones", color = EcoColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }

            // Daily Reminder Setting
            GlassCard(
                backgroundColor = EcoColors.CardPrimary.copy(alpha = 0.4f),
                cornerRadius = 24
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(EcoColors.GlassAccent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Alarm, null, tint = EcoColors.GlassAccent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recordatorio Diario", fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary)
                        Text("Te avisaremos para completar tu misión", color = EcoColors.TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = dailyReminderEnabled,
                        onCheckedChange = { dailyReminderEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EcoColors.GlassAccent, checkedTrackColor = EcoColors.GlassAccent.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Recientes", color = EcoColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItem(notification)
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: EcoNotification) {
    GlassCard(
        backgroundColor = EcoColors.CardPrimary.copy(alpha = 0.3f),
        cornerRadius = 20
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(EcoColors.TextSecondary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(notification.icon, null, tint = EcoColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(notification.title, fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary, fontSize = 14.sp)
                        if (!notification.isRead) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(EcoColors.GlassAccent, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("NUEVO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Text(notification.time, color = EcoColors.TextSecondary, fontSize = 11.sp)
                }
                Text(notification.description, color = EcoColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}
