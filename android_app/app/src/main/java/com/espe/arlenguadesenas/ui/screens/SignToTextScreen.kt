package com.espe.arlenguadesenas.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.espe.arlenguadesenas.ar.Hand3DViewer
import com.espe.arlenguadesenas.ui.components.AccessibleButton
import com.espe.arlenguadesenas.ui.components.CameraPreview
import com.espe.arlenguadesenas.ui.components.ConfidenceBadge
import com.espe.arlenguadesenas.ui.components.HandSkeletonOverlay
import com.espe.arlenguadesenas.ui.components.RecognitionOverlay
import com.espe.arlenguadesenas.vision.HandLandmarkerHelper
import com.espe.arlenguadesenas.vision.TemporalVotingBuffer

/**
 * Pantalla de Reconocimiento Bidireccional: Señas a Texto / Voz.
 * Cuenta con:
 * - Análisis fluido a 60 FPS con CameraX y MediaPipe Tasks (Red Neuronal Deep Learning).
 * - Dibujo del esqueleto anatómico de la mano (21 landmarks) en tiempo real con HandSkeletonOverlay.
 * - Modo de Prueba y Calibración interactivo para testear cada seña ecuatoriana.
 * - Réplica tridimensional (PiP) en la esquina inferior con la seña ecuatoriana activa.
 * - Botón de regreso al Menú Principal y selector de cámara frontal/trasera.
 */
@Composable
fun SignToTextScreen(
    onBack: () -> Unit,
    onSpeakResult: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var useFrontCamera by remember { mutableStateOf(false) }
    var isTestModeActive by remember { mutableStateOf(false) }
    var detectedGloss by remember { mutableStateOf("MUESTRE SU MANO A LA CÁMARA") }
    var confidence by remember { mutableStateOf(0.85f) }
    var currentLandmarks by remember { mutableStateOf(FloatArray(63)) }
    var isHandPresent by remember { mutableStateOf(false) }

    val votingBuffer = remember { TemporalVotingBuffer(windowSize = 5) }

    val testGreetings = remember {
        listOf(
            "HOLA",
            "BUENOS DÍAS",
            "BUENAS TARDES",
            "BUENAS NOCHES",
            "CHAO",
            "¿CÓMO ESTÁS?",
            "MUCHO GUSTO",
            "GRACIAS",
            "POR FAVOR",
            "DE NADA",
            "AYUDA",
            "SÍ",
            "NO",
            "TE QUIERO"
        )
    }

    val handAnalyzer = remember(context) {
        HandLandmarkerHelper(context, isFrontCamera = useFrontCamera) { rawLandmarks, predictedSign, prob, _ ->
            currentLandmarks = rawLandmarks
            val hasValidPoints = rawLandmarks.any { it != 0f }
            isHandPresent = hasValidPoints

            if (prob >= 0.45f && hasValidPoints && !isTestModeActive) {
                val smoothed = votingBuffer.addPrediction(predictedSign)
                detectedGloss = smoothed
                confidence = prob
                android.util.Log.i("LSEC_BUFFER", "Seña Final: '$smoothed' (Certeza: ${(prob * 100).toInt()}%)")
            }
        }
    }

    LaunchedEffect(useFrontCamera) {
        handAnalyzer.isFrontCamera = useFrontCamera
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            // 1. Vista de Cámara en Vivo (CameraX)
            CameraPreview(
                useFrontCamera = useFrontCamera,
                analyzer = handAnalyzer
            )

            // 2. Esqueleto anatómico 3D de los 21 landmarks en tiempo real (Visual Feedback)
            HandSkeletonOverlay(
                landmarks = currentLandmarks,
                isDetected = isHandPresent
            )

            // Retícula guía de encuadre ergonómica
            RecognitionOverlay(isHandDetected = isHandPresent)

            // 3. Barra Superior con Controles y Modo Prueba
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón al Menú Principal
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0xCC10121A), shape = CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al menú principal",
                            tint = Color.White
                        )
                    }

                    // Botón para Activar Modo de Prueba / Calibración
                    Surface(
                        color = if (isTestModeActive) MaterialTheme.colorScheme.primary else Color(0xCC10121A),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { isTestModeActive = !isTestModeActive }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = "Modo de Prueba",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTestModeActive) "Modo Test Activo" else "🧪 Modo Test",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Botón alternar cámara
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0xCC10121A), shape = CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Cameraswitch,
                            contentDescription = "Cambiar entre cámara frontal y trasera",
                            tint = Color.White
                        )
                    }
                }

                // Panel desplegable del Modo de Prueba (simulación y verificación automática)
                if (isTestModeActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xDD0D1117),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF64B5F6))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Toque una seña para verificar inferencia, 3D y voz:",
                                color = Color(0xFFFFCC80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                testGreetings.forEach { sign ->
                                    val isSelected = sign == detectedGloss
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x44FFFFFF),
                                        modifier = Modifier.clickable {
                                            detectedGloss = sign
                                            confidence = 0.99f
                                            onSpeakResult(sign)
                                        }
                                    ) {
                                        Text(
                                            text = sign,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Réplica 3D en Vivo en la Esquina (PiP) con Significado en Tiempo Real
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 145.dp)
                    .size(width = 140.dp, height = 185.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xEE101218),
                border = BorderStroke(2.dp, Color(0xFF64B5F6)),
                shadowElevation = 12.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Hand3DViewer(
                        activeGloss = detectedGloss,
                        isPlaying = true,
                        speed = 1.0f,
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC000000)
                    ) {
                        Text(
                            text = "🤟 ${detectedGloss.take(12)}",
                            color = Color(0xFFFFCC80),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x99000000)
                    ) {
                        Text(
                            text = if (isHandPresent) "Seguimiento Activo" else "Buscando Mano...",
                            color = if (isHandPresent) Color(0xFF00E676) else Color.White,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // 5. Panel Inferior con el significado reconocido y botón TTS accesible
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xEE0B0D13))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ConfidenceBadge(confidence = confidence)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detectedGloss,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                AccessibleButton(
                    text = "Pronunciar en voz alta (TTS)",
                    contentDescription = "Leer traducción por el altavoz",
                    onClick = { onSpeakResult(detectedGloss) }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Se requiere acceso a la Cámara",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "La aplicación requiere la cámara para capturar y traducir las señas de la Lengua de Señas Ecuatoriana (LSEC) en tiempo real con MediaPipe.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                AccessibleButton(
                    text = "Conceder Permiso de Cámara",
                    contentDescription = "Solicitar permiso de cámara al sistema",
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
        }
    }
}
