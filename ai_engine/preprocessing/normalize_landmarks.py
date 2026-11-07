# -*- coding: utf-8 -*-
import numpy as np

class LandmarkNormalizer:
    """
    Normalizador espacial euclidiano para 21 puntos de la mano:
    1. Traslada los puntos para que la muñeca (punto 0) quede en el origen (0,0,0).
    2. Escala por la distancia entre la muñeca y la articulación del dedo medio (punto 9).
    3. Garantiza invariancia frente a distancia a la cámara y tamaño de la mano del usuario.
    """

    @staticmethod
    def normalize_single_hand(landmarks_array):
        pts = np.array(landmarks_array, dtype=np.float32).reshape(-1, 3)
        if len(pts) != 21:
            raise ValueError(f"Se esperaban 21 landmarks, pero se recibieron {len(pts)}")

        wrist = pts[0]
        translated = pts - wrist

        # Distancia euclidiana entre muñeca y nudillo del dedo medio (landmark 9)
        ref_distance = np.linalg.norm(pts[9] - pts[0])
        if ref_distance < 1e-6:
            ref_distance = 1.0

        normalized = translated / ref_distance
        return normalized.flatten()

    @staticmethod
    def normalize_dual_hands(left_hand, right_hand):
        norm_left = LandmarkNormalizer.normalize_single_hand(left_hand) if left_hand is not None else np.zeros(63, dtype=np.float32)
        norm_right = LandmarkNormalizer.normalize_single_hand(right_hand) if right_hand is not None else np.zeros(63, dtype=np.float32)
        return np.concatenate([norm_left, norm_right])
