# EcoLens 🌍♻️

**EcoLens** es una aplicación móvil multiplataforma diseñada para revolucionar la forma en que interactuamos con el reciclaje. Utilizando Inteligencia Artificial y tecnologías de vanguardia, EcoLens permite a los usuarios identificar materiales reciclables, seguir su impacto ambiental y ganar recompensas por sus acciones ecológicas.

Desarrollada con **Kotlin Multiplatform (KMP)**, la aplicación comparte más del 85% del código entre Android e iOS, ofreciendo una experiencia nativa y fluida en ambas plataformas.

## ✨ Características Principales

- **📸 Escaneo con IA**: Reconocimiento de objetos en tiempo real (Botellas, Latas, Papel, Vidrio) utilizando Google ML Kit (Android) y modelos remotos/CoreML (iOS).
- **🗺️ Mapa de Reciclaje**: Localización inteligente de puntos de reciclaje cercanos con filtros por tipo de residuo.
- **🏆 Sistema de Gamificación**: Ranking global, misiones diarias y logros desbloqueables.
- **🎁 Recompensas**: Canjeo de puntos por cupones y beneficios ecológicos reales.
- **📊 Eco-Dex**: Colección visual de todos los objetos que has reciclado, fomentando la conciencia ambiental.

## 🛠️ Stack Tecnológico

- **Core**: Kotlin Multiplatform (KMP)
- **UI**: Compose Multiplatform (Material 3)
- **Backend**: Supabase (Postgrest, Auth, Storage, Realtime)
- **Networking**: Ktor Client (Content Negotiation, Serialization)
- **IA/ML**: Google ML Kit (Android) & Apple Vision/CoreML (iOS)
- **Inyección de Dependencias/Config**: Gradle con tareas automatizadas de secretos.
- **Persistencia**: Multiplatform Settings (Configuraciones de usuario).

## 🏗️ Estructura del Proyecto

```text
.
├── composeApp/            # Código compartido (Common), Android e iOS Main
│   ├── src/commonMain/    # Lógica de negocio, UI compartida y Repositorios
│   ├── src/androidMain/   # Implementaciones nativas de Android (Cámara, Mapas)
│   └── src/iosMain/       # Implementaciones nativas de iOS (Frameworks Apple)
├── iosApp/                # Proyecto nativo Xcode (entry point para iOS)
├── gradle/                # Configuración de dependencias (Version Catalogs)
└── local.properties       # Configuración de secretos (Privado)
```

## 🚀 Configuración e Instalación

Para ejecutar este proyecto localmente, sigue estos pasos:

1.  **Clona el repositorio**:
    ```bash
    git clone https://github.com/rubensimon1/EcoLens-KMP.git
    ```

2.  **Configura los secretos**:
    Crea un archivo `local.properties` en la raíz del proyecto basado en el ejemplo proporcionado:
    ```properties
    SUPABASE_URL=tu_url_de_supabase
    SUPABASE_KEY=tu_anon_key_de_supabase
    MAPS_API_KEY=tu_google_maps_api_key
    ML_BACKEND_URL=tu_url_del_servidor_ia
    ```

3.  **Compila y ejecuta**:
    - **Android**: Abre en Android Studio y ejecuta el módulo `composeApp`.
    - **iOS**: Abre el archivo `iosApp/iosApp.xcodeproj` en Xcode o ejecuta directamente desde Android Studio si tienes configurado el plugin de KMP.

## 📱 Capturas de Pantalla

*(Próximamente: Añade aquí tus capturas para un impacto visual total)*

---

**Desarrollado por Rubens Simon** - *Proyecto de Fin de Grado (TFG)*