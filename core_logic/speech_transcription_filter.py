# -*- coding: utf-8 -*-
import re

class SpeechTranscriptionFilter:
    """Filtra y normaliza la entrada de audio capturada por el reconocedor de voz"""

    FILLER_WORDS = {'este', 'eh', 'em', 'bueno', 'o sea', 'sabes', 'mmm'}

    @classmethod
    def filter_transcription(cls, raw_text, min_confidence=0.5):
        if not raw_text:
            return ""
        words = raw_text.strip().split()
        filtered = [w for w in words if w.lower() not in cls.FILLER_WORDS]
        cleaned = " ".join(filtered)
        return re.sub(r'\s+', ' ', cleaned).strip()
