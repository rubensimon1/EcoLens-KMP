package com.rubensimon.ecolens.data.models.maps

import kotlinx.serialization.Serializable

@Serializable
data class EcoLatLng(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class RecyclingPoint(
    val kind: String,
    val name: String,
    val position: EcoLatLng,
    val snippet: String?
)
