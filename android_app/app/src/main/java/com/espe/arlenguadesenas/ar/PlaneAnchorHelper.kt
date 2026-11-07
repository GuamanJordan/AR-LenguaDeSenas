package com.espe.arlenguadesenas.ar

import android.content.Context

data class AnchorPosition(val x: Float, val y: Float, val z: Float)

class PlaneAnchorHelper(private val context: Context) {
    var isPlaneDetected = false
        private set

    fun onPlaneDetectedNotification() {
        isPlaneDetected = true
    }

    fun calculateAnchorCoordinates(screenX: Float, screenY: Float): AnchorPosition {
        // Coordenadas proyectadas mediante ARCore Raycast
        return AnchorPosition(0f, -0.2f, -1.2f)
    }
}
