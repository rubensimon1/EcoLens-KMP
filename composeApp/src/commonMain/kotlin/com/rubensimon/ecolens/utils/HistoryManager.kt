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
    
    // Cache en memoria para la comunidad (Persistente mientras la app esté abierta)
    var globalActivityCache: List<HistoryItem> = emptyList()
    var avatarCache: MutableMap<String, String?> = mutableMapOf()

    private const val KEY_HISTORY = "lista_historial"
    private const val KEY_LAST_SYNC = "last_history_sync"
    private const val KEY_PENDING_SYNC = "pending_history_sync"
    private const val KEY_PENDING_PROFILE_SYNC = "pending_profile_sync" // Formato: "type|value"
    
    // Claves para perfil persistente (Se añadirán prefijos de userId)
    private const val PREFIX_USERNAME = "cached_username_"
    private const val PREFIX_PROFILE_PIC = "cached_profile_pic_"
    private const val PREFIX_POINTS = "cached_points_"
    private const val PREFIX_SCANS = "cached_scans_"
    private const val PREFIX_BIO = "cached_bio_"
    private const val PREFIX_USER_HISTORY = "cached_user_history_"



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
                val success = addRemoteHistoryItem(userId, objectName, points, actionType, co2Impact)
                if (!success) {
                    // Si falla el envío (offline), lo guardamos en pendientes
                    savePendingItem(userId, objectName, points, actionType, co2Impact)
                }
            }
        }
    }

    /**
     * Retorna el historial local ya procesado para la UI.
     */
    fun getHistory(): List<HistoryItem> = getLocalHistory()

    fun getCachedUsername(userId: String): String = settings.getString(PREFIX_USERNAME + userId, "EcoUser")
    fun getCachedProfilePic(userId: String): String? = settings.getStringOrNull(PREFIX_PROFILE_PIC + userId)
    fun getCachedPoints(userId: String): Int = settings.getInt(PREFIX_POINTS + userId, 0)
    fun getCachedScans(userId: String): Int = settings.getInt(PREFIX_SCANS + userId, 0)
    fun getCachedBio(userId: String): String = settings.getString(PREFIX_BIO + userId, "Eco Enthusiast")

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
                            user_id = remote.user_id,
                            nombre = remote.object_name,
                            objeto = remote.object_name,
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

    /**
     * Guarda un cambio de perfil para sincronizar después.
     */
    fun updateProfileOffline(userId: String, type: String, value: String) {
        // Guardar localmente para efecto inmediato en la UI
        if (type == "username") settings.putString(PREFIX_USERNAME + userId, value)
        if (type == "avatar") settings.putString(PREFIX_PROFILE_PIC + userId, value)
        if (type == "bio") settings.putString(PREFIX_BIO + userId, value)
        
        // Añadir a la cola de sincronización (aquí sí incluimos el userId en el valor)
        val prev = settings.getString(KEY_PENDING_PROFILE_SYNC, "")
        val entry = "$userId|$type|$value"
        val updated = if (prev.isEmpty()) entry else "$entry;$prev"
        settings.putString(KEY_PENDING_PROFILE_SYNC, updated)
    }

    /**
     * Cachea el perfil completo de un usuario.
     */
    fun cacheUserProfile(user: com.rubensimon.ecolens.data.models.social.UserModel) {
        settings.putString(PREFIX_USERNAME + user.id, user.display_name ?: user.username)
        settings.putString(PREFIX_PROFILE_PIC + user.id, user.profile_picture_url ?: "")
        settings.putInt(PREFIX_POINTS + user.id, user.puntos)
        settings.putInt(PREFIX_SCANS + user.id, user.total_scans)
        settings.putString(PREFIX_BIO + user.id, user.bio ?: "Eco Enthusiast")
    }

    /**
     * Cachea el historial de un usuario específico.
     */
    fun cacheUserHistory(userId: String, items: List<com.rubensimon.ecolens.data.models.social.HistoryItemModel>) {
        // Guardamos solo los últimos 10 para no saturar settings
        val top = items.take(10)
        val str = top.joinToString(";") { 
            "${it.object_name}|${it.points}|${it.created_at ?: ""}"
        }
        settings.putString(PREFIX_USER_HISTORY + userId, str)
    }

    /**
     * Recupera el historial cacheado de un usuario.
     */
    fun getCachedUserHistory(userId: String): List<com.rubensimon.ecolens.data.models.social.HistoryItemModel> {
        val raw = settings.getString(PREFIX_USER_HISTORY + userId, "")
        if (raw.isEmpty()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 3) {
                com.rubensimon.ecolens.data.models.social.HistoryItemModel(
                    user_id = userId,
                    object_name = parts[0],
                    points = parts[1].toIntOrNull() ?: 0,
                    created_at = parts[2],
                    action_type = "scan"
                )
            } else null
        }
    }

    private suspend fun addRemoteHistoryItem(
        userId: String,
        objectName: String,
        points: Int,
        actionType: String,
        co2Impact: Float
    ): Boolean {
        return try {
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
            true
        } catch (e: Exception) {
            println("[HistoryManager] ❌ Error addRemoteHistoryItem (Offline?): ${e.message}")
            false
        }
    }

    private fun savePendingItem(userId: String, name: String, pts: Int, type: String, co2: Float) {
        val prev = settings.getString(KEY_PENDING_SYNC, "")
        val entry = "$userId|$name|$pts|$type|$co2"
        val updated = if (prev.isEmpty()) entry else "$entry;$prev"
        settings.putString(KEY_PENDING_SYNC, updated)
    }

    /**
     * Intenta sincronizar los items pendientes cuando vuelve la conexión.
     */
    fun syncPendingItems() {
        val raw = settings.getString(KEY_PENDING_SYNC, "")
        if (raw.isEmpty()) return

        scope.launch {
            val items = raw.split(";")
            val remaining = mutableListOf<String>()

            items.forEach { entry ->
                val parts = entry.split("|")
                if (parts.size == 5) {
                    val success = addRemoteHistoryItem(
                        userId = parts[0],
                        objectName = parts[1],
                        points = parts[2].toIntOrNull() ?: 0,
                        actionType = parts[3],
                        co2Impact = parts[4].toFloatOrNull() ?: 0.5f
                    )
                    if (!success) remaining.add(entry)
                }
            }

            if (remaining.isEmpty()) {
                settings.remove(KEY_PENDING_SYNC)
                println("[HistoryManager] 🔄 All pending items synced!")
            } else {
                settings.putString(KEY_PENDING_SYNC, remaining.joinToString(";"))
            }

            // --- Sincronizar cambios de perfil pendientes ---
            val rawProfile = settings.getString(KEY_PENDING_PROFILE_SYNC, "")
            if (rawProfile.isNotEmpty()) {
                val profileItems = rawProfile.split(";")
                val profileRemaining = mutableListOf<String>()
                val userId = settings.getString("user_id", "")

                if (userId.isNotEmpty()) {
                    profileItems.forEach { entry ->
                        val parts = entry.split("|")
                        if (parts.size == 3) {
                            val targetId = parts[0]
                            val type = parts[1]
                            val value = parts[2]
                            val success = try {
                                if (type == "username") {
                                    client.from("usuarios").update({
                                        set("display_name", value)
                                    }) { filter { eq("id", targetId) } }
                                } else if (type == "avatar") {
                                    client.from("usuarios").update({
                                        set("profile_picture_url", value)
                                    }) { filter { eq("id", targetId) } }
                                }
                                true
                            } catch (e: Exception) { false }
                            
                            if (!success) profileRemaining.add(entry)
                        }
                    }
                }

                if (profileRemaining.isEmpty()) {
                    settings.remove(KEY_PENDING_PROFILE_SYNC)
                } else {
                    settings.putString(KEY_PENDING_PROFILE_SYNC, profileRemaining.joinToString(";"))
                }
            }
        }
    }

    private fun getLocalHistory(): List<HistoryItem> {
        val raw = settings.getString(KEY_HISTORY, "")
        if (raw.isEmpty()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 3) {
                HistoryItem(
                    user_id = settings.getString("user_id", ""),
                    nombre = parts[0],
                    objeto = parts[0],
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
        settings.remove(KEY_HISTORY)
        settings.remove(KEY_LAST_SYNC)
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
