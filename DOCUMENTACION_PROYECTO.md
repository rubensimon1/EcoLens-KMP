# 📗 EcoLens — Documentación Técnica del Proyecto

## 1. ¿Qué es EcoLens?

EcoLens es una **aplicación móvil multiplataforma** (Android e iOS) de **concienciación medioambiental y reciclaje**. Permite a los usuarios escanear objetos con la cámara para identificar de qué material están hechos y en qué contenedor deben depositarlos, acumulando puntos y recompensas por reciclar correctamente.

---

## 2. ¿Por qué Kotlin Multiplatform (KMP)?

| Criterio | Decisión |
|---|---|
| **Código compartido** | KMP permite escribir la lógica de negocio, modelos de datos, acceso a red y UI **una sola vez** en `commonMain`, y reutilizarla en Android e iOS. |
| **UI nativa** | Se usa **Compose Multiplatform** (de JetBrains), que compila a Jetpack Compose en Android y a SwiftUI/UIKit en iOS. |
| **Rendimiento** | A diferencia de Flutter o React Native, KMP compila a código **nativo** en cada plataforma, sin puentes ni intérpretes. |
| **Interoperabilidad** | Se puede llamar a APIs nativas (CameraX en Android, AVFoundation en iOS) usando el mecanismo `expect/actual`. |
| **Lenguaje** | Kotlin es el lenguaje oficial de Android y muy similar a Swift, lo que facilita el desarrollo iOS. |

### Tecnologías utilizadas

- **Kotlin** — Lenguaje principal del proyecto.
- **Compose Multiplatform** — Framework de UI declarativa multiplataforma.
- **Supabase** — Backend-as-a-Service (BaaS) para base de datos PostgreSQL, autenticación y almacenamiento de archivos.
- **Ktor** — Cliente HTTP multiplataforma para las peticiones de red.
- **kotlinx.serialization** — Serialización/deserialización de JSON.
- **kotlinx.datetime** — Manejo de fechas multiplataforma.
- **Multiplatform Settings** — Reemplazo multiplataforma de SharedPreferences (Android) y NSUserDefaults (iOS).
- **Coil 3** — Carga asíncrona de imágenes (avatares, etc.).
- **Google ML Kit** — Reconocimiento de objetos por IA en Android (incluyendo Modelos Personalizados).
- **CameraX** — API de cámara en Android.
- **AVFoundation** — API de cámara en iOS.
- **Google Maps SDK** — Mapas en Android.
- **MapKit** — Mapas en iOS.

---

## 3. Arquitectura del Proyecto

```
composeApp/src/
├── commonMain/        ← Código compartido (90% del proyecto)
│   └── kotlin/com/rubensimon/ecolens/
│       ├── App.kt                  ← Punto de entrada, Scaffold, barra inferior
│       ├── EcoLensConfig.kt        ← Configuración (URLs de Supabase y ML)
│       ├── data/                   ← Capa de Datos (Modelos, Repositorio, Red)
│       │   ├── models/             ← Modelos de datos (auth, items, maps, social)
│       │   ├── network/            ← Cliente Supabase y llamadas a la API
│       │   └── repository/         ← CRUD de usuarios, cupones, historial
│       ├── ui/                     ← Capa de Interfaz
│       │   ├── components/         ← Componentes reutilizables (GlassCard, etc.)
│       │   ├── navigation/         ← Sistema de rutas (Screen.kt, AppNavigation.kt)
│       │   └── screens/            ← Pantallas (auth, main, features)
│       └── utils/                  ← Managers compartidos
│
├── androidMain/       ← Código específico Android
│   └── kotlin/.../
│       ├── MainActivity.kt         ← Activity principal
│       ├── ml/                     ← NUEVO: Módulo de IA y Machine Learning
│       │   ├── EcoLensMlBackend.kt ← Interfaz del backend de IA
│       │   └── EcoLensCustomLabeler.kt ← Etiquetador personalizado de objetos
│       ├── ui/
│       │   └── screens/features/
│       │       └── MapsScreen.android.kt  ← Google Maps + filtros de proximidad
│       └── utils/
│           ├── MadridPointsFetcher.kt ← Carga de puntos de reciclaje (XML/CSV)
│           ├── CameraView.android.kt  ← CameraX + ML Kit
│           └── TimeUtils.android.kt   ← Implementación de fechas
│
└── iosMain/           ← Código específico iOS
    └── kotlin/.../
        ├── MainViewController.kt    ← Controlador raíz iOS
        ├── ui/
        │   └── screens/features/
        │       └── MapsScreen.ios.kt    ← MapKit + filtros de proximidad
        └── utils/
            ├── CameraView.ios.kt    ← AVFoundation
            └── TimeUtils.ios.kt     ← Implementación de fechas
```

