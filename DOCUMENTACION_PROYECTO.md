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
- **Google ML Kit** — Reconocimiento de objetos por IA en Android.
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
│       ├── data/
│       │   ├── models/             ← Modelos de datos (data classes)
│       │   │   ├── auth/           ← LoginResponse, RegisterRequest
│       │   │   ├── items/          ← EcoItems, RewardItem, Coupon
│       │   │   ├── maps/           ← RecyclingPoint, EcoLatLng
│       │   │   └── social/         ← UserModel, HistoryItemModel, RedemptionModel, NotificationModel
│       │   ├── network/
│       │   │   └── SupabaseClient.kt  ← Cliente Supabase singleton
│       │   └── repository/
│       │       └── UserRepository.kt  ← CRUD de usuarios, cupones, historial
│       ├── ui/
│       │   ├── components/         ← Componentes reutilizables (GlassCard, etc.)
│       │   ├── navigation/         ← Sistema de rutas (Screen.kt, AppNavigation.kt)
│       │   └── screens/
│       │       ├── auth/           ← Login, Onboarding, Welcome
│       │       ├── features/       ← Scan, Maps, Collection, Rewards, SDDR, History, Leaderboard
│       │       └── main/           ← Menu, Profile, Settings, AiChat, Notifications
│       └── utils/                  ← Managers (Points, SDDR, History, Notifications, Time)
│
├── androidMain/       ← Código específico Android
│   └── kotlin/.../
│       ├── MainActivity.kt         ← Activity principal
│       ├── ui/screens/features/
│       │   └── MapsScreen.android.kt  ← Google Maps + filtros de proximidad
│       └── utils/
│           ├── MadridPointsFetcher.kt ← Carga de puntos de reciclaje (XML/CSV)
│           ├── CameraView.android.kt  ← CameraX + ML Kit
│           └── TimeUtils.android.kt   ← Implementación de fechas
│
└── iosMain/           ← Código específico iOS
    └── kotlin/.../
        ├── MainViewController.kt    ← Controlador raíz iOS
        ├── ui/screens/features/
        │   └── MapsScreen.ios.kt    ← MapKit + filtros de proximidad
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

```
local.properties  →  (Gradle task)  →  EcoLensSecrets.kt (en build/)  →  EcoLensConfig.kt  →  SupabaseClient.kt
```

**¿Por qué así?** Para que las claves nunca estén en el código fuente que se sube a GitHub, pero sí estén disponibles al compilar.

### 4.2 Cliente Supabase (`SupabaseClient.kt`)

Es un **singleton** (`object SupabaseClientProvider`) que inicializa el cliente de Supabase con los módulos:
- **Postgrest** — Consultas a la base de datos PostgreSQL.
- **Auth** — Autenticación de usuarios (registro/login).
- **Storage** — Subida/descarga de archivos (fotos de perfil).
- **Realtime** — Suscripciones a cambios en tiempo real.

### 4.3 Repositorio (`UserRepository.kt`)

Es la **capa de acceso a datos**. Centraliza TODAS las operaciones con Supabase:

| Función | Qué hace |
|---|---|
| `createOrUpdateUser()` | Crea un usuario o actualiza sus puntos/scans |
| `getTopUsers()` | Obtiene el ranking (leaderboard) ordenado por puntos |
| `searchUserByUsername()` | Busca un usuario por nombre |
| `getUserById()` | Obtiene un usuario por su UUID |
| `updatePoints()` | Actualiza puntos y scans en la BD |
| `updateProfileInfo()` | Cambia nombre público y biografía |
| `updateProfilePictureUrl()` | Actualiza la URL del avatar |
| `uploadProfilePicture()` | Sube una imagen a Supabase Storage y devuelve la URL pública |
| `updateNotificationPreferences()` | Cambia preferencias de notificaciones |
| `getCouponsFromDb()` | Lista cupones disponibles en la tienda |
| `redeemCoupon()` | Registra un canje de cupón |
| `validateRedemption()` | Marca un cupón como "usado" (validación atómica) |
| `getUserRedemptions()` | Lista los cupones canjeados por un usuario |
| `getUserHistory()` | Obtiene el historial de escaneos |
| `getGlobalActivity()` | Obtiene la actividad de toda la comunidad |

