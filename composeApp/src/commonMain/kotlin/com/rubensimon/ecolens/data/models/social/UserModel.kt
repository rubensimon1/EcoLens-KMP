package com.rubensimon.ecolens.data.models.social

import kotlinx.serialization.Serializable

/**
 * Modelo de datos que representa a un usuario en el sistema EcoLens.
 * 
 * Este modelo está mapeado directamente con la tabla `usuarios` de Supabase
 * y se utiliza para la persistencia y transferencia de perfiles de usuario.
 *
 * @property id Identificador único universal (UUID) generado por Supabase.
 * @property username Nombre de usuario único para identificación en la comunidad.
 * @property display_name Nombre público que se muestra en la interfaz.
 * @property puntos Puntos actuales disponibles para canjear por recompensas.
 * @property total_scans Contador total de objetos reciclados/escaneados.
 * @property total_xp Experiencia total acumulada (determina el nivel del usuario).
 * @property profile_picture_url Enlace a la imagen de avatar en Supabase Storage.
 * @property bio Breve descripción o biografía del usuario.
 * @property notify_push Estado de preferencia para notificaciones push en el dispositivo.
 * @property notify_rewards Estado de preferencia para avisos sobre nuevas recompensas.
 * @property notify_email Estado de preferencia para comunicaciones vía correo electrónico.
 * @property created_at Fecha de registro del usuario en formato ISO.
 * @property updated_at Fecha de la última actualización del perfil en formato ISO.
 */
@Serializable
data class UserModel(
    val id: String = "",
    val username: String,
    val display_name: String? = null,
    val puntos: Int = 0,
    val total_scans: Int = 0,
    val total_xp: Int = 0,
    val profile_picture_url: String? = null,
    val bio: String? = null,
    val notify_push: Boolean = true,
    val notify_rewards: Boolean = true,
    val notify_email: Boolean = false,
    val created_at: String = "",
    val updated_at: String = ""
)

