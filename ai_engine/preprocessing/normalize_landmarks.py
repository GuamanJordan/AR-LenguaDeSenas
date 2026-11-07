# -*- coding: utf-8 -*-
import math

try:
    import numpy as np
    HAVE_NUMPY = True
except ImportError:
    HAVE_NUMPY = False

class LandmarkNormalizer:
    """
    Normalizador espacial euclidiano para 21 puntos de la mano:
    1. Traslada los puntos para que la muñeca (punto 0) quede en el origen (0,0,0).
    2. Escala por la distancia entre la muñeca y la articulación del dedo medio (punto 9).
    3. Garantiza invariancia frente a distancia a la cámara y tamaño de la mano del usuario.
    Compatible con NumPy y con fallback nativo en Python puro.
    """

    @staticmethod
    def normalize_single_hand(landmarks_array):
        if HAVE_NUMPY:
            pts = np.array(landmarks_array, dtype=np.float32).reshape(-1, 3)
            if len(pts) != 21:
                raise ValueError(f"Se esperaban 21 landmarks, pero se recibieron {len(pts)}")

            wrist = pts[0]
            translated = pts - wrist

            ref_distance = np.linalg.norm(pts[9] - pts[0])
            if ref_distance < 1e-6:
                ref_distance = 1.0

            normalized = translated / ref_distance
            return normalized.flatten()
        else:
            # Fallback en Python puro si NumPy no está instalado
            if len(landmarks_array) == 63:
                pts = [landmarks_array[i * 3:(i + 1) * 3] for i in range(21)]
            elif len(landmarks_array) == 21:
                pts = landmarks_array
            else:
                raise ValueError(f"Se esperaban 21 puntos o 63 valores, pero se recibieron {len(landmarks_array)}")

            wrist = pts[0]
            translated = [
                [pt[0] - wrist[0], pt[1] - wrist[1], pt[2] - wrist[2]]
                for pt in pts
            ]
            m = translated[9]
            ref_distance = math.sqrt(m[0] ** 2 + m[1] ** 2 + m[2] ** 2)
            if ref_distance < 1e-6:
                ref_distance = 1.0

            result = []
            for pt in translated:
                result.extend([pt[0] / ref_distance, pt[1] / ref_distance, pt[2] / ref_distance])
            return result

    @staticmethod
    def normalize_dual_hands(left_hand, right_hand):
        if HAVE_NUMPY:
            norm_left = LandmarkNormalizer.normalize_single_hand(left_hand) if left_hand is not None else np.zeros(63, dtype=np.float32)
            norm_right = LandmarkNormalizer.normalize_single_hand(right_hand) if right_hand is not None else np.zeros(63, dtype=np.float32)
            return np.concatenate([norm_left, norm_right])
        else:
            norm_left = LandmarkNormalizer.normalize_single_hand(left_hand) if left_hand is not None else [0.0] * 63
            norm_right = LandmarkNormalizer.normalize_single_hand(right_hand) if right_hand is not None else [0.0] * 63
            return norm_left + norm_right
