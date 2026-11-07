# -*- coding: utf-8 -*-
import numpy as np

class LandmarkDataAugmenter:
    """Aumenta el dataset de landmarks aplicando perturbaciones realistas"""

    @staticmethod
    def add_jitter(landmarks, sigma=0.015):
        noise = np.random.normal(0, sigma, landmarks.shape)
        return landmarks + noise

    @staticmethod
    def random_scale(landmarks, scale_range=(0.92, 1.08)):
        factor = np.random.uniform(scale_range[0], scale_range[1])
        return landmarks * factor

    @staticmethod
    def random_rotate_z(landmarks, angle_deg_range=(-15, 15)):
        angle_rad = np.radians(np.random.uniform(angle_deg_range[0], angle_deg_range[1]))
        cos_a, sin_a = np.cos(angle_rad), np.sin(angle_rad)
        pts = landmarks.reshape(-1, 3).copy()
        for i in range(len(pts)):
            x, y = pts[i, 0], pts[i, 1]
            pts[i, 0] = x * cos_a - y * sin_a
            pts[i, 1] = x * sin_a + y * cos_a
        return pts.flatten()
