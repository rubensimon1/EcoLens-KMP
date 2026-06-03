# Capa de Interfaz de Usuario (UI Layer)

Esta carpeta contiene todos los componentes visuales y la lógica de presentación utilizando **Compose Multiplatform**.

## 📂 Estructura

- **`components/`**: Widgets y elementos visuales reutilizables a lo largo de toda la aplicación (ej. botones de cristal, tarjetas de objetos, barras superiores personalizadas). Estos componentes aseguran consistencia visual y siguen el patrón Glassmorphism.
- **`navigation/`**: Gestión de rutas y grafos de navegación (`NavHost`). Contiene la lógica para el paso entre pantallas y el manejo de argumentos.
- **`screens/`**: Las pantallas principales de la aplicación.
  - **`auth/`**: Pantallas relacionadas con el acceso y registro de usuarios (Login, Sign Up).
  - **`main/`**: Vistas principales y menú inferior (Home, EcoDex, Perfil).
  - **`features/`**: Funcionalidades específicas y complejas (Escáner de IA, SDDR, Comunidad, Mapa, etc.).

## 🎨 Principios de Diseño
- **Material 3 y Custom UI**: Utilizamos la base de Material 3 ampliada con modificadores personalizados (Glassmorphism, desenfoques, gradientes vibrantes).
- **Separación de Responsabilidades**: Las vistas no deben realizar peticiones de red directamente. Deben delegar esa responsabilidad en funciones o en la capa de datos.
