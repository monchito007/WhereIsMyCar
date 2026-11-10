# Project Plan

Aplicación Android nativa que permite guardar la ubicación exacta donde has aparcado tu vehículo y visualizarla en un mapa OpenStreetMap usando MapLibre SDK. Muestra tu posición actual y la del coche, calcula la distancia y el tiempo estimado para llegar caminando. Guarda la información localmente mediante Room para que persista al cerrar la aplicación. Incluye opciones para eliminar la ubicación guardada y abrir navegación hacia el vehículo con aplicaciones compatibles como Google Maps, Waze u OsmAnd. Desarrollada en Kotlin, Jetpack Compose y Material 3, sin depender de APIs de pago ni claves de Google Maps. Interfaz en Español.

## Project Brief

# Project Brief: WhereIsMyCar

## Features
- **Guardar y gestionar ubicación de aparcamiento**: Permite guardar con un toque la posición exacta donde se ha aparcado el vehículo y eliminarla cuando no se necesite.
- **Mapa dinámico e interactivo**: Muestra la ubicación actual del usuario y del coche sobre un mapa OpenStreetMap usando MapLibre SDK (sin dependencias de APIs de pago o claves de Google Maps).
- **Cálculo de distancia y tiempo estimado**: Muestra la distancia restante y el tiempo estimado para llegar caminando desde la posición actual al vehículo.
- **Navegación externa**: Permite abrir la ruta hacia el coche directamente en aplicaciones externas compatibles como Google Maps, Waze u OsmAnd.

## High-Level Tech Stack
- **Lenguaje**: Kotlin
- **UI & Diseño**: Jetpack Compose con Material 3
- **Navegación**: Jetpack Navigation 3 (State-driven)
- **Estrategia Adaptativa**: Compose Material Adaptive library
- **Mapas**: MapLibre SDK (OpenStreetMap)
- **Persistencia Local**: Room Database
- **Arquitectura y Concurrencia**: ViewModel, Kotlin Coroutines y Flow
- **Servicios de Localización**: Play Services Location / Fused Location Provider Provider

## Implementation Steps

### Task_1_DependenciesAndDataLayer: Configure project dependencies (MapLibre SDK, Play Services Location, Room) and build the data layer including Room entity/DAO, Repository, and ViewModel for managing parking location and permissions.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Room database and repository configured for storing parking location coordinates and timestamp
  - FusedLocationProviderClient integration for retrieving current location
  - build pass
- **StartTime:** 2026-10-28 19:13:11 CET

### Task_2_MapAndLocationUi: Implement Jetpack Compose Material 3 UI in Spanish featuring MapLibre map view, user location marker, parked car marker, and controls to save/clear parking location.
- **Status:** PENDING
- **Acceptance Criteria:**
  - MapLibre map component integrated with Compose displaying OSM tiles and location markers
  - User can save current location as parking spot or clear saved parking spot
  - UI is fully in Spanish with Material 3 design elements
  - build pass

### Task_3_DistanceCalculationAndExternalNavigation: Implement real-time calculation of distance and estimated walking time from user location to parked car, along with external navigation launchers (Google Maps, Waze, OsmAnd).
- **Status:** PENDING
- **Acceptance Criteria:**
  - Walking distance and estimated time calculated and displayed dynamically
  - External navigation Intent opens external apps with car coordinates
  - build pass

### Task_4_RunAndVerify: Build and run the application, execute tests, and instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements in Spanish, and report critical UI issues.
- **Status:** PENDING
- **Acceptance Criteria:**
  - build pass
  - make sure all existing tests pass
  - app does not crash
  - critic_agent verifies application stability and compliance with requirements

