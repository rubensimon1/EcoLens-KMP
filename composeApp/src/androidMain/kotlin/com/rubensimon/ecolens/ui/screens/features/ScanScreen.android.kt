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
 *  CONFIGURACIÓN — Mismo enfoque que EcoLensTFG (que funciona al 99%)
 *
 *  Estrategia: ImageLabeling DIRECTO sobre el frame raw del ImageProxy.
 *  Sin conversiones a Bitmap, sin Object Detection, sin recortes.
 *  Es el approach que funciona en EcoLensTFG y la base de apps profesionales.
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
                            text = "📷 Buscando material...",
                            color = Color(0xFFBDBDBD),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp
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

    fun runPipelineBase() {
        if (!isSddr) {
            ejecutarModeloBase(inputImage, baseLabeler, isSddr, onResult, onComplete)
            return
        }
        runBarcodeThenBase(inputImage, barcodeScanner, baseLabeler, isSddr, onComplete, onResult)
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
            
            // Si es SDDR pero no se leyó QR, clasificar el envase directamente
            ejecutarModeloBase(inputImage, baseLabeler, isSddr, onResult, onComplete)
        }
        .addOnFailureListener {
            ejecutarModeloBase(inputImage, baseLabeler, isSddr, onResult, onComplete)
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
        t.contains("banana") -> "Plátano"
        t.contains("notebook") && !t.contains("computer") -> "Cuaderno"
        t.contains("plastic bottle") || (t.contains("bottle") && t.contains("plastic")) -> "Botella de Plástico"
        t.contains("glass bottle") -> "Botella de Vidrio"
        t.contains("wine bottle") -> "Botella de Vino"
        t.contains("bottle") || t.contains("water") || t.contains("liquid") || t.contains("jug") -> "Botella de Plástico"
        t.contains("soda") || t.contains("beer") -> "Lata de Refresco"
        t.contains("tin can") || t.contains("can") || t.contains("tin") || t.contains("aluminum")
                || t.contains("aluminium") -> "Lata de Conservas"
        t.contains("drinkware") || t.contains("cup") || t.contains("tableware") -> "Vaso/Taza"
        t.contains("glass jar") || t.contains("jar") -> "Tarro de Mermelada"
        t.contains("perfume") -> "Frasco de Perfume"
        t.contains("glass") -> "Botella de Vidrio"
        t.contains("newspaper") || t.contains("magazine") -> "Periódico / Revista"
        t.contains("egg carton") -> "Huevera de Cartón"
        t.contains("envelope") || t.contains("mail") || t.contains("letter") -> "Sobres y Cartas"
        t.contains("pizza box") -> "Caja de Pizza"
        t.contains("flour bag") -> "Bolsa de Harina"
        t.contains("book") || t.contains("novel") || t.contains("paperback") -> "Libros Viejos"
        t.contains("paper") -> "Periódico / Revista"
        t.contains("cardboard") || t.contains("box") -> "Caja de Cartón"
        t.contains("carton") -> "Brick de Leche"
        t.contains("shampoo") || t.contains("soap bottle") || t.contains("soap") -> "Bote de Champú"
        t.contains("snack") || t.contains("chips") || t.contains("packet") || t.contains("wrapper") || t.contains("cookie") -> "Bolsa de Snacks"
        t.contains("aluminum foil") -> "Papel de Aluminio"
        t.contains("yogurt") || t.contains("yoghurt") -> "Tarrina de Yogur"
        t.contains("styrofoam") || t.contains("polystyrene") -> "Bandeja de Corcho"
        t.contains("plastic bag") || t.contains("bag") -> "Bolsa de Plástico"
        t.contains("cap") || t.contains("lid") || t.contains("bottle cap") -> "Tapón de Plástico"
        t.contains("spray") || t.contains("aerosol") -> "Aerosol / Spray"
        t.contains("crown cap") || t.contains("beer cap") -> "Chapa de Metal"
        t.contains("film") || t.contains("cling wrap") -> "Film Transparente"
        t.contains("detergent") || t.contains("cleaner bottle") -> "Bote Detergente"
        t.contains("beverage") || t.contains("drink") || t.contains("juice") -> "Brick de Leche"
        t.contains("cylinder") -> "Bote de Champú"
        t.contains("waste container") || t.contains("recycling") -> "Contenedor de Reciclaje"
        t.contains("product") || t.contains("material property") -> "Envase/Producto"
        t.contains("coffee") -> "Posos de Café"
        t.contains("cork") -> "Tapón de Corcho"
        t.contains("tea") -> "Bolsa de Té"
        t.contains("napkin") || t.contains("tissue") || t.contains("paper towel") -> "Servilleta Sucia"
        t.contains("food") || t.contains("fruit") || t.contains("vegetable") || t.contains("bread") || t.contains("organic") -> "Restos de Comida"
        else -> "Envase Reciclable"
    }
}

