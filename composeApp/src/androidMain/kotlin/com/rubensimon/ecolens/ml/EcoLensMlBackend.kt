package com.rubensimon.ecolens.ml

import android.graphics.Bitmap
import com.rubensimon.ecolens.EcoLensSecrets
import com.rubensimon.ecolens.utils.PointsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Mismo endpoint que iOS: POST {ML_BACKEND_URL}predict con imagen multipart.
 * Útil si tu TFLite está en un servidor Python en lugar de embebido en la app.
 */
object EcoLensMlBackend {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun predict(bitmap: Bitmap): Triple<String, Int, String>? = withContext(Dispatchers.IO) {
        val base = EcoLensSecrets.ML_BACKEND_URL.trim().trimEnd('/')
        if (base.isBlank() || base.contains("example.com")) return@withContext null

        val url = if (base.endsWith("/predict")) base else "$base/predict"
        try {
            val jpeg = ByteArrayOutputStream().apply {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, this)
            }.toByteArray()

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "scan.jpg",
                    jpeg.toRequestBody("image/jpeg".toMediaType())
                )
                .addFormDataPart("username", PointsManager.getUserId())
                .build()

            val request = Request.Builder().url(url).post(body).build()
            val response = http.newCall(request).execute()
            val raw = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) {
                println("[ML Backend] HTTP ${response.code}: $raw")
                return@withContext null
            }

            val json = JSONObject(raw)
            val label = json.optString("label", "").takeIf { it.isNotBlank() && it != "null" }
                ?: return@withContext null
            val points = json.optInt("points_earned", 10)
            val message = json.optString("message", "Objeto identificado correctamente")
            println("[ML Backend] label=$label, pts=$points")
            Triple(label, points, message)
        } catch (e: Exception) {
            println("[ML Backend] Error: ${e.message}")
            null
        }
    }
}