### 4.4 Tablas en Supabase

| Tabla | Campos principales | Uso |
|---|---|---|
| `usuarios` | id, username, display_name, puntos, total_scans, total_xp, profile_picture_url, bio, sddr_balance, sddr_containers | Perfiles de usuario |
| `historial_escaneos` | id, user_id, object_name, points, co2_impact, action_type, created_at | Historial de reciclaje |
| `cupones_tienda` | id, titulo, descripcion, coste_puntos, stock, dias_validez, activo | Catálogo de recompensas |
| `cupones_canjeados` | id, user_id, cupon_id, codigo_qr, estado, fecha_canje, fecha_uso | Cupones canjeados |
| `notificaciones` | id, user_id, title, description, type, is_read, created_at | Notificaciones del usuario |
| `historial_sddr` | user_id, title, amount, created_at | Historial de devolución de envases |

---

## 5. Sistema de Navegación

### 5.1 Rutas (`Screen.kt`)

Cada pantalla tiene una ruta definida como `sealed class`:

```kotlin
sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Menu : Screen("menu")
    data object Scan : Screen("scan/{isSddr}")
    data object Maps : Screen("maps?filter={filter}")
    data object Rewards : Screen("rewards")
    data object Collection : Screen("collection")
    // ... etc.
}
```

### 5.2 Navegación (`AppNavigation.kt`)

Usa `NavHost` de Compose Navigation. Cada `composable()` mapea una ruta a una pantalla. Incluye animaciones de transición (slide + fade).

### 5.3 Barra inferior (`ModernBottomBar` en `App.kt`)

Es una barra flotante con glassmorphism que aparece solo en las pantallas principales (menu, collection, rewards, profile). Usa `navController.navigate()` con `popUpTo(startDestinationId)` para limpiar la pila de navegación y evitar acumulación de pantallas.

---

## 6. Managers (Lógica de Negocio)

### 6.1 `PointsManager`

Gestiona el sistema de **puntos, niveles y rachas**:

- **Puntos**: Se acumulan al escanear objetos (+10 por scan, +100 por misión diaria, +50 por SDDR).
- **Niveles**: Calculados según XP total acumulado (Nivel 1: 0-99, Nivel 2: 100-299, etc.).
- **Racha diaria**: Cuenta días consecutivos escaneando.
- **Misión diaria**: Al alcanzar X scans en un día, se otorga un bonus.
- **Sincronización**: Usa un sistema de **debounce** (espera 2s sin actividad) antes de sincronizar con Supabase para evitar llamadas excesivas.
- **Notificación de nivel**: Al subir de nivel, genera automáticamente una notificación.

### 6.2 `SddrManager`

Gestiona el **Sistema de Depósito, Devolución y Retorno** (Eco-Retorno):

- Simula el sistema SDDR español donde cada envase tiene un depósito de 0,10€.
- Al escanear un QR de vale SDDR (`SDDR|valor|cantidad`), suma el balance.
- Mantiene historial local + nube con recálculo de totales desde el historial cloud (fuente de verdad).
- **StateFlow** reactivo para que la UI se actualice automáticamente.

### 6.3 `HistoryManager`

Gestiona el **historial de escaneos** con sincronización offline-first:

- Guarda cada scan localmente (Settings) y remotamente (Supabase).
- Si falla la red, guarda en **cola de pendientes** (`pending_history_sync`).
- `syncPendingItems()` reintenta enviar los pendientes cuando vuelve la conexión.
- También gestiona la **caché de perfiles** de otros usuarios (para el leaderboard).
- Soporta sincronización de **cambios de perfil offline** (username, avatar).

### 6.4 `NotificationManager`

Sistema de **notificaciones persistentes**:

- Almacena notificaciones localmente (JSON serializado en Settings).
- Sincroniza con la tabla `notificaciones` de Supabase en segundo plano.
- Fusiona notificaciones locales y remotas evitando duplicados.
- Genera notificaciones de bienvenida automáticas para usuarios nuevos.
- Contador de no leídas reactivo con **StateFlow**.

---

## 7. Pantallas de la Aplicación

### 7.1 Autenticación

