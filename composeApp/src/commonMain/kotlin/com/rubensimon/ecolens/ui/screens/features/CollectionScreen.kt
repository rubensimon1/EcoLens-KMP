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
    
    var unlockedSet by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedObject by remember { mutableStateOf<EcoObject?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        var userScans = emptyList<String>()
        if (userId.isNotEmpty()) {
            val history = userRepo.getUserHistory(userId)
            if (history.isNotEmpty()) {
                userScans = history.map { it.object_name }
            }
        }
        
        val categoryCounts = mutableMapOf<String, Int>()
        for (scan in userScans) {
            val cat = getBaseCategoryForScan(scan)
            if (cat != null) {
                categoryCounts[cat] = categoryCounts.getOrElse(cat) { 0 } + 1
            }
        }
        
        val newUnlockedSet = mutableSetOf<Int>()
        val currentCounts = mutableMapOf<String, Int>()
        
        for (obj in ecoObjects) {
            val cat = obj.key
            val currentRank = currentCounts.getOrElse(cat) { 0 } + 1
            if (currentRank <= categoryCounts.getOrElse(cat) { 0 }) {
                newUnlockedSet.add(obj.id)
                currentCounts[cat] = currentRank
            }
        }
        
        unlockedSet = newUnlockedSet
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
                val unlockedCount = unlockedSet.size
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
                    modifier = Modifier.widthIn(max = 800.dp)
                ) {
                    items(ecoObjects) { obj ->
                        val isUnlocked = unlockedSet.contains(obj.id)
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
    }

    if (showSheet && selectedObject != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = EcoColors.CardBackground.copy(alpha = 0.95f), // Casi opaco para mejor visibilidad
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
            modifier = Modifier.size(100.dp).background(EcoColors.CardPrimary.copy(alpha = 0.8f), CircleShape),
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
        
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = EcoColors.CardBackground.copy(alpha = 0.8f)
        ) {
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
            .aspectRatio(0.9f)
            .clickable(enabled = isUnlocked, onClick = onClick),
        backgroundColor = if (isUnlocked) containerColor.copy(alpha = 0.15f) else EcoColors.CardPrimary.copy(alpha = 0.5f),
        cornerRadius = 24
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
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
                    Text(emoji, fontSize = 36.sp)
                } else {
                    Icon(
                        Icons.Default.Lock, 
                        null, 
                        tint = EcoColors.TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = name,
                color = if (isUnlocked) EcoColors.TextPrimary else EcoColors.TextSecondary.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 13.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
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
    val id: Int,
    val key: String, 
    val emoji: String, 
    val name: String, 
    val container: String,
    val fact: String = "",
    val decompositionTime: String = ""
)

private val baseObjects = listOf(
    EcoObject(0, "Envases", "🥤", "Envases", "Amarillo", "Los envases de plástico, latas y briks van al contenedor amarillo.", "10-500 años"),
    EcoObject(0, "Papel y Cartón", "📦", "Papel y Cartón", "Azul", "Reciclar papel ahorra agua y salva árboles.", "2-12 meses"),
    EcoObject(0, "Vidrio", "🍾", "Vidrio", "Verde", "El vidrio se puede reciclar infinitas veces sin perder sus propiedades.", "4000 años"),
    EcoObject(0, "Orgánico", "🍎", "Orgánico", "Marrón", "Los restos de comida se usan para hacer compost y abonar la tierra.", "1-6 meses")
)

private val ecoObjects = buildList {
    var idCounter = 1
    for (i in 1..13) {
        baseObjects.forEach { base ->
            add(base.copy(id = idCounter++, name = "${base.name} #$i"))
        }
    }
}

private fun getBaseCategoryForScan(scan: String): String? {
    val s = scan.lowercase().trim()
    if (s.isEmpty()) return null
    if (s.contains("papel") || s.contains("cartón") || s.contains("carton")) return "Papel y Cartón"
    if (s.contains("vidrio") || s.contains("cristal")) return "Vidrio"
    if (s.contains("envase") || s.contains("plástico") || s.contains("lata") || s.contains("botella") || s.contains("plastico")) return "Envases"
    if (s.contains("orgánico") || s.contains("organico") || s.contains("comida") || s.contains("fruta") || s.contains("manzana") || s.contains("plátano")) return "Orgánico"
    
    // Mapeos exactos por si acaso
    when (s) {
        "envases" -> return "Envases"
        "vidrio" -> return "Vidrio"
        "orgánico", "organico" -> return "Orgánico"
    }
    return null
}
