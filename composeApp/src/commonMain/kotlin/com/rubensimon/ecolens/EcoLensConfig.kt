package com.rubensimon.ecolens

object EcoLensConfig {
    // Fallbacks para evitar crash si local.properties está vacío.
    val SUPABASE_URL: String = EcoLensSecrets.SUPABASE_URL.ifBlank { "https://example.supabase.co" }
    val SUPABASE_KEY: String = EcoLensSecrets.SUPABASE_KEY.ifBlank { "missing_supabase_anon_key" }

    val ML_BACKEND_URL: String = EcoLensSecrets.ML_BACKEND_URL.ifBlank { "https://example.com/" }
}
