package com.espe.arlenguadesenas.vision

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.abs

/**
 * Guía fonológica y visual para una seña de la Lengua de Señas Ecuatoriana (LSEC).
 * Contiene descripción anatómica paso a paso de GioSEÑAS y el Diccionario CONADIS.
 */
data class LsecSignGuide(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val steps: List<String>,
    val emoji: String,
    val isBimanual: Boolean,
    val category: String
)

/**
 * Estado del flujo interactivo de calibración y entrenamiento.
 */
sealed class CalibrationState {
    object Idle : CalibrationState()
    data class Countdown(val secondsLeft: Int) : CalibrationState()
    data class Recording(val secondsLeft: Float, val sampleCount: Int) : CalibrationState()
    data class Completed(val capturedCount: Int, val signName: String) : CalibrationState()
    data class Training(val progressPercent: Int) : CalibrationState()
    data class Success(val message: String, val accuracy: Float) : CalibrationState()
}

/**
 * Gestor del Modo Estudio de Calibración y Entrenamiento Guiado LSEC.
 * Permite al usuario seleccionar una seña, ver la guía visual detallada,
 * grabar muestras biométricas reales de 5 segundos con MediaPipe y
 * ejecutar aprendizaje en el dispositivo (On-Device SGD Transfer Learning).
 */