| Pantalla | Archivo | Función |
|---|---|---|
| **Welcome** | `WelcomeScreen.kt` | Pantalla de bienvenida con animación |
| **Onboarding** | `OnboardingScreen.kt` | Tutorial de 3 pasos para nuevos usuarios |
| **Login** | `LoginScreen.kt` | Registro/Login con Supabase Auth |

### 7.2 Pantallas Principales

| Pantalla | Archivo | Función |
|---|---|---|
| **Menú** | `MenuScreen.kt` | Dashboard principal con tarjetas de acceso rápido, actividad de la comunidad, gráfico de CO2 y misión diaria |
| **Perfil** | `ProfileScreen.kt` | Foto, nombre, bio, estadísticas. Subida de avatar a Supabase Storage |
| **Ajustes** | `SettingsScreen.kt` | Tema claro/oscuro, sonidos, preferencias de notificaciones, cerrar sesión |

### 7.3 Funcionalidades

| Pantalla | Archivo | Función |
|---|---|---|
| **Escaneo** | `ScanScreen.kt` | Abre la cámara (expect/actual) para identificar objetos con IA |
| **Mapa** | `MapsScreen.kt` | Mapa con puntos de reciclaje. Filtros: TODOS, SDDR, FIJO, MOVIL, PROXIMIDAD |
| **Eco-Dex** | `CollectionScreen.kt` | Galería tipo Pokédex con +45 objetos reciclables, clasificados por contenedor |
| **Recompensas** | `RewardsScreen.kt` | Tienda de cupones canjeables + mis cupones con QR validable |
| **Eco-Retorno** | `SddrScreen.kt` | Balance SDDR, historial de devoluciones de envases |
| **Historial** | `HistoryScreen.kt` | Lista cronológica de todos los escaneos realizados |
| **Ranking** | `LeaderboardScreen.kt` | Clasificación global de usuarios por puntos |
| **Chat IA** | `AiChatScreen.kt` | Asistente de reciclaje con inteligencia artificial |
| **Notificaciones** | `NotificationsScreen.kt` | Listado de alertas (nivel, recompensas, etc.) |

---

## 8. Sistema de Mapas

### 8.1 Arquitectura

```
MapsScreen.kt (commonMain)          ← UI común: filtros, bottom sheet
    ↓ llama a
PlatformMapView (expect)             ← Interfaz multiplataforma
    ↓ implementado por
MapsScreen.android.kt               ← Google Maps + Location API
MapsScreen.ios.kt                   ← MapKit + CoreLocation
```

### 8.2 Carga de datos (Android)

`MadridPointsFetcher.kt` descarga datos del **Ayuntamiento de Madrid**:
- **Puntos Fijos**: XML de `datos.madrid.es` (17 Puntos Limpios principales).
- **Puntos Móviles**: CSV.
- **Puntos Proximidad**: CSV.
- **Puntos 24h**: CSV.
- Usa **caché local** (24h TTL) para no descargar cada vez.
- Incluye **fallback** con los 17 puntos fijos hardcodeados por si el servidor falla.

### 8.3 Filtros

| Filtro | Comportamiento |
|---|---|
| TODOS | Muestra todos los puntos sin filtrar |
| SDDR | Solo puntos de retorno de envases |
| FIJO | Solo Puntos Limpios fijos |
| MOVIL | Puntos móviles + Puntos 24h |
| PROXIMIDAD | Calcula distancia GPS real, filtra <3km, ordena por cercanía |

---

## 9. Sistema de Recompensas

### Flujo completo:

1. El usuario escanea objetos → gana **puntos**.
2. En la tienda (`RewardsScreen`), canjea puntos por **cupones** de la tabla `cupones_tienda`.
3. Se crea un registro en `cupones_canjeados` con estado `"activo"` y un QR único.
4. El usuario muestra el QR en el comercio.
5. Al pulsar "Validar", se llama a `validateRedemption()` que usa una **condición atómica** `eq("estado", "activo")` para evitar que el mismo cupón se valide dos veces simultáneamente en diferentes dispositivos.
6. El cupón pasa a estado `"usado"`.

---

## 10. Sistema de Diseño (Glassmorphism)

Definido en `GlassComponents.kt`:

