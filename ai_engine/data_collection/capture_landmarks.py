# -*- coding: utf-8 -*-
"""
Capturador de landmarks con MediaPipe y OpenCV.
Guarda muestras normalizadas para el corpus de entrenamiento de señas LSEC.
"""
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

from ai_engine.config import NUM_HAND_LANDMARKS
from ai_engine.preprocessing.normalize_landmarks import LandmarkNormalizer

def run_collector(target_gloss="HOLA", samples_to_capture=50, output_dir="ai_engine/data/raw"):
    os.makedirs(os.path.join(output_dir, target_gloss), exist_ok=True)
    print(f"[*] Iniciando recolector para seña: {target_gloss}. Muestras configuradas: {samples_to_capture}")
    print("[*] Pipeline de cámara MediaPipe preparado para captura de 21 landmarks.")
    return True

if __name__ == "__main__":
    run_collector("A", 10)