/** Traduce etiqueta de objeto NO reciclable para mostrarlo en español. */
private fun traducirNoReciclable(text: String): String {
    val t = text.lowercase()
    return when {
        t.contains("banana") -> "Plátano"
        t.contains("notebook") && !t.contains("computer") -> "Cuaderno"
        t.contains("battery") -> "Pilas y Baterías"
        t.contains("phone") || t.contains("mobile") || t.contains("smartphone") || t.contains("tablet") -> "Móvil / Tablet"
        t.contains("oil") -> "Aceite Usado"
        t.contains("lamp") || t.contains("light") || t.contains("bulb") -> "Bombilla / LED"
        t.contains("clothing") || t.contains("wear") || t.contains("shirt") || t.contains("pants") || t.contains("coat") || t.contains("jacket") -> "Ropa Usada"
        t.contains("medicine") || t.contains("pill") || t.contains("drug") -> "Medicamentos"
        t.contains("capsule") || t.contains("coffee capsule") -> "Cápsula de Café"
        t.contains("x-ray") || t.contains("radiography") -> "Radiografía"
        t.contains("paint") -> "Bote de Pintura"
        t.contains("toaster") || t.contains("blender") || t.contains("appliance") || t.contains("hardware") -> "Tostadora / Batidora"
        t.contains("cd") || t.contains("dvd") -> "CD / DVD"
        t.contains("toy") || t.contains("doll") -> "Juguete Roto"
        t.contains("thermometer") -> "Termómetro"
        t.contains("fluorescent") -> "Fluorescente"
        t.contains("pen") || t.contains("pencil") || t.contains("marker") -> "Bolígrafo/Lápiz"
        
        // Electrónica y Conectores
        t.contains("cable") || t.contains("wire") || t.contains("cord") -> "Tostadora / Batidora"
        t.contains("charger") || t.contains("adapter") || t.contains("plug") -> "Tostadora / Batidora"
        t.contains("electronic") || t.contains("device") || t.contains("gadget") -> "Móvil / Tablet"
        t.contains("computer") || t.contains("laptop") -> "Móvil / Tablet"
        t.contains("television") || t.contains("monitor") || t.contains("screen") -> "Móvil / Tablet"
        t.contains("camera") || t.contains("lens") -> "Móvil / Tablet"
        t.contains("headphone") || t.contains("earphone") || t.contains("headset") -> "Móvil / Tablet"
        t.contains("speaker") || t.contains("audio") -> "Móvil / Tablet"
        t.contains("clock") || t.contains("watch") -> "Móvil / Tablet"

        // Ropa y Accesorios Personales
        t.contains("shoe") || t.contains("footwear") || t.contains("boot") || t.contains("sneakers") || t.contains("slipper") -> "Ropa Usada"
        t.contains("glasses") || t.contains("sunglasses") || t.contains("eyewear") -> "Ropa Usada"
        t.contains("backpack") || t.contains("handbag") || t.contains("luggage") || t.contains("suitcase") || t.contains("bag") -> "Ropa Usada"
        t.contains("wallet") || t.contains("purse") -> "Ropa Usada"
        t.contains("jewelry") || t.contains("ring") || t.contains("necklace") -> "Ropa Usada"

        // Objetos de Hogar / Oficina
        t.contains("table") || t.contains("desk") || t.contains("shelf") || t.contains("cabinet") -> "Juguete Roto"
        t.contains("chair") || t.contains("stool") || t.contains("sofa") || t.contains("couch") || t.contains("furniture") -> "Juguete Roto"
        t.contains("bed") || t.contains("pillow") || t.contains("blanket") -> "Ropa Usada"
        t.contains("mirror") -> "Juguete Roto"
        t.contains("scissors") -> "Bolígrafo/Lápiz"
        t.contains("key") -> "Bolígrafo/Lápiz"
        t.contains("coin") || t.contains("money") -> "Bolígrafo/Lápiz"
        t.contains("tool") || t.contains("hammer") || t.contains("screwdriver") -> "Bolígrafo/Lápiz"

        // Cocina (no envases)
        t.contains("plate") || t.contains("dish") || t.contains("bowl") -> "Juguete Roto"
        t.contains("fork") || t.contains("knife") || t.contains("spoon") || t.contains("cutlery") -> "Bolígrafo/Lápiz"
        t.contains("cup") || t.contains("mug") || t.contains("glass") -> "Vaso/Taza"

        // Naturaleza y Seres Vivos
        t.contains("person") || t.contains("face") || t.contains("human") || t.contains("hand") || t.contains("finger") -> "Persona"
        t.contains("plant") || t.contains("flower") || t.contains("tree") || t.contains("grass") || t.contains("leaf") -> "Restos de Comida"
        contienePalabraCompleta(t, "animal") || contienePalabraCompleta(t, "dog") ||
                contienePalabraCompleta(t, "cat") || contienePalabraCompleta(t, "pet") ||
                contienePalabraCompleta(t, "bird") -> "Mascota/Animal"
        t.contains("banana") || t.contains("plantain") -> "Plátano"
        t.contains("food") || t.contains("fruit") || t.contains("vegetable") || t.contains("bread") ||
                t.contains("apple") || t.contains("orange") || t.contains("lemon") || t.contains("citrus") ||
                t.contains("tomato") || t.contains("potato") || t.contains("carrot") || t.contains("grape") ||
                t.contains("berry") || t.contains("melon") || t.contains("pineapple") || t.contains("pear") ||
                t.contains("peach") || t.contains("avocado") || t.contains("cucumber") || t.contains("broccoli") ||
                t.contains("salad") || t.contains("produce") || t.contains("bakery") || t.contains("grocery") -> "Restos de Comida"
        t.contains("sky") || t.contains("cloud") || t.contains("sun") || t.contains("moon") -> "Cielo/Naturaleza"

        // Estructuras e Información Visual
        t.contains("wall") || t.contains("floor") || t.contains("ceiling") || t.contains("window") || t.contains("door") || t.contains("building") -> "Estructura"
        t.contains("pattern") || t.contains("texture") || t.contains("design") -> "Patrón/Textura"
        t.contains("text") || t.contains("font") || t.contains("number") || t.contains("letter") || t.contains("logo") -> "Texto"
        t.contains("book") || t.contains("novel") || t.contains("magazine") -> "Libros Viejos"

        // Materiales y otros
        t.contains("leather") -> "Cuero"
        t.contains("wood") || t.contains("wooden") -> "Madera"
        t.contains("metal") || t.contains("metallic") -> "Metal"
        t.contains("plastic") -> "Plástico"
        t.contains("glass") -> "Vidrio"
        t.contains("fabric") || t.contains("textile") -> "Tela"
        
        else -> text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } // Formato Capitalizado
    }
}

