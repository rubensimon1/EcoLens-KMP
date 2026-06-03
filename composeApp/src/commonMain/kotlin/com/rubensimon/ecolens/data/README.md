# Capa de Datos (Data Layer)

Esta carpeta contiene la implementación centralizada de la gestión de datos en EcoLens, utilizando **Supabase** como backend serverless.

## 📂 Estructura

- **`network/`**: Configuraciones de clientes HTTP. Destaca `SupabaseClientProvider`, el cliente principal de KMP para autenticación, realtime y base de datos (Postgrest).
- **`repository/`**: Patrón Repositorio. Aquí se ubican clases como `UserRepository` o `HistoryManager` que actúan como puente entre la base de datos remota (Supabase) y el estado local de la app.
- **`models/`**: Data classes y esquemas (DTOs) marcados con `@Serializable`. Representan las tablas de Supabase y las respuestas de APIs externas.

## 🔒 Buenas Prácticas
- Las consultas a Supabase deben envolverse en bloques `try/catch` y ejecutarse siempre bajo el dispatcher `Dispatchers.IO` para no bloquear el hilo principal (UI).
- Los modelos deben estar sincronizados de forma estricta con el esquema de columnas y políticas RLS de Supabase.
