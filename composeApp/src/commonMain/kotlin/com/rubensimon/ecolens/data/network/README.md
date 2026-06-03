# Red e Integraciones (Network) - Capa Común

Este paquete gestiona todas las llamadas de red (HTTP) y comunicación con APIs externas del proyecto EcoLens.

## Propósito
Centralizar las peticiones al backend y el manejo de respuestas y errores HTTP utilizando bibliotecas como Ktor.

## Contenido Típico
*   **Clientes HTTP**: Configuración del cliente Ktor (o similar).
*   **Servicios/Endpoints**: Interfaces o clases que definen los endpoints (login, registro, recuperación de datos, etc.).
*   **Serialización**: Mapeo de respuestas JSON a modelos de datos de Kotlin.
