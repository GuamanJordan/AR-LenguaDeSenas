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
import com.espe.arlenguadesenas.ui.components.ArViewContainer
import com.espe.arlenguadesenas.ui.components.AvatarControlsBar
import com.espe.arlenguadesenas.ui.components.SpeechInputBar

@Composable
fun TextToSignArScreen() {
    var activeGloss by remember { mutableStateOf("GRACIAS") }
    var isPlaying by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1.0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        ArViewContainer()

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Avatar 3D en RA: $activeGloss",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            AvatarControlsBar(
                isPlaying = isPlaying,
                currentSpeed = speed,
                onTogglePlay = { isPlaying = !isPlaying },
                onRepeat = { isPlaying = true },
                onSpeedChange = { speed = it }
            )
        }

        SpeechInputBar(
            onSendText = { activeGloss = it.uppercase() },
            onStartVoice = { /* Activa SpeechRecognizer */ },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
