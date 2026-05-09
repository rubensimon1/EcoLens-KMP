package com.rubensimon.ecolens.data.repository

import com.rubensimon.ecolens.data.models.items.Coupon
import com.rubensimon.ecolens.data.models.social.RedemptionModel
import com.rubensimon.ecolens.data.models.social.UserModel
import com.rubensimon.ecolens.data.models.social.HistoryItemWithUser
import com.rubensimon.ecolens.data.models.social.HistoryItemModel
import com.rubensimon.ecolens.data.network.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.datetime.Clock

/**
 * Repositorio KMP para operaciones de usuario en Supabase.
 *
 * Migrado de Android (Context-free). Todas las operaciones son suspend
 * y se ejecutan en coroutines. No depende de Android APIs.
 *
 * ## Cambios respecto al original Android
/**
 * Repositorio de usuarios para la gestión de datos en Supabase.
 */
 * - `SimpleDateFormat` → `kotlinx-datetime`
 * - `SupabaseClient.client` → `SupabaseClientProvider.client`
 * - `gotrue` → `auth` (supabase-kt v3 renaming)
 * - Eliminado `context: Context` de todos los métodos
 */
class UserRepository {
    private val client = SupabaseClientProvider.client

    // ── Crear / Actualizar usuario ──────────────────────────────────────────

    /**
     * Crea un nuevo usuario o actualiza sus puntos y escaneos.
     */
    suspend fun createOrUpdateUser(username: String, puntos: Int, totalScans: Int): UserModel? {
        return try {
            val existing = client.from("usuarios")
                .select {
                    filter { eq("username", username) }
                }
                .decodeSingleOrNull<UserModel>()

            if (existing != null) {
                client.from("usuarios")
                    .update({
                        set("puntos", puntos)
                        set("total_scans", totalScans)
                        set("updated_at", currentTimestamp())
                    }) {
                        filter { eq("id", existing.id) }
                    }
                println("[UserRepository] Updated ${existing.username}: puntos=$puntos, scans=$totalScans")
                existing.copy(puntos = puntos, total_scans = totalScans)
            } else {
                client.from("usuarios")
                    .insert(
                        UserModel(
                            username = username,
                            display_name = username,
                            puntos = puntos,
                            total_scans = totalScans
                        )
                    )
                    .decodeSingle<UserModel>()
            }
        } catch (e: Exception) {
            println("[UserRepository] Error createOrUpdateUser: ${e.message}")
            null
        }
    }

    // ── Lectura de usuarios ─────────────────────────────────────────────────

