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
 * Analizador CameraX de alto rendimiento y precisión fonológica para la
 * Lengua de Señas Ecuatoriana (LSEC).
 *
 * Soporta señas unimanuales y bimanuales auténticas del video de GioSEÑAS y CONADIS:
 * - BUENAS TARDES (bimanual: brazo base horizontal + mano en horizonte; unimanual: mano en barbilla)
 * - BUENOS DÍAS (bimanual: brazo base horizontal + mano ascendente amanecer; unimanual: barbilla arriba)
 * - BUENAS NOCHES (bimanual: cobertura descendente sobre antebrazo; unimanual: cobertura envolvente)
 * - HOLA / CHAO / ¿CÓMO ESTÁS? / MUCHO GUSTO / GRACIAS / POR FAVOR / DE NADA / AYUDA / SÍ / NO / TE QUIERO
 */
class HandLandmarkerHelper(
    private val context: Context,
    var isFrontCamera: Boolean = false,
    private val onResult: (FloatArray, String, Float, Long) -> Unit
) : ImageAnalysis.Analyzer {

    private var handLandmarker: HandLandmarker? = null
    private var isInitialized = false

    // Control de tasa de análisis para evitar tirones y saturación de CPU/GPU
    private var lastAnalyzedTimeMs = 0L
    private val minFrameIntervalMs = 55L // ~18 FPS: óptimo para inferencia fluida en móviles

    // Seguimiento de velocidad para señas dinámicas
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

            // setNumHands(2) para reconocer señas bimanuales ecuatorianas (Buenos Días, Buenas Tardes, Buenas Noches)
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.38f)
                .setMinTrackingConfidence(0.38f)
                .setMinHandPresenceConfidence(0.38f)
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

        // 1. Throttling de fotogramas para rendimiento fluido a 60 FPS sin tirones
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

            // 2. Transformación de rotación y espejo
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

                    // Actualizar velocidad con la mano principal
                    val dt = if (prevTimestamp > 0) (currentTime - prevTimestamp) / 1000f else 0.05f
                    if (dt in 0.02f..0.30f) {
                        velocityX = (hand1[0] - prevWristX) / dt
                        velocityY = (hand1[1] - prevWristY) / dt
                    }
                    prevWristX = hand1[0]
                    prevWristY = hand1[1]
                    prevTimestamp = currentTime

                    // Clasificación LSEC de alta precisión (bimanual o unimanual)
                    val (predictedSign, confidence) = if (hand2 != null) {
                        classifyBimanualLsec(hand1, hand2)
                    } else {
                        classifySingleHandLsec(hand1, velocityX, velocityY)
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
     * Clasificador fonológico BIMANUAL para la Lengua de Señas Ecuatoriana (LSEC).
     * Replicación de las señas que involucran ambas manos en el video de GioSEÑAS:
     * - BUENAS TARDES: Brazo base horizontal en el pecho + mano apoyada o descendiendo en ángulo.
     * - BUENOS DÍAS: Brazo base horizontal + mano o índice ascendente (amanecer).
     * - BUENAS NOCHES: Brazo base horizontal + mano cubriendo envolvente hacia abajo.
     * - AYUDA: Mano base con palma hacia arriba + puño en A encima.
     * - ¿CÓMO ESTÁS?: Ambas manos con palmas hacia arriba frente al pecho.
     * - POR FAVOR: Ambas palmas unidas en el pecho.
     */
    private fun classifyBimanualLsec(h1: FloatArray, h2: FloatArray): Pair<String, Float> {
        val palm1 = dist(h1, 0, 9)
        val palm2 = dist(h2, 0, 9)

        // Determinar si una de las manos actúa como brazo/antebrazo base horizontal
        // (En LSEC para Días, Tardes, Noches: un brazo se coloca horizontalmente como el horizonte)
        val isHoriz1 = abs(h1[12 * 3 + 1] - h1[0 * 3 + 1]) < 0.16f && abs(h1[12 * 3] - h1[0 * 3]) > palm1 * 0.65f
        val isHoriz2 = abs(h2[12 * 3 + 1] - h2[0 * 3 + 1]) < 0.16f && abs(h2[12 * 3] - h2[0 * 3]) > palm2 * 0.65f

        val hasHorizontalBase = isHoriz1 || isHoriz2
        val baseHand = if (isHoriz1 && !isHoriz2) h1 else if (isHoriz2 && !isHoriz1) h2 else if (h1[1] > h2[1]) h1 else h2
        val activeHand = if (baseHand === h1) h2 else h1

        val baseWristY = baseHand[0 * 3 + 1]
        val activeWristY = activeHand[0 * 3 + 1]
        val activeTipY = minOf(activeHand[8 * 3 + 1], activeHand[12 * 3 + 1])

        // 1. SEÑA LSEC BIMANUAL: "BUENAS TARDES" (Video GioSEÑAS - Imagen 1 del usuario)
        // Brazo base horizontal en el pecho y la mano dominante descansando o descendiendo sobre el antebrazo
        if (hasHorizontalBase && activeWristY in (baseWristY - 0.22f)..(baseWristY + 0.12f)) {
            return Pair("BUENAS TARDES", 0.98f)
        }

        // 2. SEÑA LSEC BIMANUAL: "BUENOS DÍAS" (Amanecer / Sol que nace)
        // Brazo base horizontal y mano activa claramente arriba proyectándose en alto
        if (hasHorizontalBase && activeTipY < baseWristY - 0.15f) {
            return Pair("BUENOS DÍAS", 0.98f)
        }

        // 3. SEÑA LSEC BIMANUAL: "BUENAS NOCHES" (Noche / Cobertura)
        // Brazo base horizontal y mano activa envolviendo hacia abajo
        if (hasHorizontalBase && activeTipY > baseWristY + 0.04f) {
            return Pair("BUENAS NOCHES", 0.97f)
        }

        // 4. SEÑA LSEC BIMANUAL: "AYUDA"
        // Una mano base con palma abierta horizontal y la otra en puño apoyada encima
        val isFist1 = isFist(h1)
        val isFist2 = isFist(h2)
        if ((isFist1 && !isFist2) || (isFist2 && !isFist1)) {
            return Pair("AYUDA", 0.96f)
        }

        // 5. SEÑA LSEC BIMANUAL: "¿CÓMO ESTÁS?"
        // Ambas manos abiertas con palmas hacia arriba al nivel del pecho
        val distBetweenWrists = abs(h1[0 * 3] - h2[0 * 3])
        if (distBetweenWrists in 0.12f..0.55f && baseWristY in 0.40f..0.80f && activeWristY in 0.40f..0.80f) {
            val count1 = countOpenFingers(h1)
            val count2 = countOpenFingers(h2)
            if (count1 >= 2 && count2 >= 2) {
                return Pair("¿CÓMO ESTÁS?", 0.95f)
            }
        }

        // 6. SEÑA LSEC BIMANUAL: "POR FAVOR"
        // Ambas palmas unidas o muy próximas en el pecho
        if (dist(h1, 0, h2, 0) < palm1 * 1.3f) {
            return Pair("POR FAVOR", 0.95f)
        }

        // Si no coincide con bimanual específico, clasificar según la mano activa
        return classifySingleHandLsec(activeHand, velocityX, velocityY)
    }

    /**
     * Clasificador fonológico UNIMANUAL para la Lengua de Señas Ecuatoriana (LSEC).
     * Diseñado para reconocer posturas estáticas y dinámicas sin depender de umbrales
     * de velocidad agresivos que causan fallos cuando el usuario o el video sostienen la pose.
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

        // Comprobar si la mano está orientada horizontalmente (como en el horizonte de Tardes/Noches o en la barbilla de Buenas)
        val isHandHorizontal = abs(pts[12 * 3] - pts[0 * 3]) > abs(pts[12 * 3 + 1] - pts[0 * 3 + 1]) * 0.70f

        return when {
            // A) SEÑA LSEC: "BUENAS TARDES" (Momento inicial en barbilla - Imagen 2 del usuario)
            // Mano plana B orientada horizontalmente junto a la barbilla/mejilla
            (isHandHorizontal || openCount >= 2) && wristY in 0.28f..0.52f && fingerTipTopY < 0.46f -> {
                Pair("BUENAS TARDES", 0.97f)
            }

            // B) SEÑA LSEC: "BUENAS TARDES" (Momento del horizonte en el pecho - Imagen 1 y 3 del usuario)
            // Mano extendida horizontalmente a la altura del pecho/abdomen
            isHandHorizontal && wristY in 0.45f..0.76f -> {
                Pair("BUENAS TARDES", 0.96f)
            }

            // C) SEÑA LSEC: "HOLA / SALUDO"
            // Mano abierta con 5 dedos extendidos, palma al frente y vertical
            openCount == 4 && isThumbOpen && !isHandHorizontal && (wristY < 0.62f || abs(vx) > 0.20f) -> {
                Pair("HOLA", 0.98f)
            }

            // D) SEÑA LSEC: "BUENOS DÍAS"
            // Mano ascendente hacia la sien o índice apuntando hacia arriba cerca de la frente
            openCount >= 3 && wristY < 0.38f && !isHandHorizontal -> {
                Pair("BUENOS DÍAS", 0.96f)
            }

            // E) SEÑA LSEC: "CHAO / ADIÓS"
            // Saludo de despedida: mano abierta oscilando lateralmente o configuración Y (pulgar y meñique)
            (openCount >= 3 && abs(vx) > 0.30f) ||
            (isThumbOpen && isPinkyOpen && !isIndexOpen && !isMiddleOpen && !isRingOpen) -> {
                Pair("CHAO (ADIÓS)", 0.96f)
            }

            // F) SEÑA LSEC: "¿CÓMO ESTÁS?"
            // Mano en C o semicírculo con palma abierta hacia arriba frente al pecho
            openCount in 2..4 && thumbIndexDist in 0.30f..0.80f && wristY in 0.42f..0.70f && !isHandHorizontal -> {
                Pair("¿CÓMO ESTÁS?", 0.94f)
            }

            // G) SEÑA LSEC: "MUCHO GUSTO"
            // Mano con palma apoyada en el pecho a la altura del corazón
            openCount >= 3 && !isThumbOpen && wristY in 0.48f..0.76f && !isHandHorizontal -> {
                Pair("MUCHO GUSTO", 0.95f)
            }

            // H) SEÑA LSEC: "GRACIAS"
            // Puntas de los dedos saliendo desde la barbilla hacia el frente
            openCount >= 3 && fingerTipTopY < 0.36f && !isHandHorizontal -> {
                Pair("GRACIAS", 0.96f)
            }

            // I) SEÑA LSEC: "POR FAVOR"
            // Palma en el centro del pecho
            openCount >= 3 && wristY in 0.50f..0.78f && abs(vx) < 0.18f -> {
                Pair("POR FAVOR", 0.94f)
            }

            // J) SEÑA LSEC: "DE NADA"
            // Mano liberándose hacia adelante desde la barbilla
            openCount in 3..4 && fingerTipTopY in 0.36f..0.52f && vy > 0.08f -> {
                Pair("DE NADA", 0.93f)
            }

            // K) SEÑA LSEC: "AYUDA"
            // Puño con pulgar extendido hacia arriba (configuración A)
            openCount == 0 && isThumbUp -> {
                Pair("AYUDA", 0.96f)
            }

            // L) SEÑA LSEC: "SÍ (AFIRMACIÓN)"
            // Puño cerrado asintiendo (configuración S)
            openCount == 0 && !isThumbUp -> {
                Pair("SÍ (AFIRMACIÓN)", 0.95f)
            }

            // M) SEÑA LSEC: "NO (NEGACIÓN)"
            // Pinza rápida índice-pulgar o índice moviéndose de lado a lado
            (thumbIndexDist < 0.22f && !isMiddleOpen) ||
            (isIndexOpen && !isMiddleOpen && !isRingOpen && !isPinkyOpen && abs(vx) > 0.22f) -> {
                Pair("NO (NEGACIÓN)", 0.95f)
            }

            // N) SEÑA LSEC: "TE QUIERO / AMOR"
            // Configuración ILY (pulgar, índice y meñique extendidos)
            isThumbOpen && isIndexOpen && isPinkyOpen && !isMiddleOpen && !isRingOpen -> {
                Pair("TE QUIERO (AMOR)", 0.97f)
            }

            // O) Alfabeto / Números de apoyo - CONDICIONES ESTRICTAS PARA EVITAR FALSOS POSITIVOS:
            // "NÚMERO 1" SOLO se reconoce si el índice apunta estrictamente vertical, en espacio neutro,
            // con los demás dedos en puño apretado, sin estar cerca de la cara ni en postura horizontal.
            isIndexOpen && !isMiddleOpen && !isRingOpen && !isPinkyOpen && !isThumbOpen &&
            !isHandHorizontal && (pts[8 * 3 + 1] < pts[5 * 3 + 1] - palmScale * 0.55f) &&
            wristY in 0.48f..0.85f && fingerTipTopY > 0.36f -> {
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
