package com.rubensimon.ecolens.data.models.social

import kotlinx.serialization.Serializable

@Serializable
data class NotificationModel(
    val id: String? = null,
    val user_id: String,
    val title: String,
    val description: String,
    val type: String = "info",
    val is_read: Boolean = false,
    val created_at: String? = null
)
