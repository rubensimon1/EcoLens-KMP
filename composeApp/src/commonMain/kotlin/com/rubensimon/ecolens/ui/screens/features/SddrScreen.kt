package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.utils.SddrManager
import com.rubensimon.ecolens.utils.SddrHistoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SddrScreen(
    onBackClick: () -> Unit,
    onScanClick: () -> Unit,
    onMapClick: () -> Unit
) {
    val balance by SddrManager.balance.collectAsState()
    val totalRecovered by SddrManager.totalRecovered.collectAsState()
    val containersCount by SddrManager.containersCount.collectAsState()
    val history by SddrManager.history.collectAsState()
    
    var showScanSimulation by remember { mutableStateOf(false) }
    
    // Al entrar en la pantalla, forzamos un refresco de datos desde la nube
    LaunchedEffect(Unit) {
        SddrManager.fetchCloudHistory()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(EcoColors.BackgroundDark).statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Text("Eco-Retorno (SDDR)", color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, "Atrás", tint = EcoColors.TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = EcoColors.BackgroundDark)
                )
            }
        },
        containerColor = EcoColors.BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp)
        ) {
            item {
                SddrHeaderCard(balance, totalRecovered, containersCount)
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = onScanClick
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null, tint = EcoColors.GlassAccent, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Escanear Vale", color = EcoColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                    
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = onMapClick
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Puntos Retorno", color = EcoColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                StyledHeader("HISTORIAL DE DEVOLUCIONES", Icons.Default.History)
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text("Aún no has devuelto envases.\n¡Tu primera devolución te espera!", 
                            color = EcoColors.TextSecondary, 
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp)
                    }
                }
            } else {
                items(history) { item ->
                    SddrActivityRow(item)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        
        if (showScanSimulation) {
            SddrSimulationDialog(
                onDismiss = { showScanSimulation = false },
                onRedeem = { count ->
                    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    SddrManager.redeemVoucher("SDDR|0.10|$count|$now")
                    showScanSimulation = false
                }
            )
        }
    }
}

@Composable
fun SddrHeaderCard(balance: Float, total: Float, count: Int) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = EcoColors.CardPrimary.copy(alpha = 0.6f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Saldo Disponible", color = EcoColors.TextSecondary, fontSize = 14.sp)
            Text("${balance.format(2)}€", color = EcoColors.TextPrimary, fontSize = 42.sp, fontWeight = FontWeight.Black)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SddrStatItem(Icons.Default.LocalDrink, "$count", "Envases")
                SddrStatItem(Icons.Default.TrendingUp, "${total.format(2)}€", "Total")
            }
        }
    }
}

@Composable
fun SddrStatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = EcoColors.GlassAccent, modifier = Modifier.size(20.dp))
        Text(value, color = EcoColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = EcoColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun SddrActivityRow(item: SddrHistoryItem) {
    Row(
        modifier = Modifier.fillMaxWidth().background(EcoColors.CardPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(EcoColors.GlassAccent.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Done, null, tint = EcoColors.GlassAccent, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(item.date, color = EcoColors.TextSecondary, fontSize = 12.sp)
        }
        Text("+${item.amount.format(2)}€", color = EcoColors.GlassAccent, fontWeight = FontWeight.Black)
    }
}

@Composable
fun SddrSimulationDialog(onDismiss: () -> Unit, onRedeem: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simular Escaneo SDDR", color = Color.White) },
        text = {
            Column {
                Text("Esta es una simulación del futuro Sistema de Depósito. ¿Cuántos envases has devuelto?", color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    repeat(3) { i ->
                        val count = (i + 1) * 2
                        GlassButton(onClick = { onRedeem(count) }) {
                            Text("$count")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White) }
        },
        containerColor = EcoColors.BackgroundDark,
        shape = RoundedCornerShape(24.dp)
    )
}

// Extension function for formatting float in KMP
fun Float.format(digits: Int): String {
    val str = this.toString()
    val parts = str.split(".")
    val integerPart = parts[0]
    val decimalPart = if (parts.size > 1) parts[1] else ""
    val paddedDecimal = decimalPart.padEnd(digits, '0').substring(0, digits)
    return "$integerPart.$paddedDecimal"
}

