package com.espe.arlenguadesenas.domain.model

data class GestureRecognitionResult(
    val predictedGloss: String,
    val confidence: Float,
    val isConfident: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
