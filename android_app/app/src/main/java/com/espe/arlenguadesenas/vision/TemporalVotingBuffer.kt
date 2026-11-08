package com.espe.arlenguadesenas.vision

import java.util.ArrayDeque

/**
 * Filtro temporal con amortiguación e histéresis fonológica para la LSEC.
 * Elimina el parpadeo de etiquetas entre fotogramas sucesivos y requiere un
 * consenso mínimo antes de realizar transiciones de señas.
 */
class TemporalVotingBuffer(private val windowSize: Int = 6) {
    private val queue = ArrayDeque<String>(windowSize)
    private var lastStableLabel = "MUESTRE SU MANO A LA CÁMARA"
    private var consecutiveAbsenceCount = 0

    fun addPrediction(label: String): String {
        if (label.contains("BUSCANDO") || label.contains("INICIALIZANDO")) {
            consecutiveAbsenceCount++
            // Solo tras 8 fotogramas consecutivos sin mano se resetea la visualización
            if (consecutiveAbsenceCount > 8) {
                lastStableLabel = "MUESTRE SU MANO A LA CÁMARA"
                queue.clear()
            }
            return lastStableLabel
        }

        consecutiveAbsenceCount = 0

        if (queue.size >= windowSize) {
            queue.poll()
        }
        queue.offer(label)

        // Contar frecuencia de cada predicción en la ventana temporal
        val counts = mutableMapOf<String, Int>()
        for (item in queue) {
            counts[item] = (counts[item] ?: 0) + 1
        }

        var topLabel = lastStableLabel
        var maxCount = 0
        for ((candLabel, count) in counts) {
            if (count > maxCount) {
                maxCount = count
                topLabel = candLabel
            }
        }

        // Histéresis: se requiere que al menos la mitad de la ventana coincida para cambiar de seña
        val consensusThreshold = (windowSize / 2).coerceAtLeast(2)
        if (maxCount >= consensusThreshold && topLabel != "SEÑA LSEC EN CURSO") {
            lastStableLabel = topLabel
        }

        return lastStableLabel
    }

    fun clear() {
        queue.clear()
        lastStableLabel = "MUESTRE SU MANO A LA CÁMARA"
        consecutiveAbsenceCount = 0
    }
}
