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
 * Repositorio para la gestión de usuarios y operaciones relacionadas con Supabase.
 * 
 * Este componente centraliza todas las llamadas a la base de datos y autenticación
 * para los perfiles de usuario, historial de escaneos y canje de recompensas.
 * 
 * Al ser una implementación KMP, es independiente de la plataforma (Android/iOS)
 * y utiliza corrutinas para operaciones asíncronas.
 */
class UserRepository {
    /** Cliente de Supabase configurado globalmente. */
    private val client = SupabaseClientProvider.client

    // ── Crear / Actualizar usuario ──────────────────────────────────────────

    /**
     * Crea un nuevo registro de usuario o actualiza las estadísticas de uno existente.
     * 
     * @param username Nombre de usuario único.
     * @param puntos Cantidad total de puntos acumulados.
     * @param totalScans Número total de escaneos realizados.
     * @return El objeto [UserModel] creado o actualizado, o null en caso de error.
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

    /**
     * Obtiene el ranking de usuarios con mayor puntuación.
     * 
     * @param limit Número máximo de usuarios a retornar (por defecto 10).
     * @return Lista de [UserModel] ordenada de mayor a menor puntuación.
     */
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

    /**
     * Busca un usuario por su nombre de usuario exacto.
     * 
     * @param username Nombre a buscar.
     * @return [UserModel] si existe, null en caso contrario.
     */
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

    /**
     * Recupera un usuario mediante su identificador único (UUID).
     * 
     * @param userId Identificador del usuario.
     * @return [UserModel] encontrado o null si no existe.
     */
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

    /**
     * Actualiza los puntos y estadísticas de escaneo de un usuario.
     * 
     * @param userId ID del usuario.
     * @param puntos Nueva puntuación total.
     * @param totalScans Nuevo total de escaneos.
     * @param co2Saved Cantidad de CO2 ahorrado (uso estadístico).
     * @return true si la actualización fue exitosa, false de lo contrario.
     */
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

    /**
     * Actualiza la información básica del perfil (nombre público y biografía).
     * 
     * @param userId ID del usuario.
     * @param displayName Nombre que se mostrará en la app.
     * @param bio Breve descripción o biografía.
     * @return true si se guardó correctamente.
     */
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

    /**
     * Actualiza la URL de la imagen de perfil en la base de datos.
     * 
     * @param userId ID del usuario.
     * @param url Enlace público a la imagen.
     * @return true si la operación fue exitosa.
     */
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

    /**
     * Actualiza las preferencias de notificaciones del usuario.
     * 
     * @param userId ID del usuario.
     * @param push Habilitar notificaciones push.
     * @param rewards Habilitar avisos de recompensas.
     * @param email Habilitar notificaciones por correo.
     * @return true si las preferencias se actualizaron correctamente.
     */
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

    /**
     * Recupera la lista de cupones disponibles en la tienda de recompensas.
     * 
     * @return Lista de [Coupon] disponibles.
     */
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

    /**
     * Registra el canje de un cupón por parte de un usuario.
     * 
     * @param redemption Modelo con los datos del canje (usuario, cupón, fecha).
     * @return true si el canje se registró correctamente en la base de datos.
     */
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

    /**
     * Valida el uso de un cupón previamente canjeado.
     * 
     * @param redemptionId ID único del canje (opcional si se usan filtros alternativos).
     * @param userId ID del usuario.
     * @param cuponId ID del cupón.
     * @param fechaCanje Fecha en la que se realizó el canje original.
     * @return true si el cupón se marcó como "usado" correctamente.
     */
    suspend fun validateRedemption(redemptionId: String?, userId: String, cuponId: String, fechaCanje: String): Boolean {
        return try {
            val response = client.from("cupones_canjeados")
                .update({
                    set("estado", "usado")
                    set("fecha_uso", com.rubensimon.ecolens.utils.TimeUtils.getCurrentIsoDate())
                }) {
                    select() // IMPORTANTE: Pedir que devuelva las filas actualizadas
                    filter { 
                        // Seguridad atómica: Solo validar si el estado actual es "activo"
                        eq("estado", "activo")
                        
                        if (redemptionId != null) {
                            eq("id", redemptionId)
                        } else {
                            eq("user_id", userId)
                            eq("cupon_id", cuponId)
                            eq("fecha_canje", fechaCanje)
                        }
                    }
                }

            
            // Verificamos si se actualizó alguna fila. Si la lista está vacía,
            // significa que el cupón ya estaba usado o no se encontró.
            val updatedRows = response.decodeList<RedemptionModel>()
            val isSuccess = updatedRows.isNotEmpty()
            
            if (isSuccess) {
                println("[UserRepository] ✅ Cupón validado (ID: $redemptionId)")
            } else {
                println("[UserRepository] ⚠️ Intento de validación fallido: El cupón ya estaba usado o no existe.")
            }
            
            isSuccess
        } catch (e: Exception) {
            println("[UserRepository] ❌ Error validateRedemption: ${e.message}")
            false
        }
    }


