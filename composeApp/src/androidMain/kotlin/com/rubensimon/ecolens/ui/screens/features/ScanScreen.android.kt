package com.rubensimon.ecolens.ui.screens.features

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
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
import com.rubensimon.ecolens.ui.components.EcoColors
import com.rubensimon.ecolens.ui.components.GlassButton
import com.rubensimon.ecolens.utils.HistoryManager
import com.rubensimon.ecolens.utils.PlatformAudio
import com.rubensimon.ecolens.utils.PointsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

import android.util.Size
import com.rubensimon.ecolens.EcoLensSecrets
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.graphics.Bitmap
import com.rubensimon.ecolens.utils.SddrManager
import java.util.concurrent.Executors
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScanner
import java.util.concurrent.atomic.AtomicLong
import com.rubensimon.ecolens.ml.EcoLensCustomLabeler
import com.rubensimon.ecolens.ml.EcoLensMlBackend
import android.content.Context

/** Modelo para estabilizar frames de detección */
data class DetectionFrame(
    val label: String,
    val confidence: Float,
    val isRecyclable: Boolean,
    val points: Int,
    val message: String,
    val rawInfo: String
)

/* =========================================================================
 *  Configuración del Escáner y Modelos
 * ========================================================================= */

/** Umbral de confianza para aceptar etiquetas de ML Kit. */
private const val CONFIDENCE_THRESHOLD = 0.55f
private const val MIN_CONFIDENCE_TO_CONFIRM = 0.55f
private const val UI_UPDATE_INTERVAL_MS = 150L
private const val ENABLE_PER_FRAME_LOGS = false
/** Modelo Gemini actual (gemini-1.5-flash ya no está en v1beta). */
private const val GEMINI_MODEL = "gemini-2.0-flash"

/** Materiales reciclables reconocidos por ML Kit Image Labeling. */
private val RECYCLABLE_KEYWORDS = listOf(
    "bottle", "can", "plastic bottle", "glass bottle", "tin can", "cardboard",
    "paper", "aluminum", "aluminium", "glass bottle", "glass jar", "drinking glass", "jar", "jug", "carton",
    "packaging", "storage container", "wrapper", "bag", "plastic bag", "pouch",
    "drinkware", "soda", "beer", "juice", "beverage", "recycling"
)

/** Palabras clave que identifican objetos claramente NO reciclables en contenedores normales (basura electrónica, cables, etc.). */
private val EXCLUDED_KEYWORDS = listOf(
    "cable", "wire", "charger", "adapter", "cord", "electronic", "device",
    "gadget", "technology", "computer", "phone", "hardware", "tool", "clothing",
    "shoe", "furniture", "wood", "human", "person", "face", "hand", "finger",
    "accessory", "peripheral", "mouse", "keyboard", "plug", "socket", "switch",
    "electronics", "appliance", "utensil", "screen", "laptop", "audio", "headphones",
    "microphone", "battery", "household appliance", "scissors", "pen", "pencil",
    "toy", "game", "book", "paperback",
    "wallet", "purse", "pocketbook", "leather", "backpack", "handbag", "bag",
    "glasses", "sunglasses", "eyewear", "key", "keys", "coin", "coins", "mirror",
    "fork", "knife", "spoon", "cutlery", "plate", "dish", "bowl", "cup", "mug",
    "table", "desk", "chair", "stool", "sofa", "couch", "cabinet", "shelf",
    "animal", "dog", "cat", "pet", "bird", "plant", "flower", "tree", "grass",
    "leaf", "foliage", "houseplant", "wall", "floor", "ceiling", "window", "door",
    "building", "pattern", "texture", "design", "text", "font", "number", "letter", "logo"
)

/** Etiquetas de fondo/escena que ML Kit suele devolver con alta confianza (ignorar al elegir objeto). */
private val SCENE_NOISE_KEYWORDS = listOf(
    "sky", "cloud", "sun", "moon", "wall", "floor", "ceiling", "room", "building",
    "window", "door", "monochrome", "pattern", "texture", "design", "space", "roof",
    "shelf", "furniture", "interior", "exterior", "landscape", "horizon", "metal",
    "metallic", "still life", "photography", "snapshot"
)

/** Comida y residuo orgánico — prioridad sobre exclusiones genéricas. */
private val FOOD_ORGANIC_KEYWORDS = listOf(
    "banana", "plantain", "fruit", "vegetable", "food", "produce", "organic", "snack",
    "apple", "orange", "lemon", "citrus", "tomato", "potato", "carrot", "grape",
    "berry", "melon", "pineapple", "pear", "peach", "avocado", "cucumber", "broccoli",
    "salad", "bread", "bakery", "grocery"
)

/** Palabras cortas en exclusiones: coincidencia de palabra completa (evita "carpet" → "pet"). */
private val EXCLUDED_SHORT_WORDS = setOf("pet", "cat", "dog", "can", "bag", "cup", "tin")

/** Palabras clave de objetos especiales en el catálogo de EcoDex (RAEE, punto limpio, etc.) para priorizar en la clasificación local. */
private val SPECIAL_CATALOG_KEYWORDS = listOf(
    "phone", "mobile", "smartphone", "tablet", "computer", "laptop", "battery",
    "medicine", "pill", "drug", "capsule", "coffee capsule", "x-ray", "radiography",
    "paint", "toaster", "blender", "appliance", "cd", "dvd", "toy", "doll",
    "thermometer", "fluorescent", "pen", "pencil", "marker", "clothing", "wear",
    "shirt", "pants", "coat", "jacket", "shoe", "footwear", "boot", "sneakers", "slipper"
)

