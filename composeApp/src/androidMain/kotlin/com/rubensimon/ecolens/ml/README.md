# Módulo de Inteligencia Artificial (ML Layer)

Esta carpeta es exclusiva del *source set* `androidMain`, ya que contiene la implementación directa de los motores de Machine Learning nativos que procesan el flujo de la cámara en Android.

## 🧠 Arquitectura Híbrida de IA

EcoLens utiliza un enfoque de embudo (Fallback System) en **3 capas** para garantizar máxima velocidad y máxima precisión:

1. **Capa 1: Custom TFLite (Alta Precisión Local)**
   - Archivo: `EcoLensCustomLabeler.kt`
   - Función: Es el modelo de IA principal, entrenado a medida e incrustado en los `assets` de la app. Funciona 100% offline.
   - Categorías Estrictas: Agrupa todo lo que reconoce en las 4 macro-categorías clave del proyecto: **Vidrio, Papel y Cartón, Envases y Orgánico**.

2. **Capa 2: Google ML Kit (Red de Seguridad Local)**
   - Utilizado directamente en `ScanScreen.android.kt`.
   - Función: Si el modelo TFLite no reconoce el objeto o su nivel de confianza es muy bajo, se activa el modelo genérico de Google ML Kit (Image Labeling Base).
   - Capacidad: Puede devolver etiquetas más específicas como "Botella de Plástico", "Lata de Conservas" o "Periódico".

3. **Capa 3: Gemini Cloud / ML Backend (El Oráculo)**
   - Archivos: `EcoLensMlBackend.kt` (Endpoints de Python) o llamadas a Gemini (Google AI).
   - Función: Es el último recurso. Si las inteligencias locales fallan, la app congela el fotograma y lo envía a la nube en formato JSON Mode para que un modelo gigante emita un veredicto definitivo. Requiere internet y se utiliza solo cuando es estrictamente necesario.
