package com.rubensimon.ecolens.data.models.items

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
 * Datos de un cupón cargado desde la tabla `cupones_tienda` en Supabase.
 */
@Serializable
data class Coupon(
    val id: String = "",
    @SerialName("tienda_id") val tiendaId: String? = null,
    @SerialName("titulo") val title: String,
    @SerialName("descripcion") val description: String = "",
    @SerialName("coste_puntos") val pointsCost: Int,
    val stock: Int = 0,
    @SerialName("dias_validez") val daysValidity: Int = 0,
    val category: String = "",
    val activo: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    
    // Campos temporales para la UI (no se guardan en la tabla de cupones_tienda)
    @kotlinx.serialization.Transient val redemptionId: String? = null,
    @kotlinx.serialization.Transient val status: String = "activo"
)
