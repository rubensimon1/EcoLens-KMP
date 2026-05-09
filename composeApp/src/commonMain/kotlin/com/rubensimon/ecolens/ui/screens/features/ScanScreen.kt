package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.ui.components.*

/**
 * Pantalla de escaneo con cámara — migrada de MainActivity.
 *
 * ### Estrategia KMP (expect/actual)
 * La cámara es específica de la plataforma:
 * - `commonMain`: UI shell + expect fun CameraView (este archivo)
 * - `androidMain`: CameraView usando CameraX (CameraView.android.kt)
 * - `iosMain`: CameraView usando AVFoundation (CameraView.ios.kt)
 *
 * El resultado de la predicción (PredictionResponse) se devuelve
 * a través del callback [onScanComplete] con el nombre del objeto detectado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    isSddr: Boolean = false,
    onBackClick: () -> Unit,
    onScanComplete: (objectName: String, points: Int) -> Unit = { _, _ -> }
) {
    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(EcoColors.BackgroundDark).statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Text(
                            "📷 Escanear",
                            color = EcoColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Atrás",
                                tint = EcoColors.TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EcoColors.BackgroundDark,
                        titleContentColor = EcoColors.TextPrimary,
                        navigationIconContentColor = EcoColors.TextPrimary
                    )
                )
            }
        },
        containerColor = EcoColors.BackgroundDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Vista de la cámara (implementado por plataforma)
            PlatformCameraView(
                modifier = Modifier.fillMaxSize(),
                isSddr = isSddr,
                onScanComplete = onScanComplete
            )
        }
    }
}

/**
 * Composable de cámara definido como expect.
 * Cada plataforma provee su implementación `actual`.
 *
 * @param modifier Modificador de tamaño/layout
 * @param onScanComplete Callback con (objectName, pointsEarned) cuando hay resultado
 */
@Composable
expect fun PlatformCameraView(
    modifier: Modifier,
    isSddr: Boolean = false,
    onScanComplete: (objectName: String, points: Int) -> Unit
)
