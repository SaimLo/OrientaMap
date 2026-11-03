package com.showroomprive.orientamap.domain.model

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Double? = null,
    val timestampEpochMillis: Long
)
