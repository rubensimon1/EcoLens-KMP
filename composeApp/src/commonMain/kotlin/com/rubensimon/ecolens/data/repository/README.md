# Repositorios (Repository) - Capa Común

Este paquete implementa el Patrón Repositorio para la gestión de datos en EcoLens.

## Propósito
Abstraer el origen de los datos (ya sea de red, base de datos local o caché) del resto de la aplicación (ViewModel / Interfaz de usuario).

## Funcionamiento
Los repositorios deciden si obtener la información actualizada mediante llamadas de red o devolver los datos almacenados localmente, garantizando una única fuente de verdad (Single Source of Truth). Contiene los repositorios de usuarios, elementos a reciclar, etc.
