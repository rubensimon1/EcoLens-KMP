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
    var hasCenteredOnce by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var mapInstance by remember { mutableStateOf<com.google.android.gms.maps.GoogleMap?>(null) }

    val recyclingPoints = remember(allPoints, filter) {
        if (filter == "TODOS") allPoints
        else allPoints.filter { it.kind == filter }
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
        if (allPoints.isEmpty()) {
            allPoints = listOf(
                RecyclingPoint("FIJO", "EcoLens Central Madrid", EcoLatLng(40.4168, -3.7038), "Calle Mayor, 1. Punto de reciclaje premium."),
                RecyclingPoint("MOVIL", "Punto Móvil Sol", EcoLatLng(40.4172, -3.7033), "Puerta del Sol. Disponible de 10:00 a 14:00."),
                RecyclingPoint("PROXIMIDAD", "Contenedor Vidrio", EcoLatLng(40.4180, -3.7045), "Plaza de San Ginés.")
            )
        }
    }

    // Lógica de centrado reactiva: solo centra una vez al inicio o cuando se pulsa el botón
    LaunchedEffect(hasLocationPermission, recenterTrigger) {
        val googleMap = mapInstance ?: return@LaunchedEffect
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings.apply {
                    isMyLocationButtonEnabled = true
                    isScrollGesturesEnabled = true
                    isZoomGesturesEnabled = true
                    isTiltGesturesEnabled = true
                    isRotateGesturesEnabled = true
                }

                // Solo centramos si no lo hemos hecho nunca O si el trigger es > 0 (botón pulsado)
                if (!hasCenteredOnce || recenterTrigger > 0) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 17f))
                            hasCenteredOnce = true
                        }
                    }

                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 17f))
                                hasCenteredOnce = true
                            }
                        }
                }
            } catch (e: SecurityException) { }
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
                        isCompassEnabled = true
                    }
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(40.4168, -3.7038), 10f))
                }
            }
        },
        update = { mv ->
            val pointsSnapshot = recyclingPoints
            mv.getMapAsync { googleMap ->
                if (pointsSnapshot.isNotEmpty()) {
                    googleMap.clear()
                    pointsSnapshot.forEach { point ->
                        val hue = when (point.kind) {
                            "FIJO" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                            "MOVIL" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
                            "PROXIMIDAD" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
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
                    googleMap.setOnMarkerClickListener { m ->
                        (m.tag as? RecyclingPoint)?.let { onPointSelected(it) }
                        false
                    }
                }
            }
        }
    )
}
