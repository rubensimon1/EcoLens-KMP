package com.rubensimon.ecolens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import com.rubensimon.ecolens.utils.getTimeAgo

/**
 * Sistema de diseño Glassmorphism de EcoLens — migrado a Compose Multiplatform.
 *
 * Totalmente compatible con commonMain. No tiene dependencias Android específicas.
 * Migrado desde GlassComponents.kt del proyecto Android original.
 */

// ── Paletas de color ────────────────────────────────────────────────────────

// ── Paletas de color ────────────────────────────────────────────────────────

object EcoColorsDark {
    val GlassGreen = Color(0x3376D7C4) 
    val GlassDarkGreen = Color(0x662ECC71)
    val GlassAccent = Color(0xFF2ECC71) // Vibrant Emerald
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFAAAAAA)
    val BackgroundDark = Color(0xFF001A1A) // Deep Teal
    val CardBackground = Color(0xFF003333).copy(alpha = 0.4f) // Translucent elliptical background
    val CardPrimary = Color(0xFF004D4D).copy(alpha = 0.5f)
    val Success = Color(0xFF76D7C4)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFEF4444)
}

object EcoColorsLight {
    val GlassGreen = Color(0x332ECC71) 
    val GlassDarkGreen = Color(0xFF2ECC71) 
    val GlassAccent = Color(0xFF008080) // Deep Teal for contrast
    val TextPrimary = Color(0xFF003333) // Dark Teal
    val TextSecondary = Color(0xFF557B7B) 
    val BackgroundDark = Color(0xFFE0FFF0) // Mint Pastel
    val CardBackground = Color.White.copy(alpha = 0.6f)
    val CardPrimary = Color.White.copy(alpha = 0.8f)
    val Success = Color(0xFF2ECC71)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
}

/** Holder dinámico del tema actual. Se actualiza con [EcoColors.updateTheme]. */
object EcoColors {
    var GlassGreen by mutableStateOf(EcoColorsDark.GlassGreen)
    var GlassDarkGreen by mutableStateOf(EcoColorsDark.GlassDarkGreen)
    var GlassAccent by mutableStateOf(EcoColorsDark.GlassAccent)
    var TextPrimary by mutableStateOf(EcoColorsDark.TextPrimary)
    var TextSecondary by mutableStateOf(EcoColorsDark.TextSecondary)
    var BackgroundDark by mutableStateOf(EcoColorsDark.BackgroundDark)
    var CardBackground by mutableStateOf(EcoColorsDark.CardBackground)
    var CardPrimary by mutableStateOf(EcoColorsDark.CardPrimary)
    var Success by mutableStateOf(EcoColorsDark.Success)
    var Warning by mutableStateOf(EcoColorsDark.Warning)
    var Error by mutableStateOf(EcoColorsDark.Error)
    var isDark by mutableStateOf(true)

    fun updateTheme(dark: Boolean) {
        this.isDark = dark
        if (dark) {
            this.GlassGreen = EcoColorsDark.GlassGreen
            this.GlassDarkGreen = EcoColorsDark.GlassDarkGreen
            this.GlassAccent = EcoColorsDark.GlassAccent
            this.TextPrimary = EcoColorsDark.TextPrimary
            this.TextSecondary = EcoColorsDark.TextSecondary
            this.BackgroundDark = EcoColorsDark.BackgroundDark
            this.CardBackground = EcoColorsDark.CardBackground
            this.CardPrimary = EcoColorsDark.CardPrimary
            this.Success = EcoColorsDark.Success
            this.Warning = EcoColorsDark.Warning
            this.Error = EcoColorsDark.Error
        } else {
            this.GlassGreen = EcoColorsLight.GlassGreen
            this.GlassDarkGreen = EcoColorsLight.GlassDarkGreen
            this.GlassAccent = EcoColorsLight.GlassAccent
            this.TextPrimary = EcoColorsLight.TextPrimary
            this.TextSecondary = EcoColorsLight.TextSecondary
            this.BackgroundDark = EcoColorsLight.BackgroundDark
            this.CardBackground = EcoColorsLight.CardBackground
            this.CardPrimary = EcoColorsLight.CardPrimary
            this.Success = EcoColorsLight.Success
            this.Warning = EcoColorsLight.Warning
            this.Error = EcoColorsLight.Error
        }
    }

