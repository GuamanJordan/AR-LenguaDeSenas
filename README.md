# Sistema de Comunicación Bidireccional en Lengua de Señas Ecuatoriana (LSEC) con Realidad Aumentada

**Universidad de las Fuerzas Armadas ESPE**  
**Carrera:** Ingeniería en Software  
**Asignatura:** Desarrollo de Software Aplicado al Dominio de Interculturalidad  
**Autores:** Jordan Guamán, Mesías Mariscal, Denise Rea  
**Docente:** Ing. Enrique Calvopiña  

## 📌 Descripción del Proyecto
Aplicación móvil accesible que facilita la comunicación bidireccional entre personas sordas y oyentes en Ecuador:
1. **Canal Sordo ➔ Oyente:** Captura señas en tiempo real con la cámara del dispositivo, extrae 21 puntos anatómicos tridimensionales mediante **MediaPipe**, clasifica el gesto con una red neuronal en **TensorFlow Lite** en el dispositivo móvil y muestra la transcripción textual con lectura por voz (TTS).
2. **Canal Oyente ➔ Sordo:** El oyente dicta por voz (STT) o escribe texto, el sistema traduce la oración a glosas LSEC respetando la gramática visual ecuatoriana, y un **Avatar 3D en Realidad Aumentada (ARCore)** proyecta las señas sobre una superficie física.

## 🚀 Pila Tecnológica
- **Móvil:** Android Nativo (Kotlin / Jetpack Compose) con arquitectura limpia (Clean Architecture).
- **Visión por Computadora:** Google MediaPipe Tasks (Hand Landmarker 3D).
- **Inferencia en el Borde:** TensorFlow Lite / LiteRT con cuantización Float16.
- **Realidad Aumentada:** ARCore / Sceneview (Filament) para renderizado y anclaje de planos.
- **Base de Datos:** SQLite / Room Database con corpus oficial LSEC (Gabriel Román y Zenodo).
- **Accesibilidad:** Estándares WCAG 2.2 AA/AAA (alto contraste, controles hápticos, independencia auditiva).
