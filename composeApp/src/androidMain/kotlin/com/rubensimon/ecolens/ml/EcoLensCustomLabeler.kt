package com.rubensimon.ecolens.ml

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import java.util.concurrent.TimeUnit

/**
 * Clasificador de imágenes personalizado basado en TFLite.
 * Gestiona la detección local de objetos mediante el modelo entrenado.
 */
object EcoLensCustomLabeler {

    const val MODEL_ASSET = "ecolens_model.tflite"
    private const val LABELS_ASSET = "labels.txt"
    private const val ANALYZE_TIMEOUT_MS = 900L

    @Volatile
    private var labeler: ImageLabeler? = null

    @Volatile
    private var modelPresent: Boolean? = null

    fun isAvailable(context: Context): Boolean {
        modelPresent?.let { return it }
        val ok = try {
            context.assets.open(MODEL_ASSET).use { true }
        } catch (_: Exception) {
            false
        }
        modelPresent = ok
        if (ok) println("[EcoLens TFLite] Modelo encontrado: $MODEL_ASSET")
        return ok
    }

    fun getClient(context: Context): ImageLabeler? {
        if (!isAvailable(context)) return null
        return labeler ?: synchronized(this) {
            labeler ?: run {
                val localModel = LocalModel.Builder()
                    .setAssetFilePath(MODEL_ASSET)
                    .build()
                val options = CustomImageLabelerOptions.Builder(localModel)
                    .setConfidenceThreshold(0.50f)
                    .build()
                ImageLabeling.getClient(options).also {
                    labeler = it
                    println("[EcoLens TFLite] Custom labeler inicializado")
                }
            }
        }
    }

    fun close() {
        labeler?.close()
        labeler = null
    }

    /**
     * @return Pair(nombre EcoDex en español, confianza) o null si no hay resultado fiable.
     */
    fun classify(
        context: Context,
        inputImage: InputImage,
        timeoutMs: Long = ANALYZE_TIMEOUT_MS
    ): Pair<String, Float>? {
        val client = getClient(context) ?: return null
        return try {
            val labels = Tasks.await(client.process(inputImage), timeoutMs, TimeUnit.MILLISECONDS)
            interpretLabels(labels)
        } catch (e: Exception) {
            println("[EcoLens TFLite] Error: ${e.message}")
            null
        }
    }

    fun interpretLabels(labels: List<ImageLabel>): Pair<String, Float>? {
        if (labels.isEmpty()) return null
        val sorted = labels.sortedByDescending { it.confidence }
        for (label in sorted) {
            val mapped = mapRawLabelToEcoDex(label.text) ?: continue
            if (label.confidence >= 0.45f) {
                println("[EcoLens TFLite] ${label.text} => $mapped (${(label.confidence * 100).toInt()}%)")
                return mapped to label.confidence
            }
        }
        return null
    }

    /** Mapea salida del TFLite (índice, nombre de clase o labels.txt) a nombre EcoDex. */
    fun mapRawLabelToEcoDex(raw: String): String? {
        val t = raw.lowercase().trim().replace('_', ' ')
        
        // Eliminar prefijos de número y espacios (por ejemplo, "3 orgánico" -> "orgánico")
        val clean = t.replace(Regex("^[0-9\\s]+"), "").trim()

        when (clean) {
            "envases", "envase" -> return "Envases"
            "papel y cartón", "papel y carton", "papel", "cartón", "carton" -> return "Papel y Cartón"
            "vidrio" -> return "Vidrio"
            "orgánico", "organico" -> return "Orgánico"
        }

        return when {
            t.contains("banana") || t.contains("platano") || t == "plátano" -> "Orgánico"
            t.contains("organic") || t.contains("food waste") || t.contains("compost") ||
                t.contains("restos") || t.contains("organi") -> "Orgánico"
            t == "envases" || t.startsWith("envase") -> "Envases"
            t.contains("papel") || t.contains("cartón") || t.contains("carton") -> "Papel y Cartón"
            t.contains("vidrio") || t.contains("glass") -> "Vidrio"
            t.contains("plastic bottle") || t.contains("bottle plastic") || t == "plastic bottle" -> "Envases"
            t.contains("glass bottle") || t.contains("bottle glass") -> "Vidrio"
            t.contains("can") || t.contains("tin") || t.contains("aluminum") -> "Envases"
            t.contains("cardboard") || t.contains("carton") || t.contains("box") -> "Papel y Cartón"
            t.contains("paper") || t.contains("newspaper") -> "Papel y Cartón"
            t.contains("plastic bag") || t.contains("bag plastic") -> "Envases"
            t.contains("brick") || t.contains("tetra") -> "Envases"
            t.contains("coffee") || t.contains("cafe") -> "Orgánico"
            t.contains("battery") || t.contains("pila") -> "Pilas y Baterías"
            t.contains("phone") || t.contains("mobile") -> "Móvil / Tablet"
            else -> null
        }
    }
}
