package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.EcoLensConfig
import com.rubensimon.ecolens.ui.components.EcoColors
import com.rubensimon.ecolens.ui.components.GlassButton
import com.rubensimon.ecolens.utils.HistoryManager
import com.rubensimon.ecolens.utils.PointsManager
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.QuartzCore.*
import platform.UIKit.*
import platform.darwin.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformCameraView(
    modifier: Modifier,
    onScanComplete: (objectName: String, points: Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }

    val cameraState = remember { CameraSessionState() }
    val hasCamera = cameraState.hasCamera

    DisposableEffect(Unit) {
        cameraState.setupSession()
        onDispose {
            cameraState.stopSession()
        }
    }

    Box(modifier = modifier) {
        if (hasCamera) {
            // Dispositivo real con cámara
            UIKitView(
                factory = {
                    val previewLayer = AVCaptureVideoPreviewLayer(session = cameraState.session)
                    previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill

                    val container = UIView(CGRectMake(0.0, 0.0, 400.0, 800.0))
                    container.backgroundColor = UIColor.blackColor
                    previewLayer.frame = container.bounds
                    container.layer.addSublayer(previewLayer)
                    cameraState.previewLayer = previewLayer
                    container
                },
                update = { view ->
                    cameraState.previewLayer?.frame = view.bounds
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Simulador sin cámara — placeholder visual
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📷\n\nSimulador detectado\nLa cámara no está disponible.\nPulsa el botón para simular un escaneo.",
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }
        }

        if (resultMessage.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                color = EcoColors.CardBackground,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = resultMessage,
                    color = EcoColors.TextPrimary,
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        GlassButton(
            onClick = {
                if (!isScanning) {
                    isScanning = true
                    resultMessage = "⏳ Analizando..."
                    scope.launch {
                        try {
                            if (hasCamera) {
                                val jpgData = cameraState.capturePhoto()
                                val (label, pointsEarned, msg) = uploadImageToPredict(jpgData)

                                resultMessage = "✅ $label (+$pointsEarned pts)\n$msg"
                                PointsManager.addPoints(pointsEarned, "scan")
                                PointsManager.incrementScans(label)
                                HistoryManager.addHistoryItem(label, pointsEarned, PointsManager.getUserId())
                                onScanComplete(label, pointsEarned)
                            } else {
                                // Simulador: escaneo de prueba
                                delay(1500)
                                val label = "Bottle"
                                val pts = 50
                                resultMessage = "✅ $label (+$pts pts)\n(Simulación)"
                                PointsManager.addPoints(pts, "scan")
                                PointsManager.incrementScans(label)
                                HistoryManager.addHistoryItem(label, pts, PointsManager.getUserId())
                                onScanComplete(label, pts)
                            }
                            isScanning = false
                        } catch (e: Exception) {
                            resultMessage = "❌ Error: ${e.message}"
                            isScanning = false
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(72.dp),
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

@OptIn(ExperimentalForeignApi::class)
class CameraSessionState : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
    val session = AVCaptureSession()
    private val photoOutput = AVCapturePhotoOutput()
    var previewLayer: AVCaptureVideoPreviewLayer? = null

    // Detectar si hay cámara disponible (false en simulador)
    val hasCamera: Boolean = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null

    private val cameraQueue = dispatch_queue_create("com.rubensimon.ecolens.camera", null)

    fun setupSession() {
        if (!hasCamera) {
            println("Camera: No camera device available (simulator)")
            return
        }

        dispatch_async(cameraQueue) {
            if (session.inputs.isNotEmpty()) return@dispatch_async

            session.beginConfiguration()

            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            if (device != null) {
                val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) as? AVCaptureDeviceInput
                if (input != null && session.canAddInput(input)) {
                    session.addInput(input)
                }
            }

            if (session.canAddOutput(photoOutput)) {
                session.addOutput(photoOutput)
            }

            session.commitConfiguration()
            session.startRunning()
            println("Camera: Session configured and running")
        }
    }

    fun stopSession() {
        if (!hasCamera) return
        dispatch_async(cameraQueue) {
            if (session.isRunning()) {
                session.stopRunning()
            }
        }
    }

    private var captureContinuation: kotlin.coroutines.Continuation<NSData>? = null

    suspend fun capturePhoto(): NSData = suspendCoroutine { cont ->
        if (captureContinuation != null) {
            cont.resumeWithException(IllegalStateException("Capture already in progress"))
            return@suspendCoroutine
        }

        captureContinuation = cont

        dispatch_async(cameraQueue) {
            if (!session.isRunning()) {
                captureContinuation = null
                cont.resumeWithException(IllegalStateException("Camera session is not running"))
                return@dispatch_async
            }

            val settings = AVCapturePhotoSettings.photoSettings()
            photoOutput.capturePhotoWithSettings(settings, this@CameraSessionState)
            println("Camera: capturePhotoWithSettings dispatched")
        }
    }

    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        val cont = captureContinuation
        captureContinuation = null

        if (cont == null) return

        if (error != null) {
            cont.resumeWithException(Exception(error.localizedDescription))
        } else {
            val data = didFinishProcessingPhoto.fileDataRepresentation()
            if (data != null) {
                cont.resume(data)
            } else {
                cont.resumeWithException(Exception("Failed to get photo data"))
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
suspend fun uploadImageToPredict(imageData: NSData): Triple<String, Int, String> = withContext(Dispatchers.IO) {
    suspendCoroutine { cont ->
        val boundary = "----WebKitFormBoundaryE19zNvXGzXaLvS5C"
        val url = NSURL.URLWithString("${EcoLensConfig.ML_BACKEND_URL}predict")!!
        val request = NSMutableURLRequest.requestWithURL(url)
        request.HTTPMethod = "POST"
        request.setValue("multipart/form-data; boundary=$boundary", forHTTPHeaderField = "Content-Type")

        val body = NSMutableData()

        // Add image file part
        val fileBoundaryStr = "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n"
        body.appendData((fileBoundaryStr as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!)
        body.appendData(imageData)
        body.appendData(("\r\n" as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!)

        // Add username part
        val username = PointsManager.getUserId()
        val usernameStr = "--$boundary\r\nContent-Disposition: form-data; name=\"username\"\r\n\r\n$username\r\n"
        body.appendData((usernameStr as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!)

        // End boundary
        body.appendData(("--$boundary--\r\n" as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!)

        request.HTTPBody = body

        val session = NSURLSession.sharedSession
        val task = session.dataTaskWithRequest(request) { data, response, error ->
            if (error != null) {
                cont.resumeWithException(Exception(error.localizedDescription))
                return@dataTaskWithRequest
            }
            if (data != null) {
                val jsonStr = NSString.create(data, NSUTF8StringEncoding) as String
                try {
                    // Limpiamos la respuesta de posibles espacios o saltos de línea
                    val cleanJson = jsonStr.trim()
                    
                    if (cleanJson == "{}" || cleanJson.isBlank() || !cleanJson.startsWith("{")) {
                        cont.resume(Triple("Objeto desconocido", 0, "No he podido identificar el objeto. ¡Prueba con más luz!"))
                        return@dataTaskWithRequest
                    }

                    // Extracción más robusta de campos
                    val label = if (cleanJson.contains("\"label\"")) {
                        cleanJson.substringAfter("\"label\":\"").substringBefore("\"")
                            .substringAfter("\"label\":").substringBefore(",").trim('"', ' ', ':')
                    } else "Objeto"
                    
                    val ptsStr = if (cleanJson.contains("\"points_earned\"")) {
                        cleanJson.substringAfter("\"points_earned\":").substringBefore(",")
                            .substringBefore("}").trim('"', ' ', ':')
                    } else "10"
                    
                    val msg = if (cleanJson.contains("\"message\"")) {
                        cleanJson.substringAfter("\"message\":\"").substringBefore("\"")
                            .substringAfter("\"message\":").substringBefore(",").trim('"', ' ', ':')
                    } else "Objeto procesado correctamente"

                    // Limpieza final de la etiqueta para evitar basura de JSON
                    val finalLabel = label.replace("{", "").replace("}", "").replace("\"", "").trim()
                    val displayLabel = if (finalLabel.isBlank() || finalLabel == "null") "Objeto" else finalLabel
                    
                    val points = ptsStr.toIntOrNull() ?: 10
                    
                    println("[iOS Predict] Resultado: $displayLabel, Puntos: $points")
                    cont.resume(Triple(displayLabel, points, msg))
                } catch (e: Exception) {
                    println("Decode Error: ${e.message} in $jsonStr")
                    cont.resume(Triple("Objeto", 10, "Error al procesar la respuesta de la IA"))
                }
            } else {
                cont.resumeWithException(Exception("Empty response"))
            }
        }
        task.resume()
    }
}
