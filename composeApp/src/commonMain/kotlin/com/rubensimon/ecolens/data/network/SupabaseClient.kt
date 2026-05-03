package com.rubensimon.ecolens.data.network

import com.rubensimon.ecolens.EcoLensConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Cliente Supabase singleton para toda la aplicación KMP.
 *
 * Supabase-kt v3.x es completamente compatible con Kotlin Multiplatform.
 * Se utiliza en commonMain y funciona en Android e iOS.
 *
 * Las credenciales se obtienen de [EcoLensConfig].
 * IMPORTANTE: Actualiza EcoLensConfig.kt con tus credenciales reales.
 */
object SupabaseClientProvider {

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = EcoLensConfig.SUPABASE_URL,
            supabaseKey = EcoLensConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth)
            install(Storage)
            install(Realtime)
        }
    }
}
