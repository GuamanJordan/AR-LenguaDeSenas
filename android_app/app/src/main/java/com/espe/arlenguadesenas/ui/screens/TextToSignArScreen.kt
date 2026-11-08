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
        listOf("HOLA", "GRACIAS", "POR-FAVOR", "AYUDA", "SI", "NO", "A", "B", "C", "1", "2")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Renderizador 3D Interactivo 360°
        ArViewContainer(
            activeGloss = activeGloss,
            isPlaying = isPlaying,
            speed = speed,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Panel Superior: Título, Controles y Sugerencia de Rotación
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(top = 12.dp, bottom = 8.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mano 3D LSEC: $activeGloss",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF64B5F6)
            )

            // Indicador de control 360°
            Surface(
                color = Color(0x66FFFFFF),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "👆 Arrastra para rotar 360° | Pellizca para zoom",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
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

            // Fila de botones rápidos de prueba
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickSigns.forEach { sign ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeGloss == sign) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF),
                        modifier = Modifier.clickable {
                            activeGloss = sign
                            isPlaying = true
                        }
                    ) {
                        Text(
                            text = sign,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 3. Barra Inferior de Entrada (Voz y Texto)
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
                // Activar seña con entrada de voz
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