    fun getAvatarColor(name: String): Color {
        val colors = listOf(
            Color(0xFF10B981), // Eco Green
            Color(0xFF3B82F6), // Electric Blue
            Color(0xFF8B5CF6), // Vivid Purple
            Color(0xFFF59E0B), // Sunset Orange
            Color(0xFFEC4899), // Rose Pink
            Color(0xFF06B6D4), // Cyan
            Color(0xFF6366F1), // Indigo
            Color(0xFFF43F5E), // Rose Red
            Color(0xFF84CC16)  // Lime Green
        )
        val index = name.hashCode().let { if (it < 0) -it else it } % colors.size
        return colors[index]
    }

    fun getAvatarGradient(name: String): Brush {
        val baseColor = getAvatarColor(name)
        // Generamos un color un poco más oscuro para el final del degradado
        val endColor = Color(
            red = (baseColor.red * 0.7f).coerceIn(0f, 1f),
            green = (baseColor.green * 0.7f).coerceIn(0f, 1f),
            blue = (baseColor.blue * 0.7f).coerceIn(0f, 1f),
            alpha = baseColor.alpha
        )
        return Brush.linearGradient(
            colors = listOf(baseColor, endColor),
            start = Offset(0f, 0f),
            end = Offset(100f, 100f)
        )
    }
}

// ── Componentes Composable ───────────────────────────────────────────────────

@Composable
fun GlassBackground(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoColors.BackgroundDark)
            .systemBarsPadding()
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = EcoColors.CardBackground,
    cornerRadius: Int = 40,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val isDark = EcoColors.isDark

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .border(
                width = 0.5.dp,
                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = EcoColors.TextSecondary) },
        modifier = modifier,
        enabled = enabled,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        shape = androidx.compose.foundation.shape.CircleShape
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = EcoColors.GlassAccent,
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = EcoColors.CardPrimary.copy(alpha = 0.5f),
            disabledContentColor = EcoColors.TextSecondary
        ),
        shape = androidx.compose.foundation.shape.CircleShape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        content()
    }
}

@Composable
fun GlassSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = androidx.compose.foundation.shape.CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.GlassAccent),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = EcoColors.GlassAccent
        )
    ) {
        content()
    }
}

@Composable
fun EcoDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier,
        color = EcoColors.CardPrimary,
        thickness = 1.dp
    )
}

/**
 * Efecto Shimmer (Brillo animado) para estados de carga profesionales.
 */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val shimmerColors = listOf(
        EcoColors.CardPrimary.copy(alpha = 0.6f),
        EcoColors.CardPrimary.copy(alpha = 0.2f),
        EcoColors.CardPrimary.copy(alpha = 0.6f),
    )

    return this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim, y = translateAnim)
        )
    )
}

/**
 * Box con efecto shimmer para usar como placeholder de carga.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmerEffect()
    )
}
@Composable
fun StyledHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = EcoColors.GlassAccent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = if (EcoColors.isDark) EcoColors.TextPrimary else Color(0xFF1E293B),
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(
            androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(EcoColors.GlassAccent.copy(alpha = 0.4f), Color.Transparent))
        ))
    }
}

/**
 * Aplica un degradado de desvanecimiento (Fade) en la parte inferior.
 * Ideal para que las listas se desvanezcan suavemente bajo la barra de navegación.
 */
fun Modifier.fadingEdge(
    fadeHeight: androidx.compose.ui.unit.Dp = 60.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 100.dp
): Modifier = this.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fadeHeightPx = fadeHeight.toPx()
        val bottomPaddingPx = bottomPadding.toPx()
        val startFadeY = size.height - bottomPaddingPx - fadeHeightPx
        val endFadeY = size.height - bottomPaddingPx
        
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Black,
                (startFadeY / size.height).coerceIn(0f, 1f) to Color.Black,
                (endFadeY / size.height).coerceIn(0f, 1f) to Color.Transparent,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

@Composable
fun StatusBadge(icon: ImageVector, text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActivityRow(item: com.rubensimon.ecolens.data.models.social.HistoryItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).background(EcoColors.CardPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text(item.emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.nombre, fontWeight = FontWeight.Bold, color = EcoColors.TextPrimary, fontSize = 15.sp)
            Text(com.rubensimon.ecolens.utils.TimeUtils.getTimeAgo(item.fecha), color = EcoColors.TextSecondary, fontSize = 12.sp)
        }
        Text("+${item.puntos}", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}
