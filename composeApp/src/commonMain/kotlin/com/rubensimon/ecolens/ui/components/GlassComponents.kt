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
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Sistema de diseño Glassmorphism de EcoLens — migrado a Compose Multiplatform.
 *
 * Totalmente compatible con commonMain. No tiene dependencias Android específicas.
 * Migrado desde GlassComponents.kt del proyecto Android original.
 */

// ── Paletas de color ────────────────────────────────────────────────────────

// ── Paletas de color ────────────────────────────────────────────────────────

object EcoColorsDark {
    val GlassGreen = Color(0x3310B981) // Emerald transparent
    val GlassDarkGreen = Color(0x66065F46) // Dark Emerald
    val GlassAccent = Color(0xFF10B981) // Emerald 500
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF94A3B8) // Slate 400
    val BackgroundDark = Color(0xFF0F172A) // Slate 900
    val CardBackground = Color(0xFF1E293B).copy(alpha = 0.65f) // Más transparente
    val CardPrimary = Color(0xFF334155).copy(alpha = 0.4f)
    val Success = Color(0xFF34D399)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFEF4444)
}

object EcoColorsLight {
    val GlassGreen = Color(0xFFD1FAE5) // Emerald 100
    val GlassDarkGreen = Color(0xFF10B981) // Emerald 500
    val GlassAccent = Color(0xFF059669) // Emerald 600
    val TextPrimary = Color(0xFF1E293B) // Slate 800
    val TextSecondary = Color(0xFF64748B) // Slate 500
    val BackgroundDark = Color(0xFFF8FAFC) // Slate 50
    val CardBackground = Color.White.copy(alpha = 0.8f)
    val CardPrimary = Color(0xFFF1F5F9).copy(alpha = 0.5f)
    val Success = Color(0xFF10B981)
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
    cornerRadius: Int = 20,
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
        shape = RoundedCornerShape(16.dp)
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
        shape = RoundedCornerShape(20.dp),
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
        shape = RoundedCornerShape(20.dp),
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
