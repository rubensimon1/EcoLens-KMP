# Capa de Utilidades (Utils Layer)

Esta carpeta funciona como la caja de herramientas de la aplicación EcoLens. Contiene los gestores de lógica compartida, persistencia local y funciones de ayuda (helpers) multiplataforma.

## 📂 Archivos Principales

- **`PointsManager.kt`**: Corazón de la gamificación. Gestiona la puntuación (XP), el cálculo de niveles, rachas diarias y la sincronización asíncrona de puntos hacia Supabase. Utiliza almacenamiento local para permitir funcionar offline (Offline-First).
- **`HistoryManager.kt`**: Administrador del historial de escaneos. Se encarga de persistir la información cada vez que el usuario detecta un objeto o interactúa con el SDDR.
- **Helpers Específicos (`TimeUtils.kt`, `Platform*.kt`)**: Funciones de conversión de fechas en formato ISO y puentes (expect/actual) para invocar funcionalidades nativas como seleccionar imágenes de la galería, sonido o vibración.

## ⚙️ Persistencia Local
En lugar de depender exclusivamente de llamadas de red (lo que ralentizaría la app), módulos como `PointsManager` utilizan **`multiplatform-settings`** (el equivalente a SharedPreferences/NSUserDefaults en KMP). Esto garantiza una interfaz veloz, relegando la sincronización en la nube a un segundo plano (Dispatchers.IO).
