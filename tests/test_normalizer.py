# -*- coding: utf-8 -*-
import unittest
import numpy as np
from ai_engine.preprocessing.normalize_landmarks import LandmarkNormalizer

class TestLandmarkNormalizer(unittest.TestCase):
    def test_normalization_shape(self):
        raw = np.random.randn(21, 3).astype(np.float32)
        norm = LandmarkNormalizer.normalize_single_hand(raw)
        self.assertEqual(norm.shape, (63,))
        self.assertAlmostEqual(norm[0], 0.0, places=5)
        self.assertAlmostEqual(norm[1], 0.0, places=5)
        self.assertAlmostEqual(norm[2], 0.0, places=5)

if __name__ == '__main__':
    unittest.main()
