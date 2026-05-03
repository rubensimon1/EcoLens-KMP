package com.rubensimon.ecolens.data.models.items

/**
 * Item de historial para la UI. Datos ya formateados para mostrar.
 */
data class HistoryItem(
    val itemName: String,  // Ej: "Plastic Bottle"
    val points: Int,       // Ej: +10
    val date: String,      // Ej: "08 Feb, 17:30"
    val emoji: String      // Ej: "🥤"
)

/**
 * Item del ranking global.
 */
data class LeaderboardItem(
    val userId: String = "",
    val rank: Int,
    val name: String,
    val points: Int,
    val isCurrentUser: Boolean = false
)

/**
 * Respuesta del backend ML tras analizar una imagen.
 */
data class PredictionResponse(
    val label: String,          // Ej: "Botella de Plástico"
    val container: String,      // Ej: "Amarillo"
    val message: String,
    val points_earned: Int      // Ej: 10
)
