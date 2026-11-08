package com.espe.arlenguadesenas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.espe.arlenguadesenas.ui.components.ArViewContainer
import com.espe.arlenguadesenas.ui.components.AvatarControlsBar
import com.espe.arlenguadesenas.ui.components.SpeechInputBar

/**
 * Pantalla de Texto/Voz a Señas 3D (LSEC).
 * Presenta un modelo anatómico de mano humana en 3D con rotación 360°,
 * zoom táctil, y reproducción cinemática de los saludos oficiales ecuatorianos (GioSEÑAS / CONADIS).
 */
@Composable
fun TextToSignArScreen(onBack: () -> Unit) {
    var activeGloss by remember { mutableStateOf("HOLA") }
    var isPlaying by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1.0f) }

    // Saludos y expresiones oficiales de la Lengua de Señas Ecuatoriana (GioSEÑAS - CONADIS)
    val ecuadorianGreetings = remember {
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Renderizador 3D Interactivo 360° (Mano Humana Orgánica)
        ArViewContainer(
            activeGloss = activeGloss,
            isPlaying = isPlaying,
            speed = speed,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Barra Superior Estilizada y Compacta con Botón de Regreso al Menú Principal
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0xEE12141A))
                .statusBarsPadding()
                .padding(top = 8.dp, bottom = 6.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al menú principal",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Mano 3D: $activeGloss",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFCC80)
                    )
                }
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

            // Carrusel de Saludos Ecuatorianos LSEC (GioSEÑAS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ecuadorianGreetings.forEach { sign ->
                    val isSelected = sign.equals(activeGloss, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF),
                        modifier = Modifier.clickable { activeGloss = sign }
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

        // 3. Controles Inferiores: Velocidad, Pausa/Play y Barra de Entrada de Texto/Voz
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            AvatarControlsBar(
                isPlaying = isPlaying,
                playbackSpeed = speed,
                onPlayPauseToggle = { isPlaying = !isPlaying },
                onSpeedChange = { newSpeed -> speed = newSpeed },
                onResetPose = { activeGloss = "HOLA" }
            )

            SpeechInputBar(
                onSendText = { text ->
                    if (text.isNotBlank()) {
                        activeGloss = text.uppercase()
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
