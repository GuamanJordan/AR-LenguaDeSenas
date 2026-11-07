# -*- coding: utf-8 -*-
class AudioFeedbackManager:
    """Configuración de parámetros para el canal de síntesis de voz (TTS) para el usuario oyente"""

    def __init__(self, language="es-EC", pitch=1.0, speech_rate=0.95):
        self.language = language
        self.pitch = pitch
        self.speech_rate = speech_rate
        self.is_muted = False

    def get_tts_config(self):
        return {
            "locale": self.language,
            "pitch": self.pitch,
            "rate": self.speech_rate,
            "queue_mode": "FLUSH",
            "audio_stream": "STREAM_MUSIC"
        }
