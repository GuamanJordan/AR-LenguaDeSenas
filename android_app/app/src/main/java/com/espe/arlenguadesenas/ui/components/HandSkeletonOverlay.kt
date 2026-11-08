package com.espe.arlenguadesenas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

/**
 * Dibuja el esqueleto anatómico de la mano en tiempo real sobre la vista de cámara (CameraX).
 * Proporciona retroalimentación visual inmediata al usuario de los 21 landmarks detectados
 * por MediaPipe Tasks, facilitando el encuadre y diagnóstico visual.
 */
@Composable
fun HandSkeletonOverlay(
    landmarks: FloatArray,
    isDetected: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isDetected || landmarks.size < 63) return

    val connections = listOf(
        // Pulgar
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),
        // Índice
        Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),
        // Medio
        Pair(0, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),
        // Anular
        Pair(0, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),
        // Meñique
        Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20),
        // Base de la palma
        Pair(5, 9), Pair(9, 13), Pair(13, 17)
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Dibujar conexiones (huesos) en color cian translúcido
        for (conn in connections) {
            val iA = conn.first
            val iB = conn.second

            val xA = landmarks[iA * 3] * w
            val yA = landmarks[iA * 3 + 1] * h
            val xB = landmarks[iB * 3] * w
            val yB = landmarks[iB * 3 + 1] * h

            drawLine(
                color = Color(0xDD00E5FF),
                start = Offset(xA, yA),
                end = Offset(xB, yB),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        // 2. Dibujar articulaciones (21 landmarks)
        for (i in 0 until 21) {
            val x = landmarks[i * 3] * w
            val y = landmarks[i * 3 + 1] * h

            val dotColor = when (i) {
                0 -> Color(0xFFFFD54F) // Muñeca en amarillo dorado
                4, 8, 12, 16, 20 -> Color(0xFFFF4081) // Yemas de dedos en magenta vibrante
                else -> Color(0xFF00E676) // Articulaciones intermedias en verde neón
            }

            val radius = if (i in listOf(0, 4, 8, 12, 16, 20)) 10f else 7f

            drawCircle(
                color = Color(0x99000000),
                radius = radius + 2f,
                center = Offset(x, y)
            )
            drawCircle(
                color = dotColor,
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}
