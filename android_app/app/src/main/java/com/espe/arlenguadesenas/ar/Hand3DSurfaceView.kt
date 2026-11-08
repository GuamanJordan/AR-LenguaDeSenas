package com.espe.arlenguadesenas.ar

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Componente Jetpack Compose que encapsula la vista OpenGL 3D de la mano LSEC.
 * Permite rotación completa en 360 grados y zoom interactivo mediante gestos táctiles.
 */
@Composable
fun Hand3DViewer(
    activeGloss: String,
    isPlaying: Boolean = true,
    speed: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val renderer = remember { Hand3DRenderer() }

    // Actualizar parámetros reactivos del renderizador
    LaunchedEffect(activeGloss) {
        renderer.activeGloss = activeGloss
    }
    LaunchedEffect(isPlaying) {
        renderer.isPlaying = isPlaying
    }
    LaunchedEffect(speed) {
        renderer.playbackSpeed = speed
    }

    var rotX by remember { mutableStateOf(10f) }
    var rotY by remember { mutableStateOf(0f) }
    var zoomScale by remember { mutableStateOf(1.0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Control táctil de rotación 360° (arrastre con 1 dedo)
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Sensibilidad táctil suave
                    rotY += dragAmount.x * 0.45f
                    rotX = (rotX + dragAmount.y * 0.45f).coerceIn(-85f, 85f)
                    
                    renderer.rotationY = rotY
                    renderer.rotationX = rotX
                }
            }
            .pointerInput(Unit) {
                // Control táctil de zoom con pellizco (pinch to zoom)
                detectTransformGestures { _, _, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.6f, 2.5f)
                    renderer.scale = zoomScale
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
            }
        )
    }
}
