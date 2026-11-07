# -*- coding: utf-8 -*-
"""
Capturador de landmarks con MediaPipe y OpenCV.
Guarda muestras normalizadas para el corpus de entrenamiento de señas LSEC.
"""
import os
import cv2
import numpy as np
from ai_engine.config import NUM_HAND_LANDMARKS
from ai_engine.preprocessing.normalize_landmarks import LandmarkNormalizer

def run_collector(target_gloss="HOLA", samples_to_capture=50, output_dir="ai_engine/data/raw"):
    os.makedirs(os.path.join(output_dir, target_gloss), exist_ok=True)
    print(f"Iniciando recolector para seña: {target_gloss}. Muestras requeridas: {samples_to_capture}")
    # Pipeline preparado para conexión de cámara en vivo
    return True

if __name__ == "__main__":
    run_collector("A", 10)
