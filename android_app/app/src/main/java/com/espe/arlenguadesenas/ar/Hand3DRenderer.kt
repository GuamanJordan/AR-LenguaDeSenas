package com.espe.arlenguadesenas.ar

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

/**
 * Renderizador OpenGL ES 2.0 para la visualización y animación 3D de la mano LSEC.
 * Soporta rotación libre 360°, zoom orbital e interpolación de gestos según la glosa activa.
 */
class Hand3DRenderer : GLSurfaceView.Renderer {

    private lateinit var handMesh: Hand3DMesh

    // Matrices de transformación
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val normalMatrix = FloatArray(16)

    // Parámetros de vista orbital 360°
    var rotationX: Float = 10f
    var rotationY: Float = 0f
    var scale: Float = 1.0f

    // Estado de la animación
    var activeGloss: String = "HOLA"
    var isPlaying: Boolean = true
    var playbackSpeed: Float = 1.0f
    private var animationTime: Float = 0f

    // Program shader IDs
    private var program: Int = 0
    private var aPositionHandle: Int = 0
    private var aNormalHandle: Int = 0
    private var uMVPMatrixHandle: Int = 0
    private var uModelMatrixHandle: Int = 0
    private var uColorHandle: Int = 0
    private var uLightPosHandle: Int = 0

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        attribute vec4 aPosition;
        attribute vec3 aNormal;
        varying vec3 vNormal;
        varying vec3 vPosition;

        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vPosition = vec3(uModelMatrix * aPosition);
            vNormal = mat3(uModelMatrix) * aNormal;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 uColor;
        uniform vec3 uLightPos;
        varying vec3 vNormal;
        varying vec3 vPosition;

        void main() {
            vec3 normal = normalize(vNormal);
            vec3 lightDir = normalize(uLightPos - vPosition);
            
            // Iluminación difusa Phong
            float diff = max(dot(normal, lightDir), 0.0);
            vec3 ambient = vec3(0.35, 0.35, 0.38);
            vec3 diffuse = diff * vec3(0.85, 0.85, 0.85);
            vec3 result = (ambient + diffuse) * uColor.rgb;

            gl_FragColor = vec4(result, uColor.a);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.08f, 0.09f, 0.12f, 1.0f) // Fondo oscuro moderno
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        handMesh = Hand3DMesh()

        // Compilar Shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aNormalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        uColorHandle = GLES20.glGetUniformLocation(program, "uColor")
        uLightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.5f, 20f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        if (isPlaying) {
            animationTime += 0.035f * playbackSpeed
        }

        // Configurar cámara orbital
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0.2f, 3.8f / scale.coerceIn(0.5f, 3.0f), 0f, 0.2f, 0f, 0f, 1f, 0f)

        // Matriz de modelo base
        Matrix.setIdentityM(modelMatrix, 0)

        // 1. Rotación interactiva 360° del usuario
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f)

        // 2. Animación dinámica según la seña activa
        applySignAnimation(modelMatrix, activeGloss, animationTime)

        // Calcular MVP
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        // Pasar uniformes a OpenGL
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0)

        // Posición de la luz
        GLES20.glUniform3f(uLightPosHandle, 2.0f, 3.5f, 3.0f)

        // Color de la mano 3D (Tono cian / avatar accesible de alto contraste)
        GLES20.glUniform4f(uColorHandle, 0.25f, 0.72f, 0.95f, 1.0f)

        // Vincular atributos de vértices y normales
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 12, handMesh.vertexBuffer)

        GLES20.glEnableVertexAttribArray(aNormalHandle)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, 12, handMesh.normalBuffer)

        // Dibujar triángulos de la mano 3D
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, handMesh.indexCount, GLES20.GL_UNSIGNED_SHORT, handMesh.indexBuffer)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)
    }

    private fun applySignAnimation(matrix: FloatArray, gloss: String, t: Float) {
        val upper = gloss.uppercase().trim()
        when {
            upper.contains("HOLA") || upper.contains("SALUDO") -> {
                // Seña HOLA: Mano abierta ondeando de lado a lado con rotación en Z y flexión de muñeca
                val waveZ = (sin(t.toDouble() * 4.5) * 20.0).toFloat()
                val waveY = (sin(t.toDouble() * 4.5) * 8.0).toFloat()
                Matrix.rotateM(matrix, 0, waveZ, 0f, 0f, 1f)
                Matrix.rotateM(matrix, 0, waveY, 0f, 1f, 0f)
            }
            upper.contains("GRACIAS") -> {
                // Seña GRACIAS: Inclinación hacia adelante proyectándose desde el mentón
                val tiltX = (sin(t.toDouble() * 3.0) * 18.0).toFloat()
                val moveZ = (sin(t.toDouble() * 3.0) * 0.25).toFloat()
                Matrix.translateM(matrix, 0, 0f, 0f, moveZ)
                Matrix.rotateM(matrix, 0, tiltX, 1f, 0f, 0f)
            }
            upper.contains("POR-FAVOR") || upper.contains("FAVOR") -> {
                // Seña POR FAVOR: Movimiento suave circular en el pecho
                val circX = (sin(t.toDouble() * 3.0) * 0.15).toFloat()
                val circY = (sin(t.toDouble() * 3.0 + 1.57) * 0.15).toFloat()
                Matrix.translateM(matrix, 0, circX, circY, 0f)
            }
            upper.contains("AYUDAR") || upper.contains("AYUDA") -> {
                // Seña AYUDA: Impulso frontal enfático
                val pushZ = (sin(t.toDouble() * 3.5) * 0.35).toFloat()
                Matrix.translateM(matrix, 0, 0f, 0f, pushZ)
            }
            upper.contains("SI") -> {
                // Seña SÍ: Movimiento afirmativo oscilatorio vertical
                val nodX = (sin(t.toDouble() * 5.0) * 25.0).toFloat()
                Matrix.rotateM(matrix, 0, nodX, 1f, 0f, 0f)
            }
            upper.contains("NO") -> {
                // Seña NO: Negación lateral oscilatoria rápida
                val shakeY = (sin(t.toDouble() * 6.0) * 22.0).toFloat()
                Matrix.rotateM(matrix, 0, shakeY, 0f, 1f, 0f)
            }
            else -> {
                // Movimiento neutro de flotación
                val floatY = (sin(t.toDouble() * 2.0) * 0.08).toFloat()
                Matrix.translateM(matrix, 0, 0f, floatY, 0f)
            }
        }
    }

    fun resetRotation() {
        rotationX = 10f
        rotationY = 0f
        scale = 1.0f
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}

