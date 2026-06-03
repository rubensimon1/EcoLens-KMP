# Utilidades (Utils) - Android

Este paquete agrupa funciones y utilidades específicas del sistema operativo Android.

## Propósito
Aislar el código dependiente del SDK de Android para mantener la capa `commonMain` limpia de dependencias nativas.

## Tareas Comunes
*   Gestión de **Permisos** (Cámara, Ubicación) en tiempo de ejecución para Android.
*   Interacción con el sistema de archivos o `SharedPreferences`/`DataStore` nativo.
*   Servicios de background o notificaciones push específicas de Firebase Cloud Messaging para Android.
