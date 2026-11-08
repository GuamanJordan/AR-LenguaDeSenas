package com.espe.arlenguadesenas.ar

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * Malla 3D anatómica y orgánica de la mano humana para OpenGL ES 2.0.
 * Utiliza cilindros radiales suavizados con interpolación de normales (Gouraud/Phong),
 * ahusamiento natural de falanges, yemas redondeadas y curvatura metacarpiana.
 */
class Hand3DMesh {

    var vertexBuffer: FloatBuffer
    var normalBuffer: FloatBuffer
    var indexBuffer: ShortBuffer
    var indexCount: Int = 0

    init {
        val vertexList = mutableListOf<Float>()
        val normalList = mutableListOf<Float>()
        val indexList = mutableListOf<Short>()

        // 1. Palma de la mano anatómica (Contorno suave hexagonal con arco metacarpiano)
        addSmoothPalm(vertexList, normalList, indexList)

        // 2. Muñeca / Antebrazo (Cilindro elíptico suave)
        addSmoothCylinder(
            baseCenter = floatArrayOf(0f, -0.65f, -0.02f),
            topCenter = floatArrayOf(0f, -0.28f, -0.02f),
            radiusBaseX = 0.22f, radiusBaseZ = 0.12f,
            radiusTopX = 0.24f, radiusTopZ = 0.13f,
            segments = 12, vList = vertexList, nList = normalList, iList = indexList
        )

        // 3. Pulgar (Thumb) - En ángulo opositor natural (adelantado y rotado 35°)
        addFinger(
            root = floatArrayOf(-0.25f, -0.15f, 0.08f),
            p1 = floatArrayOf(-0.38f, 0.05f, 0.12f),
            p2 = floatArrayOf(-0.46f, 0.28f, 0.14f),
            tip = floatArrayOf(-0.52f, 0.48f, 0.15f),
            r1 = 0.095f, r2 = 0.085f, r3 = 0.075f,
            vertexList, normalList, indexList
        )

        // 4. Dedo Índice (Index) - 3 falanges ahusadas
        addFinger(
            root = floatArrayOf(-0.19f, 0.30f, 0.01f),
            p1 = floatArrayOf(-0.20f, 0.58f, 0.01f),
            p2 = floatArrayOf(-0.20f, 0.84f, 0.01f),
            tip = floatArrayOf(-0.20f, 1.06f, 0.01f),
            r1 = 0.080f, r2 = 0.072f, r3 = 0.062f,
            vertexList, normalList, indexList
        )

        // 5. Dedo Medio (Middle) - Más largo, punto culminante del arco
        addFinger(
            root = floatArrayOf(-0.01f, 0.33f, 0.02f),
            p1 = floatArrayOf(-0.01f, 0.64f, 0.02f),
            p2 = floatArrayOf(-0.01f, 0.92f, 0.02f),
            tip = floatArrayOf(-0.01f, 1.18f, 0.02f),
            r1 = 0.084f, r2 = 0.075f, r3 = 0.065f,
            vertexList, normalList, indexList
        )

        // 6. Dedo Anular (Ring) - Longitud intermedia
        addFinger(
            root = floatArrayOf(0.17f, 0.29f, 0.01f),
            p1 = floatArrayOf(0.18f, 0.58f, 0.01f),
            p2 = floatArrayOf(0.18f, 0.83f, 0.01f),
            tip = floatArrayOf(0.18f, 1.05f, 0.01f),
            r1 = 0.078f, r2 = 0.070f, r3 = 0.060f,
            vertexList, normalList, indexList
        )

        // 7. Dedo Meñique (Pinky) - Más delgado y proporcionado
        addFinger(
            root = floatArrayOf(0.32f, 0.23f, -0.01f),
            p1 = floatArrayOf(0.34f, 0.47f, -0.01f),
            p2 = floatArrayOf(0.35f, 0.69f, -0.01f),
            tip = floatArrayOf(0.35f, 0.88f, -0.01f),
            r1 = 0.068f, r2 = 0.060f, r3 = 0.052f,
            vertexList, normalList, indexList
        )

        indexCount = indexList.size

        // Buffers nativos directos para alto rendimiento en GPU
        vertexBuffer = ByteBuffer.allocateDirect(vertexList.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertexList.toFloatArray())
                position(0)
            }
        }

        normalBuffer = ByteBuffer.allocateDirect(normalList.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(normalList.toFloatArray())
                position(0)
            }
        }

        indexBuffer = ByteBuffer.allocateDirect(indexList.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(indexList.toShortArray())
                position(0)
            }
        }
    }

    /**
     * Construye un dedo humano completo con 3 falanges cilíndricas redondeadas y yema semiesférica.
     */
    private fun addFinger(
        root: FloatArray, p1: FloatArray, p2: FloatArray, tip: FloatArray,
        r1: Float, r2: Float, r3: Float,
        vList: MutableList<Float>, nList: MutableList<Float>, iList: MutableList<Short>
    ) {
        // Falange proximal
        addSmoothCylinder(root, p1, r1, r1, r2, r2, 10, vList, nList, iList)
        // Falange media
        addSmoothCylinder(p1, p2, r2, r2, r3, r3, 10, vList, nList, iList)
        // Falange distal con punta redondeada
        addSmoothCylinder(p2, tip, r3, r3, r3 * 0.7f, r3 * 0.7f, 10, vList, nList, iList)
        // Cúpula / Yema esférica
        addDome(tip, r3 * 0.75f, vList, nList, iList)
    }

    /**
     * Palma anatómica con relieve de eminencia tenar e hipotenar.
     */
    private fun addSmoothPalm(
        vList: MutableList<Float>, nList: MutableList<Float>, iList: MutableList<Short>
    ) {
        val baseIndex = (vList.size / 3).toShort()

        // Vértices clave de la silueta de la palma humana (cara anterior y posterior)
        val palmPointsFront = arrayOf(
            floatArrayOf(-0.24f, -0.28f, 0.08f), // Muñeca izquierda
            floatArrayOf(0.24f, -0.28f, 0.06f),  // Muñeca derecha
            floatArrayOf(0.35f, 0.22f, 0.05f),   // Nudillo meñique
            floatArrayOf(0.18f, 0.29f, 0.07f),   // Nudillo anular
            floatArrayOf(-0.01f, 0.33f, 0.08f),  // Nudillo medio
            floatArrayOf(-0.19f, 0.30f, 0.07f),  // Nudillo índice
            floatArrayOf(-0.35f, -0.05f, 0.10f)  // Monte de Venus / Tenar pulgar
        )

        val palmPointsBack = arrayOf(
            floatArrayOf(-0.24f, -0.28f, -0.08f),
            floatArrayOf(0.24f, -0.28f, -0.06f),
            floatArrayOf(0.35f, 0.22f, -0.05f),
            floatArrayOf(0.18f, 0.29f, -0.07f),
            floatArrayOf(-0.01f, 0.33f, -0.08f),
            floatArrayOf(-0.19f, 0.30f, -0.07f),
            floatArrayOf(-0.35f, -0.05f, -0.08f)
        )

        // Cara anterior (Palma)
        val centerFront = floatArrayOf(-0.02f, 0.0f, 0.09f)
        vList.addAll(listOf(centerFront[0], centerFront[1], centerFront[2]))
        nList.addAll(listOf(0f, 0f, 1f))

        for (pt in palmPointsFront) {
            vList.addAll(listOf(pt[0], pt[1], pt[2]))
            nList.addAll(listOf(0f, 0f, 1f))
        }

        val n = palmPointsFront.size
        for (i in 0 until n) {
            iList.add(baseIndex)
            iList.add((baseIndex + 1 + i).toShort())
            iList.add((baseIndex + 1 + (i + 1) % n).toShort())
        }

        // Cara posterior (Dorso)
        val baseIndexBack = (vList.size / 3).toShort()
        val centerBack = floatArrayOf(-0.02f, 0.0f, -0.08f)
        vList.addAll(listOf(centerBack[0], centerBack[1], centerBack[2]))
        nList.addAll(listOf(0f, 0f, -1f))

        for (pt in palmPointsBack) {
            vList.addAll(listOf(pt[0], pt[1], pt[2]))
            nList.addAll(listOf(0f, 0f, -1f))
        }

        for (i in 0 until n) {
            iList.add(baseIndexBack)
            iList.add((baseIndexBack + 1 + (i + 1) % n).toShort())
            iList.add((baseIndexBack + 1 + i).toShort())
        }

        // Laterales de unión entre frente y dorso
        for (i in 0 until n) {
            val iNext = (i + 1) % n
            val f1 = (baseIndex + 1 + i).toShort()
            val f2 = (baseIndex + 1 + iNext).toShort()
            val b1 = (baseIndexBack + 1 + i).toShort()
            val b2 = (baseIndexBack + 1 + iNext).toShort()

            iList.add(f1); iList.add(b1); iList.add(f2)
            iList.add(f2); iList.add(b1); iList.add(b2)
        }
    }

    /**
     * Genera un cilindro suavizado con radio elíptico y normales continuas para aspecto orgánico.
     */
    private fun addSmoothCylinder(
        baseCenter: FloatArray, topCenter: FloatArray,
        radiusBaseX: Float, radiusBaseZ: Float,
        radiusTopX: Float, radiusTopZ: Float,
        segments: Int,
        vList: MutableList<Float>, nList: MutableList<Float>, iList: MutableList<Short>
    ) {
        val baseIdx = (vList.size / 3).toShort()
        val step = (2.0 * Math.PI / segments).toFloat()

        for (i in 0..segments) {
            val angle = i * step
            val cosA = cos(angle)
            val sinA = sin(angle)

            // Vértice base
            vList.add(baseCenter[0] + radiusBaseX * cosA)
            vList.add(baseCenter[1])
            vList.add(baseCenter[2] + radiusBaseZ * sinA)
            nList.add(cosA); nList.add(0f); nList.add(sinA)

            // Vértice superior
            vList.add(topCenter[0] + radiusTopX * cosA)
            vList.add(topCenter[1])
            vList.add(topCenter[2] + radiusTopZ * sinA)
            nList.add(cosA); nList.add(0f); nList.add(sinA)
        }

        for (i in 0 until segments) {
            val v0 = (baseIdx + i * 2).toShort()
            val v1 = (baseIdx + i * 2 + 1).toShort()
            val v2 = (baseIdx + (i + 1) * 2).toShort()
            val v3 = (baseIdx + (i + 1) * 2 + 1).toShort()

            iList.add(v0); iList.add(v1); iList.add(v2)
            iList.add(v2); iList.add(v1); iList.add(v3)
        }
    }

    /**
     * Yema del dedo semiesférica para evitar puntas cuadradas.
     */
    private fun addDome(
        center: FloatArray, radius: Float,
        vList: MutableList<Float>, nList: MutableList<Float>, iList: MutableList<Short>
    ) {
        val baseIdx = (vList.size / 3).toShort()
        vList.add(center[0]); vList.add(center[1] + radius); vList.add(center[2])
        nList.add(0f); nList.add(1f); nList.add(0f)

        val segs = 8
        for (i in 0 until segs) {
            val angle = (i * 2.0 * Math.PI / segs).toFloat()
            val cosA = cos(angle)
            val sinA = sin(angle)

            vList.add(center[0] + radius * 0.85f * cosA)
            vList.add(center[1])
            vList.add(center[2] + radius * 0.85f * sinA)
            nList.add(cosA * 0.7f); nList.add(0.7f); nList.add(sinA * 0.7f)
        }

        for (i in 0 until segs) {
            val iNext = (i + 1) % segs
            iList.add(baseIdx)
            iList.add((baseIdx + 1 + i).toShort())
            iList.add((baseIdx + 1 + iNext).toShort())
        }
    }
}