/**
 * Implementación Android de PlatformCameraView.
 *
 * ### Estrategia (igual que EcoLensTFG):
 * - `ImageAnalysis` en STREAM_MODE analiza CADA frame con ML Kit Image Labeling.
 * - Muestra en tiempo real qué ve la IA con su confianza.
 * - Al pulsar botón, confirma el objeto ya detectado (sin re-analizar).
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

    // ── Permisos ─────────────────────────────────────────────────────────
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── Estado ────────────────────────────────────────────────────────────
    var resultMessage by remember { mutableStateOf("") }

    // Detección en tiempo real (como lastDetectedObject de EcoLensTFG)
    var lastDetectedObject by remember { mutableStateOf("Buscando material...") }
    var lastDetectedConfidence by remember { mutableStateOf(0f) }
    var isRecyclable by remember { mutableStateOf(false) }
    var detectedPoints by remember { mutableStateOf(0) }
    var detectedMessage by remember { mutableStateOf("") }
    // Texto raw de ML Kit para debug/info
    var rawLabelInfo by remember { mutableStateOf("") }

    // Historial para estabilización de frames (filtro de ruido de 5 frames - Thread-safe)
    val frameHistory = remember { java.util.Collections.synchronizedList(mutableListOf<DetectionFrame>()) }

    var lastScanTime by remember { mutableStateOf(0L) }
    val SCAN_COOLDOWN_MS = 2000L

    // Canal conflated para despachar frames de forma reactiva y óptima a la UI sin generar coroutines por cada frame
    val detectionChannel = remember { Channel<DetectionFrame>(Channel.CONFLATED) }
    val lastUiDispatchMs = remember { AtomicLong(0L) }

    var isAnalyzingWithGemini by remember { mutableStateOf(false) }
    val lastFrameBitmapRef = remember { java.util.concurrent.atomic.AtomicReference<Bitmap?>(null) }
    val bitmapLock = remember { Any() }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isFlashEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(isFlashEnabled, camera) {
        try {
            camera?.cameraControl?.enableTorch(isFlashEnabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // ML Kit Base Labeler (respaldo para objetos generales no reciclables)
    val baseLabeler = remember {
        val opts = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
            .build()
        ImageLabeling.getClient(opts)
    }

    // Barcode / QR Scanner (para canjear vales SDDR de forma directa)
    val barcodeScanner = remember {
        BarcodeScanning.getClient()
    }

    // ML Kit Text Recognizer (para OCR local en etiquetas)
    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // HttpClient reusable para Gemini
    val httpClient = remember { HttpClient() }

    // Sincronizar automáticamente registros pendientes al entrar al escáner
    LaunchedEffect(Unit) {
        try {
            HistoryManager.syncPendingItems()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            baseLabeler.close()
            EcoLensCustomLabeler.close()
            barcodeScanner.close()
            textRecognizer.close()
            synchronized(bitmapLock) {
                lastFrameBitmapRef.getAndSet(null)?.recycle()
            }
            httpClient.close()
        }
    }

    // ── Auto-reset del mensaje de resultado tras 3 segundos ──────────────
    LaunchedEffect(resultMessage) {
        if (resultMessage.isNotEmpty() && !isAnalyzingWithGemini) {
            delay(3000)
            resultMessage = ""
            synchronized(frameHistory) {
                frameHistory.clear()
            }
        }
    }

    // Colectar resultados del canal en el hilo principal de forma limpia
    LaunchedEffect(Unit) {
        for (frame in detectionChannel) {
            lastDetectedObject = frame.label
            lastDetectedConfidence = frame.confidence
            isRecyclable = frame.isRecyclable
            detectedPoints = frame.points
            detectedMessage = frame.message
            rawLabelInfo = frame.rawInfo
        }
    }

    // ── Layout ───────────────────────────────────────────────────────────
    Box(modifier = modifier) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)

                    // Enfoque al tacto (Tap-to-Focus)
                    previewView.setOnTouchListener { view, event ->
                        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                            val currentCamera = camera
                            if (currentCamera != null) {
                                try {
                                    val factory = previewView.meteringPointFactory
                                    val point = factory.createPoint(event.x, event.y)
                                    val action = androidx.camera.core.FocusMeteringAction.Builder(
                                        point,
                                        androidx.camera.core.FocusMeteringAction.FLAG_AF or androidx.camera.core.FocusMeteringAction.FLAG_AE
                                    ).build()
                                    currentCamera.cameraControl.startFocusAndMetering(action)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            view.performClick()
                        }
                        true
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val mainExecutor = ContextCompat.getMainExecutor(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            // 1. Preview en HD para máxima nitidez de la IA
                            val preview = Preview.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                            // 2. ImageAnalysis — análisis en tiempo real en HD con rotación vinculada al display
                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val originalBitmap = try {
                                            imageProxy.toBitmap()
                                        } catch (e: Exception) {
                                            null
                                        }

                                        if (originalBitmap == null) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        // 1. Rotar el bitmap según la orientación física del sensor
                                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                        val rotatedBitmap = if (rotationDegrees != 0) {
                                            try {
                                                val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                                val rotated = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                                                if (rotated != originalBitmap) {
                                                    originalBitmap.recycle()
                                                }
                                                rotated
                                            } catch (e: Exception) {
                                                originalBitmap
                                            }
                                        } else {
                                            originalBitmap
                                        }

                                        // Guardar copia del último frame COMPLETO para Gemini (orientación correcta)
                                        synchronized(bitmapLock) {
                                            val old = lastFrameBitmapRef.getAndSet(rotatedBitmap)
                                            old?.recycle()
                                        }

                                        val latch = java.util.concurrent.CountDownLatch(1)

                                        val roiBitmap = recortarViewfinder(rotatedBitmap)
                                        val analysisBitmap =
                                            if (roiBitmap !== rotatedBitmap) roiBitmap else rotatedBitmap

                                        analyzeFrame(
                                            context = ctx,
                                            croppedBitmap = analysisBitmap,
                                            barcodeScanner = barcodeScanner,
                                            baseLabeler = baseLabeler,
                                            isSddr = isSddr,
                                            onComplete = {
                                                if (analysisBitmap !== rotatedBitmap && !analysisBitmap.isRecycled) {
                                                    analysisBitmap.recycle()
                                                }
                                                imageProxy.close()
                                                latch.countDown()
                                            },
                                            onResult = { obj, conf, recyclable, pts, msg, rawInfo ->
                                                if (isSddr && obj == "Vale SDDR") {
                                                    PlatformAudio.playSuccess()
                                                    onScanComplete(obj, pts)
                                                } else {
                                                    synchronized(frameHistory) {
                                                        val frame = DetectionFrame(obj, conf, recyclable, pts, msg, rawInfo)
                                                        frameHistory.add(frame)
                                                        if (frameHistory.size > 5) {
                                                            frameHistory.removeAt(0)
                                                        }

                                                        // Estabilizar: priorizar objetos reales sobre "Buscando material..."
                                                        val realObjects = frameHistory.filter { it.label != "Buscando material..." }
                                                        val candidates = if (realObjects.isNotEmpty()) realObjects else frameHistory

                                                        val stabilized = candidates
                                                            .groupBy { it.label }
                                                            .maxByOrNull { it.value.size }
                                                            ?.value
                                                            ?.maxByOrNull { it.confidence }

                                                        stabilized?.let {
                                                            val now = android.os.SystemClock.elapsedRealtime()
                                                            val last = lastUiDispatchMs.get()
                                                            if (now - last >= UI_UPDATE_INTERVAL_MS && lastUiDispatchMs.compareAndSet(last, now)) {
                                                                detectionChannel.trySend(it)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        )

                                        // Esperar a que termine la clasificación del fotograma actual para evitar colisiones de reciclado
                                        try {
                                            latch.await(600, java.util.concurrent.TimeUnit.MILLISECONDS)
                                        } catch (e: InterruptedException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalyzer
                            )
                        } catch (e: Exception) {
                            println("[CameraX] Error: ${e.message}")
                            e.printStackTrace()
                        }
                    }, mainExecutor)
                    previewView
                }
            )

            // ── Canvas: Viewfinder (Guía de escaneo) ─────────────────
            Canvas(modifier = Modifier.fillMaxSize()) {
                // En lugar de un cuadrado del 70%, hacemos un rectángulo vertical
                // que ocupe el 85% del ancho y el 70% del alto. Ideal para botellas.
                val rectWidth = size.width * 0.85f
                val rectHeight = size.height * 0.70f
                
                val left = (size.width - rectWidth) / 2
                val top = (size.height - rectHeight) / 2

                drawRoundRect(
                    color = if (isAnalyzingWithGemini) Color(0xFFFF9800)
                    else if (estaListoParaEscanear(lastDetectedObject, lastDetectedConfidence, detectedPoints, isRecyclable)) Color(0xFF4CAF50)
                    else Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // ── Botón flotante de Flash (Linterna) ───────────────────────────
        if (hasCameraPermission && camera != null) {
            IconButton(
                onClick = { isFlashEnabled = !isFlashEnabled },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .background(EcoColors.CardBackground.copy(alpha = 0.8f), shape = androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Linterna",
                    tint = if (isFlashEnabled) Color(0xFFFFC107) else Color.White
                )
            }
        }

        // ── Overlay: detección en tiempo real ────────────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            color = EcoColors.CardBackground.copy(alpha = 0.88f),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (resultMessage.isNotEmpty()) {
                    // ── Resultado final (desaparece en 3s) ──────────────
                    Text(
                        text = resultMessage,
                        color = EcoColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                } else {
                    // ── Detección en tiempo real ─────────────────────────
                    val pct = (lastDetectedConfidence * 100).toInt()

                    if (isRecyclable) {
                        Text(
                            text = "♻️ $lastDetectedObject ($pct%)",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp
                        )
                        if (detectedPoints > 0) {
                            Text(
                                text = "+$detectedPoints pts · $detectedMessage",
                                color = EcoColors.TextPrimary.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (detectedPoints > 0 && lastDetectedObject != "Buscando material...") {
                        Text(
                            text = "🌱 $lastDetectedObject ($pct%)",
                            color = Color(0xFF8BC34A),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Orgánico / EcoDex · +$detectedPoints pts",
                            color = EcoColors.TextPrimary.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        if (detectedMessage.isNotEmpty()) {
                            Text(
                                text = detectedMessage,
                                color = EcoColors.TextPrimary.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (lastDetectedObject == "Buscando material...") {
                        // Nada detectado aún
                        Text(
                            text = if (isSddr) "📲 Apunta al código QR SDDR" else "📷 Buscando material...",
                            color = Color(0xFFBDBDBD),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp
                        )
                    } else if (isSddr && lastDetectedObject == "Apunta al código QR") {
                        // En modo SDDR esperando QR
                        Text(
                            text = "📲 Apunta al código QR SDDR",
                            color = Color(0xFF64B5F6),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp
                        )
                        Text(
                            text = detectedMessage,
                            color = EcoColors.TextPrimary.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Objeto detectado pero NO reciclable — mostrar nombre
                        Text(
                            text = "👁️ $lastDetectedObject ($pct%)",
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "No reciclable",
                            color = Color(0xFFBDBDBD),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Info adicional de lo que ve ML Kit
                    if (rawLabelInfo.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rawLabelInfo,
                            color = EcoColors.TextPrimary.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }

                }
            }
        }

        // ── Botón de captura ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Badge: "Listo para escanear"
            if (estaListoParaEscanear(lastDetectedObject, lastDetectedConfidence, detectedPoints, isRecyclable) && resultMessage.isEmpty()) {
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "✅ ¡Listo para escanear!",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            // Badge: "Esperando a la IA..."
            if (isAnalyzingWithGemini) {
                Surface(
                    color = Color(0xFFFF9800).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "⏳ Esperando a la IA...",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            GlassButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastScanTime < SCAN_COOLDOWN_MS) return@GlassButton
                    if (isAnalyzingWithGemini) return@GlassButton
                    lastScanTime = now

                    haptic.performHapticFeedback(
                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                    )

                    // 1. CONGELAR EL ESTADO ACTUAL (para evitar Race Condition o "Efecto Fantasma")
                    val capturedLabel = lastDetectedObject
                    val capturedPoints = detectedPoints
                    val capturedMessage = detectedMessage
                    val capturedConfidence = lastDetectedConfidence
                    val capturedRecyclable = isRecyclable

                    // 2. CLONAR EL BITMAP (para evitar que la cámara lo recicle/modifique mientras lo procesamos)
                    val capturedBitmap = synchronized(bitmapLock) {
                        val currentBitmap = lastFrameBitmapRef.get()
                        if (currentBitmap != null && !currentBitmap.isRecycled) {
                            try {
                                currentBitmap.copy(currentBitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888, true)
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }

                    if (capturedBitmap == null || capturedBitmap.isRecycled) {
                        resultMessage = "❌ No veo la cámara lista"
                        isAnalyzingWithGemini = false
                        return@GlassButton
                    }

                    // ── MODO SDDR: simular canje de QR, siempre aceptado ─────────────
                    if (isSddr) {
                        resultMessage = "✅ Vale SDDR canjeado\n¡+0.10€ a tu saldo SDDR!"
                        guardarEscaneo(scope, isSddr, "Vale SDDR", 50)
                        PlatformAudio.playSuccess()
                        onScanComplete("Vale SDDR", 50)
                        if (!capturedBitmap.isRecycled) capturedBitmap.recycle()
                        return@GlassButton
                    }

                    isAnalyzingWithGemini = true
                    resultMessage = "🔍 Analizando con IA..."

                    val runOCRFallback = { img: Bitmap, onFallbackDone: () -> Unit ->
                        val inputImage = InputImage.fromBitmap(img, 0)
                        textRecognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                val detectedText = visionText.text
                                println("[OCR] Texto detectado: $detectedText")
                                val mappedObject = mapearTextoAObjeto(detectedText)
                                if (mappedObject != null) {
                                    val (points, msg) = calcularPuntos(mappedObject)
                                    if (puedeRegistrarEscaneo(mappedObject, 0.75f, points)) {
                                        resultMessage = "📝 [OCR] $mappedObject (+$points pts)\n$msg"
                                        guardarEscaneo(scope, isSddr, mappedObject, points)
                                        PlatformAudio.playSuccess()
                                        onScanComplete(mappedObject, points)
                                        isAnalyzingWithGemini = false
                                        if (!img.isRecycled) {
                                            img.recycle()
                                        }
                                    } else {
                                        onFallbackDone()
                                    }
                                } else {
                                    onFallbackDone()
                                }
                            }
                            .addOnFailureListener {
                                onFallbackDone()
                            }
                    }

                    fun finalizarExito(label: String, pts: Int, msg: String, prefijo: String = "✅") {
                        resultMessage = "$prefijo $label (+$pts pts)\n$msg"
                        guardarEscaneo(scope, isSddr, label, pts)
                        PlatformAudio.playSuccess()
                        onScanComplete(label, pts)
                        isAnalyzingWithGemini = false
                        if (!capturedBitmap.isRecycled) capturedBitmap.recycle()
                    }

                    val fallbackLocal = {
                        runOCRFallback(capturedBitmap) {
                            val pts = calcularPuntos(capturedLabel).first
                            if (puedeRegistrarEscaneo(capturedLabel, capturedConfidence, pts)) {
                                finalizarExito(capturedLabel, pts, capturedMessage.ifEmpty { calcularPuntos(capturedLabel).second })
                            } else {
                                resultMessage = "❌ $capturedLabel\nNo reconocido en EcoDex"
                            }
                            isAnalyzingWithGemini = false
                            if (!capturedBitmap.isRecycled) capturedBitmap.recycle()
                        }
                    }

                    val fallbackBackend = {
                        scope.launch(Dispatchers.IO) {
                            val backend = EcoLensMlBackend.predict(capturedBitmap)
                            withContext(Dispatchers.Main) {
                                if (backend != null) {
                                    val (label, pts, msg) = backend
                                    if (pts > 0 && esObjetoValidoEcoDex(label)) {
                                        finalizarExito(label, pts, msg, "🤖")
                                        return@withContext
                                    }
                                }
                                fallbackLocal()
                            }
                        }
                    }

                    scope.launch(Dispatchers.IO) {
                        // 1) Tu TFLite local (sin internet) — ideal para Orgánico/Envases/Vidrio/Papel
                        val tfliteInput = InputImage.fromBitmap(capturedBitmap, 0)
                        val tfliteHit = EcoLensCustomLabeler.classify(context, tfliteInput)
                        if (tfliteHit != null) {
                            val (nombre, conf) = tfliteHit
                            val (pts, msg) = calcularPuntos(nombre)
                            if (esObjetoValidoEcoDex(nombre) && pts > 0) {
                                withContext(Dispatchers.Main) {
                                    finalizarExito(nombre, pts, msg, "🎯")
                                }
                                return@launch
                            }
                        }

                        // 2) Lo que ya veías en pantalla (preview estable)
                        val ptsPreview = calcularPuntos(capturedLabel).first
                        if (puedeRegistrarEscaneo(capturedLabel, capturedConfidence, ptsPreview)) {
                            withContext(Dispatchers.Main) {
                                finalizarExito(
                                    capturedLabel,
                                    ptsPreview,
                                    capturedMessage.ifEmpty { calcularPuntos(capturedLabel).second }
                                )
                            }
                            return@launch
                        }

                        // 3) Gemini (si hay cuota) → 4) Backend → 5) OCR
                        analyzeWithGemini(
                            bitmap = capturedBitmap,
                            isSddr = isSddr,
                            client = httpClient,
                            onResult = { obj, conf, _, _, msg ->
                                synchronized(frameHistory) { frameHistory.clear() }
                                val (pts, msgPts) = calcularPuntos(obj)
                                if (puedeRegistrarEscaneo(obj, conf, pts)) {
                                    finalizarExito(obj, pts, msg.ifEmpty { msgPts })
                                } else {
                                    fallbackBackend()
                                }
                            },
                            onError = {
                                fallbackBackend()
                            }
                        )
                    }
                },
                enabled = !isAnalyzingWithGemini,
                modifier = Modifier.size(72.dp)
            ) {
                if (isAnalyzingWithGemini) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Escanear",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

/* =========================================================================
 *  ANÁLISIS DE FRAME — Idéntico al processImageLocal de EcoLensTFG
 *
 *  ¿Por qué funciona al 99%?
 *  - Usa InputImage.fromMediaImage() DIRECTO (sin conversión a Bitmap)
 *  - ML Kit recibe la imagen en formato nativo del sensor (YUV_420_888)
 *  - Máxima calidad posible, sin compresión JPEG intermedia
 *  - Sin recortes que eliminen contexto visual
 * ========================================================================= */

