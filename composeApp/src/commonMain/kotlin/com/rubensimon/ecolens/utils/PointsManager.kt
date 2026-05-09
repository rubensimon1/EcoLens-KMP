package com.rubensimon.ecolens.utils

import com.rubensimon.ecolens.data.network.SupabaseClientProvider
import com.russhwolf.settings.Settings
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

/**
 * Gestor de puntos y escaneos con sincronización automática a Supabase.
 *
 * ### Cambios de migración Android → KMP
 * - `SharedPreferences` → `com.russhwolf:multiplatform-settings`
 * - `Log.d/e` → `println()`
 * - Eliminado parámetro `Context` de todos los métodos
 * - `System.currentTimeMillis()` → implícito en Supabase timestamps
 */
object PointsManager {

    private val settings: Settings by lazy { Settings() }
    private val client = SupabaseClientProvider.client
    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: kotlinx.coroutines.Job? = null

    private const val KEY_POINTS = "puntos"
    private const val KEY_TOTAL_EARNED = "total_puntos_ganados"
    private const val KEY_SCANS = "total_scans"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_CO2 = "co2_saved_total"
    private const val KEY_STREAK = "current_streak"
    private const val KEY_LAST_SCAN_DATE = "last_scan_date"
    private const val KEY_DAILY_SCANS = "daily_scans_count"
    private const val KEY_DAILY_MISSION_GOAL = "daily_mission_goal"
    
    private const val XP_PER_LEVEL = 100 // Cada 100 pts de por vida sube un nivel

    // ── Getters con prefijo de usuario ──────────────────────────────────────
    
    private fun getUserKey(base: String): String {
        val uid = getUserId()
        return if (uid.isEmpty()) base else "${base}_$uid"
    }

    fun getPoints(): Int = settings.getInt(getUserKey(KEY_POINTS), 0)
    fun getTotalEarned(): Int = settings.getInt(getUserKey(KEY_TOTAL_EARNED), 0)

    fun addPoints(amount: Int, reason: String = "") {
        val uid = getUserId()
        if (uid.isEmpty()) return

        val oldLevel = getLevel() // Nivel antes de sumar
        val current = getPoints()
        val currentTotal = getTotalEarned()
        
        settings.putInt(getUserKey(KEY_POINTS), current + amount)
        settings.putInt(getUserKey(KEY_TOTAL_EARNED), currentTotal + amount)
        
        val newLevel = getLevel() // Nivel después de sumar
        if (newLevel > oldLevel) {
            // ¡NUEVO: Notificación de subida de nivel!
            println("[PointsManager] 🎉 ¡SUBIDA DE NIVEL! $oldLevel -> $newLevel")
            com.rubensimon.ecolens.utils.NotificationManager.addNotification(
                title = "¡Subida de Nivel! ⬆️",
                description = "¡Felicidades, Ruben! Has alcanzado el Nivel $newLevel. ¡Sigue así!"
            )
        }
        
        println("[PointsManager] +$amount pts ($reason) para $uid → Total: ${current + amount}")
        autoSync()
    }
    
    // ── Niveles ────────────────────────────────────────────────────────────
    
    fun getLevel(): Int {
        val total = getTotalEarned()
        return when {
            total < 100 -> 1
            total < 300 -> 2
            total < 600 -> 3
            total < 1000 -> 4
            total < 1500 -> 5
            else -> (total / 500) + 3
        }
    }
    
    fun getProgressToNextLevel(): Float {
        val level = getLevel()
        val base = getBaseLevelPoints(level)
        val next = getNextLevelPoints(level)
        val total = getTotalEarned()
        
        val progress = (total - base).toFloat() / (next - base).toFloat()
        return progress.coerceIn(0f, 1f)
    }
    
    fun getPointsNeededForNextLevel(): Int = getNextLevelPoints(getLevel()) - getTotalEarned()

    fun getNextLevelPoints(level: Int): Int = when (level) {
        1 -> 100
        2 -> 300
        3 -> 600
        4 -> 1000
        5 -> 1500
        else -> (level - 2) * 500
    }

    fun getBaseLevelPoints(level: Int): Int = when (level) {
        1 -> 0
        2 -> 100
        3 -> 300
        4 -> 600
        5 -> 1000
        else -> (level - 3) * 500
    }

    fun setPoints(points: Int) {
        settings.putInt(getUserKey(KEY_POINTS), points)
    }

    fun subtractPoints(amount: Int): Boolean {
        val current = getPoints()
        return if (current >= amount) {
            settings.putInt(getUserKey(KEY_POINTS), current - amount)
            autoSync()
            true
        } else false
    }

    // ── Escaneos ───────────────────────────────────────────────────────────

    fun getTotalScans(): Int = settings.getInt(getUserKey(KEY_SCANS), 0)

    fun incrementScans(objectName: String = "Objeto", points: Int = 10) {
        val uid = getUserId()
        if (uid.isEmpty()) return
        
        val newTotal = getTotalScans() + 1
        settings.putInt(getUserKey(KEY_SCANS), newTotal)
        
        updateDailyStats()
        autoSync()
    }

