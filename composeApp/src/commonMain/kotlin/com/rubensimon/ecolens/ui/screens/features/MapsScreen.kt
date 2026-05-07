package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.data.models.maps.RecyclingPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(onBackClick: () -> Unit) {
    var selectedFilter by remember { mutableStateOf("TODOS") }
    var selectedPoint by remember { mutableStateOf<RecyclingPoint?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var recenterTrigger by remember { mutableStateOf(0) }
    val filters = listOf("TODOS", "FIJO", "MOVIL", "PROXIMIDAD")

    Box(modifier = Modifier.fillMaxSize()) {
        // ── MAPA (A pantalla completa) ──────────────────────────────────
        PlatformMapView(
            modifier = Modifier.fillMaxSize(), 
            filter = selectedFilter,
            recenterTrigger = recenterTrigger,
            onPointSelected = { point ->
                selectedPoint = point
                showSheet = true
            }
        )

        // ── CONTROLES SUPERIORES (Estilo Flotante) ──────────────────────
        // ── CONTROLES SUPERIORES (Estilo Flotante) ──────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de volver flotante
            Surface(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = EcoColors.TextPrimary, modifier = Modifier.padding(8.dp))
            }
            
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    modifier = Modifier.clickable { selectedFilter = filter }.height(40.dp),
                    color = if (isSelected) EcoColors.GlassAccent else Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            filter,
                            color = if (isSelected) Color.White else Color(0xFF4B5563),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── ACCIONES LATERALES (Centrar ubicación) ──────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { recenterTrigger++ },
                containerColor = Color.White,
                contentColor = EcoColors.GlassAccent,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.MyLocation, "Mi ubicación")
            }
            
            FloatingActionButton(
                onClick = { /* Cambiar tipo de mapa */ },
                containerColor = EcoColors.GlassAccent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.Layers, "Capas")
            }
        }

        // ── DETALLE DEL PUNTO (Bottom Sheet) ────────────────────────────
        if (showSheet && selectedPoint != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                containerColor = if (EcoColors.isDark) Color(0xFF1F2937) else Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle(color = EcoColors.TextSecondary.copy(alpha = 0.3f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 48.dp)
                ) {
                    Text(
                        selectedPoint?.name ?: "Punto de reciclaje",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = EcoColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = when(selectedPoint?.kind) {
                                "FIJO" -> Color(0xFF10B981)
                                "MOVIL" -> Color(0xFFF59E0B)
                                else -> Color(0xFF3B82F6)
                            }.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                selectedPoint?.kind ?: "GENERAL",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when(selectedPoint?.kind) {
                                    "FIJO" -> Color(0xFF10B981)
                                    "MOVIL" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF3B82F6)
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(EcoColors.GlassAccent.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocationOn, null, tint = EcoColors.GlassAccent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            selectedPoint?.snippet ?: "Sin dirección detallada",
                            color = EcoColors.TextSecondary,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { /* Navegar */ },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.GlassAccent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Directions, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cómo llegar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
expect fun PlatformMapView(
    modifier: Modifier, 
    filter: String,
    recenterTrigger: Int,
    onPointSelected: (com.rubensimon.ecolens.data.models.maps.RecyclingPoint) -> Unit
)
