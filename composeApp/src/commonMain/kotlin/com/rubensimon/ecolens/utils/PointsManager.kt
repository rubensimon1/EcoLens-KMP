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

    private const val KEY_POINTS = "puntos"
    private const val KEY_TOTAL_EARNED = "total_puntos_ganados"
    private const val KEY_SCANS = "total_scans"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_CO2 = "co2_saved_total"
    private const val KEY_STREAK = "current_streak"
    private const val KEY_LAST_SCAN_DATE = "last_scan_date"
    private const val KEY_DAILY_SCANS = "daily_scans_count"
    private const val KEY_DAILY_MISSION_GOAL = "daily_mission_goal"
    
    private const val XP_PER_LEVEL = 1000 // Cada 1000 pts de por vida sube un nivel

    // ── Puntos ─────────────────────────────────────────────────────────────

    fun getPoints(): Int = settings.getInt(KEY_POINTS, 0)
    fun getTotalEarned(): Int = settings.getInt(KEY_TOTAL_EARNED, 0)

    fun addPoints(amount: Int, reason: String = "") {
        val current = getPoints()
        val currentTotal = getTotalEarned()
        
        settings.putInt(KEY_POINTS, current + amount)
        settings.putInt(KEY_TOTAL_EARNED, currentTotal + amount)
        
        println("[PointsManager] +$amount pts ($reason) → Total: ${current + amount}")
        autoSync()
    }
    
    // ── Niveles ────────────────────────────────────────────────────────────
    
    fun getLevel(): Int {
        return (getTotalEarned() / XP_PER_LEVEL) + 1
    }
    
    fun getProgressToNextLevel(): Float {
        val pointsInLevel = getTotalEarned() % XP_PER_LEVEL
        return pointsInLevel.toFloat() / XP_PER_LEVEL.toFloat()
    }
    
    fun getPointsNeededForNextLevel(): Int {
        return XP_PER_LEVEL - (getTotalEarned() % XP_PER_LEVEL)
    }

    fun setPoints(points: Int) {
        settings.putInt(KEY_POINTS, points)
    }

    fun subtractPoints(amount: Int): Boolean {
        val current = getPoints()
        return if (current >= amount) {
            settings.putInt(KEY_POINTS, current - amount)
            autoSync()
            true
        } else {
            false
        }
    }

    // ── Escaneos ───────────────────────────────────────────────────────────

    fun getTotalScans(): Int = settings.getInt(KEY_SCANS, 0)

    fun incrementScans(objectName: String = "Objeto", points: Int = 10) {
        val newTotal = getTotalScans() + 1
        settings.putInt(KEY_SCANS, newTotal)
        
        updateDailyStats()
        autoSync()
        recordActionInHistory(objectName, points)
    }

    private fun recordActionInHistory(objectName: String, points: Int) {
        val userId = getUserId()
        if (userId.isEmpty()) {
            println("[PointsManager] ⚠️ No se puede guardar historial: userId está vacío.")
            return
        }
        scope.launch {
            try {
                println("[PointsManager] Intentando guardar acción en Supabase para user: $userId")
                val item = com.rubensimon.ecolens.data.models.social.HistoryItemModel(
                    user_id = userId,
                    object_name = objectName,
                    points = points,
                    action_type = "scan"
                )
                client.from("historial_escaneos").insert(item)
                println("[PointsManager] ✅ Historial guardado correctamente: $objectName")
            } catch (e: Exception) {
                println("[PointsManager] ❌ Error crítico al guardar historial: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun updateDailyStats() {
        val today = TimeUtils.getCurrentIsoDate().substringBefore("T")
        val lastDate = settings.getString(KEY_LAST_SCAN_DATE, "")
        
        if (today != lastDate) {
            // Es un nuevo día
            val yesterday = TimeUtils.getYesterdayIsoDate().substringBefore("T")
            if (lastDate == yesterday) {
                // Racha continúa
                val currentStreak = settings.getInt(KEY_STREAK, 0)
                settings.putInt(KEY_STREAK, currentStreak + 1)
            } else {
                // Racha rota o primera vez
                settings.putInt(KEY_STREAK, 1)
            }
            settings.putString(KEY_LAST_SCAN_DATE, today)
            settings.putInt(KEY_DAILY_SCANS, 1)
        } else {
            // Mismo día
            val dailyScans = settings.getInt(KEY_DAILY_SCANS, 0)
            settings.putInt(KEY_DAILY_SCANS, dailyScans + 1)
        }
        
        checkDailyMission()
    }

    fun getStreak(): Int = settings.getInt(KEY_STREAK, 0)
    
    fun getDailyScans(): Int {
        val today = TimeUtils.getCurrentIsoDate().substringBefore("T")
        val lastDate = settings.getString(KEY_LAST_SCAN_DATE, "")
        
        if (today != lastDate) {
            // Es un nuevo día, reseteamos localmente si se consulta
            settings.putInt(KEY_DAILY_SCANS, 0)
            return 0
        }
        return settings.getInt(KEY_DAILY_SCANS, 0)
    }
    
    fun getDailyGoal(): Int {
        var goal = settings.getInt(KEY_DAILY_MISSION_GOAL, 3)
        if (goal == 0) goal = 3 // fallback
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
        settings.putInt(KEY_SCANS, count)
    }

    // ── CO2 ────────────────────────────────────────────────────────────────

    fun getCo2Saved(): Float = settings.getFloat(KEY_CO2, 0f)

    fun addCo2(amount: Float) {
        val current = getCo2Saved()
        settings.putFloat(KEY_CO2, current + amount)
    }

    // ── User ID ────────────────────────────────────────────────────────────

    fun getUserId(): String = settings.getString(KEY_USER_ID, "")

    fun setUserId(id: String) {
        settings.putString(KEY_USER_ID, id)
    }

    // ── Sincronización ─────────────────────────────────────────────────────

    /**
     * Sincroniza los puntos locales con Supabase de forma automática en background.
     */
    private fun autoSync() {
        val userId = getUserId()
        if (userId.isEmpty()) return
        scope.launch {
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
            @kotlinx.serialization.Serializable
            data class PointsRow(val puntos: Int = 0, val total_scans: Int = 0)
            val result = client.from("usuarios")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<PointsRow>()

            result?.let {
                settings.putInt(KEY_POINTS, it.puntos)
                settings.putInt(KEY_SCANS, it.total_scans)
                // CO2 se calcula localmente: total_scans * 0.5
                settings.putFloat(KEY_CO2, it.total_scans * 0.5f)
                println("[PointsManager] Loaded from Supabase: ${it.puntos} pts, ${it.total_scans} scans")
            }
        } catch (e: Exception) {
            println("[PointsManager] Error loadFromSupabase: ${e.message}")
        }
    }

    private suspend fun syncToSupabase(userId: String) {
        try {
            client.from("usuarios").update({
                set("puntos", getPoints())
                set("total_scans", getTotalScans())
            }) {
                filter { eq("id", userId) }
            }
            println("[PointsManager] ✅ Synced: ${getPoints()} pts")
        } catch (e: Exception) {
            println("[PointsManager] ❌ Sync error: ${e.message}")
        }
    }

    // ── Reset ──────────────────────────────────────────────────────────────

    fun reset() {
        settings.putInt(KEY_POINTS, 0)
        settings.putInt(KEY_SCANS, 0)
        settings.putFloat(KEY_CO2, 0f)
        settings.putInt(KEY_STREAK, 0)
        settings.putInt(KEY_DAILY_SCANS, 0)
        settings.putString(KEY_LAST_SCAN_DATE, "")
    }
}
