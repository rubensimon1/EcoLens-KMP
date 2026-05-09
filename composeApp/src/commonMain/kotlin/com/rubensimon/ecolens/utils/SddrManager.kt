package com.rubensimon.ecolens.utils

import com.rubensimon.ecolens.data.network.SupabaseClientProvider
import com.russhwolf.settings.Settings
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gestor del Sistema de Depósito, Devolución y Retorno (SDDR).
 */
@kotlinx.serialization.Serializable
data class SddrHistoryRow(
    val user_id: String? = null, 
    val title: String, 
    val amount: Float,
    @kotlinx.serialization.SerialName("created_at")
    val date: String? = null
)

@kotlinx.serialization.Serializable
data class SddrUserData(
    val sddr_balance: Float = 0f, 
    val sddr_containers: Int = 0
)

object SddrManager {

    private val settings: Settings by lazy { Settings() }
    private val client = SupabaseClientProvider.client
    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: kotlinx.coroutines.Job? = null

    private const val KEY_SDDR_BALANCE = "sddr_balance"
    private const val KEY_SDDR_TOTAL_RECOVERED = "sddr_total_recovered"
    private const val KEY_SDDR_CONTAINERS_COUNT = "sddr_containers_count"
    private const val KEY_SDDR_HISTORY = "sddr_history_list"
    private const val KEY_USER_ID = "user_id"

    private val _balance = MutableStateFlow(settings.getFloat(KEY_SDDR_BALANCE, 0f))
    val balance: StateFlow<Float> = _balance.asStateFlow()

    private val _totalRecovered = MutableStateFlow(settings.getFloat(KEY_SDDR_TOTAL_RECOVERED, 0f))
    val totalRecovered: StateFlow<Float> = _totalRecovered.asStateFlow()

    private val _containersCount = MutableStateFlow(settings.getInt(KEY_SDDR_CONTAINERS_COUNT, 0))
    val containersCount: StateFlow<Int> = _containersCount.asStateFlow()

    private val _history = MutableStateFlow<List<SddrHistoryItem>>(emptyList())
    val history: StateFlow<List<SddrHistoryItem>> = _history.asStateFlow()

    fun setUser(userId: String) {
        if (userId.isEmpty()) return
        
        // Limpieza inmediata de RAM para evitar ver datos del usuario anterior
        _balance.value = 0f
        _totalRecovered.value = 0f
        _containersCount.value = 0
        _history.value = emptyList()
        
        // Cargar datos del nuevo usuario
        _balance.value = settings.getFloat(KEY_SDDR_BALANCE + "_" + userId, 0f)
        _totalRecovered.value = settings.getFloat(KEY_SDDR_TOTAL_RECOVERED + "_" + userId, 0f)
        _containersCount.value = settings.getInt(KEY_SDDR_CONTAINERS_COUNT + "_" + userId, 0)
        _history.value = loadLocalHistory(userId)
        
        scope.launch { 
            loadFromSupabase(userId)
            fetchCloudHistory()
        }
    }

    init {
        val userId = settings.getString(KEY_USER_ID, "")
        if (userId.isNotEmpty()) {
            setUser(userId)
        }
    }

    const val DEPOSIT_VALUE = 0.10f

    fun getBalance(): Float {
        val userId = settings.getString(KEY_USER_ID, "")
        return if (userId.isEmpty()) 0f else settings.getFloat(KEY_SDDR_BALANCE + "_" + userId, 0f)
    }

    fun getTotalRecovered(): Float {
        val userId = settings.getString(KEY_USER_ID, "")
        return if (userId.isEmpty()) 0f else settings.getFloat(KEY_SDDR_TOTAL_RECOVERED + "_" + userId, 0f)
    }

    fun getContainersCount(): Int {
        val userId = settings.getString(KEY_USER_ID, "")
        return if (userId.isEmpty()) 0 else settings.getInt(KEY_SDDR_CONTAINERS_COUNT + "_" + userId, 0)
    }

