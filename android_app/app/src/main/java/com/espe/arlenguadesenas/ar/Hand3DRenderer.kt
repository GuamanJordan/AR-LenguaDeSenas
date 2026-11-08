package com.espe.arlenguadesenas.ar

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

/**
 * Renderizador OpenGL ES 2.0 con sombreado de tono de piel humana (Subsurface Scattering wrap light)
 * y control orbital 360° para visualización de señas LSEC.
 */
class Hand3DRenderer : GLSurfaceView.Renderer {

    private lateinit var handMesh: Hand3DMesh

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Parámetros orbitales 360°
    var rotationX: Float = 8f
    var rotationY: Float = 0f
    var scale: Float = 0.95f

    // Estado y animación
    var activeGloss: String = "HOLA"
    var isPlaying: Boolean = true
    var playbackSpeed: Float = 1.0f
    private var animationTime: Float = 0f

    // Shader IDs
    private var program: Int = 0
    private var aPositionHandle: Int = 0
    private var aNormalHandle: Int = 0
    private var uMVPMatrixHandle: Int = 0
    private var uModelMatrixHandle: Int = 0
    private var uSkinColorHandle: Int = 0
    private var uLightPosHandle: Int = 0

    // Vertex Shader con normales y posición en espacio de mundo
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
            vNormal = normalize(mat3(uModelMatrix) * aNormal);
        }
    """.trimIndent()

    // Fragment Shader con modelo de piel humana (Wrap Diffuse + Specular suave)
    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 uSkinColor;
        uniform vec3 uLightPos;
        varying vec3 vNormal;
        varying vec3 vPosition;

        void main() {
            vec3 normal = normalize(vNormal);
            vec3 lightDir = normalize(uLightPos - vPosition);
            vec3 viewDir = normalize(-vPosition);
            
            // Subsurface wrap lighting para piel orgánica (evita sombras negras duras)
            float wrap = 0.35;
            float diff = max(0.0, (dot(normal, lightDir) + wrap) / (1.0 + wrap));
            
            // Tono base cálido y tono de sombra carmesí tenue
            vec3 ambientColor = vec3(0.42, 0.32, 0.28);
            vec3 diffuseLight = diff * vec3(1.0, 0.96, 0.90);
            
            // Reflejo especular tenue de piel humana
            vec3 reflectDir = reflect(-lightDir, normal);
            float spec = pow(max(dot(viewDir, reflectDir), 0.0), 12.0) * 0.18;
            
            vec3 finalColor = (ambientColor + diffuseLight) * uSkinColor.rgb + vec3(spec);
            gl_FragColor = vec4(finalColor, 1.0);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.08f, 0.09f, 0.12f, 1.0f) // Fondo oscuro de estudio
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        handMesh = Hand3DMesh()

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
        uSkinColorHandle = GLES20.glGetUniformLocation(program, "uSkinColor")
        uLightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 42f, aspect, 0.5f, 25f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        if (isPlaying) {
            animationTime += 0.038f * playbackSpeed
        }

        // Cámara orbital centrada con margen vertical para no colisionar con la UI superior
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, -0.05f, 4.3f / scale.coerceIn(0.5f, 2.5f),
            0f, -0.05f, 0f,
            0f, 1f, 0f
        )

        // Matriz de modelo base
        Matrix.setIdentityM(modelMatrix, 0)

        // Desplazamiento anatómico hacia el centro-bajo de la pantalla
        Matrix.translateM(modelMatrix, 0, 0f, -0.22f, 0f)

        // 1. Rotación interactiva 360° del usuario
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f)

        // 2. Animación fluida de la seña LSEC
        applyHumanSignAnimation(modelMatrix, activeGloss, animationTime)

        // Calcular MVP
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0)

        // Luz superior izquierda natural
        GLES20.glUniform3f(uLightPosHandle, 2.2f, 4.0f, 3.2f)

        // Tono de piel humana natural cálido
        GLES20.glUniform4f(uSkinColorHandle, 0.89f, 0.67f, 0.52f, 1.0f)

        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 12, handMesh.vertexBuffer)

        GLES20.glEnableVertexAttribArray(aNormalHandle)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, 12, handMesh.normalBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, handMesh.indexCount, GLES20.GL_UNSIGNED_SHORT, handMesh.indexBuffer)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)
    }

    private fun applyHumanSignAnimation(matrix: FloatArray, gloss: String, t: Float) {
        val upper = gloss.uppercase().trim()
        when {
            upper.contains("HOLA") || upper.contains("SALUDO") -> {
                // Seña HOLA: Saludo ondeando con la palma abierta
                val waveZ = (sin(t.toDouble() * 4.2) * 18.0).toFloat()
                val waveY = (sin(t.toDouble() * 4.2) * 6.0).toFloat()
                Matrix.rotateM(matrix, 0, waveZ, 0f, 0f, 1f)
                Matrix.rotateM(matrix, 0, waveY, 0f, 1f, 0f)
            }
            upper.contains("GRACIAS") -> {
                // Seña GRACIAS: Inclinación frontal suave
                val dipX = (sin(t.toDouble() * 3.0) * 16.0).toFloat()
                val moveZ = (sin(t.toDouble() * 3.0) * 0.20).toFloat()
                Matrix.translateM(matrix, 0, 0f, 0f, moveZ)
                Matrix.rotateM(matrix, 0, dipX, 1f, 0f, 0f)
            }
            upper.contains("POR-FAVOR") || upper.contains("FAVOR") -> {
                // Seña POR FAVOR: Círculo cordial en el pecho
                val cx = (sin(t.toDouble() * 2.8) * 0.12).toFloat()
                val cy = (sin(t.toDouble() * 2.8 + 1.57) * 0.12).toFloat()
                Matrix.translateM(matrix, 0, cx, cy, 0f)
            }
            upper.contains("AYUDAR") || upper.contains("AYUDA") -> {
                // Seña AYUDA: Impulso hacia adelante
                val pushZ = (sin(t.toDouble() * 3.2) * 0.28).toFloat()
                Matrix.translateM(matrix, 0, 0f, 0f, pushZ)
            }
            upper.contains("SI") -> {
                // Seña SÍ: Inflexión afirmativa
                val nodX = (sin(t.toDouble() * 4.5) * 20.0).toFloat()
                Matrix.rotateM(matrix, 0, nodX, 1f, 0f, 0f)
            }
            upper.contains("NO") -> {
                // Seña NO: Negación lateral
                val shakeY = (sin(t.toDouble() * 5.5) * 18.0).toFloat()
                Matrix.rotateM(matrix, 0, shakeY, 0f, 1f, 0f)
            }
            else -> {
                // Respiración y flotación sutil
                val breathY = (sin(t.toDouble() * 1.8) * 0.05).toFloat()
                Matrix.translateM(matrix, 0, 0f, breathY, 0f)
            }
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