/** Envases (amarillo/azul/verde) — no orgánico ni RAEE. */
private fun esEnvaseReciclableEcoDex(label: String): Boolean {
    if (esResiduoOrganicoEcoDex(label) || esObjetoEspecial(label)) return false
    val t = label.lowercase()
    return t.contains("envases") || t.contains("papel y cartón") || t.contains("papel y carton") || t.contains("vidrio") ||
        t.contains("botella") || t.contains("lata") || t.contains("brick") ||
        t.contains("bote") || t.contains("bolsa") || t.contains("caja") || t.contains("cartón") ||
        t.contains("carton") || t.contains("periódico") || t.contains("periodico") ||
        t.contains("tarro") || t.contains("frasco") || t.contains("aerosol") || t.contains("film") ||
        t.contains("tarrina") || t.contains("tapón") || t.contains("tapon") || t.contains("chapa")
}

private fun esResiduoOrganicoEcoDex(label: String): Boolean {
    val t = label.lowercase()
    return t.contains("orgánico") || t.contains("organico") ||
        t.contains("plátano") || t.contains("platano") || t.contains("restos de comida") ||
        t.contains("posos") || t.contains("bolsa de té") || t.contains("bolsa de te") ||
        t.contains("servilleta") || t.contains("tapón de corcho") || t.contains("tapon de corcho")
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
           t.contains("vidrio") || t.contains("orgánico") || t.contains("organico") ||
           t.contains("botella de plástico") || t.contains("botella de plastico") ||
           t.contains("lata de conservas") ||
           t.contains("lata de refresco") ||
           t.contains("brick de leche") ||
           t.contains("bote de champú") || t.contains("bote de champu") ||
           t.contains("bolsa de snacks") ||
           t.contains("papel de aluminio") ||
           t.contains("tarrina de yogur") ||
           t.contains("bandeja de corcho") ||
           t.contains("bolsa de plástico") || t.contains("bolsa de plastico") ||
           t.contains("tapón de plástico") || t.contains("tapon de plastico") ||
           t.contains("aerosol / spray") ||
           t.contains("chapa de metal") ||
           t.contains("film transparente") ||
           t.contains("bote detergente") ||
           t.contains("periódico / revista") || t.contains("periodico / revista") ||
           t.contains("caja de cartón") || t.contains("caja de carton") ||
           t.contains("huevera de cartón") || t.contains("huevera de carton") ||
           t.contains("sobres y cartas") ||
           t.contains("caja de pizza") ||
           t.contains("libros viejos") ||
           t.contains("bolsa de harina") ||
           t.contains("cuaderno") ||
           t.contains("botella de vidrio") ||
           t.contains("tarro de mermelada") ||
           t.contains("frasco de perfume") ||
           t.contains("botella de vino") ||
           t.contains("restos de comida") ||
           t.contains("posos de café") || t.contains("posos de cafe") ||
           t.contains("tapón de corcho") || t.contains("tapon de corcho") ||
           t.contains("bolsa de té") || t.contains("bolsa de te") ||
           t.contains("servilleta sucia") ||
           t.contains("plátano") || t.contains("platano") ||
           t.contains("pilas y baterías") || t.contains("pilas y baterias") ||
           t.contains("móvil / tablet") || t.contains("movil / tablet") ||
           t.contains("aceite usado") ||
           t.contains("bombilla / led") ||
           t.contains("ropa usada") ||
           t.contains("medicamentos") ||
           t.contains("cápsula de café") || t.contains("capsula de cafe") ||
           t.contains("radiografía") || t.contains("radiografia") ||
           t.contains("bote de pintura") ||
           t.contains("tostadora / batidora") ||
           t.contains("cd / dvd") ||
           t.contains("juguete roto") ||
           t.contains("termómetro") || t.contains("termometro") ||
           t.contains("fluorescente") ||
           t.contains("bolígrafo/lápiz") || t.contains("boligrafo/lapiz") ||
           t.contains("envase sddr") || t.contains("vale sddr")
}

