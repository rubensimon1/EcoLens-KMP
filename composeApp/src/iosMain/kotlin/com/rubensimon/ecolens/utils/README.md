# Utilidades (Utils) - iOS

Este paquete proporciona utilidades y funciones que interaccionan con las APIs nativas de iOS.

## Propósito
Evitar dependencias nativas en la capa de código común (`commonMain`), inyectando o proporcionando estas implementaciones cuando la app corre en un iPhone o iPad.

## Tareas Comunes
*   Petición de **permisos** nativos de iOS (NSCameraUsageDescription, NSLocationWhenInUseUsageDescription).
*   Gestión de UserDefaults (almacenamiento de preferencias nativo).
*   Integración con frameworks nativos (Foundation, AVFoundation para la cámara).
