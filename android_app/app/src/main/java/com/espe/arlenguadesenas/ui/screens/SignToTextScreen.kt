package com.espe.arlenguadesenas.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
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
import com.espe.arlenguadesenas.ui.components.RecognitionOverlay
import com.espe.arlenguadesenas.vision.HandLandmarkerHelper
import com.espe.arlenguadesenas.vision.TemporalVotingBuffer

/**
 * Pantalla de Reconocimiento Bidireccional: Señas a Texto / Voz.
 * Cuenta con:
 * - Análisis fluido a 60 FPS con CameraX y MediaPipe optimizado.
 * - Réplica tridimensional (PiP) en la esquina inferior con la seña ecuatoriana activa.
 * - Botón de regreso al Menú Principal y selector de cámara frontal/trasera.
 * - Síntesis de voz (TTS) para lectura accesible.
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
    var detectedGloss by remember { mutableStateOf("MUESTRE SU MANO A LA CÁMARA") }
    var confidence by remember { mutableStateOf(0.85f) }

    val votingBuffer = remember { TemporalVotingBuffer(windowSize = 5) }

    val handAnalyzer = remember(context) {
        HandLandmarkerHelper(context, isFrontCamera = useFrontCamera) { _, predictedSign, prob, _ ->
            if (prob > 0.60f) {
                val smoothed = votingBuffer.addPrediction(predictedSign)
                detectedGloss = smoothed
                confidence = prob
            }
        }
    }

    // Actualizar lateralidad del espejo cuando el usuario cambia de cámara
    LaunchedEffect(useFrontCamera) {
        handAnalyzer.isFrontCamera = useFrontCamera
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            // 1. Vista de Cámara en Vivo de Alta Velocidad (CameraX)
            CameraPreview(
                useFrontCamera = useFrontCamera,
                analyzer = handAnalyzer
            )

            // Retícula guía de encuadre ergonómica
            RecognitionOverlay(isHandDetected = confidence > 0.70f && !detectedGloss.contains("MUESTRE"))

            // 2. Barra Superior: Botón de Regreso al Menú Principal y Cambio de Cámara
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón al Menú Principal
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xCC10121A), shape = CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar al menú principal",
                        tint = Color.White
                    )
                }

                Surface(
                    color = Color(0xCC10121A),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (useFrontCamera) "Cámara Frontal (Selfie)" else "Cámara Trasera",
                        color = Color(0xFF90CAF9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Botón alternar cámara
                IconButton(
                    onClick = { useFrontCamera = !useFrontCamera },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xCC10121A), shape = CircleShape)
                ) {
                    Icon(
                        Icons.Default.Cameraswitch,
                        contentDescription = "Cambiar entre cámara frontal y trasera",
                        tint = Color.White
                    )
                }
            }

            // 3. Réplica 3D en Vivo en la Esquina (PiP) con Significado en Tiempo Real
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
                    // Mano 3D anatómica animada con la seña detectada
                    Hand3DViewer(
                        activeGloss = detectedGloss,
                        isPlaying = true,
                        speed = 1.0f,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Etiqueta superior de la miniatura 3D con significado inmediato
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

                    // Etiqueta inferior informativa
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x99000000)
                    ) {
                        Text(
                            text = "3D LSEC en vivo",
                            color = Color.White,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // 4. Panel Inferior con el significado reconocido y botón TTS accesible
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
                    icon = Icons.Default.VolumeUp,
                    onClick = { onSpeakResult(detectedGloss) }
                )
            }
        } else {
            // Pantalla de solicitud de permisos
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
