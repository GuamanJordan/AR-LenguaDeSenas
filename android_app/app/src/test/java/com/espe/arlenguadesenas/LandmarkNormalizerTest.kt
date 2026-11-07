package com.espe.arlenguadesenas

import com.espe.arlenguadesenas.vision.LandmarkNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LandmarkNormalizerTest {

    @Test
    fun testNormalizationSetsWristToZero() {
        val landmarks = FloatArray(63) { (it + 1).toFloat() }
        val normalized = LandmarkNormalizer.normalize(landmarks)

        assertEquals(0.0f, normalized[0], 1e-4f)
        assertEquals(0.0f, normalized[1], 1e-4f)
        assertEquals(0.0f, normalized[2], 1e-4f)
    }

    @Test
    fun testOutputDimensionsRemainSixtyThree() {
        val landmarks = FloatArray(63) { 0.5f }
        landmarks[9 * 3] = 1.0f // Para que dist > 0
        val normalized = LandmarkNormalizer.normalize(landmarks)
        assertEquals(63, normalized.size)
    }
}
