package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.data.models.maps.RecyclingPoint
import androidx.compose.foundation.shape.CircleShape

/**
 * Pantalla de mapa de puntos de reciclaje.
 *
 * ### Estrategia KMP
 * Google Maps solo está disponible en Android (Maps SDK for Android) o iOS (Apple Maps).
 * Esta pantalla usa un `expect/actual`:
 * - `commonMain`: Muestra UI placeholder + botón (esta pantalla)
 * - `androidMain`: Contiene el composable real con Google Maps (MapViewContainer.android.kt)
 *
 * Para implementación completa en iOS se puede usar Mapbox o Apple MapKit via expect/actual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(onBackClick: () -> Unit) {
    var selectedFilter by remember { mutableStateOf("TODOS") }
    var selectedPoint by remember { mutableStateOf<RecyclingPoint?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var recenterTrigger by remember { mutableStateOf(0) }
    val filters = listOf("TODOS", "FIJO", "MOVIL", "PROXIMIDAD")

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(EcoColors.BackgroundDark).statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Text(
                            "🗺️ Mapa de reciclaje",
                            color = EcoColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Atrás",
                                tint = EcoColors.TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EcoColors.BackgroundDark,
                        titleContentColor = EcoColors.TextPrimary,
                        navigationIconContentColor = EcoColors.TextPrimary
                    )
                )
                
                // Filtros
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            modifier = Modifier.clickable { selectedFilter = filter },
                            color = if (isSelected) EcoColors.GlassAccent else EcoColors.CardPrimary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, EcoColors.TextSecondary.copy(alpha = 0.2f))
                        ) {
                            Text(
                                filter,
                                color = if (isSelected) Color.White else EcoColors.TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = EcoColors.BackgroundDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // El mapa se renderiza por expect/actual según plataforma
            PlatformMapView(
                modifier = Modifier.fillMaxSize(), 
                filter = selectedFilter,
                recenterTrigger = recenterTrigger,
                onPointSelected = { point ->
                    selectedPoint = point
                    showSheet = true
                }
            )
            
            // Botón flotante para centrar (Novedad)
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = { recenterTrigger++ },
                    containerColor = EcoColors.GlassAccent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 80.dp) // Encima del BottomSheet
                ) {
                    Icon(Icons.Default.Map, "Centrar")
                }
            }

            if (showSheet && selectedPoint != null) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    containerColor = EcoColors.CardBackground,
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
                            fontSize = 20.sp,
                            color = EcoColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = when(selectedPoint?.kind) {
                                "FIJO" -> Color(0xFF10B981)
                                "MOVIL" -> Color(0xFFF59E0B)
                                else -> Color(0xFF3B82F6)
                            }.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                selectedPoint?.kind ?: "GENERAL",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when(selectedPoint?.kind) {
                                    "FIJO" -> Color(0xFF10B981)
                                    "MOVIL" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF3B82F6)
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📍", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                selectedPoint?.snippet ?: "Sin dirección detallada",
                                color = EcoColors.TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { /* Navegar */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = EcoColors.GlassAccent),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Cómo llegar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable multiplatform esperado para el mapa.
 * - Android: implementado en androidMain con Google Maps Compose
 * - iOS: implementado en iosMain con Apple MapKit o placeholder
 */
@Composable
expect fun PlatformMapView(
    modifier: Modifier, 
    filter: String,
    recenterTrigger: Int,
    onPointSelected: (com.rubensimon.ecolens.data.models.maps.RecyclingPoint) -> Unit
)
