package com.rubensimon.ecolens.data.models.auth

import kotlinx.serialization.Serializable

/**
 * Petición de registro de nuevo usuario en el backend FastAPI.
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val password: String
)

/**
 * Respuesta del endpoint /register.
 */
@Serializable
data class RegisterResponse(
    val message: String = "",
    val username: String = ""
)
