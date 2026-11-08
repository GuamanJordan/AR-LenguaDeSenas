package com.espe.arlenguadesenas.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.espe.arlenguadesenas.vision.CalibrationState
import com.espe.arlenguadesenas.vision.LsecSignGuide
import com.espe.arlenguadesenas.vision.LsecTrainingManager
import kotlinx.coroutines.delay

/**
 * Componente modal interactivo del Estudio de Calibración y Entrenamiento Guiado LSEC.
 * Muestra la postura y descripción visual de la seña, ejecuta un temporizador de 5 segundos
 * para capturar landmarks en vivo y entrena la red neuronal en el dispositivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LsecTrainingStudioSheet(
    trainingManager: LsecTrainingManager,
    isHandDetected: Boolean,
    onDismiss: () -> Unit
) {
    val selectedSign by trainingManager.selectedSign.collectAsState()
    val state by trainingManager.state.collectAsState()
    val catalog = trainingManager.catalog

    // Corrutina para manejar la cuenta regresiva 3.. 2.. 1..
    LaunchedEffect(state) {
        val s = state
        if (s is CalibrationState.Countdown) {
            if (s.secondsLeft > 0) {
                delay(1000L)
                trainingManager.updateCountdown(s.secondsLeft - 1)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF10141D),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFF64B5F6))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera del Estudio
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0x332196F3),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎓", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Estudio de Entrenamiento LSEC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Calibra la IA con tus propias señas (5s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Carrusel de Señas LSEC Disponibles
            Text(
                text = "SELECCIONA LA SEÑA A PRACTICAR Y ENTRENAR:",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFCC80),
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                catalog.forEach { guide ->
                    val isSelected = guide.name == selectedSign.name
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x22FFFFFF),
                        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF90CAF9)) else null,
                        modifier = Modifier.clickable {
                            trainingManager.selectSign(guide)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(guide.emoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = guide.name,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenedor dinámico según el estado del entrenamiento
            when (val s = state) {
                is CalibrationState.Idle -> {
                    SignGuideCard(guide = selectedSign, isHandDetected = isHandDetected) {
                        trainingManager.startCountdown()
                    }
                }

                is CalibrationState.Countdown -> {
                    CountdownCard(seconds = s.secondsLeft)
                }

                is CalibrationState.Recording -> {
                    RecordingCard(
                        guide = selectedSign,
                        secondsLeft = s.secondsLeft,
                        sampleCount = s.sampleCount,
                        isHandDetected = isHandDetected
                    )
                }

                is CalibrationState.Completed -> {
                    CompletedCard(
                        signName = s.signName,
                        capturedCount = s.capturedCount,
                        onTrainNow = {
                            trainingManager.trainNow { _, _ -> }
                        },
                        onRetry = {
                            trainingManager.startCountdown()
                        }
                    )
                }

                is CalibrationState.Training -> {
                    TrainingProgressCard(progress = s.progressPercent)
                }

                is CalibrationState.Success -> {
                    SuccessResultCard(
                        message = s.message,
                        accuracy = s.accuracy,
                        onDone = {
                            trainingManager.resetState()
                            onDismiss()
                        },
                        onTrainAnother = {
                            trainingManager.resetState()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta del Paso 1: Muestra la guía visual fonológica con pasos e instrucciones claras.
 */
@Composable
private fun SignGuideCard(
    guide: LsecSignGuide,
    isHandDetected: Boolean,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181E29)),
        border = BorderStroke(1.dp, Color(0xFF2C394B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Título de la Seña e Insignia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(guide.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = guide.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9)
                        )
                        Text(
                            text = guide.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFCC80)
                        )
                    }
                }

                Surface(
                    color = if (guide.isBimanual) Color(0x33FF9800) else Color(0x334CAF50),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (guide.isBimanual) "2 Manos" else "1 Mano",
                        color = if (guide.isBimanual) Color(0xFFFFB74D) else Color(0xFF81C784),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Descripción Fonológica
            Text(
                text = guide.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFECEFF1),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Pasos Anatómicos
            Text(
                text = "CÓMO REALIZARLA CORRECTAMENTE:",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF80D8FF),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            guide.steps.forEach { step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Estado del Sensor de Cámara MediaPipe
            Surface(
                color = if (isHandDetected) Color(0x2200E676) else Color(0x22FF5252),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isHandDetected) Color(0xFF00E676) else Color(0xFFFF5252)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isHandDetected) Icons.Default.Check else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isHandDetected) Color(0xFF00E676) else Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHandDetected)
                            "Mano alineada en la cámara (Lista para grabar)"
                        else
                            "Coloca tu mano frente a la cámara para iniciar",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Inicio
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Iniciar Grabación de Muestras (5s)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Cuenta regresiva 3, 2, 1 antes de iniciar la captura de 5 segundos.
 */
@Composable
private fun CountdownCard(seconds: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¡PREPÁRATE!",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFCC80),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Coloca tu mano en la posición inicial",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$seconds",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Grabación activa en vivo durante 5 segundos con barra de progreso y conteo de cuadros.
 */
@Composable
private fun RecordingCard(
    guide: LsecSignGuide,
    secondsLeft: Float,
    sampleCount: Int,
    isHandDetected: Boolean
) {
    val progress = ((5.0f - secondsLeft) / 5.0f).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
        border = BorderStroke(2.dp, Color(0xFF00E5FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = Color.Red,
                    shape = CircleShape,
                    modifier = Modifier.size(12.dp)
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GRABANDO SEÑA: ${guide.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Temporizador digital grande
            Text(
                text = String.format("%.1fs", secondsLeft),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00E5FF)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de progreso animada
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF00E5FF),
                trackColor = Color(0x33FFFFFF)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Métricas en tiempo real
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$sampleCount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Cuadros 3D capturados",
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isHandDetected) "Activo" else "Buscando",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHandDetected) Color(0xFF00E676) else Color(0xFFFF5252)
                    )
                    Text(
                        text = "MediaPipe Tracking",
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
            }
        }
    }
}

/**
 * Paso 3: Muestras completadas con opción para ejecutar Backpropagation SGD On-Device.
 */
@Composable
private fun CompletedCard(
    signName: String,
    capturedCount: Int,
    onTrainNow: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2332)),
        border = BorderStroke(1.dp, Color(0xFF64B5F6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "¡Grabación Finalizada!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Se capturaron $capturedCount muestras biométricas de '$signName'.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCFD8DC),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onTrainNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
            ) {
                Icon(Icons.Default.Build, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Calibrar Red Neuronal Ahora (SGD)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x66FFFFFF))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Repetir Grabación de 5s", color = Color.White)
            }
        }
    }
}

/**
 * Visualización del progreso del entrenamiento On-Device.
 */
@Composable
private fun TrainingProgressCard(progress: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF182230))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF00E5FF),
                strokeWidth = 4.dp,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Optimizando Pesos de la Red Neuronal...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Retropropagando gradientes (Backprop SGD) en la CPU del teléfono",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Pantalla de confirmación tras el ajuste fino exitoso.
 */
@Composable
private fun SuccessResultCard(
    message: String,
    accuracy: Float,
    onDone: () -> Unit,
    onTrainAnother: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132A1F)),
        border = BorderStroke(1.5.dp, Color(0xFF00E676))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "¡Calibración Exitosa!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE8F5E9),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color(0x3300E676),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = String.format("Precisión Alcanzada: %.1f%%", accuracy),
                    color = Color(0xFFB9F6CA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
            ) {
                Text("Probar Reconocimiento en Vivo", fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onTrainAnother) {
                Text("Calibrar otra seña", color = Color(0xFF80D8FF))
            }
        }
    }
}
