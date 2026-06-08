package com.rubensimon.ecolens.data.models.items

import kotlinx.serialization.Serializable

/**
 * Modelo de una tienda colaboradora cargada desde la tabla `tiendas` en Supabase.
 */
@Serializable
data class TiendaModel(
    val id: String = "",
    val nombre: String = "",
    val logo_emoji: String? = null,
    val descripcion: String? = null,
    val categoria: String? = null
)
