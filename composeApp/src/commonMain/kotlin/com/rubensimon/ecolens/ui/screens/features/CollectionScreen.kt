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
                val unlockedCount = ecoObjects.count { checkIfUnlocked(it, unlockedSet) }
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
                        val isUnlocked = checkIfUnlocked(obj, unlockedSet)
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
    val key: String, 
    val emoji: String, 
    val name: String, 
    val container: String,
    val fact: String = "",
    val decompositionTime: String = ""
)

private val ecoObjects = listOf(
    // --- CONTENEDOR AMARILLO (Envases, Plásticos y Metales) ---
    EcoObject("Bottle", "🥤", "Botella de Plástico", "Amarillo", "El PET es 100% reciclable. Se usa para fabricar forros polares.", "450 años"),
    EcoObject("Can", "🥫", "Lata de Conservas", "Amarillo", "El acero y el aluminio son infinitamente reciclables.", "10-100 años"),
    EcoObject("SodaCan", "🥤", "Lata de Refresco", "Amarillo", "Reciclar una lata ahorra energía para tener una TV encendida 3h.", "10 años"),
    EcoObject("TetraBrik", "🥛", "Brick de Leche", "Amarillo", "Están formados por cartón, plástico y aluminio.", "30 años"),
    EcoObject("Shampoo", "🧴", "Bote de Champú", "Amarillo", "Asegúrate de vaciarlos completamente antes de reciclarlos.", "450 años"),
    EcoObject("SnackBag", "🍿", "Bolsa de Snacks", "Amarillo", "Muchos envoltorios plateados también van al contenedor amarillo.", "100 años"),
    EcoObject("Aluminum", "🌯", "Papel de Aluminio", "Amarillo", "Si está muy sucio de comida, mejor al gris, si no, al amarillo.", "400 años"),
    EcoObject("Yogurt", "🍧", "Tarrina de Yogur", "Amarillo", "No es necesario lavarlos, solo vaciarlos bien.", "400 años"),
    EcoObject("Styrofoam", "🍱", "Bandeja de Corcho", "Amarillo", "El poliestireno expandido es reciclable pero ocupa mucho volumen.", "500 años"),
    EcoObject("PlasticBag", "🛍️", "Bolsa de Plástico", "Amarillo", "Las bolsas de plástico tardan siglos en degradarse en el mar.", "150 años"),
    EcoObject("BottleCap", "🔵", "Tapón de Plástico", "Amarillo", "Existen campañas de recogida de tapones con fines solidarios.", "300 años"),
    EcoObject("Spray", "💨", "Aerosol / Spray", "Amarillo", "Los botes de laca o desodorante van aquí siempre que estén vacíos.", "30 años"),
    EcoObject("BeerCap", "🍺", "Chapa de Metal", "Amarillo", "Cualquier pequeña chapa o tapa metálica va al amarillo.", "100 años"),
    EcoObject("PlasticFilm", "🎞️", "Film Transparente", "Amarillo", "El film de cocina es polietileno y es totalmente reciclable.", "100 años"),
    EcoObject("CleanerBottle", "🧼", "Bote Detergente", "Amarillo", "Envases de limpieza del hogar suelen ser HDPE, muy valorado.", "450 años"),

    // --- CONTENEDOR AZUL (Papel y Cartón) ---
    EcoObject("Paper", "📰", "Periódico / Revista", "Azul", "Reciclar papel ahorra un 70% de agua comparado con usar madera.", "2-5 meses"),
    EcoObject("Box", "📦", "Caja de Cartón", "Azul", "Desmonta las cajas para que ocupen menos espacio en el contenedor.", "1 año"),
    EcoObject("EggCarton", "🥚", "Huevera de Cartón", "Azul", "El cartón puede reciclarse hasta 7 veces.", "3-5 meses"),
    EcoObject("Mail", "✉️", "Sobres y Cartas", "Azul", "Recuerda quitar las ventanillas de plástico de los sobres.", "2 meses"),
    EcoObject("PizzaBox", "🍕", "Caja de Pizza", "Azul", "Solo si está limpia. Si tiene mucha grasa, debe ir al contenedor gris.", "4 meses"),
    EcoObject("Book", "📚", "Libros Viejos", "Azul", "Si están en buen estado, ¡mejor dónalos! Si no, al contenedor azul.", "1 año"),
    EcoObject("FlourBag", "🍞", "Bolsa de Harina", "Azul", "Las bolsas de papel de harina o azúcar son reciclables aquí.", "2 meses"),
    EcoObject("Notebook", "📓", "Cuaderno", "Azul", "Los cuadernos tienen papel reciclable, pero recuerda quitar la espiral metálica.", "1 año"),

    // --- CONTENEDOR VERDE (Vidrio) ---
    EcoObject("GlassBottle", "🍾", "Botella de Vidrio", "Verde", "El vidrio nunca pierde sus propiedades al reciclarse.", "4000 años"),
    EcoObject("JamJar", "🍯", "Tarro de Mermelada", "Verde", "Quita las tapas (van al amarillo) antes de tirar el tarro al verde.", "4000 años"),
    EcoObject("Perfume", "💎", "Frasco de Perfume", "Verde", "El cristal de los espejos NO va aquí, solo vidrio de envase.", "4000 años"),
    EcoObject("WineBottle", "🍷", "Botella de Vino", "Verde", "Reciclar 3 botellas de vidrio ahorra energía para lavar toda la ropa de un día.", "4000 años"),

    // --- CONTENEDOR MARRÓN (Orgánico) ---
    EcoObject("Food", "🍎", "Restos de Comida", "Orgánico", "Con ellos se fabrica compost para agricultura y jardinería.", "1-6 meses"),
    EcoObject("Coffee", "☕", "Posos de Café", "Orgánico", "Son excelentes fertilizantes naturales.", "1 mes"),
    EcoObject("Cork", "🍷", "Tapón de Corcho", "Orgánico", "El corcho natural es biodegradable y compostable.", "50 años"),
    EcoObject("TeaBag", "🍵", "Bolsa de Té", "Orgánico", "La mayoría son biodegradables, pero comprueba que no tengan grapas.", "2 meses"),
    EcoObject("Napkin", "🧻", "Servilleta Sucia", "Orgánico", "Si tiene restos de comida, va al orgánico. Si está limpia, al azul.", "1 mes"),
    EcoObject("Banana", "🍌", "Plátano", "Orgánico", "La piel de plátano es excelente para hacer compost de calidad.", "2-10 días"),

    // --- ESPECIALES / PUNTO LIMPIO / OTROS ---
    EcoObject("Battery", "🔋", "Pilas y Baterías", "Especial", "Altamente contaminantes. Una sola pila de botón contamina 600k L de agua.", "500 años"),
    EcoObject("Electronics", "📱", "Móvil / Tablet", "RAEE", "Contienen minerales raros como el coltán que deben recuperarse.", "Indefinido"),
    EcoObject("Oil", "🛢️", "Aceite Usado", "Especial", "Nunca lo tires por el fregadero. Con él se fabrica biodiésel.", "Indefinido"),
    EcoObject("LightBulb", "💡", "Bombilla / LED", "Especial", "Las bombillas viejas tienen mercurio y gases pesados.", "Indefinido"),
    EcoObject("Clothes", "👕", "Ropa Usada", "Especial", "La industria textil es de las más contaminantes del mundo.", "40-200 años"),
    EcoObject("Medicine", "💊", "Medicamentos", "SIGRE", "Lleva los envases y restos a la farmacia (Punto SIGRE).", "Indefinido"),
    EcoObject("Capsule", "☕", "Cápsula de Café", "Especial", "Muchas marcas tienen puntos de recogida especiales para aluminio/plástico.", "200 años"),
    EcoObject("XRay", "🩻", "Radiografía", "Punto Limpio", "Contienen sales de plata que son muy valiosas y contaminantes.", "Indefinido"),
    EcoObject("Paint", "🎨", "Bote de Pintura", "Punto Limpio", "Los restos químicos deben tratarse como residuos peligrosos.", "Indefinido"),
    EcoObject("Toaster", "🍞", "Tostadora / Batidora", "RAEE", "Cualquier aparato con cable o pilas debe ir al punto limpio.", "Indefinido"),
    EcoObject("CD", "💿", "CD / DVD", "Punto Limpio", "Están hechos de policarbonato, un plástico muy difícil de degradar.", "Indefinido"),
    EcoObject("Toy", "🧸", "Juguete Roto", "Punto Limpio", "Si tienen electrónica, al RAEE. Si son solo plástico duro, al Punto Limpio.", "500 años"),
    EcoObject("Thermometer", "🌡️", "Termómetro", "Especial", "Los antiguos de mercurio son extremadamente peligrosos si se rompen.", "Indefinido"),
    EcoObject("Fluorescent", "🔦", "Fluorescente", "Especial", "Contienen vapor de mercurio y deben reciclarse con cuidado.", "Indefinido"),
    EcoObject("Pen", "🖊️", "Bolígrafo/Lápiz", "Punto Limpio", "Los bolígrafos están hechos de múltiples plásticos y metales difíciles de separar.", "100 años"),
)