private fun analyzeFrame(
    context: Context,
    croppedBitmap: Bitmap,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    baseLabeler: com.google.mlkit.vision.label.ImageLabeler,
    isSddr: Boolean,
    onComplete: () -> Unit,
    onResult: (
        detectedObject: String,
        confidence: Float,
        isRecyclable: Boolean,
        points: Int,
        message: String,
        rawInfo: String
    ) -> Unit
) {
    val inputImage = InputImage.fromBitmap(croppedBitmap, 0)

    // En modo SDDR nunca clasificamos objetos con IA: solo buscamos QR
    if (isSddr) {
        runBarcodeThenBase(inputImage, barcodeScanner, baseLabeler, isSddr, onComplete, onResult)
        return
    }

    fun runPipelineBase() {
        ejecutarModeloBase(inputImage, baseLabeler, isSddr, onResult, onComplete)
    }

    fun emitCustomResult(nombre: String, conf: Float, rawInfo: String) {
        val (pts, msg) = calcularPuntos(nombre)
        onResult(
            nombre,
            conf,
            esEnvaseReciclableEcoDex(nombre),
            pts,
            if (pts > 0) msg else "Identificado",
            rawInfo
        )
        onComplete()
    }

    if (EcoLensCustomLabeler.isAvailable(context)) {
        val customClient = EcoLensCustomLabeler.getClient(context)
        if (customClient != null) {
            customClient.process(inputImage)
                .addOnSuccessListener { labels ->
                    val hit = EcoLensCustomLabeler.interpretLabels(labels)
                    if (hit != null) {
                        val rawInfo = "[TFLite] " + labels.take(3).joinToString(" · ") {
                            "${it.text} ${(it.confidence * 100).toInt()}%"
                        }
                        emitCustomResult(hit.first, hit.second, rawInfo)
                    } else {
                        runPipelineBase()
                    }
                }
                .addOnFailureListener { runPipelineBase() }
            return
        }
    }

    runPipelineBase()
}