/** Calcula puntos y mensaje motivacional. */
private fun calcularPuntos(label: String): Pair<Int, String> {
    val t = label.lowercase()
    if (!esObjetoValidoEcoDex(label)) {
        return Pair(0, "No reciclable / Desconocido")
    }
    return when {
        t == "envases" -> Pair(20, "¡Envase reciclado correctamente!")
        t == "papel y cartón" || t == "papel y carton" -> Pair(15, "¡Papel/Cartón reciclado!")
        t == "vidrio" -> Pair(25, "¡Vidrio reciclado correctamente!")
        t == "orgánico" || t == "organico" -> Pair(10, "¡Residuo orgánico separado correctamente!")

        t.contains("botella de plástico") -> Pair(20, "¡Botella de plástico reciclada!")
        t.contains("botella de vidrio") || t.contains("botella de vino") -> Pair(25, "¡Botella de vidrio reciclada!")
        t.contains("lata de refresco") -> Pair(25, "¡El aluminio es 100% reciclable!")
        t.contains("lata de conservas") -> Pair(25, "¡Lata de metal reciclada correctamente!")
        t.contains("brick de leche") -> Pair(20, "¡Brick reciclado correctamente!")
        t.contains("bote de champú") -> Pair(20, "¡Bote de plástico reciclado!")
        t.contains("bolsa de snacks") -> Pair(15, "¡Envoltorio de snacks reciclado!")
        t.contains("papel de aluminio") -> Pair(20, "¡Papel de aluminio reciclado!")
        t.contains("tarrina de yogur") -> Pair(20, "¡Tarrina de yogur reciclada!")
        t.contains("bandeja de corcho") -> Pair(20, "¡Bandeja de corcho reciclada!")
        t.contains("bolsa de plástico") -> Pair(10, "¡Bolsa de plástico reciclada!")
        t.contains("tapón de plástico") -> Pair(10, "¡Tapón de plástico reciclado!")
        t.contains("aerosol / spray") -> Pair(20, "¡Aerosol reciclado correctamente!")
        t.contains("chapa de metal") -> Pair(10, "¡Chapa de metal reciclada!")
        t.contains("film transparente") -> Pair(10, "¡Film de cocina reciclado!")
        t.contains("bote detergente") -> Pair(20, "¡Bote de detergente reciclado!")
        
        t.contains("periódico / revista") -> Pair(15, "¡Papel reciclado correctamente!")
        t.contains("caja de cartón") -> Pair(15, "¡Caja de cartón reciclada!")
        t.contains("huevera de cartón") -> Pair(15, "¡Huevera de cartón reciclada!")
        t.contains("sobres y cartas") -> Pair(15, "¡Sobres de papel reciclados!")
        t.contains("caja de pizza") -> Pair(15, "¡Caja de pizza reciclada!")
        t.contains("libros viejos") -> Pair(20, "¡Libro reciclado correctamente!")
        t.contains("bolsa de harina") -> Pair(15, "¡Bolsa de papel reciclada!")
        t.contains("cuaderno") -> Pair(15, "¡Cuaderno reciclado!")
        
        t.contains("tarro de mermelada") -> Pair(25, "¡Tarro de vidrio reciclado!")
        t.contains("frasco de perfume") -> Pair(25, "¡Frasco de vidrio reciclado!")
        
        t.contains("restos de comida") -> Pair(10, "¡Residuo orgánico separado correctamente!")
        t.contains("posos de café") -> Pair(10, "¡Posos de café compostados!")
        t.contains("tapón de corcho") -> Pair(10, "¡Tapón de corcho reciclado!")
        t.contains("bolsa de té") -> Pair(10, "¡Bolsa de té compostada!")
        t.contains("servilleta sucia") -> Pair(10, "¡Servilleta de papel compostada!")
        t.contains("plátano") || t.contains("platano") -> Pair(10, "¡Piel de plátano compostada!")
        
        t.contains("pilas y baterías") -> Pair(30, "¡Pilas/Baterías depositadas en contenedor especial!")
        t.contains("móvil / tablet") -> Pair(50, "¡Dispositivo electrónico llevado al punto RAEE!")
        t.contains("aceite usado") -> Pair(30, "¡Aceite llevado a punto de reciclaje!")
        t.contains("bombilla / led") -> Pair(30, "¡Bombilla llevada a contenedor especial!")
        t.contains("ropa usada") -> Pair(30, "¡Ropa llevada a contenedor de textiles!")
        t.contains("medicamentos") -> Pair(30, "¡Medicamentos llevados al punto SIGRE!")
        t.contains("cápsula de café") -> Pair(15, "¡Cápsula de café reciclada!")
        t.contains("radiografía") -> Pair(30, "¡Radiografía llevada al punto limpio!")
        t.contains("bote de pintura") -> Pair(30, "¡Pintura llevada al punto limpio!")
        t.contains("tostadora / batidora") -> Pair(40, "¡Aparato eléctrico llevado al punto RAEE!")
        t.contains("cd / dvd") -> Pair(15, "¡CD/DVD llevado al punto limpio!")
        t.contains("juguete roto") -> Pair(20, "¡Juguete llevado al punto limpio!")
        t.contains("termómetro") -> Pair(30, "¡Termómetro llevado al punto limpio!")
        t.contains("fluorescente") -> Pair(30, "¡Fluorescente llevado al punto limpio!")
        t.contains("bolígrafo/lápiz") -> Pair(10, "¡Bolígrafo/Lápiz llevado al punto limpio!")
        
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
            - Para envases/plásticos/metales:
              "Botella de Plástico", "Lata de Conservas", "Lata de Refresco", "Brick de Leche", "Bote de Champú", "Bolsa de Snacks", "Papel de Aluminio", "Tarrina de Yogur", "Bandeja de Corcho", "Bolsa de Plástico", "Tapón de Plástico", "Aerosol / Spray", "Chapa de Metal", "Film Transparente", "Bote Detergente"
            - Para papel y cartón:
              "Periódico / Revista", "Caja de Cartón", "Huevera de Cartón", "Sobres y Cartas", "Caja de Pizza", "Libros Viejos", "Bolsa de Harina", "Cuaderno"
            - Para vidrio:
              "Botella de Vidrio", "Tarro de Mermelada", "Frasco de Perfume", "Botella de Vino"
            - Para orgánico:
              "Restos de Comida", "Posos de Café", "Tapón de Corcho", "Bolsa de Té", "Servilleta Sucia", "Plátano"
            - Para puntos limpios / especiales / RAEE:
              "Pilas y Baterías", "Móvil / Tablet", "Aceite Usado", "Bombilla / LED", "Ropa Usada", "Medicamentos", "Cápsula de Café", "Radiografía", "Bote de Pintura", "Tostadora / Batidora", "CD / DVD", "Juguete Roto", "Termómetro", "Fluorescente", "Bolígrafo/Lápiz"

            Si el objeto es reciclable pero no coincide exactamente, usa el nombre más cercano. Si no es reciclable o es de fondo o no hay nada claro, responde con "Buscando material...", reciclable: false, confianza: 0.1.
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
        t.contains("cocacola") || t.contains("coca-cola") || t.contains("coca cola") || 
        t.contains("fanta") || t.contains("pepsi") || contienePalabraCompleta(t, "soda") || 
        t.contains("cerveza") || contienePalabraCompleta(t, "beer") || t.contains("refresco") -> "Lata de Refresco"
        
        contienePalabraCompleta(t, "leche") || contienePalabraCompleta(t, "milk") || contienePalabraCompleta(t, "llet") || t.contains("pascual") || 
        t.contains("asturiana") || t.contains("hacendado") || contienePalabraCompleta(t, "brick") || contienePalabraCompleta(t, "brik") -> "Brick de Leche"
        
        t.contains("shampoo") || t.contains("champú") || t.contains("champu") || contienePalabraCompleta(t, "gel") || 
        t.contains("pantene") || t.contains("h&s") || t.contains("dada") || t.contains("dove") -> "Bote de Champú"
        
        t.contains("detergente") || t.contains("suavizante") || t.contains("limpiador") || 
        t.contains("detergent") || t.contains("jabón platos") || t.contains("lavavajillas") -> "Bote Detergente"
        
        contienePalabraCompleta(t, "agua") || contienePalabraCompleta(t, "water") || t.contains("bezoya") || t.contains("lanjarón") || 
        t.contains("lanjaron") || t.contains("solán") || t.contains("solan") || t.contains("aquabona") -> "Botella de Plástico"
        
        contienePalabraCompleta(t, "vino") || contienePalabraCompleta(t, "wine") || t.contains("glass bottle") || t.contains("vidrio") -> "Botella de Vino"
        t.contains("mermelada") || contienePalabraCompleta(t, "jam") || t.contains("confitura") || t.contains("tarro") -> "Tarro de Mermelada"
        t.contains("perfume") || t.contains("colonia") || t.contains("fragancia") -> "Frasco de Perfume"
        
        t.contains("conservas") || t.contains("atún") || t.contains("atun") || t.contains("sardinas") || 
        contienePalabraCompleta(t, "lata") || t.contains("conserva") -> "Lata de Conservas"
        
        t.contains("yogur") || t.contains("yoghurt") || t.contains("danone") || t.contains("activia") -> "Tarrina de Yogur"
        
        t.contains("patatas") || t.contains("chips") || t.contains("snacks") || t.contains("doritos") || 
        t.contains("lay's") || t.contains("lays") || contienePalabraCompleta(t, "snack") || t.contains("bolsa patatas") -> "Bolsa de Snacks"
        
        // Papel y cartón
        t.contains("pizza") || t.contains("telepizza") || t.contains("domino's") || t.contains("dominos") -> "Caja de Pizza"
        t.contains("harina") || t.contains("flour") -> "Bolsa de Harina"
        t.contains("cuaderno") || t.contains("libreta") || t.contains("notebook") -> "Cuaderno"
        t.contains("periódico") || t.contains("periodico") || t.contains("revista") || t.contains("news") || t.contains("diario") -> "Periódico / Revista"
        t.contains("carta") || t.contains("sobre") || t.contains("correo") || t.contains("mail") -> "Sobres y Cartas"
        t.contains("libro") || t.contains("novela") || t.contains("book") -> "Libros Viejos"
        
        // Café/Té/Orgánico
        t.contains("café") || t.contains("cafe") || t.contains("coffee") || t.contains("nespresso") || t.contains("dolce") -> "Posos de Café"
        contienePalabraCompleta(t, "té") || contienePalabraCompleta(t, "te") || contienePalabraCompleta(t, "tea") || t.contains("infusión") || t.contains("infusion") -> "Bolsa de Té"
        t.contains("plátano") || t.contains("platano") || t.contains("banana") -> "Plátano"
        
        // Especiales
        contienePalabraCompleta(t, "pila") || contienePalabraCompleta(t, "pilas") || t.contains("batería") || t.contains("bateria") || contienePalabraCompleta(t, "battery") -> "Pilas y Baterías"
        t.contains("medicamento") || t.contains("medicamentos") || t.contains("pastillas") || t.contains("jarabe") || t.contains("aspirina") || t.contains("sigre") -> "Medicamentos"
        t.contains("bombilla") || contienePalabraCompleta(t, "led") || t.contains("foco") -> "Bombilla / LED"
        contienePalabraCompleta(t, "boli") || t.contains("bolígrafo") || t.contains("boligrafo") || t.contains("lápiz") || t.contains("lapiz") || t.contains("pluma") || t.contains("bic") -> "Bolígrafo/Lápiz"
        
        else -> null
    }
}

private fun esObjetoEspecial(label: String): Boolean {
    val t = label.lowercase()
    return t.contains("pilas y baterías") ||
           t.contains("móvil / tablet") ||
           t.contains("aceite usado") ||
           t.contains("bombilla / led") ||
           t.contains("ropa usada") ||
           t.contains("medicamentos") ||
           t.contains("cápsula de café") ||
           t.contains("radiografía") ||
           t.contains("bote de pintura") ||
           t.contains("tostadora / batidora") ||
           t.contains("cd / dvd") ||
           t.contains("juguete roto") ||
           t.contains("termómetro") ||
           t.contains("fluorescente") ||
           t.contains("bolígrafo/lápiz")
}

