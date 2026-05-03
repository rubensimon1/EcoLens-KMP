package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.ui.components.*
import com.russhwolf.settings.Settings
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Colección de objetos únicos escaneados — migrada de CollectionActivity.
 *
 * EcoDex: galería de todos los objetos descubiertos mediante escaneos.
 * Los objetos desbloqueados se guardan en Settings (key: "collection_unlocked").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(onBackClick: () -> Unit) {
    val userRepo = remember { com.rubensimon.ecolens.data.repository.UserRepository() }
    val userId = remember { com.rubensimon.ecolens.utils.PointsManager.getUserId() }
    
    val settings = remember { Settings() }
    val unlockedRaw = remember { settings.getString("collection_unlocked", "") }
    var unlockedSet by remember {
        mutableStateOf(
            if (unlockedRaw.isBlank()) emptySet<String>()
            else unlockedRaw.split(",").toSet()
        )
    }
    
    var isLoading by remember { mutableStateOf(true) }
    var selectedObject by remember { mutableStateOf<EcoObject?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    // Sincronizar con Supabase al entrar
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val history = userRepo.getUserHistory(userId)
            if (history.isNotEmpty()) {
                val remoteUnlocked = history.map { it.object_name }.toSet()
                // Combinar local con remoto
                val finalSet = unlockedSet + remoteUnlocked
                unlockedSet = finalSet
                // Guardar localmente para rapidez la próxima vez
                settings.putString("collection_unlocked", finalSet.joinToString(","))
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📗 Eco-Dex",
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
        },
        containerColor = EcoColors.BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    color = EcoColors.GlassAccent
                )
            } else {
                // Progreso
                val unlockedCount = ecoObjects.count { it.name in unlockedSet }
                val progress = if (ecoObjects.isEmpty()) 0f else unlockedCount.toFloat() / ecoObjects.size
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Objetos descubiertos:",
                            color = EcoColors.TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            "$unlockedCount / ${ecoObjects.size}",
                            color = EcoColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = EcoColors.GlassAccent,
                        trackColor = EcoColors.CardPrimary,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
            ) {
                items(ecoObjects) { obj ->
                    val isUnlocked = obj.key in unlockedSet || obj.name in unlockedSet
                    CollectionCell(
                        emoji = obj.emoji,
                        name = if (isUnlocked) obj.name else "???",
                        container = if (isUnlocked) obj.container else "",
                        isUnlocked = isUnlocked,
                        onClick = {
                            if (isUnlocked) {
                                selectedObject = obj
                                showSheet = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showSheet && selectedObject != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = EcoColors.CardBackground,
            dragHandle = { BottomSheetDefaults.DragHandle(color = EcoColors.TextSecondary.copy(alpha = 0.5f)) }
        ) {
            ObjectDetailContent(selectedObject!!)
        }
    }
}

@Composable
private fun ObjectDetailContent(obj: EcoObject) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(100.dp).background(EcoColors.CardPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(obj.emoji, fontSize = 60.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(obj.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary)
        
        Surface(
            color = EcoColors.GlassAccent.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                "Contenedor ${obj.container}", 
                color = EcoColors.GlassAccent, 
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailItem("Degradación", obj.decompositionTime, "⏳")
            DetailItem("Impacto", "Alto", "🌍")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sabías que...", fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(obj.fact, color = EcoColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Text(label, color = EcoColors.TextSecondary, fontSize = 12.sp)
        Text(value, color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun CollectionCell(
    emoji: String,
    name: String,
    container: String,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when (container.lowercase()) {
        "amarillo" -> Color(0xFFFFD60A) // Amarillo vibrante
        "azul" -> Color(0xFF0A84FF)     // Azul Apple
        "verde" -> Color(0xFF30D158)    // Verde Apple
        "marrón", "organico", "orgánico" -> Color(0xFFAC8E68) // Marrón suave
        "raee", "especial" -> Color(0xFFFF453A) // Rojo/Especial
        else -> Color(0xFF8E8E93)       // Gris
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(enabled = isUnlocked, onClick = onClick),
        backgroundColor = if (isUnlocked) containerColor.copy(alpha = 0.15f) else EcoColors.CardPrimary.copy(alpha = 0.5f),
        cornerRadius = 28
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) {
                            Brush.radialGradient(
                                colors = listOf(containerColor.copy(alpha = 0.4f), Color.Transparent)
                            )
                        } else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
            ) {
                if (isUnlocked) {
                    Text(emoji, fontSize = 40.sp)
                } else {
                    Icon(
                        Icons.Default.Lock, 
                        null, 
                        tint = EcoColors.TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = name,
                color = if (isUnlocked) EcoColors.TextPrimary else EcoColors.TextSecondary.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            if (isUnlocked) {
                Surface(
                    color = containerColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        container.uppercase(),
                        color = containerColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// Catálogo completo de objetos reciclables
private data class EcoObject(
    val key: String, 
    val emoji: String, 
    val name: String, 
    val container: String,
    val fact: String = "",
    val decompositionTime: String = ""
)

private val ecoObjects = listOf(
    EcoObject("Bottle", "🥤", "Botella Plástico", "Amarillo", "Tarda 450 años en degradarse. El PET es 100% reciclable.", "450 años"),
    EcoObject("Can", "🥫", "Lata / Metal", "Amarillo", "Reciclar aluminio ahorra un 95% de energía comparado con fabricarlo nuevo.", "10-100 años"),
    EcoObject("Paper", "📰", "Papel / Cartón", "Azul", "Por cada tonelada de papel reciclado se salvan 17 árboles.", "2-5 meses"),
    EcoObject("GlassBottle", "🍶", "Vidrio", "Verde", "El vidrio nunca pierde sus propiedades y puede reciclarse infinitamente.", "1 M de años"),
    EcoObject("Plastic", "🛍️", "Plástico", "Amarillo", "Muchos plásticos terminan en los océanos. El reciclaje es vital para la fauna.", "500 años"),
    EcoObject("Electronics", "📱", "Electrónica", "RAEE", "Contienen materiales valiosos como oro y plata que pueden ser recuperados.", "Indefinido"),
    EcoObject("Battery", "🔋", "Pila", "Especial", "Una sola pila de mercurio puede contaminar 600.000 litros de agua.", "500-1000 años"),
    EcoObject("Food", "🍎", "Orgánico", "Marrón", "Los restos orgánicos pueden convertirse en compost para nutrir la tierra.", "Semanas"),
    EcoObject("TetraBrik", "🥛", "Tetra Brik", "Amarillo", "Compuestos por cartón, polietileno y aluminio. Separarlos es un gran logro.", "30 años"),
)