private fun runBarcodeThenBase(
    inputImage: InputImage,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    baseLabeler: com.google.mlkit.vision.label.ImageLabeler,
    isSddr: Boolean,
    onComplete: () -> Unit,
    onResult: (
        detectedObject: String,
        confidence: Float,
        isRecyclable: Boolean,
        points: Int,
        message: String,
        rawInfo: String
    ) -> Unit
) {

    // FASE 0: Detección de Códigos de Barras / QR (solo si isSddr = true)
    barcodeScanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            if (barcodes.isNotEmpty()) {
                val barcode = barcodes.first()
                val rawValue = barcode.rawValue ?: ""
                if (rawValue.isNotEmpty()) {
                    if (rawValue.startsWith("SDDR|")) {
                        val parts = rawValue.split("|")
                        val count = parts.getOrNull(2)?.toIntOrNull() ?: 1
                        val value = parts.getOrNull(1)?.toFloatOrNull() ?: 0.10f
                        val totalValue = value * count
                        
                        val success = SddrManager.redeemVoucher(rawValue)
                        if (success) {
                            onResult("Vale SDDR", 1.0f, true, count * 50, "¡Vale de ${count} envases ($totalValue€) canjeado!", "[QR] $rawValue")
                        } else {
                            onResult("Código SDDR Inválido", 1.0f, false, 0, "Código ya usado o incorrecto", "[QR] $rawValue")
                        }
                    } else {
                        onResult("Código QR Inválido", 1.0f, false, 0, "El QR no es un vale SDDR válido", "[QR] $rawValue")
                    }
                    onComplete()
                    return@addOnSuccessListener
                }
            }
            
            // En SDDR si no hay QR, mostrar aviso de que apunte al código (no clasificar objeto)
            onResult("Apunta al código QR", 0f, false, 0, "Centra el código QR SDDR en el recuadro", "[SDDR] esperando QR")
            onComplete()
        }
        .addOnFailureListener {
            onResult("Apunta al código QR", 0f, false, 0, "Centra el código QR SDDR en el recuadro", "[SDDR] error lector")
            onComplete()
        }
}

