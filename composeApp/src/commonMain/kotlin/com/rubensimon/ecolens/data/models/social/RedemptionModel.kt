package com.rubensimon.ecolens.data.models.social

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Registro de cupón canjeado en la tabla `cupones_canjeados` de Supabase.
 */
@Serializable
data class RedemptionModel(
    val id: String? = null,
    val user_id: String, 
    val cupon_id: String,
    val tienda_id: String? = null,
    val codigo_qr: String? = null,
    val estado: String = "activo",
    @SerialName("fecha_canje") val fechaCanje: String? = null,
    @SerialName("fecha_uso") val fechaUso: String? = null,
    @SerialName("fecha_expiracion") val fechaExpiracion: String? = null
)
