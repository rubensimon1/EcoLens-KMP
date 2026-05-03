package com.rubensimon.ecolens.ui.screens.features

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.rubensimon.ecolens.EcoLensConfig
import com.rubensimon.ecolens.ui.components.EcoColors
import com.rubensimon.ecolens.ui.components.GlassButton
import com.rubensimon.ecolens.utils.HistoryManager
import com.rubensimon.ecolens.utils.PlatformAudio
import com.rubensimon.ecolens.utils.PointsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.concurrent.Executors

/**
 * Implementación Android de PlatformCameraView usando CameraX y ML Kit local.
 */
@Composable
actual fun PlatformCameraView(
    modifier: Modifier,
    onScanComplete: (objectName: String, points: Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isScanning by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(modifier = modifier) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        println("[CameraX] Error: ${e.message}")
                    }
                }, context.mainExecutor)
                previewView
            }
        )
        }

        // ── Overlay resultado ────────────────────────────────────────────────
        if (resultMessage.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                color = EcoColors.CardBackground.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = resultMessage,
                    color = EcoColors.TextPrimary,
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Botones de acción ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassButton(
                onClick = {
                    if (!isScanning) {
                        isScanning = true
                        resultMessage = "⏳ Analizando con IA local..."
                        captureAndPredictLocal(
                            imageCapture = imageCapture,
                            onResult = { label, points, message ->
                                resultMessage = "✅ $label (+$points pts)\n$message"
                                PointsManager.addPoints(points, "scan")
                                PointsManager.incrementScans(label, points)
                                PlatformAudio.playSuccess()
                                onScanComplete(label, points)
                                isScanning = false
                            },
                            onError = { err ->
                                resultMessage = "❌ $err"
                                isScanning = false
                            }
                        )
                    }
                },
                modifier = Modifier.size(72.dp),
                enabled = !isScanning
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Escanear", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

/**
 * Nueva función que usa Google ML Kit para reconocimiento LOCAL.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun captureAndPredictLocal(
    imageCapture: ImageCapture?,
    onResult: (label: String, points: Int, message: String) -> Unit,
    onError: (err: String) -> Unit
) {
    val capture = imageCapture ?: run {
        onError("Cámara no disponible")
        return
    }

    capture.takePicture(Executors.newSingleThreadExecutor(), object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        // Lógica de mapeo de etiquetas de Google a categorías de reciclaje con umbral de confianza
                        var detectedLabel = "Objeto"
                        var points = 10
                        var message = "¡Buen trabajo reciclando!"
                        var highestConfidence = 0.0f

                        // Filtramos solo etiquetas con confianza > 55%
                        val reliableLabels = labels.filter { it.confidence > 0.55f }
                        
                        if (reliableLabels.isEmpty()) {
                            detectedLabel = "Objeto"
                            message = "No estoy muy seguro de qué es esto. ¡Asegúrate de que haya buena luz!"
                        } else {
                            // Analizamos las top 5 etiquetas confiables buscando palabras clave
                            val allLabelTexts = reliableLabels.take(5).joinToString(" ") { it.text.lowercase() }
                            val bestLabel = reliableLabels.first()
                            highestConfidence = bestLabel.confidence
                            
                            println("[MLKit] Etiquetas confiables: $allLabelTexts (Top: ${bestLabel.text} @ $highestConfidence)")

                            when {
                                allLabelTexts.contains("bottle") || allLabelTexts.contains("plastic") || allLabelTexts.contains("pet") -> {
                                    detectedLabel = "Botella Plástico"
                                    points = 25
                                    message = "Detectada botella de plástico. Va al contenedor AMARILLO."
                                }
                                allLabelTexts.contains("can") || allLabelTexts.contains("tin") || allLabelTexts.contains("metal") || allLabelTexts.contains("aluminum") -> {
                                    detectedLabel = "Lata / Metal"
                                    points = 30
                                    message = "Detectado metal/lata. Va al contenedor AMARILLO."
                                }
                                allLabelTexts.contains("paper") || allLabelTexts.contains("cardboard") || allLabelTexts.contains("box") || allLabelTexts.contains("newspaper") -> {
                                    detectedLabel = "Papel / Cartón"
                                    points = 20
                                    message = "Detectado papel/cartón. Va al contenedor AZUL."
                                }
                                allLabelTexts.contains("glass") || allLabelTexts.contains("bottle") && allLabelTexts.contains("wine") -> {
                                    detectedLabel = "Vidrio"
                                    points = 40
                                    message = "Detectado vidrio. Va al contenedor VERDE."
                                }
                                else -> {
                                    detectedLabel = bestLabel.text.replaceFirstChar { it.uppercase() }
                                    points = 15
                                    message = "He detectado $detectedLabel. ¡Gracias por reciclar!"
                                }
                            }
                        }

                        // Aseguramos que el resultado se procese en el hilo principal
                        CoroutineScope(Dispatchers.Main).launch {
                            onResult(detectedLabel, points, message)
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        CoroutineScope(Dispatchers.Main).launch {
                            onError("Fallo en la IA: ${e.message}")
                        }
                        imageProxy.close()
                    }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    onError("No se pudo obtener la imagen")
                }
                imageProxy.close()
            }
        }

        override fun onError(exception: ImageCaptureException) {
            CoroutineScope(Dispatchers.Main).launch {
                onError("Error de captura: ${exception.message}")
            }
        }
    })
}
