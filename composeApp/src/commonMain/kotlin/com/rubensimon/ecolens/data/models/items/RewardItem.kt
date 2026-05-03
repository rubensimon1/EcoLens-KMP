package com.rubensimon.ecolens.data.models.items

import androidx.compose.ui.graphics.Color

/**
 * Premio canjeable en la tienda de recompensas.
 * El campo `color` es Compose Color (disponible en commonMain con Compose Multiplatform).
 */
data class RewardItem(
    val title: String,   // Ej: "Tarjeta Amazon 10€"
    val cost: Int,       // Puntos necesarios
    val color: Color     // Color de la tarjeta
)

/**
 * Datos de un cupón cargado desde Supabase.
 */
data class Coupon(
    val id: String = "",
    val title: String,
    val description: String = "",
    val pointsCost: Int,
    val store: String = "",
    val category: String = "",
    val expiresAt: String? = null,
    val redeemedAt: String? = null // Nueva propiedad para historial
)
