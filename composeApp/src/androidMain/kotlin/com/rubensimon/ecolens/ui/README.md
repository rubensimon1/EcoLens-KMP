# Interfaz de Usuario (UI) - Android

Este paquete contiene los componentes de Interfaz de Usuario (UI) que son exclusivos de Android.

## Propósito
Implementar funcionalidades o vistas que no son soportadas o son difíciles de implementar en Compose Multiplatform común, y requieren el uso de APIs nativas de Android.

## Casos de Uso
*   **Cámara**: Implementación de `CameraX` para captura y previsualización de imágenes enfocadas al modelo de Machine Learning.
*   **Mapas**: Integración de `Google Maps SDK` para Android si la abstracción común no es suficiente.
*   **Puntos de Entrada**: `MainActivity` y configuraciones a nivel de actividad.
