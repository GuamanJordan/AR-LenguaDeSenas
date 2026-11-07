package com.espe.arlenguadesenas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.espe.arlenguadesenas.ui.components.AccessibleButton
import com.espe.arlenguadesenas.ui.components.CameraPreview
import com.espe.arlenguadesenas.ui.components.ConfidenceBadge
import com.espe.arlenguadesenas.ui.components.RecognitionOverlay

@Composable
fun SignToTextScreen(onSpeakResult: (String) -> Unit) {
    var detectedGloss by remember { mutableStateOf("HOLA") }
    var confidence by remember { mutableStateOf(0.92f) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview()
        RecognitionOverlay(isHandDetected = true)

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
    }
}
