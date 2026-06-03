# Módulo de Machine Learning (ML) - Android

Este paquete contiene la implementación específica de Android para el procesamiento de imágenes y la detección de objetos mediante Inteligencia Artificial en el proyecto EcoLens.

## Componentes Principales

*   **EcoLensMlBackend.kt**: Interfaz y lógica de backend para la integración de modelos de Machine Learning.
*   **EcoLensCustomLabeler.kt**: Implementación del etiquetador personalizado para el análisis de imágenes y reconocimiento de objetos de reciclaje utilizando modelos entrenados.

## Funcionamiento

Este módulo se encarga de recibir los fotogramas de la cámara (capturados en la capa de UI específica de Android o común) y pasarlos por el modelo de IA para identificar si los objetos en pantalla son reciclables y a qué contenedor pertenecen.

## Notas

Al ser un módulo bajo `androidMain`, utiliza librerías nativas de Android y Google ML Kit / TensorFlow Lite que no están disponibles en la capa común multiplataforma.
