package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.rubensimon.ecolens.utils.MadridPointsFetcherIOS
import com.rubensimon.ecolens.data.models.maps.RecyclingPoint
import platform.CoreLocation.*
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapView(
    modifier: Modifier, 
    filter: String,
    recenterTrigger: Int,
    onPointSelected: (RecyclingPoint) -> Unit
) {
    var allPoints by remember { mutableStateOf<List<RecyclingPoint>>(emptyList()) }
    var userLocation by remember { mutableStateOf<CLLocation?>(null) }
    var mapViewRef by remember { mutableStateOf<MKMapView?>(null) }

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
                        val pointLoc = CLLocation(
                            latitude = point.position.latitude,
                            longitude = point.position.longitude
                        )
                        loc.distanceFromLocation(pointLoc) < 3000.0 // 3km
                    }.sortedBy { point ->
                        val pointLoc = CLLocation(
                            latitude = point.position.latitude,
                            longitude = point.position.longitude
                        )
                        loc.distanceFromLocation(pointLoc)
                    }
                } else {
                    allPoints
                }
            }
            else -> allPoints.filter { it.kind == filter }
        }
    }

    // CLLocationManager para solicitar permiso GPS y capturar la ubicación del usuario
    // Esto es necesario para que MKMapView reciba actualizaciones de ubicación
    val locationManager = remember { CLLocationManager() }

    val locationDelegate = remember {
        object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val lastLocation = didUpdateLocations.lastOrNull() as? CLLocation
                if (lastLocation != null) {
                    userLocation = lastLocation
                }
            }

            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                val status = manager.authorizationStatus
                if (status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                    status == kCLAuthorizationStatusAuthorizedAlways) {
                    manager.startUpdatingLocation()
                }
            }
        }
    }

    // Delegado del mapa para clics en pins
    val mapDelegate = remember {
        object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(mapView: MKMapView, didUpdateUserLocation: MKUserLocation) {
                // También actualizamos desde el mapa como respaldo
                val loc = didUpdateUserLocation.location
                if (loc != null) {
                    userLocation = loc
                }
            }

            override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
                val annotation = didSelectAnnotationView.annotation as? MKPointAnnotation ?: return
                val title = annotation.title ?: ""
                val point = allPoints.find { it.name == title }
                if (point != null) {
                    onPointSelected(point)
                }
                // Deseleccionar para que se pueda volver a clicar
                mapView.deselectAnnotation(annotation, animated = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        // Configurar y solicitar permiso de localización ANTES de que el mapa lo necesite
        locationManager.delegate = locationDelegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.requestWhenInUseAuthorization()

        val authStatus = locationManager.authorizationStatus
        if (authStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
            authStatus == kCLAuthorizationStatusAuthorizedAlways) {
            locationManager.startUpdatingLocation()
        }

        val basePoints = MadridPointsFetcherIOS.loadAllRecyclingPoints()
        
        // Red extensa de Mercadona (SDDR) para iOS
        val sddrPoints = listOf(
            RecyclingPoint("SDDR", "Mercadona - Calle de Ayala", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4285, -3.6785), "Punto de retorno SDDR (Máquina automática)."),
            RecyclingPoint("SDDR", "Mercadona - Calle de Serrano", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4320, -3.6880), "Punto de retorno SDDR. Disponible en horario comercial."),
            RecyclingPoint("SDDR", "Mercadona - Pº de la Castellana", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4450, -3.6910), "Punto de retorno SDDR. Acceso por planta -1."),
            RecyclingPoint("SDDR", "Mercadona - Moncloa", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4345, -3.7185), "Punto de retorno SDDR. Máquina de alta capacidad."),
            RecyclingPoint("SDDR", "Mercadona - Chamberí", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4360, -3.7030), "Punto de retorno SDDR. Calle de Santa Engracia."),
            RecyclingPoint("SDDR", "Mercadona - Bravo Murillo", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4520, -3.7035), "Punto de retorno SDDR (Tetuán)."),
            RecyclingPoint("SDDR", "Mercadona - Atocha/Delicias", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4020, -3.6940), "Punto de retorno SDDR (Arganzuela)."),
            RecyclingPoint("SDDR", "Mercadona - Retiro/Ibiza", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4185, -3.6750), "Punto de retorno SDDR (Zona Retiro)."),
            RecyclingPoint("SDDR", "Mercadona - Goya", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4250, -3.6720), "Punto de retorno SDDR (Salamanca)."),
            RecyclingPoint("SDDR", "Mercadona - Puente de Vallecas", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.3950, -3.6680), "Punto de retorno SDDR (Vallecas)."),
            RecyclingPoint("SDDR", "Mercadona - Plaza de España", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4235, -3.7120), "Punto de retorno SDDR (Centro)."),
            RecyclingPoint("SDDR", "Mercadona - Aluche", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.3880, -3.7620), "Punto de retorno SDDR (Latina)."),
            RecyclingPoint("SDDR", "Mercadona - Arturo Soria", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4580, -3.6550), "Punto de retorno SDDR (Ciudad Lineal)."),
            RecyclingPoint("SDDR", "Mercadona - Las Tablas", com.rubensimon.ecolens.data.models.maps.EcoLatLng(40.4950, -3.6700), "Punto de retorno SDDR (Norte).")
        )
        
        allPoints = basePoints + sddrPoints
    }
    
    LaunchedEffect(recenterTrigger, filter) {
        val mapView = mapViewRef ?: return@LaunchedEffect
        // Si estamos en modo SDDR, centramos en la zona de Mercadonas
        // En PROXIMIDAD centramos con zoom más cercano; la ubicación exacta la maneja el delegate
        val centerCoords = if (filter == "SDDR") {
            CLLocationCoordinate2DMake(40.4285, -3.6880)
        } else {
            CLLocationCoordinate2DMake(40.4168, -3.7038)
        }
        val zoomLevel = if (filter == "SDDR") 0.05 else if (filter == "PROXIMIDAD") 0.04 else 0.1
        val span = MKCoordinateSpanMake(zoomLevel, zoomLevel)
        mapView.setRegion(MKCoordinateRegionMake(centerCoords, span), animated = true)
    }

    Box(modifier = modifier) {
        val pointsSnapshot = recyclingPoints
        UIKitView(
            factory = {
                val mapView = MKMapView()
                val madridCenter = CLLocationCoordinate2DMake(40.4168, -3.7038)
                val span = MKCoordinateSpanMake(0.1, 0.1)
                mapView.setRegion(MKCoordinateRegionMake(madridCenter, span), animated = false)
                // Habilitar explícitamente toda la interacción del usuario con el mapa
                mapView.scrollEnabled = true
                mapView.zoomEnabled = true
                mapView.rotateEnabled = true
                mapView.pitchEnabled = true
                mapView.showsUserLocation = true
                mapView.delegate = mapDelegate
                mapViewRef = mapView
                mapView
            },
            update = { mapView ->
                if (pointsSnapshot.isNotEmpty() && (mapView.annotations.size <= 1 || filter != "TODOS")) {
                    mapView.removeAnnotations(mapView.annotations)
                    val annotations = pointsSnapshot.map { point ->
                        val annotation = MKPointAnnotation()
                        annotation.setCoordinate(CLLocationCoordinate2DMake(point.position.latitude, point.position.longitude))
                        annotation.setTitle(point.name)
                        annotation.setSubtitle(point.snippet)
                        annotation
                    }
                    mapView.addAnnotations(annotations)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
