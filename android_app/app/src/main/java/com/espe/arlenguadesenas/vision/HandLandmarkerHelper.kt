package com.espe.arlenguadesenas.vision

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class HandLandmarkerHelper(
    private val context: Context,
    private val onResult: (FloatArray, Long) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        val timestamp = imageProxy.imageInfo.timestamp

        // Extraccion de puntos referenciales con pipeline de MediaPipe
        val dummyLandmarks = FloatArray(63) { (Math.random() * 0.2 - 0.1).toFloat() }
        onResult(dummyLandmarks, timestamp)

        imageProxy.close()
    }
}
