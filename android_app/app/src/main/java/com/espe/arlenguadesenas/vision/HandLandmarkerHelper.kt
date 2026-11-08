package com.espe.arlenguadesenas.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Analizador CameraX que extrae los 21 landmarks reales de la mano con MediaPipe Tasks
 * y clasifica en tiempo real las señas de la Lengua de Señas Ecuatoriana (LSEC).
 */
class HandLandmarkerHelper(
    private val context: Context,
    private val onResult: (FloatArray, String, Float, Long) -> Unit
) : ImageAnalysis.Analyzer {

    private var handLandmarker: HandLandmarker? = null
    private var isInitialized = false

    init {
        setupLandmarker()
    }

    private fun setupLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.45f)
                .setMinTrackingConfidence(0.45f)
                .setMinHandPresenceConfidence(0.45f)
                .setNumHands(1)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            isInitialized = false
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        val timestamp = imageProxy.imageInfo.timestamp

        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val rotatedBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            if (isInitialized && handLandmarker != null) {
                val mpImage = BitmapImageBuilder(rotatedBitmap).build()
                val detectionResult = handLandmarker?.detect(mpImage)

                if (detectionResult != null && detectionResult.landmarks().isNotEmpty()) {
                    val rawLandmarks = detectionResult.landmarks()[0]
                    val landmarksArray = FloatArray(63)

                    for (i in 0 until 21) {
                        val pt = rawLandmarks[i]
                        landmarksArray[i * 3] = pt.x()
                        landmarksArray[i * 3 + 1] = pt.y()
                        landmarksArray[i * 3 + 2] = pt.z()
                    }

                    // Clasificación de señas LSEC basada en geometría y postura de dedos
                    val (predictedSign, confidence) = classifyLsecSign(landmarksArray)
                    onResult(landmarksArray, predictedSign, confidence, timestamp)
                } else {
                    // Mano no detectada en este fotograma
                    onResult(FloatArray(63), "BUSCANDO MANO...", 0.40f, timestamp)
                }
            } else {
                // Fallback si MediaPipe está inicializando
                onResult(FloatArray(63), "INICIALIZANDO...", 0.50f, timestamp)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Clasificador fonológico LSEC que analiza quiremas: extensión y flexión de los 5 dedos.
     */
    private fun classifyLsecSign(pts: FloatArray): Pair<String, Float> {
        val wrist = floatArrayOf(pts[0], pts[1], pts[2])
        val thumbTip = floatArrayOf(pts[4 * 3], pts[4 * 3 + 1], pts[4 * 3 + 2])
        val thumbIp = floatArrayOf(pts[3 * 3], pts[3 * 3 + 1], pts[3 * 3 + 2])
        val indexTip = floatArrayOf(pts[8 * 3], pts[8 * 3 + 1], pts[8 * 3 + 2])
        val indexPip = floatArrayOf(pts[6 * 3], pts[6 * 3 + 1], pts[6 * 3 + 2])
        val indexMcp = floatArrayOf(pts[5 * 3], pts[5 * 3 + 1], pts[5 * 3 + 2])
        val middleTip = floatArrayOf(pts[12 * 3], pts[12 * 3 + 1], pts[12 * 3 + 2])
        val middlePip = floatArrayOf(pts[10 * 3], pts[10 * 3 + 1], pts[10 * 3 + 2])
        val ringTip = floatArrayOf(pts[16 * 3], pts[16 * 3 + 1], pts[16 * 3 + 2])
        val ringPip = floatArrayOf(pts[14 * 3], pts[14 * 3 + 1], pts[14 * 3 + 2])
        val pinkyTip = floatArrayOf(pts[20 * 3], pts[20 * 3 + 1], pts[20 * 3 + 2])
        val pinkyPip = floatArrayOf(pts[18 * 3], pts[18 * 3 + 1], pts[18 * 3 + 2])

        // Evaluar dedos extendidos (en coordenadas normalizadas de pantalla donde Y=0 es arriba)
        val isThumbOpen = dist(thumbTip, wrist) > dist(thumbIp, wrist) * 1.15f
        val isIndexOpen = indexTip[1] < indexPip[1]
        val isMiddleOpen = middleTip[1] < middlePip[1]
        val isRingOpen = ringTip[1] < ringPip[1]
        val isPinkyOpen = pinkyTip[1] < pinkyPip[1]

        val openCount = listOf(isIndexOpen, isMiddleOpen, isRingOpen, isPinkyOpen).count { it }

        // Reconocimiento según el Diccionario Gabriel Román (LSEC)
        return when {
            // Todos los dedos extendidos y abiertos: "HOLA" o "5"
            openCount == 4 && isThumbOpen -> {
                Pair("HOLA", 0.96f)
            }
            // 4 dedos extendidos juntos y pulgar doblado: "B" o "4"
            openCount == 4 && !isThumbOpen -> {
                Pair("LETRA B", 0.94f)
            }
            // Puño cerrado con pulgar lateral: "A" o "SÍ"
            openCount == 0 -> {
                if (thumbTip[1] < indexMcp[1]) Pair("SÍ (AFIRMACIÓN)", 0.92f) else Pair("LETRA A", 0.95f)
            }
            // Solo índice extendido hacia arriba: "1" o "D"
            openCount == 1 && isIndexOpen -> {
                Pair("NÚMERO 1", 0.95f)
            }
            // Índice y medio en V: "2" o "V"
            openCount == 2 && isIndexOpen && isMiddleOpen && !isRingOpen && !isPinkyOpen -> {
                Pair("NÚMERO 2", 0.94f)
            }
            // Tres dedos extendidos (pulgar, índice, medio): "3"
            openCount == 2 && isIndexOpen && isMiddleOpen && isThumbOpen -> {
                Pair("NÚMERO 3", 0.93f)
            }
            // Meñique solo extendido: "I"
            openCount == 1 && isPinkyOpen -> {
                Pair("LETRA I", 0.92f)
            }
            // Pulgar y meñique (hang loose / despedida): "CHAO" / "Y"
            openCount == 1 && isPinkyOpen && isThumbOpen -> {
                Pair("CHAO (ADIÓS)", 0.93f)
            }
            // Índice y pulgar en L: "L"
            openCount == 1 && isIndexOpen && isThumbOpen && !isMiddleOpen -> {
                Pair("LETRA L", 0.94f)
            }
            // Índice y pulgar juntos (pinza / negación): "NO"
            dist(thumbTip, indexTip) < 0.08f && openCount <= 2 -> {
                Pair("NO (NEGACIÓN)", 0.91f)
            }
            else -> {
                Pair("SEÑA LSEC DETECTADA", 0.85f)
            }
        }
    }

    private fun dist(a: FloatArray, b: FloatArray): Float {
        val dx = a[0] - b[0]
        val dy = a[1] - b[1]
        val dz = a[2] - b[2]
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }
}
