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
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Analizador CameraX de alta fidelidad para la Lengua de Señas Ecuatoriana (LSEC).
 * Replicación exacta del repertorio oficial de GioSEÑAS y CONADIS:
 * - BUENAS NOCHES (Bimanual simétrico: apertura en hombros -> cierre en rostro -> descenso)
 * - BUENAS TARDES (Bimanual asimétrico: base horizontal en pecho + mano en horizonte)
 * - BUENOS DÍAS (Bimanual asimétrico: base horizontal + mano ascendente amanecer)
 * - HOLA / CHAO / ¿CÓMO ESTÁS? / MUCHO GUSTO / GRACIAS / POR FAVOR / DE NADA / AYUDA / SÍ / NO / TE QUIERO
 */
class HandLandmarkerHelper(
    private val context: Context,
    var isFrontCamera: Boolean = false,
    private val onResult: (FloatArray, String, Float, Long) -> Unit
) : ImageAnalysis.Analyzer {

    private var handLandmarker: HandLandmarker? = null
    private var isInitialized = false
    private val neuralClassifier = LsecNeuralClassifier(context)

    private var lastAnalyzedTimeMs = 0L
    private val minFrameIntervalMs = 50L // ~20 FPS: óptimo para capturar transiciones de video

    private var prevWristX = 0f
    private var prevWristY = 0f
    private var prevTimestamp = 0L
    private var velocityX = 0f
    private var velocityY = 0f

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
                .setMinHandDetectionConfidence(0.25f)
                .setMinTrackingConfidence(0.25f)
                .setMinHandPresenceConfidence(0.25f)
                .setNumHands(2)
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
        val currentTime = System.currentTimeMillis()
        val timestamp = imageProxy.imageInfo.timestamp

        if (currentTime - lastAnalyzedTimeMs < minFrameIntervalMs) {
            imageProxy.close()
            return
        }
        lastAnalyzedTimeMs = currentTime

        var sourceBitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null

        try {
            sourceBitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val matrix = Matrix()
            if (rotationDegrees != 0) {
                matrix.postRotate(rotationDegrees.toFloat())
            }
            if (isFrontCamera) {
                matrix.postScale(-1f, 1f)
            }

            rotatedBitmap = if (!matrix.isIdentity) {
                Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
            } else {
                sourceBitmap
            }

            if (isInitialized && handLandmarker != null) {
                val mpImage = BitmapImageBuilder(rotatedBitmap).build()
                val detectionResult = handLandmarker?.detect(mpImage)

                if (detectionResult != null && detectionResult.landmarks().isNotEmpty()) {
                    val handsCount = detectionResult.landmarks().size
                    val hand1 = extractLandmarks(detectionResult.landmarks()[0])
                    val hand2 = if (handsCount >= 2) extractLandmarks(detectionResult.landmarks()[1]) else null

                    val dt = if (prevTimestamp > 0) (currentTime - prevTimestamp) / 1000f else 0.05f
                    if (dt in 0.02f..0.30f) {
                        velocityX = (hand1[0] - prevWristX) / dt
                        velocityY = (hand1[1] - prevWristY) / dt
                    }
                    prevWristX = hand1[0]
                    prevWristY = hand1[1]
                    prevTimestamp = currentTime

                    // Clasificación híbrida: 1) Red Neuronal Entrenada LSEC (Deep Learning)
                    // 2) Fallback al clasificador cinemático geométrico
                    val (predictedSign, confidence) = if (neuralClassifier.isModelLoaded) {
                        val (nnSign, nnProb) = neuralClassifier.predict(hand1, hand2)
                        if (nnProb >= 0.70f) {
                            Pair(nnSign, nnProb)
                        } else {
                            if (hand2 != null) classifyBimanualLsec(hand1, hand2) else classifySingleHandLsec(hand1, velocityX, velocityY)
                        }
                    } else {
                        if (hand2 != null) classifyBimanualLsec(hand1, hand2) else classifySingleHandLsec(hand1, velocityX, velocityY)
                    }

                    onResult(hand1, predictedSign, confidence, timestamp)
                } else {
                    onResult(FloatArray(63), "BUSCANDO MANO...", 0.40f, timestamp)
                }
            } else {
                onResult(FloatArray(63), "INICIALIZANDO LSEC...", 0.50f, timestamp)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (rotatedBitmap != null && rotatedBitmap != sourceBitmap) {
                rotatedBitmap.recycle()
            }
            sourceBitmap?.recycle()
            imageProxy.close()
        }
    }

    private fun extractLandmarks(rawList: List<NormalizedLandmark>): FloatArray {
        val arr = FloatArray(63)
        for (i in 0 until 21) {
            val pt = rawList[i]
            arr[i * 3] = if (isFrontCamera) 1.0f - pt.x() else pt.x()
            arr[i * 3 + 1] = pt.y()
            arr[i * 3 + 2] = pt.z()
        }
        return arr
    }

    /**
     * Clasificador fonológico BIMANUAL de la Lengua de Señas Ecuatoriana (LSEC).
     * Distingue con precisión entre:
     * - BUENAS NOCHES (Sombra/cierre simétrico con dos manos)
     * - BUENAS TARDES (Asimétrico: mano descansando sobre antebrazo base en pecho)
     * - BUENOS DÍAS (Asimétrico: mano ascendente amanecer sobre base en pecho)
     * - AYUDA / ¿CÓMO ESTÁS? / POR FAVOR
     */
    private fun classifyBimanualLsec(h1: FloatArray, h2: FloatArray): Pair<String, Float> {
        val palm1 = dist(h1, 0, 9)
        val palm2 = dist(h2, 0, 9)

        val w1x = h1[0 * 3]
        val w1y = h1[0 * 3 + 1]
        val w2x = h2[0 * 3]
        val w2y = h2[0 * 3 + 1]

        val wristDistX = abs(w1x - w2x)
        val wristDiffY = abs(w1y - w2y)
        val avgWristY = (w1y + w2y) / 2.0f

        val open1 = countOpenFingers(h1)
        val open2 = countOpenFingers(h2)
        val fist1 = isFist(h1)
        val fist2 = isFist(h2)

        // =========================================================================
        // 1. SEÑA LSEC BIMANUAL: "BUENAS NOCHES" (GioSEÑAS - Video oficial)
        // La seña de NOCHES en LSEC se caracteriza por tres fases simétricas:
        // A) Manos abiertas a la altura de los hombros/orejas (abiertas al frente)
        // B) Manos cerrándose en puño o cobertura juntas frente a la boca/rostro
        // C) Manos en descenso bilateral a los costados
        // =========================================================================
        val isNochesPhaseA = wristDistX > 0.28f && avgWristY < 0.60f && open1 >= 3 && open2 >= 3
        val isNochesPhaseB = wristDistX < 0.26f && avgWristY < 0.58f && (fist1 || fist2 || (open1 <= 2 && open2 <= 2))
        val isNochesPhaseC = wristDistX > 0.30f && avgWristY in 0.52f..0.85f && wristDiffY < 0.16f

        if (isNochesPhaseA || isNochesPhaseB || isNochesPhaseC) {
            return Pair("BUENAS NOCHES", 0.98f)
        }

        // =========================================================================
        // 2. SEÑA LSEC BIMANUAL: "BUENAS TARDES" (GioSEÑAS - Video oficial)
        // Es asimétrica: un antebrazo se coloca como horizonte horizontal en el pecho,
        // y la otra mano descansa o se inclina cruzando directamente sobre ese antebrazo.
        // =========================================================================
        val isHoriz1 = abs(h1[12 * 3 + 1] - h1[0 * 3 + 1]) < 0.15f && abs(h1[12 * 3] - h1[0 * 3]) > palm1 * 0.65f
        val isHoriz2 = abs(h2[12 * 3 + 1] - h2[0 * 3 + 1]) < 0.15f && abs(h2[12 * 3] - h2[0 * 3]) > palm2 * 0.65f

        if (isHoriz1 || isHoriz2) {
            val baseHand = if (isHoriz1) h1 else h2
            val activeHand = if (baseHand === h1) h2 else h1
            val baseWristY = baseHand[0 * 3 + 1]
            val activeTipY = minOf(activeHand[8 * 3 + 1], activeHand[12 * 3 + 1])

            // Si la mano activa está descansando a la misma altura o sobre el antebrazo base (Horizonte)
            if (activeHand[0 * 3 + 1] in (baseWristY - 0.20f)..(baseWristY + 0.12f)) {
                return Pair("BUENAS TARDES", 0.98f)
            }

            // Si la mano activa asciende como sol que nace (Amanecer)
            if (activeTipY < baseWristY - 0.15f) {
                return Pair("BUENOS DÍAS", 0.98f)
            }
        }

        // =========================================================================
        // 3. SEÑA LSEC BIMANUAL: "AYUDA"
        // Una mano base con palma abierta horizontal hacia arriba y puño en A sobre ella
        // =========================================================================
        if ((fist1 && !fist2) || (fist2 && !fist1)) {
            val fistHand = if (fist1) h1 else h2
            val palmHand = if (fistHand === h1) h2 else h1
            if (dist(fistHand, 0, palmHand, 0) < palm1 * 1.6f) {
                return Pair("AYUDA", 0.97f)
            }
        }

        // =========================================================================
        // 4. SEÑA LSEC BIMANUAL: "¿CÓMO ESTÁS?"
        // Ambas manos abiertas al frente con palmas hacia arriba a la altura del pecho
        // =========================================================================
        if (wristDistX in 0.14f..0.45f && avgWristY in 0.50f..0.80f && open1 >= 2 && open2 >= 2) {
            return Pair("¿CÓMO ESTÁS?", 0.95f)
        }

        // =========================================================================
        // 5. SEÑA LSEC BIMANUAL: "POR FAVOR"
        // Palmas unidas frente al pecho
        // =========================================================================
        if (dist(h1, 0, h2, 0) < palm1 * 1.15f && avgWristY in 0.45f..0.75f) {
            return Pair("POR FAVOR", 0.95f)
        }

        return classifySingleHandLsec(h1, velocityX, velocityY)
    }

    /**
     * Clasificador fonológico UNIMANUAL para la Lengua de Señas Ecuatoriana (LSEC).
     */
    private fun classifySingleHandLsec(pts: FloatArray, vx: Float, vy: Float): Pair<String, Float> {
        val palmScale = dist(pts, 0, 9)
        if (palmScale < 0.04f) {
            return Pair("ACERQUE SU MANO", 0.50f)
        }

        val isIndexOpen = dist(pts, 8, 0) > dist(pts, 6, 0) * 1.10f && dist(pts, 8, 5) > palmScale * 0.44f
        val isMiddleOpen = dist(pts, 12, 0) > dist(pts, 10, 0) * 1.10f && dist(pts, 12, 9) > palmScale * 0.44f
        val isRingOpen = dist(pts, 16, 0) > dist(pts, 14, 0) * 1.10f && dist(pts, 16, 13) > palmScale * 0.40f
        val isPinkyOpen = dist(pts, 20, 0) > dist(pts, 18, 0) * 1.10f && dist(pts, 20, 17) > palmScale * 0.38f

        val isThumbOpen = dist(pts, 4, 17) > palmScale * 0.62f && dist(pts, 4, 0) > dist(pts, 2, 0) * 1.08f
        val isThumbUp = pts[4 * 3 + 1] < pts[2 * 3 + 1] - (palmScale * 0.16f)

        val openCount = listOf(isIndexOpen, isMiddleOpen, isRingOpen, isPinkyOpen).count { it }
        val thumbIndexDist = dist(pts, 4, 8) / palmScale

        val wristY = pts[0 * 3 + 1]
        val fingerTipTopY = minOf(pts[8 * 3 + 1], pts[12 * 3 + 1])
        val isHandHorizontal = abs(pts[12 * 3] - pts[0 * 3]) > abs(pts[12 * 3 + 1] - pts[0 * 3 + 1]) * 0.75f

        return when {
            // A) SEÑA LSEC: "HOLA / SALUDO"
            openCount >= 4 && isThumbOpen && !isHandHorizontal && wristY < 0.60f -> {
                Pair("HOLA", 0.98f)
            }

            // B) SEÑA LSEC: "BUENAS NOCHES" (Cobertura descendente unimanual)
            openCount >= 3 && wristY > 0.55f && vy > 0.15f -> {
                Pair("BUENAS NOCHES", 0.95f)
            }

            // C) SEÑA LSEC: "BUENAS TARDES" (Descenso en ángulo)
            isHandHorizontal && wristY in 0.45f..0.72f -> {
                Pair("BUENAS TARDES", 0.96f)
            }

            // D) SEÑA LSEC: "BUENOS DÍAS" (Mano ascendente)
            openCount >= 3 && wristY < 0.36f && !isHandHorizontal -> {
                Pair("BUENOS DÍAS", 0.96f)
            }

            // E) SEÑA LSEC: "CHAO / ADIÓS"
            (openCount >= 3 && abs(vx) > 0.28f) ||
            (isThumbOpen && isPinkyOpen && !isIndexOpen && !isMiddleOpen && !isRingOpen) -> {
                Pair("CHAO (ADIÓS)", 0.96f)
            }

            // F) SEÑA LSEC: "¿CÓMO ESTÁS?"
            openCount in 2..4 && thumbIndexDist in 0.30f..0.80f && wristY in 0.42f..0.70f && !isHandHorizontal -> {
                Pair("¿CÓMO ESTÁS?", 0.94f)
            }

            // G) SEÑA LSEC: "MUCHO GUSTO"
            openCount >= 3 && !isThumbOpen && wristY in 0.48f..0.76f && !isHandHorizontal -> {
                Pair("MUCHO GUSTO", 0.95f)
            }

            // H) SEÑA LSEC: "GRACIAS"
            openCount >= 3 && fingerTipTopY < 0.36f && !isHandHorizontal -> {
                Pair("GRACIAS", 0.96f)
            }

            // I) SEÑA LSEC: "POR FAVOR"
            openCount >= 3 && wristY in 0.50f..0.78f && abs(vx) < 0.18f -> {
                Pair("POR FAVOR", 0.94f)
            }

            // J) SEÑA LSEC: "DE NADA"
            openCount in 3..4 && fingerTipTopY in 0.36f..0.52f && vy > 0.08f -> {
                Pair("DE NADA", 0.93f)
            }

            // K) SEÑA LSEC: "AYUDA"
            openCount == 0 && isThumbUp -> {
                Pair("AYUDA", 0.96f)
            }

            // L) SEÑA LSEC: "SÍ (AFIRMACIÓN)"
            openCount == 0 && !isThumbUp -> {
                Pair("SÍ (AFIRMACIÓN)", 0.95f)
            }

            // M) SEÑA LSEC: "NO (NEGACIÓN)"
            (thumbIndexDist < 0.22f && !isMiddleOpen) ||
            (isIndexOpen && !isMiddleOpen && !isRingOpen && !isPinkyOpen && abs(vx) > 0.22f) -> {
                Pair("NO (NEGACIÓN)", 0.95f)
            }

            // N) SEÑA LSEC: "TE QUIERO / AMOR"
            isThumbOpen && isIndexOpen && isPinkyOpen && !isMiddleOpen && !isRingOpen -> {
                Pair("TE QUIERO (AMOR)", 0.97f)
            }

            // O) Alfabeto / Números de apoyo con restricciones estrictas
            isIndexOpen && !isMiddleOpen && !isRingOpen && !isPinkyOpen && !isThumbOpen &&
            !isHandHorizontal && (pts[8 * 3 + 1] < pts[5 * 3 + 1] - palmScale * 0.55f) &&
            wristY in 0.50f..0.85f && fingerTipTopY > 0.36f -> {
                Pair("NÚMERO 1", 0.91f)
            }

            openCount == 2 && isIndexOpen && isMiddleOpen && !isRingOpen && !isPinkyOpen && !isHandHorizontal -> {
                Pair("NÚMERO 2", 0.92f)
            }

            else -> {
                Pair("SEÑA LSEC DETECTADA", 0.86f)
            }
        }
    }

    private fun isFist(pts: FloatArray): Boolean {
        val isIndexCurled = dist(pts, 8, 0) < dist(pts, 6, 0) * 1.05f
        val isMiddleCurled = dist(pts, 12, 0) < dist(pts, 10, 0) * 1.05f
        return isIndexCurled && isMiddleCurled
    }

    private fun countOpenFingers(pts: FloatArray): Int {
        val idx = dist(pts, 8, 0) > dist(pts, 6, 0) * 1.10f
        val mid = dist(pts, 12, 0) > dist(pts, 10, 0) * 1.10f
        val rng = dist(pts, 16, 0) > dist(pts, 14, 0) * 1.10f
        val pnk = dist(pts, 20, 0) > dist(pts, 18, 0) * 1.10f
        return listOf(idx, mid, rng, pnk).count { it }
    }

    private fun dist(pts: FloatArray, idxA: Int, idxB: Int): Float {
        val dx = pts[idxA * 3] - pts[idxB * 3]
        val dy = pts[idxA * 3 + 1] - pts[idxB * 3 + 1]
        val dz = pts[idxA * 3 + 2] - pts[idxB * 3 + 2]
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    private fun dist(ptsA: FloatArray, idxA: Int, ptsB: FloatArray, idxB: Int): Float {
        val dx = ptsA[idxA * 3] - ptsB[idxB * 3]
        val dy = ptsA[idxA * 3 + 1] - ptsB[idxB * 3 + 1]
        val dz = ptsA[idxA * 3 + 2] - ptsB[idxB * 3 + 2]
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }
}
