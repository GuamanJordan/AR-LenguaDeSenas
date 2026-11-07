package com.espe.arlenguadesenas.vision

import kotlin.math.sqrt

object LandmarkNormalizer {

    fun normalize(landmarks: FloatArray): FloatArray {
        if (landmarks.size != 63) {
            throw IllegalArgumentException("Se requieren 63 valores (21 puntos x 3 coordenadas)")
        }

        val result = FloatArray(63)
        val wristX = landmarks[0]
        val wristY = landmarks[1]
        val wristZ = landmarks[2]

        // Trasladar muñeca al origen (0,0,0)
        for (i in 0 until 21) {
            result[i * 3] = landmarks[i * 3] - wristX
            result[i * 3 + 1] = landmarks[i * 3 + 1] - wristY
            result[i * 3 + 2] = landmarks[i * 3 + 2] - wristZ
        }

        // Calcular distancia al nudillo del dedo medio (punto 9)
        val mX = result[9 * 3]
        val mY = result[9 * 3 + 1]
        val mZ = result[9 * 3 + 2]
        var dist = sqrt((mX * mX + mY * mY + mZ * mZ).toDouble()).toFloat()
        if (dist < 1e-5f) dist = 1.0f

        // Escalar uniformemente
        for (i in 0 until 63) {
            result[i] /= dist
        }

        return result
    }
}
