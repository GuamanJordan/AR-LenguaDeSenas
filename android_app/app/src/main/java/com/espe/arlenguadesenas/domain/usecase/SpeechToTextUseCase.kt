package com.espe.arlenguadesenas.domain.usecase

data class SpeechRecognitionState(
    val isListening: Boolean = false,
    val spokenText: String = "",
    val error: String? = null
)
