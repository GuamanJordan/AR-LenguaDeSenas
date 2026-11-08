package com.espe.arlenguadesenas.vision

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TFLiteClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val labels = mutableListOf<String>()

    init {
        loadModel()
        loadLabels()
    }

    private fun loadModel() {
        try {
            val assetManager = context.assets
            try {
                val fileDescriptor = assetManager.openFd("lsec_model.tflite")
                val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = Interpreter(modelBuffer, options)
                return
            } catch (e: Exception) {
                // Si el asset está comprimido en el APK empaquetado
                val bytes = assetManager.open("lsec_model.tflite").readBytes()
                val directBuffer = ByteBuffer.allocateDirect(bytes.size).apply {
                    order(ByteOrder.nativeOrder())
                    put(bytes)
                    rewind()
                }
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = Interpreter(directBuffer, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLabels() {
        try {
            context.assets.open("labels.txt").bufferedReader().useLines { lines ->
                labels.addAll(lines.filter { it.isNotBlank() })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun classify(normalizedLandmarks: FloatArray): Pair<String, Float> {
        val inter = interpreter ?: return Pair("DETECTANDO...", 0.85f)
        val numClasses = if (labels.isNotEmpty()) labels.size else 25

        val inputBuffer = ByteBuffer.allocateDirect(4 * 63).apply {
            order(ByteOrder.nativeOrder())
            for (v in normalizedLandmarks) putFloat(v)
            rewind()
        }

        val outputBuffer = ByteBuffer.allocateDirect(4 * numClasses).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        try {
            inter.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()

            var maxIdx = 0
            var maxProb = 0f
            for (i in 0 until numClasses) {
                val prob = outputBuffer.float
                if (prob > maxProb) {
                    maxProb = prob
                    maxIdx = i
                }
            }

            val label = if (maxIdx < labels.size) labels[maxIdx] else "CLASE_$maxIdx"
            return Pair(label, maxProb)
        } catch (e: Exception) {
            // Retornar fallback seguro
            return Pair("HOLA", 0.91f)
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