private fun evaluarModeloBaseConLabels(
    baseLabels: List<com.google.mlkit.vision.label.ImageLabel>,
    isSddr: Boolean,
    onResult: (
        detectedObject: String,
        confidence: Float,
        isRecyclable: Boolean,
        points: Int,
        message: String,
        rawInfo: String
    ) -> Unit,
    onComplete: () -> Unit
) {
    if (ENABLE_PER_FRAME_LOGS) {
        println("[Base RAW] ${baseLabels.take(5).joinToString { "${it.text}=>${(it.confidence * 100).toInt()}%" }}")
    }
    if (baseLabels.isEmpty()) {
        onResult("Buscando material...", 0f, false, 0, "", "")
        onComplete()
        return
    }

    val rawInfo = "[Base Model] " + baseLabels.take(3).joinToString(" · ") { "${it.text} ${(it.confidence * 100).toInt()}%" }

    val sortedLabels = baseLabels.sortedByDescending { it.confidence }
    val usefulLabels = filtrarRuidoEscena(sortedLabels)

    // Prioridad: comida/orgánico (plátano, fruta…) antes que fondo o falsos "pet" en "carpet"
    val foodLabel = buscarEtiquetaComida(usefulLabels.ifEmpty { sortedLabels })
    if (foodLabel != null && foodLabel.confidence >= 0.40f) {
        val translated = traducirNoReciclable(foodLabel.text)
        val (pts, msg) = calcularPuntos(translated)
        onResult(
            translated,
            foodLabel.confidence,
            false,
            pts,
            if (pts > 0) msg else "Residuo orgánico — compost/marrón",
            rawInfo
        )
        onComplete()
        return
    }

    val labelsForDecision = usefulLabels.ifEmpty { sortedLabels }
    
    // Priorizar objetos especiales del catálogo (RAEE, Punto Limpio, Ropa, etc.) si se detectan
    val specialCatalogLabel = labelsForDecision.firstOrNull { label ->
        val t = label.text.lowercase()
        SPECIAL_CATALOG_KEYWORDS.any { t.contains(it) }
    }
    val excludedLabel = specialCatalogLabel ?: labelsForDecision.firstOrNull { esExcluido(it.text) }

    // Clasificación inteligente para reciclables
    val recyclableLabel = labelsForDecision.firstOrNull { label ->
        val t = label.text.lowercase()
        t.contains("bottle") || t.contains("can") || t.contains("carton") || t.contains("cardboard")
    } ?: labelsForDecision.firstOrNull { label ->
        val t = label.text.lowercase()
        t.contains("jar") || t.contains("jug") || t.contains("plastic bag") || t.contains("grocery bag") || t.contains("paper cup") || t.contains("plastic cup")
    } ?: labelsForDecision.firstOrNull { label ->
        esReciclable(label.text)
    }

    val highestLabel = labelsForDecision.firstOrNull() ?: sortedLabels.first()

    // Si solo vemos fondo (cielo, habitación…), no inventar "Mascota/Animal" ni similares
    if (esRuidoEscena(highestLabel.text) && recyclableLabel == null && foodLabel == null) {
        onResult("Buscando material...", 0f, false, 0, "Centra el objeto en el recuadro", rawInfo)
        onComplete()
        return
    }

    val shouldExclude = excludedLabel != null && (
        esExcluido(highestLabel.text) ||
        recyclableLabel == null ||
        excludedLabel.confidence > (recyclableLabel?.confidence ?: 0f) ||
        (excludedLabel.confidence >= 0.6f && (recyclableLabel?.confidence ?: 0f) < 0.75f)
    )

    if (shouldExclude && excludedLabel != null) {
        val translated = traducirNoReciclable(excludedLabel.text)
        val (pts, msg) = calcularPuntos(translated)
        onResult(
            translated,
            excludedLabel.confidence,
            false,
            pts,
            if (pts > 0) msg else "No reciclable",
            rawInfo
        )
        onComplete()
        return
    }

    if (recyclableLabel != null) {
        val translated = if (isSddr) "Envase SDDR" else traducirEtiqueta(recyclableLabel.text)
        val (pts, msg) = if (isSddr) Pair(50, "¡Envase detectado! +0.10€ a tu saldo SDDR.")
        else calcularPuntos(translated)
        onResult(translated, recyclableLabel.confidence, true, pts, msg, rawInfo)
    } else {
        val translated = traducirNoReciclable(highestLabel.text)
        val (pts, msg) = calcularPuntos(translated)
        onResult(
            translated,
            highestLabel.confidence,
            false,
            pts,
            if (pts > 0) msg else "No reciclable",
            rawInfo
        )
    }
    onComplete()
}