private fun checkIfUnlocked(obj: EcoObject, unlockedSet: Set<String>): Boolean {
    val unlockedSetLower = unlockedSet.map { it.lowercase().trim() }.toSet()
    val objNameLower = obj.name.lowercase().trim()
    val objKeyLower = obj.key.lowercase().trim()
    
    if (objKeyLower in unlockedSetLower || objNameLower in unlockedSetLower) return true
    
    // Mapeo de categorías generales y etiquetas específicas a claves únicas en la colección
    for (u in unlockedSetLower) {
        if (u.isNotEmpty() && (objNameLower.contains(u) || u.contains(objNameLower) || objKeyLower.contains(u) || u.contains(objKeyLower))) {
            return true
        }
        
        when {
            // Papel y Cartón
            u == "papel y cartón" || u == "cartón/papel" || u == "carton" || u == "papel" -> {
                if (obj.key == "Box" || obj.key == "Paper" || obj.key == "Notebook") return true
            }
            // Vidrio
            u == "vidrio" || u == "cristal" -> {
                if (obj.key == "GlassBottle") return true
            }
            // Envases
            u == "envases" || u == "envase plástico" || u == "envase" -> {
                if (obj.key == "Bottle") return true
            }
            // Orgánico
            u == "orgánico" || u == "organico" -> {
                if (obj.key == "Food" || obj.key == "Banana") return true
            }
            // Latas
            u == "lata" || u.contains("lata") -> {
                if (obj.key == "SodaCan" || obj.key == "Can") return true
            }
            // Bolsa de plástico
            u == "bolsa de plástico" || u == "bolsa" || u.contains("bolsa") -> {
                if (obj.key == "PlasticBag" || obj.key == "SnackBag") return true
            }
            // Tapón/Tapa
            u == "tapón/tapa" || u.contains("tapón") || u.contains("tapa") -> {
                if (obj.key == "BottleCap") return true
            }
            // Dispositivos y Electrónica
            u == "dispositivo electrónico" || u == "teléfono móvil" || u == "móvil / tablet" || u.contains("electrónico") || u.contains("móvil") || u.contains("movil") || u.contains("ordenador") || u.contains("pantalla") -> {
                if (obj.key == "Electronics") return true
            }
            // Cables y cargadores
            u == "cable/cableado" || u == "cargador/adaptador" || u.contains("cable") || u.contains("cargador") -> {
                if (obj.key == "Toaster") return true
            }
            // Pilas
            u == "pila/batería" || u.contains("pila") || u.contains("batería") || u.contains("bateria") -> {
                if (obj.key == "Battery") return true
            }
            // Bombillas e Iluminación
            u == "lámpara/iluminación" || u.contains("bombilla") || u.contains("iluminación") || u.contains("iluminacion") -> {
                if (obj.key == "LightBulb") return true
            }
            // Ropa y calzado
            u == "ropa/vestimenta" || u == "zapato/calzado" || u.contains("ropa") || u.contains("calzado") || u.contains("mochila") || u.contains("bolso") -> {
                if (obj.key == "Clothes") return true
            }
            // Juguetes
            u == "juguete" || u.contains("juguete") -> {
                if (obj.key == "Toy") return true
            }
            // Bolígrafos
            u == "bolígrafo/lápiz" || u.contains("bolígrafo") || u.contains("lápiz") || u.contains("tijeras") || u.contains("herramienta") -> {
                if (obj.key == "Pen") return true
            }
        }
    }
    return false
}
