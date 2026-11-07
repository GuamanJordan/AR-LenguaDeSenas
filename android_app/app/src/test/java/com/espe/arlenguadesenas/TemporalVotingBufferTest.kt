package com.espe.arlenguadesenas

import com.espe.arlenguadesenas.vision.TemporalVotingBuffer
import org.junit.Assert.assertEquals
import org.junit.Test

class TemporalVotingBufferTest {

    @Test
    fun testMajorityVotingReturnsMostFrequentLabel() {
        val buffer = TemporalVotingBuffer(windowSize = 5)
        buffer.addPrediction("HOLA")
        buffer.addPrediction("GRACIAS")
        buffer.addPrediction("HOLA")
        buffer.addPrediction("HOLA")
        val result = buffer.addPrediction("AYUDA")

        assertEquals("HOLA", result)
    }

    @Test
    fun testClearEmptiesBuffer() {
        val buffer = TemporalVotingBuffer(windowSize = 3)
        buffer.addPrediction("A")
        buffer.clear()
        val result = buffer.addPrediction("B")
        assertEquals("B", result)
    }
}