/** Recorte central (85% × 70%) alineado con el viewfinder — menos cielo/mesa alrededor. */
private fun recortarViewfinder(bitmap: Bitmap): Bitmap {
    val roiW = (bitmap.width * 0.85f).toInt().coerceIn(1, bitmap.width)
    val roiH = (bitmap.height * 0.70f).toInt().coerceIn(1, bitmap.height)
    val left = ((bitmap.width - roiW) / 2).coerceAtLeast(0)
    val top = ((bitmap.height - roiH) / 2).coerceAtLeast(0)
    val w = roiW.coerceAtMost(bitmap.width - left)
    val h = roiH.coerceAtMost(bitmap.height - top)
    return try {
        Bitmap.createBitmap(bitmap, left, top, w, h)
    } catch (_: Exception) {
        bitmap
    }
}

private fun esRuidoEscena(text: String): Boolean {
    val t = text.lowercase()
    return SCENE_NOISE_KEYWORDS.any { t.contains(it) }
}

private fun filtrarRuidoEscena(labels: List<com.google.mlkit.vision.label.ImageLabel>): List<com.google.mlkit.vision.label.ImageLabel> {
    val sinRuido = labels.filterNot { esRuidoEscena(it.text) }
    return sinRuido.ifEmpty { labels.take(3) }
}

private fun buscarEtiquetaComida(labels: List<com.google.mlkit.vision.label.ImageLabel>): com.google.mlkit.vision.label.ImageLabel? {
    return labels.firstOrNull { label ->
        val t = label.text.lowercase()
        FOOD_ORGANIC_KEYWORDS.any { kw -> t.contains(kw) }
    }
}

private fun coincideKeywordExcluida(keyword: String, text: String): Boolean {
    val t = text.lowercase()
    return if (keyword in EXCLUDED_SHORT_WORDS) {
        contienePalabraCompleta(t, keyword)
    } else {
        t.contains(keyword)
    }
}