    suspend fun getTopUsers(limit: Int = 10): List<UserModel> {
        return try {
            client.from("usuarios")
                .select {
                    order("puntos", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<UserModel>()
        } catch (e: Exception) {
            println("[UserRepository] Error getTopUsers: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchUserByUsername(username: String): UserModel? {
        return try {
            client.from("usuarios")
                .select {
                    filter { eq("username", username) }
                }
                .decodeSingleOrNull<UserModel>()
        } catch (e: Exception) {
            println("[UserRepository] Error searchUserByUsername: ${e.message}")
            null
        }
    }

    suspend fun getUserById(userId: String): UserModel? {
        return try {
            client.from("usuarios")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<UserModel>()
        } catch (e: Exception) {
            println("[UserRepository] Error getUserById: ${e.message}")
            null
        }
    }

    // ── Actualización de perfil ─────────────────────────────────────────────

    suspend fun updatePoints(userId: String, puntos: Int, totalScans: Int, co2Saved: Float): Boolean {
        return try {
            client.from("usuarios").update({
                set("puntos", puntos)
                set("total_scans", totalScans)
                set("updated_at", currentTimestamp())
            }) {
                filter { eq("id", userId) }
            }
            true
        } catch (e: Exception) {
            println("[UserRepository] Error updatePoints: ${e.message}")
            false
        }
    }

    suspend fun updateProfileInfo(
        userId: String,
        displayName: String?,
        bio: String?
    ): Boolean {
        return try {
            client.from("usuarios").update({
                displayName?.let { set("display_name", it) }
                bio?.let { set("bio", it) }
                set("updated_at", currentTimestamp())
            }) {
                filter { eq("id", userId) }
            }
            true
        } catch (e: Exception) {
            println("[UserRepository] Error updateProfileInfo: ${e.message}")
            false
        }
    }

    suspend fun updateProfilePictureUrl(userId: String, url: String): Boolean {
        return try {
            client.from("usuarios").update({
                set("profile_picture_url", url)
                set("updated_at", currentTimestamp())
            }) {
                filter { eq("id", userId) }
            }
            true
        } catch (e: Exception) {
            println("[UserRepository] Error updateProfilePictureUrl: ${e.message}")
            false
        }
    }

    suspend fun updateNotificationPreferences(
        userId: String,
        push: Boolean,
        rewards: Boolean,
        email: Boolean
    ): Boolean {
        return try {
            client.from("usuarios").update({
                set("notify_push", push)
                set("notify_rewards", rewards)
                set("notify_email", email)
                set("updated_at", currentTimestamp())
            }) {
                filter { eq("id", userId) }
            }
            true
        } catch (e: Exception) {
            println("[UserRepository] Error updateNotificationPreferences: ${e.message}")
            false
        }
    }

    // ── Cupones ─────────────────────────────────────────────────────────────

    suspend fun getCouponsFromDb(): List<Coupon> {
        return try {
            // La tabla 'cupones_tienda' contiene las recompensas disponibles
            client.from("cupones_tienda")
                .select()
                .decodeList<Coupon>()
        } catch (e: Exception) {
            println("[UserRepository] Error getCouponsFromDb: ${e.message}")
            emptyList()
        }
    }

    suspend fun redeemCoupon(redemption: RedemptionModel): Boolean {
        return try {
            client.from("cupones_canjeados").insert(redemption)
            println("[UserRepository] ✅ Cupón canjeado: ${redemption.cupon_id}")
            true
        } catch (e: Exception) {
            println("[UserRepository] ❌ Error redeemCoupon (id=${redemption.cupon_id}): ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun validateRedemption(redemptionId: String?, userId: String, cuponId: String, fechaCanje: String): Boolean {
        return try {
            client.from("cupones_canjeados")
                .update({
                    set("estado", "usado")
                    set("fecha_uso", com.rubensimon.ecolens.utils.TimeUtils.getCurrentIsoDate())
                }) {
                    filter { 
                        if (redemptionId != null) {
                            eq("id", redemptionId)
                        } else {
                            eq("user_id", userId)
                            eq("cupon_id", cuponId)
                            eq("fecha_canje", fechaCanje)
                        }
                    }
                }
            println("[UserRepository] ✅ Cupón validado (ID: $redemptionId)")
            true
        } catch (e: Exception) {
            println("[UserRepository] ❌ Error validateRedemption: ${e.message}")
            false
        }
    }

    // ── Upload de foto de perfil ─────────────────────────────────────────────

    /**
     * Sube una imagen a Supabase Storage.
     * @param userId ID del usuario (usado como nombre de archivo)
     * @param imageBytes Bytes de la imagen (JPEG comprimido)
     * @return URL pública de la imagen o null si falla
     */
    suspend fun uploadProfilePicture(userId: String, imageBytes: ByteArray): String? {
        return try {
            val bucket = client.storage["profile-pics"]
            // Usamos un timestamp para que la URL sea siempre distinta y fuerce el refresco
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val fileName = "${userId}_$timestamp.jpg"
            
            println("[UserRepository] Subiendo nueva foto: $fileName")
            bucket.upload(fileName, imageBytes) {
                upsert = true
            }
            
            val publicUrl = bucket.publicUrl(fileName)
            
            // Guardar la nueva URL en la tabla de usuarios
            client.from("usuarios").update({
                set("profile_picture_url", publicUrl)
                set("updated_at", currentTimestamp())
            }) {
                filter { eq("id", userId) }
            }
            
            println("[UserRepository] Foto actualizada en DB: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            println("[UserRepository] Error uploadProfilePicture: ${e.message}")
            null
        }
    }

    // ── Autenticación ───────────────────────────────────────────────────────

    suspend fun getCurrentSessionUserId(): String? {
        return try {
            client.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCurrentUserEmail(): String? {
        return try {
            client.auth.currentSessionOrNull()?.user?.email
        } catch (e: Exception) {
            null
        }
    }

    suspend fun signIn(email: String, password: String): Boolean {
        return try {
            client.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            println("[UserRepository] Error signIn: ${e.message}")
            false
        }
    }

    suspend fun signUp(email: String, password: String): Boolean {
        return try {
            client.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            println("[UserRepository] Error signUp: ${e.message}")
            false
        }
    }

    suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (e: Exception) {
            println("[UserRepository] Error signOut: ${e.message}")
        }
    }

    suspend fun updateUserEmail(newEmail: String): Boolean {
        return try {
            // En Supabase v3, updateUser envía un correo de confirmación al nuevo email
            client.auth.updateUser {
                email = newEmail
            }
            println("[UserRepository] ✉️ Solicitud de cambio de email enviada a: $newEmail")
            true
        } catch (e: Exception) {
            println("[UserRepository] ❌ Error updateEmail: ${e.message}")
            false
        }
    }

    suspend fun updateUserPassword(newPassword: String): Boolean {
        return try {
            client.auth.updateUser {
                password = newPassword
            }
            println("[UserRepository] 🔑 Contraseña actualizada en Supabase")
            true
        } catch (e: Exception) {
            println("[UserRepository] ❌ Error updatePassword: ${e.message}")
            false
        }
    }

    suspend fun getRedemptions(userId: String): List<RedemptionModel> {
        return try {
            client.from("cupones_canjeados")
                .select {
                    filter { eq("user_id", userId) }
                    order("fecha_canje", Order.DESCENDING)
                }
                .decodeList<RedemptionModel>()
        } catch (e: Exception) {
            println("[UserRepository] Error getRedemptions: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRedemptionsForDate(userId: String, isoDate: String): List<RedemptionModel> {
        return try {
            // isoDate formato "YYYY-MM-DD"
            client.from("cupones_canjeados")
                .select {
                    filter { 
                        eq("user_id", userId) 
                        gte("fecha_canje", "${isoDate}T00:00:00")
                        lte("fecha_canje", "${isoDate}T23:59:59")
                    }
                }
                .decodeList<RedemptionModel>()
        } catch (e: Exception) {
            println("[UserRepository] Error getRedemptionsForDate: ${e.message}")
            emptyList()
        }
    }

    /**
     * Obtiene la actividad global reciente para el feed de comunidad.
     */
    suspend fun getGlobalActivity(limit: Int = 10): List<com.rubensimon.ecolens.data.models.social.HistoryItemModel> {
        return try {
            println("[UserRepository] Consultando actividad global en Supabase...")
            val list = client.from("historial_escaneos")
                .select {
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<com.rubensimon.ecolens.data.models.social.HistoryItemModel>()
            println("[UserRepository] Actividad global recibida: ${list.size} items")
            list
        } catch (e: Exception) {
            println("[UserRepository] ❌ Error getGlobalActivity: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Obtiene la actividad global reciente junto con la información de perfil de los usuarios
     * en una sola consulta optimizada (Join).
     */
    suspend fun getGlobalActivityWithProfiles(limit: Int = 10): List<Pair<com.rubensimon.ecolens.data.models.social.HistoryItemModel, UserModel>> {
        return try {
            // Seleccionamos campos de historial y unimos con la tabla usuarios
            // Nota: El formato de select para joins en supabase-kt es "*, usuarios(*)"
            val result = client.from("historial_escaneos")
                .select(Columns.raw("*, usuarios(*)")) {
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
            
            // Decodificamos la respuesta. Supabase-kt permite decodificar estructuras anidadas si los modelos coinciden
            // Si no, podemos usar la respuesta cruda y mapear manualmente
            val data = result.decodeList<com.rubensimon.ecolens.data.models.social.HistoryItemWithUser>()
            data.map { it.toPair() }
        } catch (e: Exception) {
            println("[UserRepository] ❌ Error getGlobalActivityWithProfiles: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Obtiene el historial completo de un usuario específico.
     */
    suspend fun getUserHistory(userId: String): List<com.rubensimon.ecolens.data.models.social.HistoryItemModel> {
        return try {
            client.from("historial_escaneos")
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<com.rubensimon.ecolens.data.models.social.HistoryItemModel>()
        } catch (e: Exception) {
            println("[UserRepository] ❌ Error getUserHistory: ${e.message}")
            emptyList()
        }
    }

    suspend fun getUserProfilesMap(userIds: List<String>): Map<String, Pair<String, String?>> {
        return try {
            val users = client.from("usuarios")
                .select {
                    filter {
                        isIn("id", userIds)
                    }
                }
                .decodeList<UserModel>()
            users.associate { it.id to ((it.display_name ?: it.username) to it.profile_picture_url) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun currentTimestamp(): String {
        return com.rubensimon.ecolens.utils.TimeUtils.getCurrentIsoDate() 
    }
}
