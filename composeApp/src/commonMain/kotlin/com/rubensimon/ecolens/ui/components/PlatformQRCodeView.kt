package com.rubensimon.ecolens.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Componente multiplataforma para dibujar un código QR dado su contenido (String).
 */
@Composable
expect fun PlatformQRCodeView(
    content: String,
    modifier: Modifier = Modifier
)