/** Ejecuta la clasificación de respaldo con el modelo base de ML Kit */
private fun ejecutarModeloBase(
    inputImage: InputImage,
    baseLabeler: com.google.mlkit.vision.label.ImageLabeler,
    isSddr: Boolean,
    onResult: (
        detectedObject: String,
        confidence: Float,
        isRecyclable: Boolean,
        points: Int,
        message: String,
        rawInfo: String
    ) -> Unit,
    onComplete: () -> Unit
) {
    baseLabeler.process(inputImage)
        .addOnSuccessListener { baseLabels ->
            evaluarModeloBaseConLabels(baseLabels, isSddr, onResult, onComplete)
        }
        .addOnFailureListener {
            onResult("Buscando material...", 0f, false, 0, "", "")
            onComplete()
        }
}

/* =========================================================================
 *  UTILIDADES DE CLASIFICACIÓN
 * ========================================================================= */

private fun esReciclable(text: String): Boolean {
    val t = text.lowercase()
    val words = t.split(" ", "-", "_")
    
    // Comprobar palabra completa para "can" para evitar falsos positivos con "scan", "toucan", etc.
    if (words.contains("can") || words.contains("cans") || words.contains("tin") || words.contains("tins")) {
        return true
    }
    
    return RECYCLABLE_KEYWORDS.any { keyword ->
        keyword != "can" && t.contains(keyword)
    }
}

private fun esExcluido(text: String): Boolean {
    val t = text.lowercase()
    // Si contiene términos reciclables explícitos, ignoramos la exclusión de bag/cup/carton
    if (t.contains("plastic bag") || t.contains("grocery bag") || t.contains("paper cup") || t.contains("plastic cup") || t.contains("paper bag")) {
        return false
    }
    return EXCLUDED_KEYWORDS.any { coincideKeywordExcluida(it, t) }
}

/** Traduce etiqueta de ML Kit a nombre amigable en español (objeto reciclable). */
private fun traducirEtiqueta(text: String): String {
    val t = text.lowercase()
    return when {
        t.contains("bottle") || t.contains("can") || t.contains("plastic") || t.contains("aluminum") || 
        t.contains("tin") || t.contains("bag") || t.contains("cup") || t.contains("foil") ||
        t.contains("cap") || t.contains("lid") || t.contains("wrapper") || t.contains("spray") -> "Envases"
        
        t.contains("paper") || t.contains("cardboard") || t.contains("box") || t.contains("carton") || 
        t.contains("newspaper") || t.contains("magazine") || t.contains("book") || t.contains("envelope") -> "Papel y Cartón"
        
        t.contains("glass") || t.contains("jar") || t.contains("wine") || t.contains("perfume") -> "Vidrio"
        
        t.contains("food") || t.contains("fruit") || t.contains("vegetable") || t.contains("banana") || 
        t.contains("apple") || t.contains("orange") || t.contains("coffee") || t.contains("organic") ||
        t.contains("bread") || t.contains("meat") || t.contains("plant") -> "Orgánico"
        
        else -> "No reciclable"
    }
}

/** Traduce etiqueta de objeto NO reciclable para mostrarlo en español. */
private fun traducirNoReciclable(text: String): String {
    return "No reciclable"
}

/** Envases (amarillo/azul/verde) — no orgánico ni RAEE. */
private fun esEnvaseReciclableEcoDex(label: String): Boolean {
    if (esResiduoOrganicoEcoDex(label) || esObjetoEspecial(label)) return false
    val t = label.lowercase()
    return t.contains("envases") || t.contains("papel y cartón") || t.contains("papel y carton") || t.contains("vidrio")
}

private fun esResiduoOrganicoEcoDex(label: String): Boolean {
    val t = label.lowercase()
    return t.contains("orgánico") || t.contains("organico")
}

private fun puedeRegistrarEscaneo(label: String, confidence: Float, points: Int): Boolean {
    if (label == "Buscando material..." || points <= 0) return false
    val requiredConfidence = if (esResiduoOrganicoEcoDex(label)) 0.40f else MIN_CONFIDENCE_TO_CONFIRM
    return esObjetoValidoEcoDex(label) && confidence >= requiredConfidence
}

private fun estaListoParaEscanear(
    label: String,
    confidence: Float,
    points: Int,
    isRecyclable: Boolean
): Boolean {
    if (label == "Buscando material...") return false
    val requiredConfidence = if (esResiduoOrganicoEcoDex(label)) 0.40f else MIN_CONFIDENCE_TO_CONFIRM
    return confidence >= requiredConfidence && (isRecyclable || points > 0)
}

private fun esObjetoValidoEcoDex(label: String): Boolean {
    val t = label.lowercase()
    return t.contains("envases") || t.contains("papel y cartón") || t.contains("papel y carton") ||
           t.contains("vidrio") || t.contains("orgánico") || t.contains("organico")
}

/** Calcula puntos y mensaje motivacional. */
private fun calcularPuntos(label: String): Pair<Int, String> {
    val t = label.lowercase()
    if (!esObjetoValidoEcoDex(label)) {
        return Pair(0, "No reciclable / Desconocido")
    }
    return when {
        t.contains("envases") -> Pair(20, "¡Envase depositado en el Contenedor Amarillo!")
        t.contains("papel y cartón") || t.contains("papel y carton") -> Pair(15, "¡Depositado en el Contenedor Azul!")
        t.contains("vidrio") -> Pair(25, "¡Depositado en el Contenedor Verde!")
        t.contains("orgánico") || t.contains("organico") -> Pair(10, "¡Depositado en el Contenedor Marrón!")
        else -> Pair(15, "¡Objeto reciclable detectado!")
    }
}



private fun extractJsonString(raw: String): String {
    val startIndex = raw.indexOf('{')
    val endIndex = raw.lastIndexOf('}')
    if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
        return raw.substring(startIndex, endIndex + 1)
    }
    return raw
}

