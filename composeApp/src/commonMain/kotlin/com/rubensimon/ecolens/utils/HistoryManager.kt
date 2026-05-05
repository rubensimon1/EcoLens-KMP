package com.rubensimon.ecolens.utils

import com.rubensimon.ecolens.data.models.social.HistoryItem
import com.rubensimon.ecolens.data.models.social.HistoryItemModel
import com.rubensimon.ecolens.data.models.social.SyncedHistory
import com.rubensimon.ecolens.data.network.SupabaseClientProvider
import com.russhwolf.settings.Settings
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Gestor de historial de escaneos.
 *
 * ### Cambios de migración Android → KMP
 * - `SharedPreferences` → `com.russhwolf:multiplatform-settings`
 * - `SimpleDateFormat` → `kotlinx-datetime`
 * - `System.currentTimeMillis()` → `Clock.System.now().toEpochMilliseconds()`
 * - `Log.d/e` → `println()`
 * - `CoroutineScope(lifecycleScope)` → `CoroutineScope(Dispatchers.IO)`
 *
 * Mantiene la lógica de fusión local ↔ remoto intacta.
 */
object HistoryManager {

    private val settings: Settings by lazy { Settings() }
    private val client = SupabaseClientProvider.client
    private val scope = CoroutineScope(Dispatchers.IO)

    private const val KEY_HISTORY = "lista_historial"
    private const val KEY_LAST_SYNC = "last_history_sync"

    // ── Método principal ───────────────────────────────────────────────────

    /**
     * Añade un item al historial local y lo sincroniza con Supabase.
     */
    fun addHistoryItem(
        objectName: String,
        points: Int,
        actionType: String = "scan",
        co2Impact: Float = 0.5f,
        userId: String = ""
    ) {
        addLocalHistoryItem(objectName, points)

        if (userId.isNotEmpty()) {
            scope.launch {
                addRemoteHistoryItem(userId, objectName, points, actionType, co2Impact)
            }
        }
    }

    /**
     * Retorna el historial local ya procesado para la UI.
     */
    fun getHistory(): List<HistoryItem> = getLocalHistory()

    /**
     * Retorna el historial filtrado por una fecha específica (formato dd/MM).
     */
    fun getHistoryForDate(day: Int, month: Int): List<HistoryItem> {
        val search = "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}"
        return getLocalHistory().filter { it.fecha.startsWith(search) }
    }

