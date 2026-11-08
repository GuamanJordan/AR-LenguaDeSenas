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
        loadModel(context)
    }

    fun loadModel(context: Context) {
        try {
            val customFile = context.getFileStreamPath("lsec_nn_weights_custom.bin")
            if (customFile != null && customFile.exists()) {
                context.openFileInput("lsec_nn_weights_custom.bin").use { inputStream ->
                    loadWeightsFromStream(inputStream)
                }
                android.util.Log.i("LSEC_NN", "[+] Pesos personalizados calibrados cargados con éxito")
            } else {
                context.assets.open("lsec_nn_weights.bin").use { inputStream ->
                    loadWeightsFromStream(inputStream)
                }
                android.util.Log.i("LSEC_NN", "[+] Pesos base de fábrica LSEC cargados con éxito")
            }
            isModelLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            isModelLoaded = false
        }
    }

    fun getClassIndex(name: String): Int {
        val normalized = name.uppercase().trim()
        val exact = classes.indexOfFirst { it.equals(normalized, ignoreCase = true) }
        if (exact >= 0) return exact
        return classes.indexOfFirst { normalized.contains(it) || it.contains(normalized) }
    }

    /**
     * Ajuste fino On-Device (Transfer Learning SGD) para calibrar la red
     * neuronal con la anatomía y cadencia gestual del usuario en tiempo real.
     */
    @Synchronized
    fun fineTuneOnSamples(
        samples: List<FloatArray>,
        targetClassIdx: Int,
        epochs: Int = 20,
        lr: Float = 0.008f
    ): Float {
        if (!isModelLoaded || samples.isEmpty() || targetClassIdx !in 0 until numClasses) {
            return 0f
        }

        val n = samples.size
        for (epoch in 0 until epochs) {
            for (x in samples) {
                // 1. Forward Pass con caché
                val z1 = FloatArray(hidden1)
                val a1 = FloatArray(hidden1)
                for (j in 0 until hidden1) {
                    var sum = b1[j]
                    for (i in 0 until inputDim) {
                        sum += x[i] * w1[i * hidden1 + j]
                    }
                    z1[j] = sum
                    a1[j] = max(0f, sum)
                }

                val z2 = FloatArray(hidden2)
                val a2 = FloatArray(hidden2)
                for (j in 0 until hidden2) {
                    var sum = b2[j]
                    for (i in 0 until hidden1) {
                        sum += a1[i] * w2[i * hidden2 + j]
                    }
                    z2[j] = sum
                    a2[j] = max(0f, sum)
                }

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

                var sumExp = 0.0f
                val probs = FloatArray(numClasses)
                for (j in 0 until numClasses) {
                    probs[j] = exp((logits[j] - maxLogit).toDouble()).toFloat()
                    sumExp += probs[j]
                }
                for (j in 0 until numClasses) {
                    probs[j] /= sumExp
                }

                // 2. Backward Pass (Retropropagación)
                val dlogits = FloatArray(numClasses)
                for (j in 0 until numClasses) {
                    val target = if (j == targetClassIdx) 1.0f else 0.0f
                    dlogits[j] = (probs[j] - target)
                }

                val da2 = FloatArray(hidden2)
                for (i in 0 until hidden2) {
                    var sum = 0f
                    for (j in 0 until numClasses) {
                        sum += dlogits[j] * w3[i * numClasses + j]
                        w3[i * numClasses + j] -= lr * a2[i] * dlogits[j]
                    }
                    da2[i] = sum
                }
                for (j in 0 until numClasses) {
                    b3[j] -= lr * dlogits[j]
                }

                val dz2 = FloatArray(hidden2)
                for (i in 0 until hidden2) {
                    dz2[i] = if (z2[i] > 0f) da2[i] else 0f
                }

                val da1 = FloatArray(hidden1)
                for (i in 0 until hidden1) {
                    var sum = 0f
                    for (j in 0 until hidden2) {
                        sum += dz2[j] * w2[i * hidden2 + j]
                        w2[i * hidden2 + j] -= lr * a1[i] * dz2[j]
                    }
                    da1[i] = sum
                }
                for (j in 0 until hidden2) {
                    b2[j] -= lr * dz2[j]
                }

                for (i in 0 until inputDim) {
                    val xi = x[i]
                    if (xi != 0f) {
                        for (j in 0 until hidden1) {
                            val dz1 = if (z1[j] > 0f) da1[j] else 0f
                            w1[i * hidden1 + j] -= lr * xi * dz1
                        }
                    }
                }
                for (j in 0 until hidden1) {
                    val dz1 = if (z1[j] > 0f) da1[j] else 0f
                    b1[j] -= lr * dz1
                }
            }
        }

        var correct = 0
        for (x in samples) {
            val (pred, _) = predictFeatures(x)
            if (pred == classes[targetClassIdx]) {
                correct++
            }
        }
        return (correct.toFloat() / n.toFloat()) * 100f
    }

    private fun predictFeatures(feat: FloatArray): Pair<String, Float> {
        val a1 = FloatArray(hidden1)
        for (j in 0 until hidden1) {
            var sum = b1[j]
            for (i in 0 until inputDim) {
                sum += feat[i] * w1[i * hidden1 + j]
            }
            a1[j] = max(0f, sum)
        }

        val a2 = FloatArray(hidden2)
        for (j in 0 until hidden2) {
            var sum = b2[j]
            for (i in 0 until hidden1) {
                sum += a1[i] * w2[i * hidden2 + j]
            }
            a2[j] = max(0f, sum)
        }

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

    /**
     * Guarda los pesos actualizados en el almacenamiento interno privado de la aplicación.
     */
    fun saveCustomWeights(context: Context) {
        try {
            val bufferSize = 4 + 20 + (w1.size + b1.size + w2.size + b2.size + w3.size + b3.size) * 4
            val buffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("LSEC".toByteArray(Charsets.US_ASCII))
            buffer.putInt(inputDim)
            buffer.putInt(hidden1)
            buffer.putInt(hidden2)
            buffer.putInt(numClasses)
            buffer.putInt(3) // layersCount

            for (v in w1) buffer.putFloat(v)
            for (v in b1) buffer.putFloat(v)
            for (v in w2) buffer.putFloat(v)
            for (v in b2) buffer.putFloat(v)
            for (v in w3) buffer.putFloat(v)
            for (v in b3) buffer.putFloat(v)

            context.openFileOutput("lsec_nn_weights_custom.bin", Context.MODE_PRIVATE).use { out ->
                out.write(buffer.array())
            }
            android.util.Log.i("LSEC_NN", "[+] Pesos personalizados guardados exitosamente (${bufferSize / 1024} KB)")
        } catch (e: Exception) {
            android.util.Log.e("LSEC_NN", "Error guardando pesos personalizados", e)
        }
    }

    /**
     * Restablece los pesos a la versión original de fábrica de los assets.
     */
    fun resetToFactoryWeights(context: Context) {
        val customFile = context.getFileStreamPath("lsec_nn_weights_custom.bin")
        if (customFile != null && customFile.exists()) {
            customFile.delete()
        }
        loadModel(context)
        android.util.Log.i("LSEC_NN", "[*] Modelo restablecido a valores de fábrica")
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

        val predicted = classes[topIdx]
        android.util.Log.d("LSEC_NN", "Top Class: '$predicted' | Prob: ${(maxProb * 100).toInt()}%")
        return Pair(predicted, maxProb)
    }
}
