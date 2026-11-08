package com.espe.arlenguadesenas.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.espe.arlenguadesenas.ar.Hand3DViewer

/**
 * Contenedor de Realidad Aumentada y Visualizador 3D interactivo.
 * Proyecta la mano o manos en 3D con rotación orbital libre 360° y animación gestual.
 */
@Composable
fun ArViewContainer(
    activeGloss: String,
    isPlaying: Boolean = true,
    speed: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Hand3DViewer(
            activeGloss = activeGloss,
            isPlaying = isPlaying,
            speed = speed,
            modifier = Modifier.fillMaxSize()
        )
    }
}
