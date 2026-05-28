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
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.rubensimon.ecolens.utils.SddrManager
import java.util.concurrent.Executors

/**
 * Implementación Android de PlatformCameraView usando CameraX y ML Kit local.
 */
@Composable
actual fun PlatformCameraView(
    modifier: Modifier,
    isSddr: Boolean,
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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(modifier = modifier) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val mainExecutor = ContextCompat.getMainExecutor(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            println("[CameraX] Error al iniciar cámara: ${e.message}")
                            e.printStackTrace()
                        }
                    }, mainExecutor)
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
                            isSddr = isSddr,
                            onResult = { label, points, message ->
                                // Feedback háptico profesional
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                
                                resultMessage = "✅ $label (+$points pts)\n$message"
                                
                                // SOLO sumamos puntos de reciclaje si NO es un canje de dinero SDDR
                                if (!isSddr) {
                                    PointsManager.addPoints(points, "scan")
                                    PointsManager.incrementScans(label, points)
                                    HistoryManager.addHistoryItem(
                                        objectName = label,
                                        points = points,
                                        userId = PointsManager.getUserId()
                                    )
                                }
                                
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
    isSddr: Boolean,
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
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
                val barcodeScanner = BarcodeScanning.getClient(options)

                println("[Scanner] 🔄 Iniciando análisis de imagen...")
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcode = barcodes.first()
                            val rawValue = barcode.rawValue ?: ""
                            println("[Scanner] 🎯 Barcode DETECTADO: $rawValue")
                            
                            if (rawValue.isNotEmpty()) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    // Solo procesamos como dinero si venimos de la pantalla SDDR
                                    if (isSddr && rawValue.startsWith("SDDR|")) {
                                        val success = SddrManager.redeemVoucher(rawValue)
                                        if (success) {
                                            onResult("Vale SDDR Detectado", 100, "¡Vale canjeado con éxito!")
                                        } else {
                                            onError("Error al validar el vale SDDR")
                                        }
                                    } else {
                                        // Si es un escaneo normal o el QR no es de SDDR, damos puntos normales
                                        onResult("Código Detectado", 20, "Objeto reciclado correctamente")
                                    }
                                }
                                imageProxy.close()
                                return@addOnSuccessListener
                            }
                        } else {
                            println("[Scanner] ℹ️ No se detectaron códigos de barras, probando con etiquetas de imagen...")
                        }
                        
                        // Si no hay barcode, usamos el labeler de imagen
                        labeler.process(image)
                            .addOnSuccessListener { labels ->
                                var detectedLabel = "Objeto"
                                var points = 10
                                var message = "¡Buen trabajo reciclando!"
                                
                                val reliableLabels = labels.filter { it.confidence > 0.55f }
                                
                                if (reliableLabels.isEmpty() && labels.isEmpty()) {
                                    detectedLabel = "Objeto"
                                    message = "No estoy muy seguro de qué es esto. ¡Asegúrate de que haya buena luz!"
                                } else {
                                    if (isSddr) {
                                        detectedLabel = "Envase SDDR (Simulado)"
                                        points = 50
                                        message = "¡Envase detectado! +0.10€ a tu saldo SDDR."
                                        
                                        // Trigger SDDR Flow Real
                                        CoroutineScope(Dispatchers.Main).launch {
                                            println("[Scanner] 🤖 MODO DEMO: Forzando éxito SDDR")
                                            val sddrCode = "SDDR|0.10|1|${System.currentTimeMillis()}"
                                            SddrManager.redeemVoucher(sddrCode)
                                        }
                                    } else {
                                        val permitidos = listOf("bottle", "can", "plastic", "metal", "paper", "cardboard", "glass", "beverage", "tin", "water", "liquid", "drink", "box", "aluminum")
                                        val objetoValido = labels.firstOrNull { label -> 
                                            permitidos.any { label.text.lowercase().contains(it) }
                                        }

                                        if (objetoValido != null) {
                                            val text = objetoValido.text.lowercase()
                                            when {
                                                text.contains("bottle") || text.contains("water") || text.contains("liquid") -> {
                                                    detectedLabel = "Botella"
                                                    points = 20
                                                    message = "¡Buen trabajo reciclando esta botella!"
                                                }
                                                text.contains("plastic") -> {
                                                    detectedLabel = "Envase Plástico"
                                                    points = 20
                                                    message = "¡Plástico reciclado correctamente!"
                                                }
                                                text.contains("can") || text.contains("tin") || text.contains("aluminum") -> {
                                                    detectedLabel = "Lata Metálica"
                                                    points = 25
                                                    message = "¡El aluminio es 100% reciclable!"
                                                }
                                                text.contains("glass") -> {
                                                    detectedLabel = "Vidrio"
                                                    points = 25
                                                    message = "¡El vidrio se recicla indefinidamente!"
                                                }
                                                text.contains("paper") || text.contains("cardboard") || text.contains("box") -> {
                                                    detectedLabel = "Cartón/Papel"
                                                    points = 15
                                                    message = "¡Buen trabajo con el papel/cartón!"
                                                }
                                                else -> {
                                                    detectedLabel = "Envase Mixto"
                                                    points = 15
                                                    message = "Objeto reciclable detectado."
                                                }
                                            }
                                        } else {
                                            val bestLabel = reliableLabels.firstOrNull() ?: labels.firstOrNull()
                                            if (bestLabel != null) {
                                                detectedLabel = bestLabel.text
                                                points = 10
                                                message = "Viendo: ${bestLabel.text} (No reciclable)"
                                            } else {
                                                detectedLabel = "Objeto"
                                                message = "No reconozco este objeto. ¡Intenta con mejor luz!"
                                            }
                                        }
                                    }
                                }
                                
                                println("[Scanner] ✅ Resultado final: $detectedLabel")
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
                    }
                    .addOnFailureListener { e ->
                        // Si falla el barcode scanner, al menos intentamos el labeler
                        imageProxy.close()
                        onError("Error al escanear: ${e.message}")
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
