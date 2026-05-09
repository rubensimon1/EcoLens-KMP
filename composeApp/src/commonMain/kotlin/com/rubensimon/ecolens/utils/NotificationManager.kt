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
    private const val KEY_DYNAMIC_NOTIFICATIONS = "dynamic_notifications_json"

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // Clase interna para representar la notificación persistente
    @kotlinx.serialization.Serializable
    data class PersistedNotification(
        val id: String,
        val title: String,
        val description: String,
        val time: String,
        val isRead: Boolean = false
    )

    private val _notifications = MutableStateFlow<List<PersistedNotification>>(emptyList())
    val notifications: StateFlow<List<PersistedNotification>> = _notifications.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        val json = settings.getString(KEY_DYNAMIC_NOTIFICATIONS, "")
        if (json.isNotEmpty()) {
            try {
                val list = Json.decodeFromString<List<PersistedNotification>>(json)
                _notifications.value = list
            } catch (e: Exception) {
                _notifications.value = getDefaultNotifications()
            }
        } else {
            _notifications.value = getDefaultNotifications()
        }
        updateUnreadCount()
    }

    private fun getDefaultNotifications() = listOf(
        PersistedNotification("notif_record", "¡Nuevo Récord!", "Has superado tu media de escaneos semanales. ¡Sigue así!", "Hace 2h"),
        PersistedNotification("notif_level", "Logro Desbloqueado", "Has alcanzado el Nivel 2. ¡Felicidades!", "Ayer"),
        PersistedNotification("notif_community", "Comunidad", "@admin ha compartido una nueva idea de upcycling.", "Hace 2 días", isRead = true)
    )

    fun addNotification(title: String, description: String) {
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
        val updatedList = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updatedList
        saveNotifications(updatedList)
        updateUnreadCount()
    }

    private fun saveNotifications(list: List<PersistedNotification>) {
        val json = Json.encodeToString(list)
        settings.putString(KEY_DYNAMIC_NOTIFICATIONS, json)
    }

    fun updateUnreadCount() {
        _unreadCount.value = _notifications.value.count { !it.isRead }
    }
}