### Patrón `expect/actual`

Es el mecanismo de KMP para definir una interfaz en `commonMain` y proporcionar implementaciones específicas por plataforma:

```kotlin
// commonMain — Define QUÉ se necesita
@Composable
expect fun PlatformCameraView(modifier: Modifier, isSddr: Boolean, onScanComplete: (String, Int) -> Unit)

// androidMain — Implementa CÓMO con CameraX + ML Kit
@Composable
actual fun PlatformCameraView(...) { /* CameraX + Google ML Kit */ }

// iosMain — Implementa CÓMO con AVFoundation
@Composable
actual fun PlatformCameraView(...) { /* AVFoundation */ }
```

Se usa en: `PlatformCameraView`, `PlatformMapView`, `PlatformImagePicker`, `PlatformAudio`, `PlatformShare`, `TimeUtils`.

---

## 4. Flujo de Datos y Conexión con Supabase

### 4.1 Configuración de Secretos

Las credenciales (URL de Supabase, API Key, URL del backend ML) se guardan en `local.properties` (NO se sube a Git). El script de Gradle `generateSecrets` las convierte automáticamente en un archivo Kotlin `EcoLensSecrets.kt` dentro de `build/`, que es leído por `EcoLensConfig.kt`.

### 4.2 Cliente Supabase (`SupabaseClient.kt`)

Es un **singleton** que inicializa el cliente de Supabase con los módulos:
- **Postgrest** — Consultas a la base de datos PostgreSQL.
- **Auth** — Autenticación de usuarios (registro/login).
- **Storage** — Subida/descarga de archivos (fotos de perfil).
- **Realtime** — Suscripciones a cambios en tiempo real.

### 4.3 Repositorio (`UserRepository.kt`)

Es la **capa de acceso a datos**. Centraliza TODAS las operaciones con Supabase:
Gestión de usuarios (`createOrUpdateUser`, `getTopUsers`, `updateProfileInfo`), historial (`getUserHistory`, `getGlobalActivity`) y cupones (`getCouponsFromDb`, `redeemCoupon`, `validateRedemption`).

### 4.4 Tablas en Supabase

| Tabla | Campos principales | Uso |
|---|---|---|
| `usuarios` | id, username, display_name, puntos, total_scans, total_xp, profile_picture_url | Perfiles de usuario |
| `historial_escaneos` | id, user_id, object_name, points, action_type, created_at | Historial de reciclaje |
| `cupones_tienda` | id, titulo, descripcion, coste_puntos, stock, dias_validez, activo | Catálogo de recompensas |
| `cupones_canjeados` | id, user_id, cupon_id, codigo_qr, estado, fecha_canje, fecha_uso | Cupones canjeados |
| `notificaciones` | id, user_id, title, description, type, is_read | Notificaciones del usuario |
| `historial_sddr` | user_id, title, amount | Historial Eco-Retorno |

---

## 5. Sistema de Navegación

### 5.1 Rutas (`Screen.kt`)
Cada pantalla tiene una ruta definida como `sealed class` (Welcome, Menu, Scan, Maps, etc.).

### 5.2 Navegación (`AppNavigation.kt`)
Usa `NavHost` de Compose Navigation. Cada `composable()` mapea una ruta a una pantalla. Incluye animaciones de transición (slide + fade).

### 5.3 Barra inferior (`ModernBottomBar` en `App.kt`)
Es una barra flotante con glassmorphism que aparece solo en las pantallas principales (menu, collection, rewards, profile). Usa `navController.navigate()` con `popUpTo(startDestinationId)` para limpiar la pila de navegación y evitar acumulación de pantallas.

---

## 6. Managers (Lógica de Negocio)

### 6.1 `PointsManager`
Gestiona el sistema de **puntos, niveles y rachas**. Se acumulan al escanear objetos. Utiliza debounce (2s) antes de sincronizar con Supabase para optimizar llamadas de red. Genera notificaciones al subir de nivel.

### 6.2 `SddrManager`
Gestiona el **Sistema de Depósito, Devolución y Retorno** (Eco-Retorno). Simula el sistema SDDR español.

### 6.3 `HistoryManager`
Gestiona el **historial de escaneos** con sincronización offline-first. Usa una cola de elementos pendientes en caso de falta de red.

### 6.4 `NotificationManager`
Sistema de **notificaciones persistentes** local y remota, evitando duplicados.

---

## 7. Pantallas de la Aplicación

- **Autenticación**: Welcome, Onboarding, Login.
- **Principales**: Menu (Dashboard), Profile, Settings.
- **Funcionalidades**: Scan (IA), Maps, Collection (Eco-Dex), Rewards (Cupones), Sddr (Eco-Retorno), History, Leaderboard, AiChat, Notifications.