    /**
     * Carga el historial desde Supabase y lo almacena localmente si es nuevo.
     */
    suspend fun loadFromDatabase(userId: String, force: Boolean = false): Boolean {
        return try {
            if (userId.isEmpty()) return false

            val remoteItems = getRemoteHistoryItems(userId)
            val lastSync = settings.getLong(KEY_LAST_SYNC, 0L)
            val localItems = getLocalHistory()
            val oneDayMs = 24 * 60 * 60 * 1000L
            val now = com.rubensimon.ecolens.utils.TimeUtils.getCurrentTimestamp()

            if (remoteItems.isNotEmpty()) {
                if (force || localItems.isEmpty() || (now - lastSync) > oneDayMs) {
                    val remoteAsLocal = remoteItems.map { remote ->
                        HistoryItem(
                            nombre = remote.object_name,
                            puntos = remote.points,
                            fecha = formatRemoteDate(remote.created_at),
                            emoji = getEmojiForObject(remote.object_name)
                        )
                    }
                    // Fusionar: mantener items locales recientes que aún no estén en remoto
                    val remoteKeys = remoteAsLocal.map { "${it.nombre}|${it.fecha}" }.toSet()
                    val localOnly = localItems.filter { "${it.nombre}|${it.fecha}" !in remoteKeys }
                    val merged = (localOnly + remoteAsLocal).sortedByDescending { it.fecha }
                    
                    saveLocalHistory(merged)
                    settings.putLong(KEY_LAST_SYNC, now)
                    println("[HistoryManager] Merged: ${localOnly.size} local-only + ${remoteAsLocal.size} remote = ${merged.size} total (force=$force)")
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            println("[HistoryManager] Error loadFromDatabase: ${e.message}")
            false
        }
    }

    // ── Privados ───────────────────────────────────────────────────────────

    private fun addLocalHistoryItem(objectName: String, points: Int) {
        val prev = settings.getString(KEY_HISTORY, "")
        val fecha = simpleDate()
        val entry = "$objectName|$points|$fecha"
        val updated = if (prev.isEmpty()) entry else "$entry;$prev"
        settings.putString(KEY_HISTORY, updated)
    }

    private suspend fun addRemoteHistoryItem(
        userId: String,
        objectName: String,
        points: Int,
        actionType: String,
        co2Impact: Float
    ) {
        try {
            val historyItem = HistoryItemModel(
                user_id = userId,
                object_name = objectName,
                points = points,
                co2_impact = co2Impact,
                created_at = com.rubensimon.ecolens.utils.TimeUtils.getCurrentIsoDate(),
                action_type = actionType
            )
            client.from("historial_escaneos").insert(historyItem)
            println("[HistoryManager] ✅ Saved remote: $objectName")
        } catch (e: Exception) {
            println("[HistoryManager] ❌ Error addRemoteHistoryItem: ${e.message}")
        }
    }

    private fun getLocalHistory(): List<HistoryItem> {
        val raw = settings.getString(KEY_HISTORY, "")
        if (raw.isEmpty()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 3) {
                HistoryItem(
                    nombre = parts[0],
                    puntos = parts[1].toIntOrNull() ?: 0,
                    fecha = parts[2],
                    emoji = getEmojiForObject(parts[0])
                )
            } else null
        }
    }

    private suspend fun getRemoteHistoryItems(userId: String): List<HistoryItemModel> {
        return try {
            client.from("historial_escaneos")
                .select { filter { eq("user_id", userId) } }
                .decodeList<HistoryItemModel>()
        } catch (e: Exception) {
            println("[HistoryManager] Error getRemoteHistoryItems: ${e.message}")
            emptyList()
        }
    }

    private fun saveLocalHistory(items: List<HistoryItem>) {
        val str = items.joinToString(";") { "${it.nombre}|${it.puntos}|${it.fecha}" }
        settings.putString(KEY_HISTORY, str)
    }

    fun clearHistory() {
        settings.putString(KEY_HISTORY, "")
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    fun getEmojiForObject(objectName: String): String = when {
        objectName.contains("Bottle", ignoreCase = true) -> "🥤"
        objectName.contains("Can", ignoreCase = true) -> "🥫"
        objectName.contains("Paper", ignoreCase = true) -> "📰"
        objectName.contains("Cardboard", ignoreCase = true) -> "📦"
        objectName.contains("Glass", ignoreCase = true) -> "🍶"
        objectName.contains("Plastic", ignoreCase = true) -> "🛍️"
        else -> "♻️"
    }

    private fun formatRemoteDate(remoteDate: String?): String {
        if (remoteDate == null) return simpleDate()
        // Convertir fecha remota UTC a hora local para consistencia con simpleDate()
        return try {
            val cleanDate = remoteDate.substringBefore("+").let { if (it.endsWith("Z")) it else it + "Z" }
            val instant = Instant.parse(cleanDate)
            val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val dd = local.dayOfMonth.toString().padStart(2, '0')
            val mm = local.monthNumber.toString().padStart(2, '0')
            val hh = local.hour.toString().padStart(2, '0')
            val min = local.minute.toString().padStart(2, '0')
            "$dd/$mm $hh:$min"
        } catch (_: Exception) {
            // Fallback: parseo manual si el formato no es ISO estándar
            try {
                val parts = remoteDate.split("T")
                if (parts.size >= 2) {
                    val dateParts = parts[0].split("-")
                    val timeParts = parts[1].take(5)
                    if (dateParts.size == 3) {
                        "${dateParts[2]}/${dateParts[1]} $timeParts"
                    } else remoteDate
                } else remoteDate
            } catch (_: Exception) {
                remoteDate
            }
        }
    }

    private fun simpleDate(): String {
        // Usar hora LOCAL (no UTC) para que coincida con los días del gráfico
        val now = Clock.System.now()
        val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val dd = local.dayOfMonth.toString().padStart(2, '0')
        val mm = local.monthNumber.toString().padStart(2, '0')
        val hh = local.hour.toString().padStart(2, '0')
        val min = local.minute.toString().padStart(2, '0')
        return "$dd/$mm $hh:$min"
    }
}
