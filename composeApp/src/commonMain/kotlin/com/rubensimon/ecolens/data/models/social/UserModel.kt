package com.rubensimon.ecolens.data.models.social

import kotlinx.serialization.Serializable

/**
 * Modelo de usuario en Supabase (tabla `usuarios`).
 * Serializable con kotlinx.serialization para Postgrest-kt.
 */
@Serializable
data class UserModel(
    val id: String = "",
    val username: String,
    val display_name: String? = null,
    val puntos: Int = 0,
    val total_scans: Int = 0,
    val profile_picture_url: String? = null,
    val bio: String? = null,
    val notify_push: Boolean = true,
    val notify_rewards: Boolean = true,
    val notify_email: Boolean = false,
    val created_at: String = "",
    val updated_at: String = ""
)