    // ── Upload de foto de perfil ─────────────────────────────────────────────

    /**
     * Sube una nueva foto de perfil a Supabase Storage y actualiza la URL en el perfil del usuario.
     * 
     * @param userId ID del usuario.
     * @param imageBytes Datos binarios de la imagen.
     * @return URL pública de la imagen subida o null si falla.
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

    /**
     * Obtiene el ID del usuario actualmente autenticado en la sesión.
     * 
     * @return UUID del usuario o null si no hay sesión iniciada.
     */
    suspend fun getCurrentSessionUserId(): String? {
        return try {
            client.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtiene el correo electrónico del usuario autenticado.
     * 
     * @return Email del usuario o null si no hay sesión.
     */
    suspend fun getCurrentUserEmail(): String? {
        return try {
            client.auth.currentSessionOrNull()?.user?.email
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Inicia sesión utilizando email y contraseña.
     * 
     * @param email Correo electrónico.
     * @param password Contraseña.
     * @return true si el inicio de sesión fue exitoso.
     */
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

    /**
     * Registra un nuevo usuario en Supabase Auth.
     * 
     * @param email Correo electrónico.
     * @param password Contraseña.
     * @return true si el registro fue exitoso.
     */
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

    /**
     * Cierra la sesión activa del usuario.
     */
    suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (e: Exception) {
            println("[UserRepository] Error signOut: ${e.message}")
        }
    }

    /**
     * Solicita un cambio de correo electrónico. Supabase enviará un correo de confirmación.
     * 
     * @param newEmail Nueva dirección de correo.
     * @return true si la solicitud fue enviada correctamente.
     */
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

    /**
     * Actualiza la contraseña del usuario autenticado.
     * 
     * @param newPassword Nueva contraseña.
     * @return true si la contraseña se actualizó correctamente.
     */
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

    /**
     * Obtiene la lista de todos los cupones canjeados por un usuario.
     * 
     * @param userId ID del usuario.
     * @return Lista de [RedemptionModel] ordenada por fecha descendente.
     */
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

    /**
     * Obtiene los canjes realizados en una fecha específica.
     * 
     * @param userId ID del usuario.
     * @param isoDate Fecha en formato ISO "YYYY-MM-DD".
     * @return Lista de [RedemptionModel] para esa fecha.
     */
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
     * Obtiene la actividad global reciente (historial de escaneos) para la comunidad.
     * 
     * @param limit Número máximo de items a recuperar.
     * @return Lista de [HistoryItemModel].
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
     * Obtiene la actividad global reciente unida con la información de perfil de los usuarios.
     * 
     * @param limit Número máximo de items.
     * @return Lista de pares [HistoryItemModel] y [UserModel].
     */
    suspend fun getGlobalActivityWithProfiles(limit: Int = 10): List<Pair<com.rubensimon.ecolens.data.models.social.HistoryItemModel, UserModel>> {
        return try {
            val result = client.from("historial_escaneos")
                .select(Columns.raw("*, usuarios(*)")) {
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
            
            val data = result.decodeList<com.rubensimon.ecolens.data.models.social.HistoryItemWithUser>()
            data.map { it.toPair() }
        } catch (e: Exception) {
            println("[UserRepository] ❌ Error getGlobalActivityWithProfiles: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Recupera el historial de escaneos completo de un usuario específico.
     * 
     * @param userId ID del usuario.
     * @return Lista de [HistoryItemModel] del usuario.
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

    /**
     * Obtiene un mapa de perfiles de usuario a partir de una lista de IDs.
     * Útil para resolver nombres y avatares en listas de actividad.
     * 
     * @param userIds Lista de UUIDs de usuarios.
     * @return Mapa donde la clave es el ID y el valor es un Par (Nombre, URL de Avatar).
     */
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

    /**
     * Obtiene el timestamp actual en formato ISO mediante [TimeUtils].
     */
    private fun currentTimestamp(): String {
        return com.rubensimon.ecolens.utils.TimeUtils.getCurrentIsoDate() 
    }
}