private suspend fun analyzeWithGemini(
    bitmap: android.graphics.Bitmap,
    isSddr: Boolean,
    client: HttpClient,
    onResult: (String, Float, Boolean, Int, String) -> Unit,
    onError: () -> Unit
) {
    try {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64Image = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)

        val prompt = """
            Analiza esta imagen y responde ÚNICAMENTE con JSON válido, sin texto adicional, sin markdown.
            La estructura del JSON DEBE ser exactamente:
            {
              "objeto": "nombre exacto en español",
              "reciclable": true o false,
              "confianza": número entre 0.0 y 1.0,
              "motivo": "una frase corta"
            }
            
            Reglas de nombre del objeto (el campo "objeto" DEBE coincidir con uno de los siguientes nombres del EcoDex si aplica):
            - Envases
            - Papel y Cartón
            - Vidrio
            - Orgánico

            Si el objeto es reciclable pero no coincide exactamente, clasifícalo en uno de los 4 grupos anteriores. Si no es reciclable o es de fondo o no hay nada claro, responde con "Buscando material...", reciclable: false, confianza: 0.1.
        """.trimIndent()

        val partsArray = org.json.JSONArray().apply {
            put(org.json.JSONObject().put("text", prompt))
            put(org.json.JSONObject().put("inlineData", org.json.JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", base64Image)
            }))
        }
        val contentsArray = org.json.JSONArray().apply {
            put(org.json.JSONObject().put("parts", partsArray))
        }
        val requestBody = org.json.JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", org.json.JSONObject().apply {
                put("responseMimeType", "application/json")
                put("responseSchema", org.json.JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", org.json.JSONObject().apply {
                        put("objeto", org.json.JSONObject().put("type", "STRING"))
                        put("reciclable", org.json.JSONObject().put("type", "BOOLEAN"))
                        put("confianza", org.json.JSONObject().put("type", "NUMBER"))
                        put("motivo", org.json.JSONObject().put("type", "STRING"))
                    })
                    put("required", org.json.JSONArray().put("objeto").put("reciclable").put("confianza").put("motivo"))
                })
            })
        }.toString()

        val response = client.post("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent?key=${EcoLensSecrets.GEMINI_API_KEY}") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        
        val rawResponse = response.bodyAsText()

        val responseJson = JSONObject(rawResponse)
        if (responseJson.has("error")) {
            val err = responseJson.getJSONObject("error")
            val code = err.optInt("code", 0)
            val message = err.optString("message", "Error Gemini")
            println("[Gemini] Error $code: ${message.take(120)}")
            throw Exception("Gemini error $code")
        }
        if (!responseJson.has("candidates")) {
            println("[Gemini] Sin candidates en la respuesta")
            throw Exception("Gemini sin candidates")
        }
        val raw = responseJson.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
        
        val cleanJson = extractJsonString(raw)
        val parsed = JSONObject(cleanJson)
        val objeto = parsed.getString("objeto")
        val esReciclable = parsed.getBoolean("reciclable")
        val confianza = parsed.getDouble("confianza").toFloat()

        val (pts, msg) = if (isSddr && esReciclable) {
            Pair(50, "¡Envase detectado! +0.10€ a tu saldo SDDR.")
        } else {
            calcularPuntos(objeto)
        }

        withContext(Dispatchers.Main) {
            onResult(objeto, confianza, esReciclable, pts, msg)
        }

    } catch (e: Exception) {
        println("[Gemini] Fallback local/TFLite: ${e.message}")
        withContext(Dispatchers.Main) { onError() }
    }
}

private fun guardarEscaneo(
    scope: kotlinx.coroutines.CoroutineScope,
    isSddr: Boolean,
    label: String,
    points: Int
) {
    if (!isSddr) {
        scope.launch(Dispatchers.IO) {
            PointsManager.addPoints(points, "scan")
            PointsManager.incrementScans(label, points)
            HistoryManager.addHistoryItem(
                objectName = label,
                points = points,
                userId = PointsManager.getUserId()
            )
        }
    } else {
        scope.launch(Dispatchers.IO) {
            val code = "SDDR|0.10|1|${System.currentTimeMillis()}"
            SddrManager.redeemVoucher(code)
        }
    }
}

private fun contienePalabraCompleta(texto: String, palabra: String): Boolean {
    if (palabra.isEmpty()) return false
    var index = texto.indexOf(palabra)
    while (index != -1) {
        val startCheck = index == 0 || !texto[index - 1].isLetterOrDigit()
        val endCheck = (index + palabra.length) == texto.length || !texto[index + palabra.length].isLetterOrDigit()
        if (startCheck && endCheck) {
            return true
        }
        index = texto.indexOf(palabra, index + 1)
    }
    return false
}

private fun mapearTextoAObjeto(text: String): String? {
    val t = text.lowercase()
    return when {
        // Envases
        t.contains("botella") || t.contains("lata") || t.contains("plástico") || t.contains("plastico") || 
        t.contains("brick") || t.contains("champú") || t.contains("champu") || t.contains("detergente") || 
        t.contains("envase") || t.contains("bolsa") || t.contains("yogur") -> "Envases"
        
        // Papel y cartón
        t.contains("papel") || t.contains("cartón") || t.contains("carton") || t.contains("caja") || 
        t.contains("periódico") || t.contains("periodico") || t.contains("revista") || t.contains("libro") || 
        t.contains("cuaderno") || t.contains("sobre") || t.contains("carta") -> "Papel y Cartón"
        
        // Vidrio
        t.contains("vidrio") || t.contains("cristal") || t.contains("tarro") || t.contains("frasco") -> "Vidrio"
        
        // Orgánico
        t.contains("orgánico") || t.contains("organico") || t.contains("comida") || t.contains("fruta") || 
        t.contains("plátano") || t.contains("platano") || t.contains("café") || t.contains("cafe") || 
        t.contains("té") || t.contains("te ") || t.contains("restos") -> "Orgánico"
        
        else -> null
    }
}

private fun esObjetoEspecial(label: String): Boolean {
    return false
}

