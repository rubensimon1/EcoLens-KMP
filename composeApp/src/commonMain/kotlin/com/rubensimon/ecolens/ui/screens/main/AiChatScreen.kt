package com.rubensimon.ecolens.ui.screens.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.ui.components.EcoColors
import com.rubensimon.ecolens.ui.components.GlassCard
import com.rubensimon.ecolens.ui.components.GlassBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(onBackClick: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { 
        mutableStateListOf(
            ChatMessage("¡Hola! Soy tu asistente EcoLens. ¿En qué puedo ayudarte hoy? Puedes preguntarme sobre reciclaje, ideas de upcycling o cómo mejorar tu impacto ambiental.", false)
        ) 
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isTyping by remember { mutableStateOf(false) }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, null, tint = EcoColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(40.dp).background(EcoColors.GlassAccent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = EcoColors.GlassAccent, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Eco-Asistente", color = EcoColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(if (isTyping) "Escribiendo..." else "En línea", color = if (isTyping) EcoColors.GlassAccent else EcoColors.TextSecondary, fontSize = 12.sp)
                }
            }

            // Chat Messages
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { message ->
                        ChatBubble(message)
                    }
                    if (isTyping) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }

            // Input area
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = EcoColors.CardPrimary.copy(alpha = 0.6f),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Escribe un mensaje...", color = EcoColors.TextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = EcoColors.GlassAccent
                        ),
                        textStyle = LocalTextStyle.current.copy(color = EcoColors.TextPrimary)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val userMsg = messageText
                                messages.add(ChatMessage(userMsg, true))
                                messageText = ""
                                scope.launch {
                                    listState.animateScrollToItem(messages.size - 1)
                                    delay(500)
                                    isTyping = true
                                    delay(2000)
                                    isTyping = false
                                    messages.add(ChatMessage(generateAiResponse(userMsg), false))
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        containerColor = EcoColors.GlassAccent,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Send, null)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) EcoColors.GlassAccent else EcoColors.CardPrimary.copy(alpha = 0.8f)
    val textColor = if (message.isUser) Color.White else EcoColors.TextPrimary
    val shape = if (message.isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start) {
            Surface(
                color = bgColor,
                shape = shape,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition()
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape).background(EcoColors.TextSecondary.copy(alpha = alpha))
            )
        }
    }
}

private fun generateAiResponse(query: String): String {
    val q = query.lowercase()
    return when {
        q.contains("hola") || q.contains("buenas") -> 
            "¡Hola! Soy tu asistente EcoLens. Estoy aquí para ayudarte a reciclar mejor. ¿Tienes dudas sobre dónde tirar algo o buscas ideas para reutilizar objetos?"
        
        q.contains("plastico") || q.contains("envase") || q.contains("lata") || q.contains("brik") -> 
            "¡Al contenedor AMARILLO! 🟡 Ahí van los envases de plástico, latas y briks. Recuerda enjuagarlos un poco si tienen muchos restos de comida."
        
        q.contains("papel") || q.contains("carton") || q.contains("caja") -> 
            "¡Al contenedor AZUL! 🔵 Deposita aquí cajas de cartón, folios y revistas. ¡Ojo! Si el cartón tiene mucha grasa (como una caja de pizza usada), mejor al gris/orgánico."
        
        q.contains("vidrio") || q.contains("botella") || q.contains("frasco") -> 
            "¡Al contenedor VERDE! 🟢 Las botellas y frascos de vidrio van aquí. Pero cuidado: los espejos o vasos de cristal normales van al punto limpio o gris, ya que tienen componentes distintos."
        
        q.contains("pila") || q.contains("bateria") || q.contains("electronico") || q.contains("movil") -> 
            "⚠️ ¡PUNTO LIMPIO! Estos objetos son altamente contaminantes. Busca el contenedor de pilas más cercano (suele haber en supermercados) o llévalo a un punto limpio móvil."
        
        q.contains("comida") || q.contains("organico") || q.contains("fruta") -> 
            "¡Al contenedor MARRÓN o GRIS! 🟤 Restos de comida, pieles de fruta y posos de café. Si tienes jardín, ¡es el material perfecto para hacer compost!"
        
        q.contains("donde") || q.contains("tiro") || q.contains("meto") -> 
            "Depende del material. Por lo general: Amarillo (envases), Azul (papel), Verde (vidrio), Marrón (orgánico). ¿De qué objeto se trata exactamente?"
            
        q.contains("idea") || q.contains("reutilizar") || q.contains("upcycling") || q.contains("hacer con") -> 
            "¡Me encanta esa iniciativa! Aquí tienes unas ideas rápidas:\n- Con botellas de plástico: Haz un sistema de autorregado para plantas.\n- Con cajas de cartón: Organizadores de cajones o casas para mascotas.\n- Con tarros de cristal: Botes para legumbres o portavelas.\n¿Qué materiales tienes a mano?"

        else -> 
            "Esa es una buena pregunta. Como regla general, si es un envase va al amarillo, y si es papel al azul. Si me dices de qué material está hecho el objeto, te daré la instrucción exacta. ¡Sigue así, Eco-Guerrero! 🌍"
    }
}
