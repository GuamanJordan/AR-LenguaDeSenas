package com.espe.arlenguadesenas.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.espe.arlenguadesenas.ui.components.AccessibleButton
import com.espe.arlenguadesenas.ui.components.CameraPreview
import com.espe.arlenguadesenas.ui.components.ConfidenceBadge
import com.espe.arlenguadesenas.ui.components.RecognitionOverlay
import com.espe.arlenguadesenas.vision.TFLiteClassifier
import com.espe.arlenguadesenas.vision.TemporalVotingBuffer

@Composable
fun SignToTextScreen(onSpeakResult: (String) -> Unit) {
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

    var useFrontCamera by remember { mutableStateOf(false) } // Trasera por defecto para apuntar a la persona
    var detectedGloss by remember { mutableStateOf("DETECTANDO...") }
    var confidence by remember { mutableStateOf(0.88f) }

    val classifier = remember { TFLiteClassifier(context) }
    val votingBuffer = remember { TemporalVotingBuffer(windowSize = 5) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            // Vista de Cámara Real CameraX
            CameraPreview(
                useFrontCamera = useFrontCamera,
                onFrameAnalyzed = { rawLandmarks ->
                    val (label, prob) = classifier.classify(rawLandmarks)
                    val smoothLabel = votingBuffer.addPrediction(label)
                    detectedGloss = smoothLabel
                    confidence = prob
                }
            )

            // Retícula de encuadre
            RecognitionOverlay(isHandDetected = true)

            // Botón para alternar entre Cámara Frontal y Trasera
            IconButton(
                onClick = { useFrontCamera = !useFrontCamera },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .background(Color(0x88000000), shape = CircleShape)
            ) {
                Icon(
                    Icons.Default.Cameraswitch,
                    contentDescription = "Cambiar cámara frontal/trasera",
                    tint = Color.White
                )
            }

            // Panel inferior con resultado de la seña y botón TTS
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ConfidenceBadge(confidence = confidence)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Seña detectada: $detectedGloss",
                    style = MaterialTheme.typography.titleLarge,
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
            // Pantalla informativa si no tiene permisos
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
