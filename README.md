# EcoLens 🌍♻️ — Tu Compañero de Reciclaje Inteligente

**EcoLens** es una solución móvil de vanguardia diseñada para transformar el reciclaje en una experiencia gratificante y tecnológicamente avanzada. Mediante el uso de **Inteligencia Artificial** y un sistema de **Gamificación** de alto impacto, EcoLens motiva a los ciudadanos a reciclar correctamente, recompensando sus acciones y rastreando su impacto ambiental positivo.

Desarrollada bajo la filosofía de **Kotlin Multiplatform (KMP)**, EcoLens ofrece una experiencia premium nativa tanto en Android como en iOS, compartiendo más del 90% de su lógica de negocio y UI mediante **Compose Multiplatform**.

---

## ✨ Características de Última Generación

### 📸 Escaneo con IA y Reconocimiento de Materiales
*   **Identificación Instantánea**: Reconoce plástico, latas, papel, vidrio y más mediante modelos de Computer Vision optimizados para móvil.
*   **Guía de Contenedores**: Indica el color exacto del contenedor (Amarillo, Azul, Verde, Marrón, RAEE) para evitar errores de separación.

### 🏆 Gamificación y Progresión (XP & Niveles)
*   **Sistema de Niveles Blindado**: Progresión basada en puntos de experiencia (XP) acumulados de por vida, sincronizada de forma segura en la nube.
*   **Misiones Diarias**: Desafíos diarios que incentivan el hábito del reciclaje con bonos especiales.
*   **Notificaciones en Tiempo Real**: Feedback instantáneo al subir de nivel o completar hitos importantes.

### 🎁 Ecosistema de Recompensas
*   **Tienda de Cupones**: Canjea tus puntos por descuentos reales en tiendas locales y beneficios ecológicos.
*   **Validación Segura**: Sistema de cupones con códigos QR de alto contraste (optimizados para iOS) y validación en tiempo real mediante UUIDs únicos.
*   **Persistencia Total**: Tus cupones te acompañan siempre, vinculados a tu cuenta y protegidos contra pérdida de datos.

### ♻️ Eco-Retorno (SDDR)
*   **Sistema de Depósito y Retorno**: Simulación y gestión del futuro sistema de retorno de envases (SDDR), permitiendo acumular saldo económico por cada botella o lata devuelta.
*   **Historial Sincronizado**: Registro detallado de todas tus devoluciones con sincronización inteligente en background.

### 📊 Eco-Dex y Estadísticas
*   **Colección de Objetos**: Una galería visual (tipo Pokédex) de todos los materiales que has descubierto y reciclado.
*   **Impacto Ambiental**: Cálculo en tiempo real del CO2 ahorrado y envases recuperados.

---

## 🛡️ Seguridad y Robustez Técnica

*   **Seguridad Bancaria (Supabase RLS)**: Implementación estricta de *Row Level Security* (RLS), asegurando que cada usuario solo pueda acceder y modificar sus propios datos. Nadie puede ver ni alterar tu progreso desde el exterior.
*   **Sincronización Inteligente (Debouncing)**: Algoritmos de sincronización que agrupan las acciones del usuario para reducir el tráfico de red y asegurar que los datos lleguen siempre de forma consistente a la nube.
*   **Diseño Premium (Glassmorphism)**: Interfaz viva, dinámica y moderna basada en efectos de cristal, transparencias controladas y micro-animaciones fluidas que proporcionan una sensación de alta gama.
*   **Offline-First**: La app funciona perfectamente sin conexión, guardando tus progresos localmente y sincronizándolos automáticamente en cuanto recuperas internet.

---

## 🛠️ Stack Tecnológico

*   **Lenguaje**: Kotlin 1.9+
*   **UI Framework**: Compose Multiplatform (Material 3)
*   **Arquitectura**: MVVM con Repositorios y Managers centralizados.
*   **Backend**: Supabase (Auth, Postgrest, Realtime)
*   **Local Storage**: Multiplatform Settings
*   **Networking**: Ktor Client con serialización JSON.

---

## 🚀 Configuración Rápida

1.  **Clonado**: `git clone https://github.com/rubensimon1/EcoLens-KMP.git`
2.  **Secretos**: Configura tu `local.properties` con las credenciales de Supabase y Maps.
3.  **Base de Datos**: Importante configurar las tablas `usuarios`, `cupones_canjeados` e `historial_sddr` con las columnas y políticas de RLS indicadas en la documentación técnica del backend.

---

**Desarrollado por Rubens Simon** - *Proyecto de Fin de Grado (TFG)*  
*Comprometidos con un futuro más verde a través de la tecnología.*