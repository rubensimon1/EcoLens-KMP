package com.rubensimon.ecolens

/**
 * Configuración global de la aplicación EcoLens.
 * Contiene las constantes y secretos necesarios para la conexión con servicios externos.
 */
object EcoLensConfig {
    /** URL del proyecto Supabase. Utiliza un fallback si no se encuentra en secretos. */
    val SUPABASE_URL: String = EcoLensSecrets.SUPABASE_URL.ifBlank { "https://example.supabase.co" }
    
    /** Clave anónima de Supabase para peticiones públicas. */
    val SUPABASE_KEY: String = EcoLensSecrets.SUPABASE_KEY.ifBlank { "missing_supabase_anon_key" }

    /** URL del servidor de Machine Learning para el reconocimiento de objetos. */
    val ML_BACKEND_URL: String = EcoLensSecrets.ML_BACKEND_URL.ifBlank { "https://example.com/" }
}

