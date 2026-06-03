# Navegación (Navigation) - Capa Común

Este paquete contiene la lógica de enrutamiento y navegación de la aplicación EcoLens.

## Responsabilidades
*   **Gestión de Rutas**: Definir las distintas pantallas de la aplicación y sus rutas.
*   **Navegadores**: Componentes que controlan el estado de navegación (NavHost) en Jetpack Compose Multiplatform.
*   **Transiciones**: Lógica para pasar de una pantalla a otra (animaciones de transición si aplican).

## Funcionamiento
Se utiliza el sistema de navegación de Compose para moverse entre los distintos flujos (autenticación, principal, escáner, etc.) de forma compartida entre Android e iOS.
