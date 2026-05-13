package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.location.Location
import com.rubensimon.ecolens.ui.components.EcoColors
import com.rubensimon.ecolens.data.models.maps.RecyclingPoint
import com.rubensimon.ecolens.data.models.maps.EcoLatLng
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

@Composable
actual fun PlatformMapView(
    modifier: Modifier, 
    filter: String,
    recenterTrigger: Int,
    onPointSelected: (RecyclingPoint) -> Unit
) {
    var hasLocationPermission by remember { mutableStateOf(false) }
    var allPoints by remember { mutableStateOf<List<RecyclingPoint>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var mapInstance by remember { mutableStateOf<com.google.android.gms.maps.GoogleMap?>(null) }

    val recyclingPoints = remember(allPoints, filter, userLocation) {
        when (filter) {
            "TODOS" -> allPoints
            "SDDR" -> allPoints.filter { it.kind == "SDDR" }
            "FIJO" -> allPoints.filter { it.kind == "FIJO" }
            "MOVIL" -> allPoints.filter { it.kind == "MOVIL" || it.kind == "MOVIL_24H" }
            "PROXIMIDAD" -> {
                val loc = userLocation
                if (loc != null) {
                    allPoints.filter { point ->
                        val distance = FloatArray(1)
                        Location.distanceBetween(
                            loc.latitude, loc.longitude,
                            point.position.latitude, point.position.longitude,
                            distance
                        )
                        distance[0] < 3000 // Radio de 3km
                    }.sortedBy { point ->
                        val distance = FloatArray(1)
                        Location.distanceBetween(
                            loc.latitude, loc.longitude,
                            point.position.latitude, point.position.longitude,
                            distance
                        )
                        distance[0]
                    }
                } else {
                    allPoints // Fallback: mostrar todos si no hay GPS aún
                }
            }
            else -> allPoints.filter { it.kind == filter }
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { granted: Boolean -> 
            hasLocationPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        allPoints = com.rubensimon.ecolens.utils.MadridPointsFetcher.loadAllRecyclingPoints(context)
        
        // Red extensa de Mercadona (SDDR) para que parezca "toda la ciudad"
        val sddrPoints = listOf(
            RecyclingPoint("SDDR", "Mercadona - Calle de Ayala", EcoLatLng(40.4285, -3.6785), "Punto de retorno SDDR (Máquina automática)."),
            RecyclingPoint("SDDR", "Mercadona - Calle de Serrano", EcoLatLng(40.4320, -3.6880), "Punto de retorno SDDR. Disponible en horario comercial."),
            RecyclingPoint("SDDR", "Mercadona - Pº de la Castellana", EcoLatLng(40.4450, -3.6910), "Punto de retorno SDDR. Acceso por planta -1."),
            RecyclingPoint("SDDR", "Mercadona - Moncloa", EcoLatLng(40.4345, -3.7185), "Punto de retorno SDDR. Máquina de alta capacidad."),
            RecyclingPoint("SDDR", "Mercadona - Chamberí", EcoLatLng(40.4360, -3.7030), "Punto de retorno SDDR. Calle de Santa Engracia."),
            RecyclingPoint("SDDR", "Mercadona - Bravo Murillo", EcoLatLng(40.4520, -3.7035), "Punto de retorno SDDR (Tetuán)."),
            RecyclingPoint("SDDR", "Mercadona - Atocha/Delicias", EcoLatLng(40.4020, -3.6940), "Punto de retorno SDDR (Arganzuela)."),
            RecyclingPoint("SDDR", "Mercadona - Retiro/Ibiza", EcoLatLng(40.4185, -3.6750), "Punto de retorno SDDR (Zona Retiro)."),
            RecyclingPoint("SDDR", "Mercadona - Goya", EcoLatLng(40.4250, -3.6720), "Punto de retorno SDDR (Salamanca)."),
            RecyclingPoint("SDDR", "Mercadona - Puente de Vallecas", EcoLatLng(40.3950, -3.6680), "Punto de retorno SDDR (Vallecas)."),
            RecyclingPoint("SDDR", "Mercadona - Plaza de España", EcoLatLng(40.4235, -3.7120), "Punto de retorno SDDR (Centro)."),
            RecyclingPoint("SDDR", "Mercadona - Aluche", EcoLatLng(40.3880, -3.7620), "Punto de retorno SDDR (Latina)."),
            RecyclingPoint("SDDR", "Mercadona - Arturo Soria", EcoLatLng(40.4580, -3.6550), "Punto de retorno SDDR (Ciudad Lineal)."),
            RecyclingPoint("SDDR", "Mercadona - Las Tablas", EcoLatLng(40.4950, -3.6700), "Punto de retorno SDDR (Norte).")
        )
        
        if (allPoints.isEmpty()) {
            allPoints = listOf(
                RecyclingPoint("FIJO", "EcoLens Central Madrid", EcoLatLng(40.4168, -3.7038), "Calle Mayor, 1. Punto de reciclaje premium."),
                RecyclingPoint("MOVIL", "Punto Móvil Sol", EcoLatLng(40.4172, -3.7033), "Puerta del Sol. Disponible de 10:00 a 14:00."),
                RecyclingPoint("PROXIMIDAD", "Contenedor Vidrio", EcoLatLng(40.4180, -3.7045), "Plaza de San Ginés.")
            ) + sddrPoints
        } else {
            allPoints = allPoints + sddrPoints
        }
    }

    // Función auxiliar para centrar el mapa en la ubicación GPS actual
    fun centerOnGps(googleMap: com.google.android.gms.maps.GoogleMap) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        try {
            googleMap.isMyLocationEnabled = true
            // Primero intentamos con la última ubicación conocida (rápido)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = location
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 17f))
                }
            }
            // Luego pedimos una ubicación fresca de alta precisión (GPS real)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLocation = location
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 17f))
                    }
                }
        } catch (e: SecurityException) {
            println("[MapsScreen] SecurityException al obtener ubicación: ${e.message}")
        }
    }

    // Recentrar cuando se pulsa el botón del FAB
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            mapInstance?.let { centerOnGps(it) }
        }
    }

    // Recentrar cuando se concede el permiso de ubicación
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            mapInstance?.let { centerOnGps(it) }
        }
    }

    val mapView = remember { MapView(context) }
    
    // ── Gestión del Ciclo de Vida del Mapa ──
    val lifecycleObserver = remember(mapView) {
        androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_CREATE -> mapView.onCreate(android.os.Bundle())
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
    }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { 
            mapView.apply {
                getMapAsync { googleMap ->
                    mapInstance = googleMap
                    googleMap.uiSettings.apply {
                        isMyLocationButtonEnabled = true
                        isScrollGesturesEnabled = true
                        isZoomGesturesEnabled = true
                        isTiltGesturesEnabled = true
                        isRotateGesturesEnabled = true
                        isCompassEnabled = true
                    }
                    
                    // Centrar inmediatamente en la ubicación GPS del usuario
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        centerOnGps(googleMap)
                    } else {
                        // Fallback: si no hay permiso, poner zoom amplio en España
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(40.4168, -3.7038), 6f))
                    }
                }
            }
        },
        update = { mv ->
            val pointsSnapshot = recyclingPoints
            mv.getMapAsync { googleMap ->
                // Limpiar siempre para asegurar frescura
                googleMap.clear()
                
                if (pointsSnapshot.isNotEmpty()) {
                    pointsSnapshot.forEach { point ->
                        val hue = when (point.kind) {
                            "SDDR" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                            "FIJO" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                            "MOVIL" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
                            "PROXIMIDAD" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_CYAN
                            "MOVIL_24H" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_VIOLET
                            else -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                        }
                        try {
                            val marker = googleMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(point.position.latitude, point.position.longitude))
                                    .title(point.name)
                                    .snippet(point.snippet)
                                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hue))
                            )
                            marker?.tag = point
                        } catch (e: Exception) {
                            println("[MapsScreen] Error adding marker: ${e.message}")
                        }
                    }
                    
                    // MODO DEMO: Si estamos en SDDR, centramos la cámara en los puntos de Madrid
                    if (filter == "SDDR") {
                        val madridCenter = LatLng(40.4285, -3.6880)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(madridCenter, 13f))
                    }

                    googleMap.setOnMarkerClickListener { m ->
                        (m.tag as? RecyclingPoint)?.let { onPointSelected(it) }
                        false
                    }
                }
            }
        }
    )
}
