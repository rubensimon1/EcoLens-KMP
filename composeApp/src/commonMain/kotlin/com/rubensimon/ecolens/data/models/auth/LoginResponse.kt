package com.rubensimon.ecolens.data.models.auth

import kotlinx.serialization.Serializable

/**
 * Respuesta del endpoint /token tras autenticación exitosa.
 * Contiene el token JWT de sesión.
 */
@Serializable
data class LoginResponse(
    val access_token: String,
    val token_type: String
)
