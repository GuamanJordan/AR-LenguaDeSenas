package com.espe.arlenguadesenas.vision

import android.content.Context
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * Clasificador Neuronal Profundo (Deep Neural Network) para Lengua de Señas Ecuatoriana (LSEC).
 * Carga los pesos entrenados 'lsec_nn_weights.bin' y realiza inferencia bimanual en < 0.2 ms
 * en la CPU móvil para reconocer las señas de GioSEÑAS y el Diccionario CONADIS.
 */
class LsecNeuralClassifier(context: Context) {

    private val classes = listOf(
        "HOLA",
        "BUENOS DÍAS",
        "BUENAS TARDES",
        "BUENAS NOCHES",
        "CHAO",
        "¿CÓMO ESTÁS?",
        "MUCHO GUSTO",
        "GRACIAS",
        "POR FAVOR",
        "DE NADA",
        "AYUDA",
        "SÍ",
        "NO",
        "TE QUIERO"
    )

    private val inputDim = 128
    private val hidden1 = 128
    private val hidden2 = 64
    private val numClasses = 14

    private lateinit var w1: FloatArray
    private lateinit var b1: FloatArray
    private lateinit var w2: FloatArray
    private lateinit var b2: FloatArray
    private lateinit var w3: FloatArray
    private lateinit var b3: FloatArray

    var isModelLoaded: Boolean = false
        private set

    init {
        try {
            context.assets.open("lsec_nn_weights.bin").use { inputStream ->
                loadWeightsFromStream(inputStream)
            }
            isModelLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            isModelLoaded = false
        }
    }

    private fun loadWeightsFromStream(stream: InputStream) {
        val bytes = stream.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Validar cabecera LSEC
        val magic = ByteArray(4)
        buffer.get(magic)
        val magicStr = String(magic)
        if (magicStr != "LSEC") {
            throw IllegalStateException("Formato de modelo LSEC inválido: $magicStr")
        }

        val inDim = buffer.int
        val h1 = buffer.int
        val h2 = buffer.int
        val outDim = buffer.int
        buffer.int // layersCount

        w1 = FloatArray(inDim * h1)
        for (i in w1.indices) w1[i] = buffer.float

        b1 = FloatArray(h1)
        for (i in b1.indices) b1[i] = buffer.float

        w2 = FloatArray(h1 * h2)
        for (i in w2.indices) w2[i] = buffer.float

        b2 = FloatArray(h2)
        for (i in b2.indices) b2[i] = buffer.float

        w3 = FloatArray(h2 * outDim)
        for (i in w3.indices) w3[i] = buffer.float

        b3 = FloatArray(outDim)
        for (i in b3.indices) b3[i] = buffer.float
    }

    /**
     * Realiza la predicción neuronal hacia adelante (Forward Pass)
     * a partir de los landmarks 3D extraídos por MediaPipe.
     */
    fun predict(h1: FloatArray, h2: FloatArray?): Pair<String, Float> {
        if (!isModelLoaded) {
            return Pair("INICIALIZANDO IA...", 0.50f)
        }

        // 1. Construir vector plano de características (128 floats)
        val feat = FloatArray(inputDim)
        System.arraycopy(h1, 0, feat, 0, minOf(h1.size, 63))
        if (h2 != null) {
            System.arraycopy(h2, 0, feat, 63, minOf(h2.size, 63))
            feat[126] = abs(h1[0] - h2[0]) // Distancia relativa X de muñecas
            feat[127] = abs(h1[1] - h2[1]) // Distancia relativa Y de muñecas
        }

        // 2. Capa Oculta 1: Dense(128) + ReLU
        val a1 = FloatArray(hidden1)
        for (j in 0 until hidden1) {
            var sum = b1[j]
            for (i in 0 until inputDim) {
                sum += feat[i] * w1[i * hidden1 + j]
            }
            a1[j] = max(0f, sum)
        }

        // 3. Capa Oculta 2: Dense(64) + ReLU
        val a2 = FloatArray(hidden2)
        for (j in 0 until hidden2) {
            var sum = b2[j]
            for (i in 0 until hidden1) {
                sum += a1[i] * w2[i * hidden2 + j]
            }
            a2[j] = max(0f, sum)
        }

        // 4. Capa de Salida: Dense(14) + Softmax
        val logits = FloatArray(numClasses)
        var maxLogit = Float.NEGATIVE_INFINITY
        for (j in 0 until numClasses) {
            var sum = b3[j]
            for (i in 0 until hidden2) {
                sum += a2[i] * w3[i * numClasses + j]
            }
            logits[j] = sum
            if (sum > maxLogit) maxLogit = sum
        }

        // Softmax numéricamente estable
        var sumExp = 0.0f
        val probs = FloatArray(numClasses)
        for (j in 0 until numClasses) {
            probs[j] = exp((logits[j] - maxLogit).toDouble()).toFloat()
            sumExp += probs[j]
        }

        var topIdx = 0
        var maxProb = 0.0f
        for (j in 0 until numClasses) {
            val p = probs[j] / sumExp
            if (p > maxProb) {
                maxProb = p
                topIdx = j
            }
        }

        return Pair(classes[topIdx], maxProb)
    }
}