---

## 8. Inteligencia Artificial y Reconocimiento de Objetos

La detección de objetos en tiempo real es el pilar central de EcoLens. Inicialmente construida sobre modelos genéricos, **ha sido mejorada sustancialmente** implementando un etiquetador personalizado.

### 8.1 EcoLens Custom Labeler
Ubicado en `androidMain/ml/EcoLensCustomLabeler.kt`, este módulo se encarga de analizar los fotogramas (frames) de la cámara mediante IA. 
A diferencia del etiquetador por defecto, el `CustomLabeler` está configurado para:
- Detectar un umbral de confianza mínimo **mejorado (% de coincidencia)**.
- Clasificar los objetos reconociendo específicamente envases y materiales orientados al reciclaje (plástico, vidrio, cartón, orgánico).
- Filtrar detecciones genéricas irrelevantes para aumentar la precisión de los objetos reciclables.

### 8.2 Interfaz de Backend de IA (`EcoLensMlBackend.kt`)
Sirve como capa de abstracción para conectar la predicción del modelo de Machine Learning con el sistema de puntuación y la UI de la cámara (`CameraView`). Recibe los metadatos del objeto detectado, calcula si es un objeto válido, asigna los puntos y notifica al `PointsManager`.

---

## 9. Sistema de Mapas

### 9.1 Arquitectura
`MapsScreen.kt` (UI) llama a `PlatformMapView` (expect). Se implementa nativamente en `MapsScreen.android.kt` (Google Maps) y `MapsScreen.ios.kt` (MapKit).

### 9.2 Carga de datos
`MadridPointsFetcher.kt` descarga Puntos Limpios (XML/CSV) desde `datos.madrid.es`, utilizando una caché de 24h.

### 9.3 Filtros
TODOS, SDDR, FIJO, MOVIL, PROXIMIDAD (<3km, ordenador por cercanía).

---

## 10. Sistema de Recompensas

1. El usuario escanea objetos → gana **puntos**.
2. En la tienda (`RewardsScreen`), canjea puntos por **cupones** (`cupones_tienda`).
3. Se crea registro en `cupones_canjeados` con estado `"activo"` y QR único.
4. Al validar el QR en tienda, `validateRedemption()` ejecuta una transacción atómica `eq("estado", "activo")` para evitar dobles gastos.

---

## 11. Sistema de Diseño (Glassmorphism)

- **EcoColors**: Paleta de colores reactiva.
- **GlassCard / GlassButton / GlassTextField**: Componentes con fondo translúcido y desenfoque (blur).
- Da un aspecto premium a la interfaz sin sobrecargar visualmente la aplicación.

---

## 12. Flujo de Datos Completo (Ejemplo: Escaneo)

```
1. Usuario pulsa "Escanear" en MenuScreen
     ↓
2. navController.navigate("scan/false")
     ↓
3. ScanScreen → PlatformCameraView (expect/actual)
     ↓
4. [Android] CameraX captura frame → EcoLensCustomLabeler analiza 
   → (Calcula % de precisión) → devuelve "Botella de Plástico (95%)"
     ↓
5. onScanComplete("Botella de Plástico", 10)
     ↓
6. PointsManager.addPoints(10, "scan")
   ├─ Actualiza Settings local
   └─ autoSync() → (debounce 2s) → UPDATE usuarios en Supabase
     ↓
7. HistoryManager.addHistoryItem("Botella de Plástico", 10, userId)
     ↓
8. navController.popBackStack() → vuelve al menú
```

---

## 13. Persistencia Offline-First

1. **Settings local** como caché.
2. **Supabase** como BD remota.
3. **Cola de pendientes** que se sincroniza al recuperar red.

---

## 14. Seguridad

- Claves protegidas en `local.properties`.
- Row Level Security (RLS) en PostgreSQL.
- Transacciones atómicas para canjeo de cupones.

---

## 15. Dependencias Principales

| Librería | Uso |
|---|---|
| Kotlin / Compose Multiplatform | Framework base y UI |
| Supabase-kt / Ktor | Backend y HTTP |
| ML Kit Custom Models | IA reconocimiento mejorado Android |
| CameraX / AVFoundation | Cámara multiplataforma |
| Google Maps SDK | Mapas |

---

## 16. Cómo Compilar y Ejecutar

### Android
```bash
./gradlew :composeApp:assembleDebug
```
### iOS
Abriendo `iosApp/iosApp.xcodeproj` en Xcode y ejecutando en simulador o dispositivo físico.

---

*Documento actualizado en Junio de 2026 para reflejar las mejoras del modelo de IA (TFG de EcoLens).*
