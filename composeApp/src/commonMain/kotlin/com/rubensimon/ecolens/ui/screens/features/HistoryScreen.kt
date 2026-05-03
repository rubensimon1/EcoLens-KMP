package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.data.models.social.HistoryItem
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.utils.HistoryManager

/**
 * Historial de escaneos — migrado de HistoryActivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBackClick: () -> Unit) {
    val history = remember { HistoryManager.getHistory() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("📜 Historial", color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = EcoColors.TextPrimary)
                    }
                },
            )
        },
        containerColor = EcoColors.BackgroundDark
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("♻️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sin escaneos aún",
                        color = EcoColors.TextSecondary,
                        fontSize = 16.sp
                    )
                    Text(
                        "¡Escanea tu primer objeto!",
                        color = EcoColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            val groupedHistory = remember(history) {
                history.groupBy { it.fecha.substringBefore(" ") } // Agrupar por fecha sin hora
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                item {
                    Text(
                        "Historial de Reciclaje",
                        color = EcoColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                groupedHistory.forEach { (date, items) ->
                    item {
                        Text(
                            text = date,
                            color = EcoColors.GlassAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(items) { item ->
                        HistoryItemCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: HistoryItem) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.emoji, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text(text = item.nombre, color = EcoColors.TextPrimary, fontWeight = FontWeight.Medium)
                    Text(text = item.fecha, color = EcoColors.TextSecondary, fontSize = 12.sp)
                }
            }
            Text(
                text = "+${item.puntos} pts",
                color = EcoColors.Success,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
