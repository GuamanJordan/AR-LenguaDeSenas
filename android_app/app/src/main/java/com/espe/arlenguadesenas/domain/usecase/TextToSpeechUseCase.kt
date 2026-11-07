package com.espe.arlenguadesenas.domain.usecase

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechUseCase(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "EC")
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LSEC_TTS")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
