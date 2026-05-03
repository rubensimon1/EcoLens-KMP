package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.ui.components.*

/**
 * Pantalla de ideas de upcycling — migrada de UpcyclingActivity.
 * Contenido estático con ideas de reutilización DIY.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcyclingScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(EcoColors.BackgroundDark).statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Text(
                            "💡 Upcycling",
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
            }
        },
        containerColor = EcoColors.BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ideas para reutilizar tus objetos",
                color = EcoColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            upcyclingIdeas.forEach { idea ->
                UpcyclingCard(idea)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun UpcyclingCard(idea: UpcyclingIdea) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(idea.emoji, fontSize = 32.sp, modifier = Modifier.padding(end = 12.dp, top = 2.dp))
            Column {
                Text(idea.title, color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(idea.material, color = EcoColors.GlassAccent, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(idea.description, color = EcoColors.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "⭐ ${idea.difficulty} · ${idea.timeEstimate}",
                    color = EcoColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private data class UpcyclingIdea(
    val emoji: String,
    val title: String,
    val material: String,
    val description: String,
    val difficulty: String,
    val timeEstimate: String
)

private val upcyclingIdeas = listOf(
    UpcyclingIdea(
        "🥤", "Macetero de botella PET",
        "Botella de plástico",
        "Corta la botella por la mitad, añade agujeros de drenaje, decora con pintura y planta tus semillas favoritas.",
        "Fácil", "30 min"
    ),
    UpcyclingIdea(
        "🥫", "Portalápices de lata",
        "Latas de conserva",
        "Lija los bordes, pinta la lata con colores vibrantes y pégale tela o cuerda para un organizador de escritorio único.",
        "Fácil", "20 min"
    ),
    UpcyclingIdea(
        "📦", "Organizador de cartón",
        "Cajas de cartón",
        "Recorta y dobla cajas para crear divisores personalizados para cajones o estantes.",
        "Muy fácil", "15 min"
    ),
    UpcyclingIdea(
        "🍶", "Lámpara de botella de vidrio",
        "Botella de vidrio",
        "Introduce una tira LED dentro de la botella y en la boca crea un tapón con el cable. Luz ambiental sostenible.",
        "Medio", "45 min"
    ),
    UpcyclingIdea(
        "📰", "Papel maché artístico",
        "Periódicos y revistas",
        "Mezcla agua y cola blanca, sumerge tiras de periódico y crea esculturas, cuencos o marcos decorativos.",
        "Medio", "2h + secado"
    ),
    UpcyclingIdea(
        "🛍️", "Monedero de bolsas plásticas",
        "Bolsas de plástico",
        "Plancha varias bolsas entre papel de horno para fusionarlas en una lámina resistente y cose tu propio monedero.",
        "Avanzado", "1h"
    ),
    UpcyclingIdea(
        "🥛", "Comedero para pájaros",
        "Tetra Brik",
        "Haz ventanas laterales al tetra brik, añade una varilla de madera como palo y cuelga el comedero en el jardín.",
        "Fácil", "20 min"
    ),
    UpcyclingIdea(
        "🔋", "Banco de energía casero",
        "Pilas 18650",
        "Con pilas de portátil recicladas (testea voltaje primero), módulos BMS y una carcasa impresa en 3D.",
        "Experto", "4h"
    ),
)
