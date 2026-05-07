package com.rubensimon.ecolens.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ecolens.composeapp.generated.resources.Res
import ecolens.composeapp.generated.resources.logo_ecolens
import org.jetbrains.compose.resources.painterResource

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val oledBlack = Color(0xFF000000) // Negro OLED puro
    val vividTurquoise = Color(0xFF1DE9B6) // Turquesa verdoso claro y vivo

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(vividTurquoise)
    ) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // TOP TURQUOISE SECTION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(minHeight * 0.45f),
                contentAlignment = Alignment.Center
            ) {
                // EcoLens Logo
                Surface(
                    modifier = Modifier.size(140.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(36.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(20.dp)) {
                        Image(
                            painter = painterResource(Res.drawable.logo_ecolens),
                            contentDescription = "EcoLens Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // BOTTOM WHITE SECTION
            val waveHeightPx = with(LocalDensity.current) { 60.dp.toPx() }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                color = Color.White,
                shape = GenericShape { size, _ ->
                    moveTo(0f, waveHeightPx * 0.5f)
                    cubicTo(
                        size.width * 0.3f, 0f,
                        size.width * 0.7f, waveHeightPx,
                        size.width, waveHeightPx * 0.5f
                    )
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, end = 32.dp, top = 64.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Welcome",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = oledBlack // Letra oscura y negra OLED
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Escanea. Recicla. Salva el planeta.",
                            color = Color.Gray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        )
                    }

                    // Continue Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        IconButton(
                            onClick = onStartClick,
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(8.dp, CircleShape, spotColor = vividTurquoise)
                                .background(vividTurquoise, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Continue",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
