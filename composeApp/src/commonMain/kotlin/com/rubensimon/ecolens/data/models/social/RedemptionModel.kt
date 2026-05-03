package com.rubensimon.ecolens.data.models.social

import kotlinx.serialization.Serializable

/**
 * Registro de cupón canjeado en la tabla `cupones_canjeados` de Supabase.
 */
@Serializable
data class RedemptionModel(
    val id: String? = null,
    val user_id: String, 
    val cupon_id: String,
    val nombre_cupon: String? = null,
    val puntos_gastados: Int = 0,
    val tienda_id: String? = "tienda_general",
    val codigo_qr: String? = null,
    val estado: String = "activo",
    val created_at: String? = null
)
