package com.espe.arlenguadesenas.vision

import java.util.ArrayDeque

class TemporalVotingBuffer(private val windowSize: Int = 5) {
    private val queue = ArrayDeque<String>(windowSize)

    fun addPrediction(label: String): String {
        if (queue.size >= windowSize) {
            queue.poll()
        }
        queue.offer(label)

        // Contar ocurrencias por mayoría simple
        val counts = mutableMapOf<String, Int>()
        for (item in queue) {
            counts[item] = (counts[item] ?: 0) + 1
        }

        var topLabel = label
        var maxCount = 0
        for ((k, v) in counts) {
            if (v > maxCount) {
                maxCount = v
                topLabel = k
            }
        }
        return topLabel
    }

    fun clear() {
        queue.clear()
    }
}
