# Especificación de Arquitectura de Software

## 1. Patrón Arquitectónico
El sistema implementa **Clean Architecture (Arquitectura Limpia)** desacoplada en tres capas principales:
- **Presentation Layer (UI / AR):** Jetpack Compose, CameraX Preview, Sceneview ARCore Scene.
- **Domain Layer (Casos de Uso):** Lógica pura de negocio, conversión de glosas y orquestación de inferencia.
- **Data Layer (Repositorio y Fuentes):** Room Database local, MediaPipe HandLandmarker, TensorFlow Lite Classifier.

## 2. Flujo de Datos Bidireccional
```
[Cámara] -> [MediaPipe 21 Landmarks] -> [Normalizador Euclidiano] -> [TFLite Model] -> [Votación Temporal] -> [Texto / TTS]
[Voz / Texto] -> [SpeechRecognizer] -> [LSEC Grammar Engine] -> [Glosas LSEC] -> [Avatar 3D ARCore] -> [Animación RA]
```

## 3. Garantías de Rendimiento
- Latencia de inferencia en el borde < 150 ms.
- Procesamiento en tiempo real a >= 30 FPS en AR.
- Inferencia 100% offline sin envío de imágenes a la nube para garantizar privacidad.
