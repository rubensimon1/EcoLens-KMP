package com.rubensimon.ecolens.ui.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.ui.components.EcoColors
import com.rubensimon.ecolens.ui.components.GlassButton
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val settings = Settings()

    val pages = listOf(
        OnboardingPage(
            "Escanea y Aprende",
            "Usa nuestra IA para identificar objetos reciclables y descubre cómo impactan al planeta.",
            "📷"
        ),
        OnboardingPage(
            "Gana Recompensas",
            "Cada objeto cuenta. Acumula puntos por tus buenas acciones y canjéalos por premios reales.",
            "🎁"
        ),
        OnboardingPage(
            "Únete a la Comunidad",
            "Compite en el ranking global y conviértete en un Guardián del planeta.",
            "🌍"
        )
    )

    val darkGreenBg = Color(0xFF134533)
    val vividTurquoise = Color(0xFF1DE9B6)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkGreenBg)
            // .navigationBarsPadding() // Lo quitamos del contenedor principal para que el fondo llegue abajo
    ) {
        // Fondo Topográfico Neumórfico
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0..7) {
                val progress = i / 7f
                val startY = size.height * (1.1f - progress * 0.9f)
                val endY = size.height * (1.0f - progress * 0.8f)

                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, 0f)
                    lineTo(0f, startY)
                    cubicTo(
                        size.width * 0.3f, startY + size.height * 0.1f,
                        size.width * 0.7f, endY - size.height * 0.1f,
                        size.width, endY
                    )
                    lineTo(size.width, 0f)
                    close()
                }

                // Sombra Neumórfica
                for (s in 1..6) {
                    val offset = s * 3f
                    translate(left = offset, top = offset * 1.5f) {
                        drawPath(path, color = Color.Black.copy(alpha = 0.08f / s))
                    }
                }

                // Brillo Neumórfico
                for (s in 1..3) {
                    val offset = s * 2f
                    translate(left = -offset, top = -offset) {
                        drawPath(path, color = Color.White.copy(alpha = 0.04f / s))
                    }
                }

                drawPath(path, color = darkGreenBg)
            }
        }

        // Contenido del Onboarding
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(page.emoji, fontSize = 100.sp)
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        page.title,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        page.description,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            // Indicadores y Botones
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier.height(50.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) vividTurquoise else Color.White.copy(alpha = 0.2f)
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(if (pagerState.currentPage == iteration) 12.dp else 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            settings.putBoolean("onboarding_completed", true)
                            onFinish()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = vividTurquoise),
                    shape = CircleShape
                ) {
                    Text(
                        if (pagerState.currentPage == pages.size - 1) "¡Empezar!" else "Siguiente",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

private data class OnboardingPage(val title: String, val description: String, val emoji: String)
