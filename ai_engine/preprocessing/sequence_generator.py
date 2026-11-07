# -*- coding: utf-8 -*-
import numpy as np
from collections import deque

class TemporalSequenceBuffer:
    """Buffer circular para recolectar secuencias temporales deslizantes (sliding window)"""

    def __init__(self, sequence_length=30, num_features=63):
        self.sequence_length = sequence_length
        self.num_features = num_features
        self.buffer = deque(maxlen=sequence_length)

    def append(self, frame_features):
        if len(frame_features) != self.num_features:
            raise ValueError(f"Dimensión incorrecta: {len(frame_features)} != {self.num_features}")
        self.buffer.append(frame_features)

    def is_ready(self):
        return len(self.buffer) == self.sequence_length

    def get_sequence_array(self):
        if not self.is_ready():
            # Rellenar con padding ceros si aun no se llena la ventana
            padded = list(self.buffer)
            while len(padded) < self.sequence_length:
                padded.insert(0, np.zeros(self.num_features, dtype=np.float32))
            return np.array(padded, dtype=np.float32)
        return np.array(list(self.buffer), dtype=np.float32)

    def clear(self):
        self.buffer.clear()