    private fun updateDailyStats() {
        val uid = getUserId()
        if (uid.isEmpty()) return

        val today = TimeUtils.getCurrentIsoDate().substringBefore("T")
        val lastDate = settings.getString(getUserKey(KEY_LAST_SCAN_DATE), "")
        
        if (today != lastDate) {
            // Es un nuevo día
            val yesterday = TimeUtils.getYesterdayIsoDate().substringBefore("T")
            if (lastDate == yesterday) {
                // Racha continúa
                val currentStreak = settings.getInt(getUserKey(KEY_STREAK), 0)
                settings.putInt(getUserKey(KEY_STREAK), currentStreak + 1)
            } else {
                // Racha rota o primera vez
                settings.putInt(getUserKey(KEY_STREAK), 1)
            }
            settings.putString(getUserKey(KEY_LAST_SCAN_DATE), today)
            settings.putInt(getUserKey(KEY_DAILY_SCANS), 1)
        } else {
            // Mismo día
            val dailyScans = settings.getInt(getUserKey(KEY_DAILY_SCANS), 0)
            settings.putInt(getUserKey(KEY_DAILY_SCANS), dailyScans + 1)
        }
        
        checkDailyMission()
    }

    fun getStreak(): Int = settings.getInt(getUserKey(KEY_STREAK), 0)
    
    fun getDailyScans(): Int {
        val today = TimeUtils.getCurrentIsoDate().substringBefore("T")
        val lastDate = settings.getString(getUserKey(KEY_LAST_SCAN_DATE), "")
        
        if (today != lastDate) {
            // Es un nuevo día, reseteamos localmente si se consulta
            settings.putInt(getUserKey(KEY_DAILY_SCANS), 0)
            return 0
        }
        return settings.getInt(getUserKey(KEY_DAILY_SCANS), 0)
    }
    
    fun getDailyGoal(): Int {
        var goal = settings.getInt(getUserKey(KEY_DAILY_MISSION_GOAL), 3)
        if (goal <= 0) goal = 3
        return goal
    }

    private fun checkDailyMission() {
        val dailyScans = getDailyScans()
        val goal = getDailyGoal()
        if (dailyScans == goal) {
            // Misión completada por primera vez hoy
            addPoints(100, "daily_mission_bonus")
            println("[PointsManager] 🎉 Misión diaria completada!")
        }
    }

    fun setTotalScans(count: Int) {
        settings.putInt(getUserKey(KEY_SCANS), count)
    }

    // ── CO2 ────────────────────────────────────────────────────────────────

    fun getCo2Saved(): Float = settings.getFloat(getUserKey(KEY_CO2), 0f)

    fun addCo2(amount: Float) {
        val current = getCo2Saved()
        settings.putFloat(getUserKey(KEY_CO2), current + amount)
    }

    // ── User ID ────────────────────────────────────────────────────────────

    fun getUserId(): String = settings.getString(KEY_USER_ID, "")

    fun setUserId(id: String) {
        settings.putString(KEY_USER_ID, id)
        println("[PointsManager] 👤 Sesión activa para: $id")
    }

    // ── Sincronización ─────────────────────────────────────────────────────

    /**
     * Sincroniza los puntos locales con Supabase de forma automática en background.
     */
    private fun autoSync() {
        val userId = getUserId()
        if (userId.isEmpty()) return
        
        syncJob?.cancel()
        syncJob = scope.launch {
            // Esperamos 2 segundos sin actividad para enviar (Debounce)
            kotlinx.coroutines.delay(2000)
            syncToSupabase(userId)
        }
    }

    /**
     * Fuerza sincronización inmediata (usar desde ViewModel/coroutine ya suspendida).
     */
    suspend fun forceSync() {
        val userId = getUserId()
        if (userId.isEmpty()) return
        syncToSupabase(userId)
    }

    /**
     * Carga los puntos desde Supabase sobreescribiendo el estado local.
     */
    suspend fun loadFromSupabase(userId: String) {
        try {
            println("[PointsManager] ☁️ Intentando descargar puntos para $userId...")
            @kotlinx.serialization.Serializable
            data class PointsRow(val puntos: Int = 0, val total_scans: Int = 0, val total_xp: Int = 0)
            val result = client.from("usuarios")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<PointsRow>()

            result?.let {
                settings.putInt(getUserKey(KEY_POINTS), it.puntos)
                // Migración segura: si total_xp es 0 (columna nueva), usamos los puntos actuales como base
                val xpBase = maxOf(it.total_xp, it.puntos)
                settings.putInt(getUserKey(KEY_TOTAL_EARNED), xpBase) 
                settings.putInt(getUserKey(KEY_SCANS), it.total_scans)
                // CO2 se calcula localmente: total_scans * 0.5
                settings.putFloat(getUserKey(KEY_CO2), it.total_scans * 0.5f)
                println("[PointsManager] ✅ Descargado: ${it.puntos} pts, ${it.total_scans} scans, $xpBase XP")
            }
        } catch (e: Exception) {
            println("[PointsManager] ❌ Error loadFromSupabase: ${e.message}")
        }
    }

    private suspend fun syncToSupabase(userId: String) {
        try {
            client.from("usuarios").update({
                set("puntos", getPoints())
                set("total_scans", getTotalScans())
                set("total_xp", getTotalEarned()) // Guardamos el XP total
            }) {
                filter { eq("id", userId) }
            }
            println("[PointsManager] ✅ Sincronizado XP a la nube!")
        } catch (e: Exception) {
            println("[PointsManager] ❌ Error sincronizando a Supabase: ${e.message}")
        }
    }

    // ── Reset ──────────────────────────────────────────────────────────────

    fun reset() {
        settings.putInt(KEY_POINTS, 0)
        settings.putInt(KEY_TOTAL_EARNED, 0)
        settings.putInt(KEY_SCANS, 0)
        settings.putFloat(KEY_CO2, 0f)
        settings.putInt(KEY_STREAK, 0)
        settings.putInt(KEY_DAILY_SCANS, 0)
        settings.putString(KEY_LAST_SCAN_DATE, "")
        settings.putString(KEY_USER_ID, "")
        settings.remove("onboarding_completed") // Opcional, dependiendo de si queremos re-mostrar onboarding
    }
}
