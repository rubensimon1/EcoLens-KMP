package com.rubensimon.ecolens.ui.screens.auth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoColors.BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
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
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    page.description,
                    fontSize = 16.sp,
                    color = EcoColors.TextSecondary,
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
                    val color = if (pagerState.currentPage == iteration) EcoColors.GlassAccent else EcoColors.CardPrimary
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

            GlassButton(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        settings.putBoolean("onboarding_completed", true)
                        onFinish()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (pagerState.currentPage == pages.size - 1) "¡Empezar!" else "Siguiente")
            }
        }
    }
}

private data class OnboardingPage(val title: String, val description: String, val emoji: String)
