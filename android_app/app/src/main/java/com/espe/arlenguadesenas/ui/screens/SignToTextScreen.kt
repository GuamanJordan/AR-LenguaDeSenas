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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    val votingBuffer = remember { TemporalVotingBuffer(windowSize = 4) }

    val handAnalyzer = remember(context) {
        HandLandmarkerHelper(context) { _, predictedSign, prob, _ ->
            if (prob > 0.65f) {
                val smoothed = votingBuffer.addPrediction(predictedSign)
                detectedGloss = smoothed
                confidence = prob
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            // 1. Vista de Cámara en Vivo con MediaPipe
            CameraPreview(
                useFrontCamera = useFrontCamera,
                analyzer = handAnalyzer
            )

            // Retícula guía de encuadre
            RecognitionOverlay(isHandDetected = confidence > 0.70f)

            // 2. Barra Superior: Botón de Regreso al Menú y Botón de Cámara
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón para volver al Menú Principal
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color(0x99000000), shape = CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar al menú principal",
                        tint = Color.White
                    )
                }

                // Botón para alternar cámara Frontal / Trasera
                IconButton(
                    onClick = { useFrontCamera = !useFrontCamera },
                    modifier = Modifier.background(Color(0x99000000), shape = CircleShape)
                ) {
                    Icon(
                        Icons.Default.Cameraswitch,
                        contentDescription = "Cambiar cámara frontal/trasera",
                        tint = Color.White
                    )
                }
            }

            // 3. Ventana Flotante en la Esquina (PiP): Mano 3D Humana ejecutando la seña en tiempo real
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 150.dp)
                    .size(width = 135.dp, height = 180.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xEE101218),
                border = BorderStroke(2.dp, Color(0xFF64B5F6)),
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Mano 3D interactiva sincronizada con la seña detectada
                    Hand3DViewer(
                        activeGloss = detectedGloss,
                        isPlaying = true,
                        speed = 1.0f,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Etiqueta superior de la miniatura 3D
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x99000000)
                    ) {
                        Text(
                            text = "Réplica 3D en vivo",
                            color = Color(0xFFFFCC80),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 4. Panel Inferior con el significado reconocido y botón TTS
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xEE000000))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ConfidenceBadge(confidence = confidence)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Seña: $detectedGloss",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                AccessibleButton(
                    text = "Pronunciar en voz alta (TTS)",
                    contentDescription = "Leer traduccion por los altavoces",
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
                    text = "La aplicación necesita la cámara para capturar y traducir las señas de la Lengua de Señas Ecuatoriana (LSEC) en tiempo real.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                AccessibleButton(
                    text = "Conceder Permiso de Cámara",
                    contentDescription = "Solicitar permiso de camara al sistema",
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
        }
    }
}
