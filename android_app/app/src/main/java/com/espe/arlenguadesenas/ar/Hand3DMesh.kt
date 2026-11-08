package com.espe.arlenguadesenas.ar

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Geometría 3D de la mano anatómica para renderizado OpenGL ES 2.0.
 * Construye vértices, normales y caras para la palma y los 5 dedos con articulaciones.
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

        // 1. Crear cuboide de la Palma (Palm)
        addBox(
            center = floatArrayOf(0f, -0.1f, 0f),
            size = floatArrayOf(0.7f, 0.75f, 0.16f),
            vertexList, normalList, indexList
        )

        // 2. Muñeca / Base
        addBox(
            center = floatArrayOf(0f, -0.6f, 0f),
            size = floatArrayOf(0.5f, 0.35f, 0.18f),
            vertexList, normalList, indexList
        )

        // 3. Pulgar (Thumb) - 2 falanges en diagonal
        addBox(
            center = floatArrayOf(-0.45f, -0.05f, 0.05f),
            size = floatArrayOf(0.18f, 0.35f, 0.14f),
            vertexList, normalList, indexList
        )
        addBox(
            center = floatArrayOf(-0.55f, 0.25f, 0.08f),
            size = floatArrayOf(0.16f, 0.32f, 0.13f),
            vertexList, normalList, indexList
        )

        // 4. Dedo Índice (Index) - 3 falanges
        addBox(center = floatArrayOf(-0.25f, 0.42f, 0f), size = floatArrayOf(0.15f, 0.32f, 0.13f), vertexList, normalList, indexList)
        addBox(center = floatArrayOf(-0.25f, 0.75f, 0f), size = floatArrayOf(0.14f, 0.30f, 0.12f), vertexList, normalList, indexList)
        addBox(center = floatArrayOf(-0.25f, 1.05f, 0f), size = floatArrayOf(0.13f, 0.26f, 0.11f), vertexList, normalList, indexList)

        // 5. Dedo Medio (Middle) - 3 falanges
        addBox(center = floatArrayOf(-0.02f, 0.45f, 0f), size = floatArrayOf(0.16f, 0.35f, 0.13f), vertexList, normalList, indexList)
        addBox(center = floatArrayOf(-0.02f, 0.82f, 0f), size = floatArrayOf(0.15f, 0.34f, 0.12f), vertexList, normalList, indexList)
        addBox(center = floatArrayOf(-0.02f, 1.15f, 0f), size = floatArrayOf(0.14f, 0.28f, 0.11f), vertexList, normalList, indexList)

        // 6. Dedo Anular (Ring) - 3 falanges
        addBox(center = floatArrayOf(0.20f, 0.42f, 0f), size = floatArrayOf(0.15f, 0.32f, 0.13f), vertexList, normalList, indexList)
        addBox(center = floatArrayOf(0.20f, 0.75f, 0f), size = floatArrayOf(0.14f, 0.30f, 0.12f), vertexList, normalList, indexList)
        addBox(center = floatArrayOf(0.20f, 1.04f, 0f), size = floatArrayOf(0.13f, 0.25f, 0.11f), vertexList, normalList, indexList)

        // 7. Dedo Meñique (Pinky) - 3 falanges
        addBox(center = floatArrayOf(0.40f, 0.35f, 0f), size = floatArrayOf(0.14f, 0.28f, 0.12f), vertexList, normalList, indexList)
        addBox(center = floatArrayOf(0.40f, 0.63f, 0f), size = floatArrayOf(0.13f, 0.26f, 0.11f), vertexList, normalList, indexList)
        addBox(center = floatArrayOf(0.40f, 0.88f, 0f), size = floatArrayOf(0.12f, 0.22f, 0.10f), vertexList, normalList, indexList)

        indexCount = indexList.size

        // Buffers nativos directos
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

    private fun addBox(
        center: FloatArray,
        size: FloatArray,
        vList: MutableList<Float>,
        nList: MutableList<Float>,
        iList: MutableList<Short>
    ) {
        val hx = size[0] / 2f
        val hy = size[1] / 2f
        val hz = size[2] / 2f
        val cx = center[0]
        val cy = center[1]
        val cz = center[2]

        val baseIndex = (vList.size / 3).toShort()

        // 24 vertices (4 por cada una de las 6 caras para normales planas nítidas)
        val faceNormals = arrayOf(
            floatArrayOf(0f, 0f, 1f),   // Frente
            floatArrayOf(0f, 0f, -1f),  // Atrás
            floatArrayOf(0f, 1f, 0f),   // Arriba
            floatArrayOf(0f, -1f, 0f),  // Abajo
            floatArrayOf(1f, 0f, 0f),   // Derecha
            floatArrayOf(-1f, 0f, 0f)   // Izquierda
        )

        val faceVertices = arrayOf(
            // Frente
            floatArrayOf(cx - hx, cy - hy, cz + hz, cx + hx, cy - hy, cz + hz, cx + hx, cy + hy, cz + hz, cx - hx, cy + hy, cz + hz),
            // Atrás
            floatArrayOf(cx + hx, cy - hy, cz - hz, cx - hx, cy - hy, cz - hz, cx - hx, cy + hy, cz - hz, cx + hx, cy + hy, cz - hz),
            // Arriba
            floatArrayOf(cx - hx, cy + hy, cz + hz, cx + hx, cy + hy, cz + hz, cx + hx, cy + hy, cz - hz, cx - hx, cy + hy, cz - hz),
            // Abajo
            floatArrayOf(cx - hx, cy - hy, cz - hz, cx + hx, cy - hy, cz - hz, cx + hx, cy - hy, cz + hz, cx - hx, cy - hy, cz + hz),
            // Derecha
            floatArrayOf(cx + hx, cy - hy, cz + hz, cx + hx, cy - hy, cz - hz, cx + hx, cy + hy, cz - hz, cx + hx, cy + hy, cz + hz),
            // Izquierda
            floatArrayOf(cx - hx, cy - hy, cz - hz, cx - hx, cy - hy, cz + hz, cx - hx, cy + hy, cz + hz, cx - hx, cy + hy, cz - hz)
        )

        for (face in 0 until 6) {
            val normal = faceNormals[face]
            val fVerts = faceVertices[face]
            val fBase = (baseIndex + face * 4).toShort()

            for (v in 0 until 4) {
                vList.add(fVerts[v * 3])
                vList.add(fVerts[v * 3 + 1])
                vList.add(fVerts[v * 3 + 2])

                nList.add(normal[0])
                nList.add(normal[1])
                nList.add(normal[2])
            }

            // 2 triángulos por cara
            iList.add(fBase)
            iList.add((fBase + 1).toShort())
            iList.add((fBase + 2).toShort())

            iList.add(fBase)
            iList.add((fBase + 2).toShort())
            iList.add((fBase + 3).toShort())
        }
    }
}