class LsecTrainingManager(
    private val context: Context,
    private val classifier: LsecNeuralClassifier
) {

    val catalog: List<LsecSignGuide> = listOf(
        LsecSignGuide(
            id = "HOLA",
            name = "HOLA",
            title = "Saludo Frontal Oscilante",
            description = "Mano a la altura de la sien con la palma abierta hacia el frente. Realiza un movimiento oscilante lateral amigable.",
            steps = listOf(
                "1. Levanta la mano dominante a la altura de la oreja o sien.",
                "2. Abre los 5 dedos con la palma orientada a la cámara.",
                "3. Oscila suavemente de izquierda a derecha 2 o 3 veces."
            ),
            emoji = "👋",
            isBimanual = false,
            category = "Saludos"
        ),
        LsecSignGuide(
            id = "BUENOS DÍAS",
            name = "BUENOS DÍAS",
            title = "Saludo Matutino Compuesto",
            description = "Mano en la barbilla o pecho ('Bueno') y luego se abre hacia el frente simulando la salida del sol ('Día').",
            steps = listOf(
                "1. Toca la barbilla o el pecho con las yemas de la mano abierta.",
                "2. Desplaza la mano hacia adelante y arriba suavemente.",
                "3. Mantén la palma orientada hacia ti o hacia el cielo."
            ),
            emoji = "🌅",
            isBimanual = false,
            category = "Saludos"
        ),
        LsecSignGuide(
            id = "BUENAS TARDES",
            name = "BUENAS TARDES",
            title = "Saludo Vespertino Bimanual",
            description = "Brazo base horizontal frente al pecho (horizonte). La mano dominante apoya su codo e inclina el antebrazo simulando el atardecer.",
            steps = listOf(
                "1. Coloca el brazo no dominante horizontal frente a tu pecho.",
                "2. Apoya el codo dominante sobre los dedos o dorso de la mano base.",
                "3. Inclina suavemente la mano dominante hacia abajo en ángulo de 45°."
            ),
            emoji = "☀️",
            isBimanual = true,
            category = "Saludos"
        ),
        LsecSignGuide(
            id = "BUENAS NOCHES",
            name = "BUENAS NOCHES",
            title = "Saludo Nocturno Bimanual",
            description = "Ambas manos abiertas frente al rostro bajan y se cruzan cerrándose suavemente, representando el anochecer.",
            steps = listOf(
                "1. Ubica ambas manos a la altura de las mejillas con palmas al frente.",
                "2. Desciende ambas manos cruzándolas frente al pecho.",
                "3. Cierra o relaja los dedos al cruzarse simulando la noche."
            ),
            emoji = "🌙",
            isBimanual = true,
            category = "Saludos"
        ),
        LsecSignGuide(
            id = "CHAO",
            name = "CHAO",
            title = "Despedida Oscilante",
            description = "Mano levantada a la altura del hombro abriendo y cerrando los dedos suavemente en ademán de despedida.",
            steps = listOf(
                "1. Levanta la mano dominante al frente.",
                "2. Flexiona y extiende las puntas de los 4 dedos hacia la palma repetidamente."
            ),
            emoji = "🙋",
            isBimanual = false,
            category = "Saludos"
        ),
        LsecSignGuide(
            id = "¿CÓMO ESTÁS?",
            name = "¿CÓMO ESTÁS?",
            title = "Pregunta de Bienestar Bimanual",
            description = "Ambas manos abiertas con palmas hacia arriba giran o se desplazan ligeramente al frente con expresión interrogativa.",
            steps = listOf(
                "1. Coloca ambas manos al nivel del abdomen con palmas hacia arriba.",
                "2. Muévelas suavemente hacia adelante o en pequeño círculo amigable."
            ),
            emoji = "🤝",
            isBimanual = true,
            category = "Preguntas"
        ),
        LsecSignGuide(
            id = "MUCHO GUSTO",
            name = "MUCHO GUSTO",
            title = "Expresión de Cortesía",
            description = "Mano dominante sobre el corazón con un giro circular suave, denotando agrado de conocer a alguien.",
            steps = listOf(
                "1. Apoya la palma abierta sobre el pecho izquierdo.",
                "2. Realiza un movimiento circular suave en sentido horario."
            ),
            emoji = "😊",
            isBimanual = false,
            category = "Cortesía"
        ),
        LsecSignGuide(
            id = "GRACIAS",
            name = "GRACIAS",
            title = "Agradecimiento desde la Barbilla",
            description = "Yemas de los dedos de la mano dominante tocan la barbilla o boca y se proyectan hacia el frente con la palma hacia arriba.",
            steps = listOf(
                "1. Toca la barbilla con las yemas de los dedos juntos.",
                "2. Desplaza la mano hacia adelante en dirección a la otra persona."
            ),
            emoji = "🙏",
            isBimanual = false,
            category = "Cortesía"
        ),
        LsecSignGuide(
            id = "POR FAVOR",
            name = "POR FAVOR",
            title = "Petición Amable",
            description = "Palma plana sobre el centro del pecho rotando suavemente en sentido horario.",
            steps = listOf(
                "1. Coloca la palma de la mano dominante sobre el centro del esternón.",
                "2. Frota en círculo suave con los dedos juntos."
            ),
            emoji = "🤲",
            isBimanual = false,
            category = "Cortesía"
        ),
        LsecSignGuide(
            id = "DE NADA",
            name = "DE NADA",
            title = "Respuesta de Cortesía",
            description = "Mano a la altura de la mejilla con el pulgar rozando o desplazando la palma hacia afuera con calidez.",
            steps = listOf(
                "1. Mano cerca de la barbilla o mejilla.",
                "2. Mueve suavemente la mano hacia un lado abriéndola en señal de cortesía."
            ),
            emoji = "✨",
            isBimanual = false,
            category = "Cortesía"
        ),
        LsecSignGuide(
            id = "AYUDA",
            name = "AYUDA",
            title = "Solicitud de Asistencia Bimanual",
            description = "Mano base plana con palma hacia arriba. Mano dominante en puño con pulgar erguido sobre la palma base, ambas suben juntas.",
            steps = listOf(
                "1. Palma no dominante extendida horizontal hacia arriba.",
                "2. Mano dominante en puño con pulgar arriba (👍) apoyada sobre la palma base.",
                "3. Eleva ambas manos juntas unos centímetros hacia arriba."
            ),
            emoji = "🆘",
            isBimanual = true,
            category = "Emergencia"
        ),
        LsecSignGuide(
            id = "SÍ",
            name = "SÍ",
            title = "Afirmación Gestual",
            description = "Mano cerrada en puño asintiendo verticalmente (arriba hacia abajo) simulando el movimiento de cabeza.",
            steps = listOf(
                "1. Cierra la mano en puño frente a ti.",
                "2. Flexiona la muñeca hacia abajo y arriba dos veces (asentimiento)."
            ),
            emoji = "👍",
            isBimanual = false,
            category = "Respuestas"
        ),
        LsecSignGuide(
            id = "NO",
            name = "NO",
            title = "Negación Dactilar Oscilante",
            description = "Dedo índice extendido (o índice y medio) oscilando de lado a lado con energía.",
            steps = listOf(
                "1. Extiende el dedo índice con los demás dedos doblados.",
                "2. Oscila rápidamente el índice a la izquierda y derecha."
            ),
            emoji = "☝️",
            isBimanual = false,
            category = "Respuestas"
        ),
        LsecSignGuide(
            id = "TE QUIERO",
            name = "TE QUIERO",
            title = "Afecto y Amor LSEC",
            description = "Mano extendida con pulgar, índice y meñique abiertos; medio y anular flexionados sobre la palma.",
            steps = listOf(
                "1. Extiende el pulgar, el índice y el meñique.",
                "2. Flexiona el dedo medio y anular hacia la palma (signo ILY).",
                "3. Orienta la palma hacia el frente con calidez."
            ),
            emoji = "🤟",
            isBimanual = false,
            category = "Afecto"
        )
    )

    private val _selectedSign = MutableStateFlow(catalog[0])
    val selectedSign: StateFlow<LsecSignGuide> = _selectedSign.asStateFlow()

    private val _state = MutableStateFlow<CalibrationState>(CalibrationState.Idle)
    val state: StateFlow<CalibrationState> = _state.asStateFlow()

    private val currentSamples = mutableListOf<FloatArray>()
    private var recordStartTime = 0L
    private val recordDurationMs = 5000L

    fun selectSign(guide: LsecSignGuide) {
        _selectedSign.value = guide
        _state.value = CalibrationState.Idle
        currentSamples.clear()
    }

    fun startCountdown() {
        currentSamples.clear()
        _state.value = CalibrationState.Countdown(3)
    }

    fun updateCountdown(seconds: Int) {
        if (seconds > 0) {
            _state.value = CalibrationState.Countdown(seconds)
        } else {
            recordStartTime = System.currentTimeMillis()
            _state.value = CalibrationState.Recording(5.0f, 0)
        }
    }

    /**
     * Invocado por HandLandmarkerHelper en cada frame de cámara analizado.
     * Si la grabación de 5 segundos está activa, agrega el vector de 128 características.
     */
    fun onNewFrame(h1: FloatArray, h2: FloatArray?) {
        val currentState = _state.value
        if (currentState is CalibrationState.Recording) {
            val elapsed = System.currentTimeMillis() - recordStartTime
            val remainingMs = recordDurationMs - elapsed
            val remainingSec = (remainingMs / 1000f).coerceAtLeast(0f)

            // Validar que la mano tenga datos reales detectados
            val hasPoints = h1.any { it != 0f }
            if (hasPoints) {
                val features = buildFeatureVector(h1, h2)
                currentSamples.add(features)
            }

            if (remainingMs <= 0) {
                // Finalizado el periodo de 5 segundos
                val captured = currentSamples.size
                _state.value = CalibrationState.Completed(captured, _selectedSign.value.name)
                android.util.Log.i("LSEC_CALIBRATION", "Grabación 5s finalizada. Muestras recolectadas: $captured para ${_selectedSign.value.name}")
            } else {
                _state.value = CalibrationState.Recording(remainingSec, currentSamples.size)
            }
        }
    }

    private fun buildFeatureVector(h1: FloatArray, h2: FloatArray?): FloatArray {
        val feat = FloatArray(128)
        System.arraycopy(h1, 0, feat, 0, minOf(h1.size, 63))
        if (h2 != null) {
            System.arraycopy(h2, 0, feat, 63, minOf(h2.size, 63))
            feat[126] = abs(h1[0] - h2[0])
            feat[127] = abs(h1[1] - h2[1])
        }
        return feat
    }

    /**
     * Ejecuta el entrenamiento inmediato en el procesador (On-Device SGD Backpropagation).
     */
    fun trainNow(onComplete: (Boolean, String) -> Unit) {
        val capturedCount = currentSamples.size
        if (capturedCount < 5) {
            onComplete(false, "Se necesitan al menos 5 cuadros válidos con la mano detectada.")
            return
        }

        _state.value = CalibrationState.Training(progressPercent = 10)

        try {
            val signName = _selectedSign.value.name
            val classIdx = classifier.getClassIndex(signName)
            if (classIdx < 0) {
                onComplete(false, "Clase no reconocida en el clasificador: $signName")
                return
            }

            // Ejecutar SGD de calibración directamente sobre los pesos de la red neuronal
            val accuracy = classifier.fineTuneOnSamples(currentSamples, classIdx, epochs = 20, lr = 0.008f)

            // Guardar pesos personalizados en memoria persistente
            classifier.saveCustomWeights(context)

            _state.value = CalibrationState.Success(
                message = "¡Red Neuronal calibrada con éxito para '$signName' ($capturedCount muestras)!",
                accuracy = accuracy
            )

            android.util.Log.i("LSEC_TRAIN", "Calibración On-Device completada: $capturedCount muestras, certeza: $accuracy%")
            onComplete(true, "Calibración completada con éxito.")
        } catch (e: Exception) {
            android.util.Log.e("LSEC_TRAIN", "Error en calibración on-device", e)
            _state.value = CalibrationState.Idle
            onComplete(false, "Error: ${e.message}")
        }
    }

    fun resetState() {
        currentSamples.clear()
        _state.value = CalibrationState.Idle
    }
}

