# -*- coding: utf-8 -*-
import re
import unicodedata

class LsecGlossMapper:
    """
    Traductor de lenguaje natural (Español oral/escrito) a glosas de la LSEC.
    Aplica lematización básica, supresión de elementos gramaticales vacíos y
    reordenamiento viso-espacial característico de la lengua de señas ecuatoriana.
    """

    STOPWORDS_SPANISH = {
        'el', 'la', 'los', 'las', 'un', 'una', 'unos', 'unas',
        'de', 'del', 'a', 'al', 'en', 'por', 'para', 'con', 'sin', 'sobre',
        'que', 'y', 'e', 'o', 'u'
    }

    DICTIONARY_MAPPING = {
        'hola': 'HOLA',
        'buenos dias': 'BUENO DIA',
        'buenos': 'BUENO',
        'dias': 'DIA',
        'buenas tardes': 'BUENO TARDE',
        'tardes': 'TARDE',
        'buenas noches': 'BUENO NOCHE',
        'noches': 'NOCHE',
        'gracias': 'GRACIAS',
        'muchas gracias': 'GRACIAS MUCHO',
        'muchas': 'MUCHO',
        'mucho': 'MUCHO',
        'por favor': 'POR-FAVOR',
        'adios': 'CHAO',
        'chao': 'CHAO',
        'ayuda': 'AYUDAR',
        'ayudame': 'AYUDAR YO',
        'bano': 'BANO',
        'sanitario': 'BANO',
        'necesito': 'NECESITAR',
        'quiero': 'QUERER',
        'profesor': 'PROFESOR',
        'docente': 'PROFESOR',
        'maestro': 'PROFESOR',
        'clase': 'CLASE',
        'aula': 'CLASE',
        'entiendo': 'ENTENDER',
        'no entiendo': 'ENTENDER NO',
        'si': 'SI',
        'no': 'NO',
        'como estas': 'COMO TU',
        'donde': 'DONDE',
        'universidad': 'UNIVERSIDAD',
        'espe': 'UNIVERSIDAD ESPE'
    }

    def __init__(self):
        pass

    def clean_text(self, text):
        if not text:
            return ""
        # Normalizar acentos y pasar a minúsculas
        text = text.lower().strip()
        text = ''.join(c for c in unicodedata.normalize('NFD', text) if unicodedata.category(c) != 'Mn')
        text = re.sub(r'[^a-z0-9\s]', ' ', text)
        return re.sub(r'\s+', ' ', text).strip()

    def translate_to_glosses(self, text):
        clean = self.clean_text(text)
        if not clean:
            return []

        # 1. Comprobar coincidencias de frases completas
        if clean in self.DICTIONARY_MAPPING:
            return self.DICTIONARY_MAPPING[clean].split()

        # 2. Análisis por palabras y eliminación de stopwords
        words = clean.split()
        meaningful_words = [w for w in words if w not in self.STOPWORDS_SPANISH]

        glosses = []
        is_negated = False

        for w in meaningful_words:
            if w == 'no':
                is_negated = True
                continue
            if w in self.DICTIONARY_MAPPING:
                glosses.extend(self.DICTIONARY_MAPPING[w].split())
            else:
                # Si la palabra no existe en el corpus acotado, hacer deletreo dactilológico
                for letter in w.upper():
                    glosses.append(f"DACT_{letter}")

        # En LSEC la negación suele posicionarse al final de la expresión
        if is_negated:
            glosses.append("NO")

        return glosses

if __name__ == "__main__":
    mapper = LsecGlossMapper()
    ejemplos = [
        "Hola, buenos días",
        "Muchas gracias por favor",
        "No entiendo la clase",
        "¿Dónde está el baño?",
        "Profesor necesito ayuda"
    ]
    for ej in ejemplos:
        print(f"'{ej}' ➔ {mapper.translate_to_glosses(ej)}")
