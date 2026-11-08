package com.espe.arlenguadesenas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.espe.arlenguadesenas.ui.components.ArViewContainer
import com.espe.arlenguadesenas.ui.components.AvatarControlsBar
import com.espe.arlenguadesenas.ui.components.SpeechInputBar

@Composable
fun TextToSignArScreen() {
    var activeGloss by remember { mutableStateOf("HOLA") }
    var isPlaying by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1.0f) }

    val quickSigns = remember {
        listOf("HOLA", "GRACIAS", "POR-FAVOR", "AYUDA", "SI", "NO", "LETRA A", "LETRA B", "NÚMERO 1", "NÚMERO 2")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Renderizador 3D Interactivo 360° (Mano Humana Orgánica)
        ArViewContainer(
            activeGloss = activeGloss,
            isPlaying = isPlaying,
            speed = speed,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Barra Superior Estilizada y Compacta
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0xEE12141A))
                .padding(top = 10.dp, bottom = 6.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mano 3D: $activeGloss",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFCC80) // Tono cálido acorde a piel humana
                )
                Surface(
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "👆 360° táctil",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            AvatarControlsBar(
                isPlaying = isPlaying,
                currentSpeed = speed,
                onTogglePlay = { isPlaying = !isPlaying },
                onRepeat = {
                    isPlaying = false
                    isPlaying = true
                },
                onSpeedChange = { speed = it }
            )

            // Atajos de señas rápidas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickSigns.forEach { sign ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeGloss == sign) MaterialTheme.colorScheme.primary else Color(0x22FFFFFF),
                        modifier = Modifier.clickable {
                            activeGloss = sign
                            isPlaying = true
                        }
                    ) {
                        Text(
                            text = sign,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 3. Barra Inferior de Entrada (Dictado por Voz y Escritura)
        SpeechInputBar(
            onSendText = { text ->
                val clean = text.trim().uppercase()
                activeGloss = when {
                    clean.contains("HOLA") || clean.contains("BUENOS") -> "HOLA"
                    clean.contains("GRACIAS") -> "GRACIAS"
                    clean.contains("FAVOR") -> "POR-FAVOR"
                    clean.contains("AYUDA") -> "AYUDA"
                    clean.contains("SI") || clean.contains("SÍ") -> "SI"
                    clean.contains("NO") -> "NO"
                    else -> clean
                }
                isPlaying = true
            },
            onStartVoice = {
                // Entrada por voz activa
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
