package com.rubensimon.ecolens.utils

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.rubensimon.ecolens.data.network.SupabaseClientProvider
import com.rubensimon.ecolens.data.models.social.NotificationModel
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

object NotificationManager {
    private val settings: Settings by lazy { Settings() }
    private var currentUserId: String = ""
    
    private fun getStorageKey() = "notifications_json_$currentUserId"

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _notifications = MutableStateFlow<List<PersistedNotification>>(emptyList())
    val notifications: StateFlow<List<PersistedNotification>> = _notifications.asStateFlow()

    @kotlinx.serialization.Serializable
    data class PersistedNotification(
        val id: String,
        val title: String,
        val description: String,
        val time: String,
        val isRead: Boolean = false
    )

    /**
     * Establece el usuario actual y carga sus notificaciones específicas.
     */
    fun setUser(userId: String) {
        println("[NotificationManager] 👤 Intentando establecer usuario: $userId")
        if (userId.isEmpty()) {
            println("[NotificationManager] ⚠️ ID de usuario vacío, abortando.")
            return
        }
        
        // Limpieza inmediata antes de cargar para evitar "fantasmas"
        _notifications.value = emptyList()
        currentUserId = userId
        
        println("[NotificationManager] 📂 Cargando notificaciones para la llave: ${getStorageKey()}")
        loadNotifications()
    }

    /**
     * Limpia el estado al cerrar sesión.
     */
    fun clearForLogout() {
        currentUserId = ""
        _notifications.value = emptyList()
        updateUnreadCount()
    }

    private val client = SupabaseClientProvider.client
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    private fun loadNotifications() {
        if (currentUserId.isEmpty()) return
        
        // 1. Carga local inmediata
        val json = settings.getString(getStorageKey(), "")
        if (json.isNotEmpty()) {
            try {
                val list = Json.decodeFromString<List<PersistedNotification>>(json)
                _notifications.value = list
            } catch (e: Exception) {
                _notifications.value = emptyList()
            }
        }
        
        // 2. Carga remota en segundo plano
        scope.launch {
            try {
                println("[NotificationManager] ☁️ Sincronizando con Supabase...")
                val remoteNotifs = client.from("notificaciones")
                    .select {
                        filter {
                            eq("user_id", currentUserId)
                        }
                    }
                    .decodeList<NotificationModel>()
                
                if (remoteNotifs.isNotEmpty()) {
                    val remoteAsPersisted = remoteNotifs.map { remote ->
                        PersistedNotification(
                            id = remote.id ?: "rem_${remote.created_at}",
                            title = remote.title,
                            description = remote.description,
                            time = formatTimestamp(remote.created_at),
                            isRead = remote.is_read
                        )
                    }
                    
                    // Fusionar (evitar duplicados)
                    val localList = _notifications.value
                    val localKeys = localList.map { "${it.title}|${it.description}" }.toSet()
                    val newOnes = remoteAsPersisted.filter { "${it.title}|${it.description}" !in localKeys }
                    
                    if (newOnes.isNotEmpty()) {
                        val merged = (localList + newOnes).sortedByDescending { it.time }
                        _notifications.value = merged
                        saveNotifications(merged)
                        println("[NotificationManager] ✅ Sincronizadas ${newOnes.size} nuevas notificaciones.")
                    }
                } else if (json.isEmpty()) {
                    generateWelcomeNotifications()
                }
            } catch (e: Exception) {
                println("[NotificationManager] ℹ️ Error sync nube: ${e.message}")
                if (json.isEmpty()) generateWelcomeNotifications()
            }
            updateUnreadCount()
        }
    }

    private fun generateWelcomeNotifications() {
        val welcomeList = listOf(
            PersistedNotification(
                id = "welcome_1",
                title = "¡Bienvenido a EcoLens! 🌿",
                description = "Tus notificaciones ahora se sincronizan en la nube. ¡Las verás en todos tus dispositivos!",
                time = "Recién ahora"
            )
        )
        _notifications.value = welcomeList
        saveNotifications(welcomeList)
    }

    private fun formatTimestamp(iso: String?): String {
        if (iso == null) return "Recién"
        return try {
            // Ejemplo iso: 2026-05-09T15:12:00Z
            val dateTime = iso.substringBefore(".").replace("Z", "")
            val parts = dateTime.split("T")
            if (parts.size == 2) {
                val dateParts = parts[0].split("-") // [2026, 05, 09]
                val timeParts = parts[1].split(":") // [15, 12, 00]
                "${dateParts[2]}/${dateParts[1]} ${timeParts[0]}:${timeParts[1]}"
            } else "Recién"
        } catch (e: Exception) { "Hoy" }
    }

    fun addNotification(title: String, description: String) {
        if (currentUserId.isEmpty()) return
        
        val newNotif = PersistedNotification(
            id = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString(),
            title = title,
            description = description,
            time = "Ahora"
        )
        
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, newNotif)
        _notifications.value = currentList
        saveNotifications(currentList)

        // Enviar a Supabase
        scope.launch {
            try {
                val model = com.rubensimon.ecolens.data.models.social.NotificationModel(
                    user_id = currentUserId,
                    title = title,
                    description = description
                )
                client.from("notificaciones").insert(model)
                println("[NotificationManager] ☁️ Notificación enviada a la nube.")
            } catch (e: Exception) {
                println("[NotificationManager] ⚠️ Error enviando a la nube: ${e.message}")
            }
        }
        updateUnreadCount()
    }

    fun markAllAsRead() {
        if (currentUserId.isEmpty()) return
        
        val updatedList = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updatedList
        saveNotifications(updatedList)
        updateUnreadCount()
    }

    private fun saveNotifications(list: List<PersistedNotification>) {
        if (currentUserId.isEmpty()) return
        val json = Json.encodeToString(list)
        println("[NotificationManager] 💾 Guardando ${list.size} notificaciones para el usuario $currentUserId")
        settings.putString(getStorageKey(), json)
    }

    fun updateUnreadCount() {
        _unreadCount.value = _notifications.value.count { !it.isRead }
    }
}
