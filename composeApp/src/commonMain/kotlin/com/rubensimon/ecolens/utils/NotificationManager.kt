package com.rubensimon.ecolens.utils

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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

    private fun loadNotifications() {
        if (currentUserId.isEmpty()) return
        
        val json = settings.getString(getStorageKey(), "")
        println("[NotificationManager] 📄 JSON cargado para $currentUserId: ${if(json.isEmpty()) "Vacío" else "Con contenido"}")
        
        if (json.isNotEmpty()) {
            try {
                val list = Json.decodeFromString<List<PersistedNotification>>(json)
                println("[NotificationManager] ✅ Se han cargado ${list.size} notificaciones.")
                _notifications.value = list
            } catch (e: Exception) {
                println("[NotificationManager] ❌ Error al decodificar: ${e.message}")
                _notifications.value = emptyList()
            }
        } else {
            println("[NotificationManager] ℹ️ No hay notificaciones guardadas. Cuenta nueva.")
            _notifications.value = emptyList()
        }
        updateUnreadCount()
    }

    fun addNotification(title: String, description: String) {
        if (currentUserId.isEmpty()) return
        
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val newNotif = PersistedNotification(
            id = "notif_$now",
            title = title,
            description = description,
            time = "Ahora mismo"
        )
        val updatedList = listOf(newNotif) + _notifications.value
        _notifications.value = updatedList
        saveNotifications(updatedList)
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
