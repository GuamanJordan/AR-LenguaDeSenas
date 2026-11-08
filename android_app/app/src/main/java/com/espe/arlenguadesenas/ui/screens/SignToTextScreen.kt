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
import com.espe.arlenguadesenas.vision.HandLandmarkerHelper
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

    var useFrontCamera by remember { mutableStateOf(false) } // Trasera para enfocar al interlocutor
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
            // Vista de Cámara Real analizada con MediaPipe
            CameraPreview(
                useFrontCamera = useFrontCamera,
                analyzer = handAnalyzer
            )

            // Retícula de encuadre
            RecognitionOverlay(isHandDetected = confidence > 0.70f)

            // Botón para alternar cámara Frontal / Trasera
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

            // Panel inferior con resultado en tiempo real y lectura TTS
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xDD000000))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ConfidenceBadge(confidence = confidence)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = detectedGloss,
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
