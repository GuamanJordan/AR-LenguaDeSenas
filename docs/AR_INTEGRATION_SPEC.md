# Especificación de Integración de Realidad Aumentada (ARCore)

## 1. Detección de Planos y Posicionamiento
- Uso de `ARPlaneManager` para identificar planos horizontales (mesas, piso).
- Algoritmo de raycasting (`ARRaycastManager`) para ubicar el cursor virtual (Reticle).
- Anclaje espacial (`ARAnchor`) para fijar el Avatar 3D de forma estable ante movimientos de cámara.

## 2. Avatar 3D y Rango de Movimiento
- Formato: glTF / GLB optimizado para móviles (< 15 MB).
- Rigging con esqueletos humanoides estándar y huesos dedicados para falanges de dedos.
- Control de animación: Maquina de estados (State Machine) con estados Idle, Transition y Sign execution.