- **EcoColors**: Objeto singleton con paleta de colores reactiva (claro/oscuro).
- **GlassCard**: Tarjeta con efecto cristal esmerilado (fondo translúcido + borde luminoso).
- **GlassButton / GlassSecondaryButton**: Botones con esquinas redondeadas y elevación.
- **GlassTextField**: Campo de texto estilizado.
- **shimmerEffect()**: Animación de brillo para estados de carga.
- **fadingEdge()**: Desvanecimiento inferior para listas.
- **StatusBadge**: Etiqueta de estado con color e icono.

**¿Por qué Glassmorphism?** Proporciona una estética moderna y premium, con transparencias y desenfoque que dan profundidad a la interfaz sin ser visualmente pesada.

---

## 11. Flujo de Datos Completo (Ejemplo: Escaneo)

```
1. Usuario pulsa "Escanear" en MenuScreen
     ↓
2. navController.navigate("scan/false")
     ↓
3. ScanScreen → PlatformCameraView (expect/actual)
     ↓
4. [Android] CameraX captura frame → ML Kit analiza → devuelve "Plastic Bottle"
   [iOS] AVFoundation captura frame → Vision analiza → devuelve "Plastic Bottle"
     ↓
5. onScanComplete("Plastic Bottle", 10)
     ↓
6. PointsManager.addPoints(10, "scan")
   ├─ Actualiza Settings local (puntos, XP, scans)
   ├─ Comprueba subida de nivel → NotificationManager.addNotification()
   └─ autoSync() → (debounce 2s) → syncToSupabase() → UPDATE usuarios SET puntos=...
     ↓
7. HistoryManager.addHistoryItem("Plastic Bottle", 10, userId)
   ├─ Guarda en Settings local
   └─ INSERT en historial_escaneos de Supabase
     ↓
8. navController.popBackStack() → vuelve al menú con datos actualizados
```

---

## 12. Persistencia Offline-First

La app sigue una estrategia **offline-first**:

1. **Settings local** (Multiplatform Settings) como caché inmediata.
2. **Supabase** como fuente de verdad remota.
3. **Cola de pendientes** para operaciones fallidas (sin conexión).
4. Al volver online, `syncPendingItems()` procesa la cola.

Esto garantiza que la app **siempre funciona**, incluso sin internet.

---

## 13. Seguridad

| Aspecto | Implementación |
|---|---|
| **Claves** | En `local.properties` (no versionado). Se genera `EcoLensSecrets.kt` en build/ |
| **Autenticación** | Supabase Auth con JWT |
| **Row Level Security** | Las tablas de Supabase tienen RLS activado |
| **Validación atómica** | Los cupones usan `eq("estado", "activo")` para evitar doble uso |
| **API Key** | La `anon key` de Supabase solo permite operaciones autorizadas por RLS |

---

## 14. Dependencias Principales

| Librería | Versión | Uso |
|---|---|---|
| Kotlin Multiplatform | 2.x | Framework base |
| Compose Multiplatform | 1.7+ | UI declarativa |
| Supabase-kt | 3.x | Backend (DB, Auth, Storage) |
| Ktor | 3.x | HTTP client |
| kotlinx.serialization | 1.7+ | JSON |
| kotlinx.datetime | 0.6+ | Fechas multiplataforma |
| Multiplatform Settings | 1.2+ | Key-value storage local |
| Coil 3 | 3.x | Carga de imágenes |
| Google Maps SDK | 18.2 | Mapas Android |
| ML Kit Image Labeling | 17.0 | IA reconocimiento Android |
| CameraX | 1.3 | Cámara Android |
| OkHttp | 4.12 | HTTP Android |
| ZXing | 3.5 | Generación de QR |

---

## 15. Cómo Compilar y Ejecutar

### Android
```bash
./gradlew :composeApp:assembleDebug
# O directamente desde Android Studio → Run
```

### iOS
```bash
# Abrir iosApp/iosApp.xcodeproj en Xcode
# Seleccionar simulador o dispositivo → Run
```

### Requisitos
- Android Studio con plugin KMP
- Xcode 15+ (para iOS)
- JDK 11+
- Archivo `local.properties` con las claves de Supabase y Google Maps

---

*Documento generado el 19/05/2026 para la presentación del TFG de EcoLens.*
