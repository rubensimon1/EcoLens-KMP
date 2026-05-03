package com.rubensimon.ecolens.data.models.social

import kotlinx.serialization.Serializable

/**
 * Registro de escaneo/acción en la tabla `historial_escaneos` de Supabase.
 */
@Serializable
data class HistoryItemModel(
    val id: String? = null,
    val user_id: String,
    val object_name: String,
    val points: Int,
    val co2_impact: Float = 0.5f,
    val created_at: String? = null,
    val action_type: String = "scan" // scan | recycle | reward
)

/**
 * Modelo de UI para mostrar items en la pantalla de historial.
 * Equivalente ligero al HistoryItemModel, con formato ya procesado.
 */
data class HistoryItem(
    val nombre: String,
    val puntos: Int,
    val fecha: String,
    val emoji: String = "♻️"
)

/**
 * Modelo de sincronización local ↔ remoto.
 */
data class SyncedHistory(
    val localItems: List<HistoryItem>,
    val remoteItems: List<HistoryItemModel>,
    val hasConflicts: Boolean = false,
    val lastSyncTime: Long = 0L
)
