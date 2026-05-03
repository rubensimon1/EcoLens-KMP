package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.rubensimon.ecolens.utils.MadridPointsFetcherIOS
import com.rubensimon.ecolens.data.models.maps.RecyclingPoint
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation
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
    var mapViewRef by remember { mutableStateOf<MKMapView?>(null) }
    
    val recyclingPoints = remember(allPoints, filter) {
        if (filter == "TODOS") allPoints
        else allPoints.filter { it.kind == filter }
    }

    LaunchedEffect(Unit) {
        allPoints = MadridPointsFetcherIOS.loadAllRecyclingPoints()
    }
    
    LaunchedEffect(recenterTrigger) {
        val mapView = mapViewRef ?: return@LaunchedEffect
        val madridCenter = CLLocationCoordinate2DMake(40.4168, -3.7038)
        val span = MKCoordinateSpanMake(0.1, 0.1)
        mapView.setRegion(MKCoordinateRegionMake(madridCenter, span), animated = true)
    }

    Box(modifier = modifier) {
        val pointsSnapshot = recyclingPoints
        UIKitView(
            factory = {
                val mapView = MKMapView()
                val madridCenter = CLLocationCoordinate2DMake(40.4168, -3.7038)
                val span = MKCoordinateSpanMake(0.1, 0.1)
                mapView.setRegion(MKCoordinateRegionMake(madridCenter, span), animated = true)
                mapView.showsUserLocation = true
                mapViewRef = mapView
                mapView
            },
            update = { mapView ->
                if (pointsSnapshot.isNotEmpty() && mapView.annotations.size <= 1) {
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