    fun redeemVoucher(qrCode: String): Boolean {
        val userId = settings.getString(KEY_USER_ID, "")
        println("[SddrManager] 🔍 Intentando canjear código para usuario [$userId]: $qrCode")
        
        if (userId.isEmpty()) {
            println("[SddrManager] ❌ ERROR: No hay sesión de usuario activa. No se puede canjear.")
            return false
        }
        
        return try {
            if (qrCode.startsWith("SDDR|")) {
                val parts = qrCode.split("|")
                if (parts.size >= 3) {
                    val value = parts[1].toFloatOrNull() ?: DEPOSIT_VALUE
                    val count = parts[2].toIntOrNull() ?: 1
                    val totalValue = value * count
                    
                    println("[SddrManager] ✅ Código válido: $count envases, total $totalValue€")

                    val newBalance = getBalance() + totalValue
                    val newTotal = getTotalRecovered() + totalValue
                    val newCount = getContainersCount() + count
                    
                    settings.putFloat(KEY_SDDR_BALANCE + "_" + userId, newBalance)
                    settings.putFloat(KEY_SDDR_TOTAL_RECOVERED + "_" + userId, newTotal)
                    settings.putInt(KEY_SDDR_CONTAINERS_COUNT + "_" + userId, newCount)
                    
                    _balance.value = newBalance
                    _totalRecovered.value = newTotal
                    _containersCount.value = newCount
                    
                    saveToHistory(userId, "Vale SDDR ($count envases)", totalValue)
                    PointsManager.addPoints(count * 50, "sddr_bonus")
                    
                    autoSync()
                    true
                } else false
            } else false
        } catch (e: Exception) {
            println("[SddrManager] ❌ Error crítico en redeemVoucher: ${e.message}")
            false
        }
    }

    private fun saveToHistory(userId: String, title: String, amount: Float) {
        if (userId.isEmpty()) return
        
        val prev = settings.getString(KEY_SDDR_HISTORY + "_" + userId, "")
        val fecha = TimeUtils.getCurrentIsoDate().substringBefore("T")
        val entry = "$title|$amount|$fecha"
        val updated = if (prev.isEmpty()) entry else "$entry;$prev"
        settings.putString(KEY_SDDR_HISTORY + "_" + userId, updated)
        
        _history.value = loadLocalHistory(userId)

        scope.launch {
            try {
                client.from("historial_sddr").insert(SddrHistoryRow(userId, title, amount))
                HistoryManager.addHistoryItem(objectName = title, points = 50, userId = userId)
                fetchCloudHistory()
            } catch (e: Exception) {
                println("[SddrManager] ❌ Error Supabase Historial: ${e.message}")
            }
        }
    }

    private fun loadLocalHistory(userId: String): List<SddrHistoryItem> {
        if (userId.isEmpty()) return emptyList()
        val raw = settings.getString(KEY_SDDR_HISTORY + "_" + userId, "")
        if (raw.isEmpty()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 3) {
                SddrHistoryItem(parts[0], parts[1].toFloatOrNull() ?: 0f, parts[2])
            } else null
        }
    }

    fun getHistory(): List<SddrHistoryItem> = _history.value

    private fun autoSync() {
        val userId = settings.getString(KEY_USER_ID, "")
        if (userId.isEmpty()) return
        
        syncJob?.cancel()
        syncJob = scope.launch {
            // Esperamos 2 segundos sin actividad para enviar (Debounce)
            kotlinx.coroutines.delay(2000)
            try {
                println("[SddrManager] 🚀 Sincronizando balance con Supabase...")
                client.from("usuarios").update({
                    set("sddr_balance", getBalance())
                    set("sddr_containers", getContainersCount())
                }) {
                    filter { eq("id", userId) }
                }
                println("[SddrManager] ✅ Sincronización exitosa")
            } catch (e: Exception) {
                println("[SddrManager] ❌ Error Sincronización: ${e.message}")
            }
        }
    }

    suspend fun loadFromSupabase(userId: String) {
        try {
            val result = client.from("usuarios")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<SddrUserData>()

            result?.let {
                settings.putFloat(KEY_SDDR_BALANCE + "_" + userId, it.sddr_balance)
                settings.putInt(KEY_SDDR_CONTAINERS_COUNT + "_" + userId, it.sddr_containers)
                
                _balance.value = it.sddr_balance
                _totalRecovered.value = it.sddr_balance 
                _containersCount.value = it.sddr_containers
                println("[SddrManager] ☁️ Datos cargados desde Supabase para $userId: ${it.sddr_balance}€, ${it.sddr_containers} envases")
            }
        } catch (e: Exception) {
            println("[SddrManager] ❌ Error cargando desde Supabase: ${e.message}")
        }
    }

    fun fetchCloudHistory() {
        val userId = settings.getString(KEY_USER_ID, "")
        if (userId.isEmpty()) return
        
        scope.launch {
            try {
                val results = client.from("historial_sddr")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<SddrHistoryRow>()
                
                _history.value = results.map { 
                    SddrHistoryItem(
                        it.title, 
                        it.amount, 
                        (it.date ?: "Hoy").substringBefore("T")
                    ) 
                }
                println("[SddrManager] ☁️ Historial sincronizado (${results.size} elementos)")
            } catch (e: Exception) {
                println("[SddrManager] ❌ Error fetchCloudHistory: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

data class SddrHistoryItem(val title: String, val amount: Float, val date: String)
