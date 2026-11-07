package com.espe.arlenguadesenas.domain.usecase

import com.espe.arlenguadesenas.domain.model.GestureRecognitionResult

class RecognizeSignUseCase {
    operator fun invoke(probabilities: FloatArray, labels: List<String>, threshold: Float = 0.70f): GestureRecognitionResult {
        if (probabilities.isEmpty() || labels.isEmpty()) {
            return GestureRecognitionResult("DESCONOCIDO", 0f, false)
        }

        var maxIdx = 0
        var maxProb = probabilities[0]
        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIdx = i
            }
        }

        val label = if (maxIdx < labels.size) labels[maxIdx] else "DESCONOCIDO"
        val isConfident = maxProb >= threshold

        return GestureRecognitionResult(
            predictedGloss = label,
            confidence = maxProb,
            isConfident = isConfident
        )
    }
}
